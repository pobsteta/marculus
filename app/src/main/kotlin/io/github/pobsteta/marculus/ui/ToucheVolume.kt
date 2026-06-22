package io.github.pobsteta.marculus.ui

/**
 * Relais entre l'Activity (qui reçoit les touches matérielles de volume) et la feuille de
 * martelage (qui sait quelle cellule est active). La feuille enregistre `onVolume` quand le
 * réglage « boutons de volume » est actif ; `null` sinon (le volume retrouve son rôle normal).
 */
object ToucheVolume {
    /** Reçoit `haut = true` pour volume +, `false` pour volume − ; renvoie true si l'action est consommée. */
    var onVolume: ((haut: Boolean) -> Boolean)? = null
}
