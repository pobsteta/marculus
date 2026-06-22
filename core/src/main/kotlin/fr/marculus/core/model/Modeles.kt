package fr.marculus.core.model

/** Convention de mesure fixée au niveau du contexte, commune à toutes les essences. */
enum class ModeMesure { DIAMETRE, CIRCONFERENCE }

/** Tarif de cubage à une entrée appliqué au contexte (volume bois fort tige). */
enum class TarifCubage { AUCUN, SCHAEFFER_RAPIDE, SCHAEFFER_LENT, EMERGE }

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

/** Une colonne de la feuille : une essence avec ses couleurs de cellule (ARGB). */
data class EssenceColonne(
    val nom: String,
    val couleurFondArgb: Int,
    val couleurTexteArgb: Int,
)

/** Clé d'un compteur : une cellule de la feuille de martelage. */
data class CompteurCle(val essence: String, val classe: Int)

/**
 * Réglages par compteur (cellule) : seuils numériques.
 * `avisSiMoins` = borne basse, `avisSiPlus` = borne haute (doit être > avisSiMoins).
 * Une alerte s'affiche tant que le total sort de l'intervalle.
 */
data class ConfigCompteur(
    val essence: String,
    val classe: Int,
    val avisSiPlus: Int? = null,
    val avisSiMoins: Int? = null,
) {
    fun alerteMoins(total: Int): Boolean = avisSiMoins != null && total < avisSiMoins
    fun alertePlus(total: Int): Boolean = avisSiPlus != null && total > avisSiPlus
}

/** Une opération de martelage. Remplace la notion de « groupe » de l'app de référence. */
data class Contexte(
    val id: String,
    val nom: String,
    val mode: ModeMesure,
    val axe: AxeClasses,
    val essences: List<EssenceColonne>,
    val commentaire: String? = null,
    val increment: Int = 1,
    val exporte: Boolean = false,
    /** Chemin du GeoPackage (parcelles + ortho) rattaché à ce contexte, ou null. */
    val cheminGpkg: String? = null,
    /** Tarif de cubage choisi pour estimer les volumes (ou AUCUN). */
    val tarif: TarifCubage = TarifCubage.AUCUN,
    /** Numéro de tarif (1..20) : fixe M = 0,8 + 0,1·N (volume de l'arbre de 45 cm). */
    val tarifNumero: Int = 0,
) {
    val essencesNoms: List<String> get() = essences.map { it.nom }
}

/**
 * Un événement du journal append-only. Une tige = un arbre martelé (PLUS) ou une annulation
 * conservée (ANNULATION). `quantite` porte l'incrément du contexte (1 par défaut = 1 arbre).
 * Les totaux sont dérivés du journal, jamais stockés.
 */
data class Tige(
    val uuid: String,
    val contexteId: String,
    val essence: String,
    val classe: Int,
    val action: ActionTige,
    val horodatage: Long,
    val quantite: Int = 1,
    val hauteurTexte: String? = null,
    val qualiteArbre: String? = null,
    val position: Position? = null,
    val operateur: String? = null,
    /** Parcelle attribuée au moment du martelage (instantané figé), ou null. */
    val parcelle: String? = null,
)
