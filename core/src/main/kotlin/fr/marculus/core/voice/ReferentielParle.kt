package fr.marculus.core.voice

import fr.marculus.core.Cubage
import java.text.Normalizer

/**
 * Génération du référentiel *parlé* d'un contexte de martelage : à partir des libellés réels
 * (essences de la matrice, qualités du référentiel), produit les formes à dicter et le code
 * métier renvoyé par le parseur.
 *
 * Trois règles structurent la génération :
 *
 *  1. **Forme à deux mots imposée** pour tout couple d'essences partageant le premier mot
 *     (« chêne sessile » / « chêne pédonculé ») ou dont les premiers mots sont quasi-homophones
 *     (chêne/frêne, orme/charme, aulne/orme, pin/sapin). Une seule syllabe de différence sur un
 *     mot isolé ne suffit pas en forêt ; deux mots donnent deux endroits où le décodeur tranche.
 *  2. Quand le libellé est **d'un seul mot** et qu'il entre en conflit, le second mot est la
 *     lettre discriminante du code ONF en alphabet radio (« chêne » CHR / « frêne » FRC →
 *     « chene charlie » / « frene fox »). Aucune espèce n'est inventée : la forme parlée est un
 *     alias de dictée, le libellé enregistré dans la tige reste celui du contexte.
 *  3. **Rien n'est promis que le modèle ne sache décoder** : [motConnu] interroge le vocabulaire
 *     du modèle acoustique. Un libellé qu'il ignore (« Volis », absent du français courant) est
 *     remplacé par l'épellation radio de ses initiales — « victor » —, et les mots radio eux-mêmes
 *     ont des variantes de repli (« foxtrot », « juliett », « uniform » et « xray » ne figurent pas
 *     dans le modèle small-fr).
 */
object ReferentielParle {

    /** Une essence du contexte, telle qu'elle s'enregistre et telle qu'elle se dicte. */
    data class EssenceParlee(
        /** Libellé exact de la colonne du contexte — c'est lui qui part dans la tige. */
        val nom: String,
        /** Clé métier renvoyée par le parseur (code ONF quand il est connu), unique par contexte. */
        val codeOnf: String,
        /** Forme parlée, en jetons minuscules sans accents. */
        val spoken: List<String>,
    ) {
        fun versGrammaire(): SpokenEssence = SpokenEssence(codeOnf, spoken)
    }

    /**
     * Alphabet radio : le mot de l'OTAN d'abord, puis des replis présents dans les modèles
     * français quand il en est absent. Le premier mot connu du modèle gagne.
     */
    private val RADIO: Map<Char, List<String>> = mapOf(
        'A' to listOf("alpha"),
        'B' to listOf("bravo"),
        'C' to listOf("charlie"),
        'D' to listOf("delta"),
        'E' to listOf("echo"),
        'F' to listOf("foxtrot", "fox"),
        'G' to listOf("golf"),
        'H' to listOf("hotel"),
        'I' to listOf("india"),
        'J' to listOf("juliett", "juliette"),
        'K' to listOf("kilo"),
        'L' to listOf("lima"),
        'M' to listOf("mike"),
        'N' to listOf("november"),
        'O' to listOf("oscar"),
        'P' to listOf("papa"),
        'Q' to listOf("quebec"),
        'R' to listOf("romeo"),
        'S' to listOf("sierra"),
        'T' to listOf("tango"),
        'U' to listOf("uniform", "uniforme"),
        'V' to listOf("victor"),
        'W' to listOf("whisky"),
        'X' to listOf("xray", "xavier"),
        'Y' to listOf("yankee"),
        'Z' to listOf("zoulou"),
    )

    /** Alphabet radio canonique (premier choix par lettre), pour l'affichage et les tests. */
    val ALPHABET_RADIO: Map<Char, String> = RADIO.mapValues { it.value.first() }

    /** Mot radio d'une lettre, en préférant une variante que le modèle sait décoder. */
    fun motRadio(lettre: Char, motConnu: (String) -> Boolean = { true }): String {
        val variantes = RADIO[lettre.uppercaseChar()] ?: return "unite"
        return variantes.firstOrNull(motConnu) ?: variantes.first()
    }

    /**
     * Familles de premiers mots acoustiquement voisins : deux essences d'une même famille sont
     * traitées comme si elles partageaient leur premier mot.
     */
    private val FAMILLES_VOISINES: List<Set<String>> = listOf(
        setOf("chene", "frene"),
        setOf("orme", "charme"),
        setOf("aulne", "orme"),
        setOf("pin", "sapin"),
    )

    /** Nombre maximal de mots d'une forme parlée (borne la fenêtre d'appariement du parseur). */
    private const val MOTS_MAX = 3

    // Normalisation identique à celle de Cubage/EMERGE : minuscules, sans accent,
    // caractères non alphanumériques → espace, espaces compactés.
    private fun normaliser(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex(" +"), " ")
            .trim()

    /**
     * Construit le référentiel parlé des essences d'un contexte. Seules les essences passées en
     * argument sont dictables : la grammaire fermée ne contient jamais les 226 essences ONF.
     *
     * @param motConnu appartenance au vocabulaire du modèle acoustique ; tout accepter par défaut
     *   (utile aux tests et tant que le modèle n'est pas chargé).
     */
    fun essences(noms: List<String>, motConnu: (String) -> Boolean = { true }): List<EssenceParlee> {
        val bases = noms.filter { it.isNotBlank() }.distinct().map { nom ->
            Base(nom = nom, mots = motsDe(nom), code = codeDe(nom))
        }
        val codes = codesUniques(bases)
        val enConflit = conflits(bases)

        val formes = bases.mapIndexed { i, base ->
            val code = codes[i]
            val mots = when {
                // Libellé que le modèle ne sait pas décoder : on épelle le code, toujours prononçable.
                !base.mots.all(motConnu) -> epelerCode(code, motConnu)
                !enConflit[i] -> base.mots
                base.mots.size >= 2 -> base.mots
                // Un seul mot en conflit : on impose un second mot discriminant.
                else -> base.mots + motRadio(lettreDiscriminante(codes, enConflit, i), motConnu)
            }
            EssenceParlee(nom = base.nom, codeOnf = code, spoken = mots)
        }
        return leverDoublons(formes, codes, motConnu)
    }

    /**
     * Construit le référentiel parlé des qualités depuis les libellés du référentiel de
     * l'application. [SpokenQualite.code] est le libellé d'origine : c'est lui qui est
     * enregistré dans la tige, sans conversion.
     *
     * Un code en lettres est épelé en radio (« AB » → « alpha bravo ») ; un libellé en toutes
     * lettres se dicte tel quel, sauf si le modèle l'ignore — auquel cas ses initiales sont
     * épelées, sur la longueur minimale qui le distingue des autres qualités.
     */
    fun qualites(libelles: List<String>, motConnu: (String) -> Boolean = { true }): List<SpokenQualite> {
        val retenus = libelles.filter { it.isNotBlank() }.distinct()
        val vues = mutableSetOf<String>()
        return retenus.mapNotNull { libelle ->
            val parle = when {
                estCodeLettres(libelle) -> libelle.uppercase().map { motRadio(it, motConnu) }.joinToString(" ")
                else -> {
                    val mots = normaliser(libelle).split(" ").filter { it.isNotEmpty() }.take(MOTS_MAX)
                    if (mots.isNotEmpty() && mots.all(motConnu)) {
                        mots.joinToString(" ")
                    } else {
                        epelerInitiales(libelle, retenus, motConnu)
                    }
                }
            }
            if (parle.isBlank() || !vues.add(parle)) null else SpokenQualite(libelle, parle)
        }
    }

    /**
     * Lettres de qualité bois présentes dans le référentiel (« A », « AB », « CD » → A, B, C, D).
     * Ce sont elles, épelées en radio, qui composent la découpe dictée avec la hauteur.
     */
    fun lettresBois(libelles: List<String>): List<Char> =
        libelles.flatMap { it.uppercase().toList() }.filter { it in 'A'..'Z' }.distinct().sorted()

    /**
     * Tous les mots qu'une grammaire pourrait vouloir employer pour ces libellés : les mots des
     * libellés eux-mêmes et l'alphabet radio complet (variantes de repli comprises). C'est la
     * liste à confronter au vocabulaire du modèle avant de construire les formes définitives.
     */
    fun motsCandidats(essences: List<String>, qualites: List<String>): Set<String> = buildSet {
        (essences + qualites).forEach { libelle ->
            addAll(normaliser(libelle).split(" ").filter { it.isNotEmpty() })
        }
        RADIO.values.forEach { addAll(it) }
        add(VoiceCommands.ANNULE)
        add(VoiceCommands.REPETE)
        add(VoiceCommands.HAUTEUR)
    }

    /** Un code de qualité est une suite courte de lettres majuscules (« A », « AB », « CD »). */
    private fun estCodeLettres(libelle: String): Boolean {
        val t = libelle.trim()
        return t.length <= MOTS_MAX && t.isNotEmpty() && t.all { it in 'A'..'Z' }
    }

    /** Épelle un code en alphabet radio : « CHS » → « charlie hotel sierra ». */
    private fun epelerCode(code: String, motConnu: (String) -> Boolean): List<String> =
        code.filter { it.isLetter() }.uppercase().take(MOTS_MAX).map { motRadio(it, motConnu) }

    /**
     * Épelle les initiales d'un libellé, sur la longueur minimale qui le sépare des autres
     * libellés de la liste (« Volis » seul → « victor »).
     */
    private fun epelerInitiales(
        libelle: String,
        tous: List<String>,
        motConnu: (String) -> Boolean,
    ): String {
        val lettres = normaliser(libelle).filter { it.isLetter() }.uppercase()
        if (lettres.isEmpty()) return ""
        val autres = tous.filterNot { it == libelle }
            .map { normaliser(it).filter { c -> c.isLetter() }.uppercase() }
        val longueur = (1..minOf(MOTS_MAX, lettres.length)).firstOrNull { n ->
            autres.none { it.take(n) == lettres.take(n) }
        } ?: minOf(MOTS_MAX, lettres.length)
        return lettres.take(longueur).map { motRadio(it, motConnu) }.joinToString(" ")
    }

    private data class Base(val nom: String, val mots: List<String>, val code: String)

    private fun motsDe(nom: String): List<String> {
        val mots = normaliser(nom).split(" ").filter { it.isNotEmpty() }.take(MOTS_MAX)
        return mots.ifEmpty { listOf("essence") }
    }

    /**
     * Code ONF de l'essence si la table gftools la reconnaît, sinon un code de repli lisible
     * dérivé du libellé (« Autres feuillus » → AUF, « Autres résineux » → AUR).
     */
    private fun codeDe(nom: String): String {
        Cubage.codeEssence(nom)?.let { return it }
        val mots = normaliser(nom).split(" ").filter { it.isNotEmpty() }
        val brut = when {
            mots.isEmpty() -> nom
            mots.size == 1 -> mots[0].take(3)
            else -> mots[0].take(2) + mots[1].take(1)
        }
        return brut.uppercase().padEnd(3, 'X')
    }

    /** Rend les codes uniques dans le contexte (deux libellés peuvent viser la même entrée gftools). */
    private fun codesUniques(bases: List<Base>): List<String> {
        val vus = mutableSetOf<String>()
        return bases.map { base ->
            var code = base.code
            var n = 2
            while (!vus.add(code)) code = "${base.code}$n".also { n++ }
            code
        }
    }

    /** Marque les essences dont le premier mot est partagé ou voisin de celui d'une autre. */
    private fun conflits(bases: List<Base>): List<Boolean> = bases.map { base ->
        bases.any { autre ->
            autre !== base && (
                autre.mots[0] == base.mots[0] || FAMILLES_VOISINES.any {
                    base.mots[0] in it && autre.mots[0] in it
                }
                )
        }
    }

    /**
     * Lettre du code qui sépare le mieux l'essence [i] des autres essences en conflit : la
     * première position où les codes du groupe ne sont pas tous identiques.
     */
    private fun lettreDiscriminante(codes: List<String>, enConflit: List<Boolean>, i: Int): Char {
        val groupe = codes.filterIndexed { j, _ -> enConflit[j] }
        val code = codes[i].filter { it.isLetter() }.uppercase().padEnd(3, 'X')
        val position = (0 until 3).firstOrNull { p ->
            groupe.map { it.filter(Char::isLetter).uppercase().padEnd(3, 'X')[p] }.distinct().size > 1
        } ?: 0
        return code[position]
    }

    /**
     * Filet de sécurité : si deux formes parlées restent identiques (libellés distincts par les
     * seuls accents, ou par un quatrième mot), on suffixe chacune de sa lettre de code radio.
     */
    private fun leverDoublons(
        formes: List<EssenceParlee>,
        codes: List<String>,
        motConnu: (String) -> Boolean,
    ): List<EssenceParlee> {
        val comptes = formes.groupingBy { it.spoken }.eachCount()
        if (comptes.values.all { it == 1 }) return formes
        val ambigues = formes.indices.filter { comptes.getValue(formes[it].spoken) > 1 }
        val marquees = formes.indices.map { it in ambigues }
        return formes.mapIndexed { i, forme ->
            if (!marquees[i]) {
                forme
            } else {
                forme.copy(
                    spoken = forme.spoken + motRadio(lettreDiscriminante(codes, marquees, i), motConnu),
                )
            }
        }
    }
}
