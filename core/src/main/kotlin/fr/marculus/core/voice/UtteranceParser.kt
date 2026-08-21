package fr.marculus.core.voice

/**
 * Parseur d'énoncé : transforme la sortie texte de Vosk en événement métier.
 *
 * Formes acceptées (v1) :
 *   "hetre quarante cinq"            -> Tige(HET, 45, qualité nulle)
 *   "hetre quarante cinq bravo"      -> Tige(HET, 45, B)
 *   "quarante cinq"                  -> Tige(essence courante, 45)   [mode rafale]
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
    data class Tige(val codeOnf: String, val classe: Int, val qualite: String?) : VoiceEvent
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

        val ess = essence ?: essenceCourante
            ?: return VoiceEvent.Rejet(voskText, VoiceEvent.Raison.INCOMPLET)
        val cls = classe ?: return VoiceEvent.Rejet(voskText, VoiceEvent.Raison.INCOMPLET)
        return VoiceEvent.Tige(ess, cls, qualite)
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
