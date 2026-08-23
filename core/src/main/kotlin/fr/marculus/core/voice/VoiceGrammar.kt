package fr.marculus.core.voice

/**
 * Grammaire fermée pour la reconnaissance vocale Vosk (module :core, pur JVM).
 *
 * Principe : Vosk accepte au démarrage du Recognizer une liste JSON de mots/phrases
 * autorisés. Le décodage est alors restreint à ce lexique — tout ce qui n'y figure
 * pas est capté par le jeton spécial "[unk]" et rejeté par le parseur.
 *
 * La grammaire est reconstruite à chaque changement de contexte de martelage
 * (les essences de la matrice et l'axe des classes varient par contexte).
 */

/** Une essence telle que dictée. [spoken] peut être multi-mots ("chene sessile"). */
data class SpokenEssence(
    val codeOnf: String,          // ex. "HET", "CHS"
    val spoken: List<String>,     // formes parlées acceptées, en minuscules SANS accents
)

/**
 * Une qualité dictée. [spoken] peut être multi-mots : les codes en lettres passent par
 * l'alphabet radio ("AB" → "alpha bravo"), une lettre nue étant le pire cas en ASR.
 */
data class SpokenQualite(
    val code: String,             // ex. "A", "AB", "Chablis"
    val spoken: String,           // ex. "alpha", "alpha bravo", "chablis"
)

object VoiceCommands {
    const val ANNULE = "annule"       // retire la dernière tige (le journal append-only s'en charge)
    const val REPETE = "repete"       // re-annonce la dernière tige par TTS
    val ALL = listOf(ANNULE, REPETE)

    /**
     * Mot-clé ouvrant un énoncé de hauteur : « hauteur vingt sept six alpha bravo ». Il bascule
     * le parseur dans un mode où les nombres sont des mètres, pas des classes — un même
     * « quarante cinq » ne veut pas dire la même chose des deux côtés.
     */
    const val HAUTEUR = "hauteur"

    /**
     * Mot-clé de **découpe seule** : « decoupe six alpha bravo » pose la découpe sur la hauteur
     * **déjà portée** par la dernière tige — estimée depuis le MNH, dictée ou saisie — sans avoir
     * à la redire. Sans lui, ajouter une qualité bois obligeait à répéter la hauteur entière, et
     * une hauteur redite écrase une hauteur mesurée.
     *
     * Plusieurs variantes sont mises dans la grammaire : le modèle ignore silencieusement celles
     * qu'il ne connaît pas (accentuation, mot absent du lexique), les autres restent dictables.
     * L'aide « Formes à dicter » affiche la première que le modèle sait décoder.
     */
    val DECOUPE = listOf("decoupe", "découpe", "billon")
}

/**
 * Dictée de la hauteur et de la découpe. [maxMetres] borne les nombres admis (hauteur totale et
 * longueurs de billon) ; [lettresBois] sont les lettres du référentiel de qualité bois, épelées
 * en alphabet radio.
 */
data class OptionsHauteur(
    val maxMetres: Int = 60,
    val lettresBois: List<Char> = emptyList(),
    /** Mot radio de chaque lettre, résolu contre le vocabulaire du modèle. */
    val motRadio: (Char) -> String = { ReferentielParle.motRadio(it) },
)

object GrammarBuilder {

    /**
     * Construit la chaîne JSON à passer à `Recognizer(model, 16000f, grammar)`.
     *
     * IMPORTANT : le modèle vosk-model-small-fr est entraîné sur du texte SANS
     * majuscules ni ponctuation ; les accents sont dans le lexique ("chêne" existe)
     * mais pour rester robuste on normalise tout en minuscules non accentuées et
     * on fournit les deux formes quand elles diffèrent.
     */
    fun buildJson(
        essences: List<SpokenEssence>,
        classes: List<Int>,
        qualites: List<SpokenQualite>,
        hauteur: OptionsHauteur? = null,
    ): String {
        val phrases = buildSet {
            essences.forEach { add(it.spoken.joinToString(" ")) }
            classes.forEach { add(FrenchNumbers.toTokens(it).joinToString(" ")) }
            qualites.forEach { add(it.spoken) }
            addAll(VoiceCommands.ALL)
            if (hauteur != null) {
                add(VoiceCommands.HAUTEUR)
                addAll(VoiceCommands.DECOUPE)
                (1..hauteur.maxMetres).forEach { add(FrenchNumbers.toTokens(it).joinToString(" ")) }
                hauteur.lettresBois.forEach { add(hauteur.motRadio(it)) }
            }
            add("[unk]")
        }
        return phrases.joinToString(prefix = "[", postfix = "]") { "\"${it.replace("\"", "")}\"" }
    }

    /** Index inverse : suite de jetons parlés -> valeur métier. Utilisé par le parseur. */
    fun buildLexicon(
        essences: List<SpokenEssence>,
        classes: List<Int>,
        qualites: List<SpokenQualite>,
        hauteur: OptionsHauteur? = null,
    ): Lexicon = Lexicon(
        essences = essences.associateBy { it.spoken },
        classes = classes.associateBy { FrenchNumbers.toTokens(it) },
        // Les qualités sont multi-mots dès qu'elles sont épelées en radio ("alpha bravo").
        qualites = qualites.associateBy { it.spoken.split(" ").filter(String::isNotEmpty) },
        nombres = hauteur?.let { h ->
            (1..h.maxMetres).associateBy { FrenchNumbers.toTokens(it) }
        } ?: emptyMap(),
        lettresBois = hauteur?.let { h ->
            h.lettresBois.associateBy { listOf(h.motRadio(it)) }
        } ?: emptyMap(),
    )
}

/** Lexique inversé, clés = suites de jetons. */
data class Lexicon(
    val essences: Map<List<String>, SpokenEssence>,
    val classes: Map<List<String>, Int>,
    val qualites: Map<List<String>, SpokenQualite>,
    /** Nombres en mètres (hauteur totale et longueurs de découpe), vide si la hauteur est hors grammaire. */
    val nombres: Map<List<String>, Int> = emptyMap(),
    /** Lettres de qualité bois épelées en radio. */
    val lettresBois: Map<List<String>, Char> = emptyMap(),
) {
    val maxKeyLen: Int =
        (essences.keys + classes.keys + qualites.keys + nombres.keys + lettresBois.keys)
            .maxOfOrNull { it.size } ?: 1

    /** La hauteur est dictable dans ce contexte. */
    val hauteurDictable: Boolean get() = nombres.isNotEmpty()
}

/**
 * Nombres français en jetons séparés (pas de traits d'union : Vosk sort des mots).
 * Couvre 1..999 — les classes de circonférence dépassent 200 cm sur les gros bois.
 * "zéro accent" volontaire pour coller à la normalisation de la grammaire.
 */
object FrenchNumbers {
    private val units = arrayOf(
        "", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
        "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
        "dix sept", "dix huit", "dix neuf",
    )

    fun toTokens(n: Int): List<String> {
        require(n in 1..999) { "Classe hors bornes: $n" }
        val words = when {
            n < 20 -> units[n]
            n < 70 -> tens(n / 10) + joinUnit(n % 10)
            n < 80 -> "soixante" + joinTeens(n - 60)
            n < 100 -> "quatre vingt" + if (n == 80) "" else " " + units[n - 80]
            // Centaines sans le « s » du pluriel : Vosk restitue « cent » dans les deux cas.
            else -> {
                val tete = if (n / 100 == 1) "cent" else units[n / 100] + " cent"
                if (n % 100 == 0) tete else tete + " " + toTokens(n % 100).joinToString(" ")
            }
        }
        return words.trim().split(" ")
    }

    private fun tens(t: Int) = arrayOf("", "", "vingt", "trente", "quarante", "cinquante", "soixante")[t]
    private fun joinUnit(u: Int) = when (u) { 0 -> ""; 1 -> " et un"; else -> " " + units[u] }
    private fun joinTeens(v: Int) = if (v == 11) " et onze" else " " + units[v]
}
