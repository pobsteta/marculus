package fr.marculus.core.voice

/**
 * Parseur d'énoncé : transforme la sortie texte de Vosk en événement métier.
 *
 * Formes acceptées :
 *   "hetre quarante cinq"            -> Tige(HET, 45, qualité nulle)
 *   "hetre quarante cinq chablis"    -> Tige(HET, 45, Chablis)
 *   "quarante cinq"                  -> Tige(essence courante, 45)   [mode rafale]
 *   "chablis"                        -> Qualite(Chablis)             [annote la dernière tige]
 *   "hauteur vingt sept six alpha"   -> Hauteur("27-6A")             [annote la dernière tige]
 *   "decoupe six alpha bravo"        -> Decoupe("6AB")                [sur la hauteur existante]
 *   "hetre quarante cinq hauteur vingt sept"
 *                                    -> Tige(HET, 45, null, "27")    [tout d'un seul tenant]
 *   "annule"                         -> Commande(ANNULE)
 *   "repete"                         -> Commande(REPETE)
 *
 * Le "mode rafale" reprend l'ergonomie réelle du martelage : le marteleur annonce
 * l'essence une fois puis enchaîne les classes ; l'essence courante est portée
 * par l'appelant (dernier Tige émis) et passée en [essenceCourante].
 *
 * Tout jeton "[unk]" invalide l'énoncé entier (Rejet) : en environnement bruyant,
 * mieux vaut redemander que d'enregistrer une tige douteuse — la confirmation TTS
 * existante de Marculus ferme la boucle.
 */
sealed interface VoiceEvent {
    /**
     * Tige à créer. [hauteurTexte] n'est renseigné que si la hauteur a été dictée dans le même
     * énoncé (« hetre quarante cinq hauteur vingt sept ») : une seule insertion porte alors les
     * trois attributs, sans annotation après coup.
     */
    data class Tige(
        val codeOnf: String,
        val classe: Int,
        val qualite: String?,
        val hauteurTexte: String? = null,
    ) : VoiceEvent

    /**
     * Hauteur dictée pour la dernière tige. [texte] est déjà au format saisi à la main et lu par
     * `HauteurParser` : « 27 », ou « 27-6AB4CD » avec la découpe.
     */
    data class Hauteur(val texte: String) : VoiceEvent

    /**
     * Découpe dictée seule pour la dernière tige, sans redire la hauteur : [segments] est la
     * partie qui suit le tiret (« 6AB4CD »), à greffer sur la hauteur que la tige porte déjà.
     * Sans hauteur, une longueur de billon ne veut rien dire : l'appelant refuse alors l'énoncé.
     */
    data class Decoupe(val segments: String) : VoiceEvent

    /**
     * Qualité arbre dictée seule, pour la dernière tige : l'équivalent vocal du bouton Q.
     * [code] est le libellé du référentiel, celui qui part dans la tige.
     */
    data class Qualite(val code: String) : VoiceEvent

    data class Commande(val nom: String) : VoiceEvent
    data class Rejet(val brut: String, val raison: Raison) : VoiceEvent
    enum class Raison { UNK, INCOMPLET, AMBIGU }
}

class UtteranceParser(private val lexicon: Lexicon) {

    fun parse(voskText: String, essenceCourante: String? = null): VoiceEvent {
        val tokens = voskText.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return VoiceEvent.Rejet(voskText, VoiceEvent.Raison.INCOMPLET)
        if (tokens.contains("[unk]")) return VoiceEvent.Rejet(voskText, VoiceEvent.Raison.UNK)

        if (tokens.size == 1 && tokens[0] in VoiceCommands.ALL) {
            return VoiceEvent.Commande(tokens[0])
        }

        // Le mot-clé « hauteur » bascule la SUITE de l'énoncé en mètres : sans lui, « quarante
        // cinq » est une classe de diamètre, avec lui c'est une longueur. Il peut ouvrir l'énoncé
        // (annotation de la dernière tige) ou le couper en deux (tige + sa hauteur d'un seul tenant).
        // « decoupe six alpha bravo » : la découpe seule, sur la hauteur déjà portée par la tige.
        // Le mot-clé doit ouvrir l'énoncé — « hetre quarante cinq decoupe six alpha bravo » serait
        // une découpe sur une hauteur qui n'existe pas encore, donc jetée en silence : on rejette.
        if (tokens[0] in VoiceCommands.DECOUPE) return parseDecoupe(tokens.drop(1), voskText)
        if (tokens.any { it in VoiceCommands.DECOUPE }) {
            return VoiceEvent.Rejet(voskText, VoiceEvent.Raison.AMBIGU)
        }

        val coupure = tokens.indexOf(VoiceCommands.HAUTEUR)
        if (coupure == 0) return parseHauteur(tokens.drop(1), voskText)
        if (coupure > 0) {
            val hauteur = parseHauteur(tokens.drop(coupure + 1), voskText)
            if (hauteur !is VoiceEvent.Hauteur) return hauteur
            // Devant le mot-clé, on exige une tige COMPLÈTE : « chablis hauteur vingt sept »
            // annoterait deux choses à la fois. Surtout, renvoyer ici l'événement de tête
            // reviendrait à jeter la hauteur en silence — jamais.
            return when (val tige = parseTige(tokens.take(coupure), voskText, essenceCourante)) {
                is VoiceEvent.Tige -> tige.copy(hauteurTexte = hauteur.texte)
                is VoiceEvent.Rejet -> tige
                else -> VoiceEvent.Rejet(voskText, VoiceEvent.Raison.INCOMPLET)
            }
        }
        return parseTige(tokens, voskText, essenceCourante)
    }

    /** Énoncé de création : essence (ou essence courante) + classe, qualité optionnelle. */
    private fun parseTige(tokens: List<String>, voskText: String, essenceCourante: String?): VoiceEvent {
        if (tokens.isEmpty()) return VoiceEvent.Rejet(voskText, VoiceEvent.Raison.INCOMPLET)

        var i = 0
        var essence: String? = null
        var classe: Int? = null
        var qualite: String? = null

        while (i < tokens.size) {
            val match = longestMatch(tokens, i) ?: return VoiceEvent.Rejet(voskText, VoiceEvent.Raison.AMBIGU)
            when (match) {
                is Match.Ess -> if (essence == null) essence = match.value.codeOnf
                    else return VoiceEvent.Rejet(voskText, VoiceEvent.Raison.AMBIGU)
                is Match.Cls -> if (classe == null) classe = match.value
                    else return VoiceEvent.Rejet(voskText, VoiceEvent.Raison.AMBIGU)
                is Match.Qua -> if (qualite == null) qualite = match.value.code
                    else return VoiceEvent.Rejet(voskText, VoiceEvent.Raison.AMBIGU)
            }
            i += match.len
        }

        // Une qualité dictée SEULE annote la dernière tige, comme la hauteur : ni essence ni
        // classe n'ont été dites, il n'y a donc rien à créer. « hetre chablis » (essence sans
        // classe) reste un énoncé incomplet : on ne devine pas la classe manquante.
        if (classe == null && qualite != null && essence == null) return VoiceEvent.Qualite(qualite)

        val ess = essence ?: essenceCourante
            ?: return VoiceEvent.Rejet(voskText, VoiceEvent.Raison.INCOMPLET)
        val cls = classe ?: return VoiceEvent.Rejet(voskText, VoiceEvent.Raison.INCOMPLET)
        return VoiceEvent.Tige(ess, cls, qualite)
    }

    /**
     * « hauteur vingt sept » → « 27 » ; « hauteur vingt sept six alpha bravo quatre charlie delta »
     * → « 27-6AB4CD ». La hauteur totale d'abord, puis autant de couples (longueur, lettres) que
     * l'opérateur en dicte. Le texte produit est celui de la saisie manuelle : rien de nouveau
     * n'entre dans le schéma.
     */
    private fun parseHauteur(tokens: List<String>, brut: String): VoiceEvent {
        if (!lexicon.hauteurDictable) return VoiceEvent.Rejet(brut, VoiceEvent.Raison.AMBIGU)
        var i = 0
        val totale = plusLongNombre(tokens, i) ?: return VoiceEvent.Rejet(brut, VoiceEvent.Raison.INCOMPLET)
        i += totale.len

        val segments = when (val r = segments(tokens, i)) {
            is Segments.Err -> return VoiceEvent.Rejet(brut, r.raison)
            is Segments.Ok -> r.texte
        }

        val texte = if (segments.isEmpty()) "${totale.valeur}" else "${totale.valeur}-$segments"
        return VoiceEvent.Hauteur(texte)
    }

    /**
     * « decoupe six alpha bravo » → « 6AB ». Aucune hauteur totale n'est dite : elle vient de la
     * tige. Une découpe vide n'annoterait rien, c'est un énoncé incomplet.
     */
    private fun parseDecoupe(tokens: List<String>, brut: String): VoiceEvent {
        if (!lexicon.hauteurDictable) return VoiceEvent.Rejet(brut, VoiceEvent.Raison.AMBIGU)
        return when (val r = segments(tokens, 0)) {
            is Segments.Err -> VoiceEvent.Rejet(brut, r.raison)
            // « decoupe » tout court n'annoterait rien : énoncé incomplet.
            is Segments.Ok ->
                if (r.texte.isEmpty()) VoiceEvent.Rejet(brut, VoiceEvent.Raison.INCOMPLET)
                else VoiceEvent.Decoupe(r.texte)
        }
    }

    private sealed interface Segments {
        data class Ok(val texte: String) : Segments
        data class Err(val raison: VoiceEvent.Raison) : Segments
    }

    /**
     * Suite de couples (longueur, lettres de qualité bois) → « 6AB4CD ». Deux façons d'échouer,
     * qu'on distingue car elles ne disent pas la même chose du bruit ambiant : un mot là où on
     * attendait une longueur est **ambigu**, une longueur sans qualité derrière est **incomplète**.
     */
    private fun segments(tokens: List<String>, from: Int): Segments {
        var i = from
        val sortie = StringBuilder()
        while (i < tokens.size) {
            val longueur = plusLongNombre(tokens, i) ?: return Segments.Err(VoiceEvent.Raison.AMBIGU)
            i += longueur.len
            val lettres = StringBuilder()
            while (i < tokens.size) {
                val lettre = lexicon.lettresBois[listOf(tokens[i])] ?: break
                lettres.append(lettre)
                i++
            }
            if (lettres.isEmpty()) return Segments.Err(VoiceEvent.Raison.INCOMPLET)
            sortie.append(longueur.valeur).append(lettres)
        }
        return Segments.Ok(sortie.toString())
    }

    private data class Nombre(val valeur: Int, val len: Int)

    private fun plusLongNombre(tokens: List<String>, from: Int): Nombre? {
        val maxLen = minOf(lexicon.maxKeyLen, tokens.size - from)
        for (len in maxLen downTo 1) {
            lexicon.nombres[tokens.subList(from, from + len)]?.let { return Nombre(it, len) }
        }
        return null
    }

    // ---- appariement au plus long sur le lexique fermé ----

    private sealed interface Match {
        val len: Int
        data class Ess(val value: SpokenEssence, override val len: Int) : Match
        data class Cls(val value: Int, override val len: Int) : Match
        data class Qua(val value: SpokenQualite, override val len: Int) : Match
    }

    private fun longestMatch(tokens: List<String>, from: Int): Match? {
        val maxLen = minOf(lexicon.maxKeyLen, tokens.size - from)
        for (len in maxLen downTo 1) {
            val key = tokens.subList(from, from + len)
            lexicon.essences[key]?.let { return Match.Ess(it, len) }
            lexicon.classes[key]?.let { return Match.Cls(it, len) }
            lexicon.qualites[key]?.let { return Match.Qua(it, len) }
        }
        return null
    }
}
