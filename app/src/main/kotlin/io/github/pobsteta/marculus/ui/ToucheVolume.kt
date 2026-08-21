package io.github.pobsteta.marculus.ui

/**
 * Relais entre l'Activity (qui reçoit les touches matérielles de volume) et la feuille de
 * martelage (qui sait quelle cellule est active). La feuille enregistre `onVolume` quand le
 * réglage « boutons de volume » est actif ; `null` sinon (le volume retrouve son rôle normal).
 *
 * `onPtt` suit la même logique pour la dictée vocale : quand il est posé, le volume **bas**
 * distingue deux gestes — appui court = comptage (inchangé), appui long ≈ 0,5 s = push-to-talk.
 */
object ToucheVolume {
    /** Reçoit `haut = true` pour volume +, `false` pour volume − ; renvoie true si l'action est consommée. */
    var onVolume: ((haut: Boolean) -> Boolean)? = null

    /** Push-to-talk sur appui long du volume bas : `true` à l'ouverture, `false` au relâchement. */
    var onPtt: ((tenu: Boolean) -> Unit)? = null
}
