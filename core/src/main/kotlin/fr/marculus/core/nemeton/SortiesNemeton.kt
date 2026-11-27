package fr.marculus.core.nemeton

/**
 * Sorties NEMETON d'un arbre (mission 039).
 *
 * @property valeurs clé = nom de sortie du jeu de contrôle (`V_aerien_total_m3`, `V_bois_fort_total_m3`,
 *   `V_bois_fort_tige_m3`, `biomasse_aerienne_t`, `carbone_aerien_t`), dans cet ordre ; une sortie qui n'existe pas
 *   pour l'essence est absente, jamais 0.
 * @property horsDomaine l'arbre sort du domaine de calage : les valeurs sont calculées, l'écran doit alerter.
 * @property essence libellé (colonne `essence` de `constantes.csv`) de la ligne réellement utilisée.
 * @property version version du paquet (clé `version` du manifeste), qui voyage avec chaque calcul.
 */
data class SortiesNemeton(
    val valeurs: Map<String, Double>,
    val horsDomaine: Boolean,
    val essence: String,
    val version: String,
)
