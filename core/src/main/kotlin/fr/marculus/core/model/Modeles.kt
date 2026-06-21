package fr.marculus.core.model

/** Convention de mesure fixée au niveau du contexte, commune à toutes les essences. */
enum class ModeMesure { DIAMETRE, CIRCONFERENCE }

/** Un `+` ajoute une tige ; un `−` enregistre une annulation (jamais d'effacement). */
enum class ActionTige { PLUS, ANNULATION }

/** Position GPS brute (pas de carte en v1). */
data class Position(val latitude: Double, val longitude: Double)

/**
 * Axe des classes (diamètre ou circonférence) d'un contexte : commun à toutes les essences,
 * ce qui rend la feuille de martelage rectangulaire.
 */
data class AxeClasses(val min: Int, val max: Int, val pas: Int) {
    init {
        require(pas > 0) { "Le pas doit être strictement positif (reçu $pas)" }
        require(min <= max) { "min ($min) doit être inférieur ou égal à max ($max)" }
    }

    /** Classes de min à max (inclus), par pas. S'arrête à la dernière classe ≤ max. */
    fun classes(): List<Int> =
        generateSequence(min) { it + pas }.takeWhile { it <= max }.toList()
}

/** Clé d'un compteur : une cellule de la feuille de martelage. */
data class CompteurCle(val essence: String, val classe: Int)

/** Une opération de martelage. Remplace la notion de « groupe » de l'app de référence. */
data class Contexte(
    val id: String,
    val nom: String,
    val mode: ModeMesure,
    val axe: AxeClasses,
    val essencesActives: List<String>,
)

/**
 * Un événement du journal append-only. Une tige = un arbre martelé (PLUS) ou une annulation
 * conservée (ANNULATION). Les totaux sont dérivés du journal, jamais stockés.
 */
data class Tige(
    val uuid: String,
    val contexteId: String,
    val essence: String,
    val classe: Int,
    val action: ActionTige,
    val horodatage: Long,
    val hauteurTexte: String? = null,
    val qualiteArbre: String? = null,
    val position: Position? = null,
    val operateur: String? = null,
)
