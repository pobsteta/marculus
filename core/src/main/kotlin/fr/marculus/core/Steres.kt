package fr.marculus.core

import fr.marculus.core.model.Contexte

/**
 * Stères à côté des m³ (mission 017). Une seule conversion, appelée par l'écran Statut / historique
 * et par le CSV du bilan par lot. Les stères se calculent sur le m³ non arrondi.
 */
object Steres {
    /** Affouage avec un volume maximal de lot renseigné (en m³, même saisi en stères) et un coefficient positif. */
    fun actif(contexte: Contexte): Boolean =
        contexte.affouage && contexte.volumeMaxLotM3 > 0 && contexte.steresParM3 > 0

    /** m³ -> stères, non arrondi. */
    fun deM3(volumeM3: Double, contexte: Contexte): Double = volumeM3 * contexte.steresParM3
}
