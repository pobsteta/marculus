package fr.marculus.core

import fr.marculus.core.model.Position
import kotlin.math.roundToInt

/** Un houppier segmenté depuis un MNH : son contour (WGS84) et la hauteur de son apex, en mètres. */
data class Houppier(val hauteurM: Double, val anneaux: List<List<Position>>)

/**
 * Estimation de la hauteur d'une tige par la couche `houppier` du GPKG (MNH) : point-dans-polygone
 * sur la position GNSS, la hauteur retenue est celle de l'apex du houppier.
 *
 * L'estimation est une **aide**, jamais une valeur d'autorité : elle ne complète que les tiges
 * **sans** hauteur mesurée, et l'opérateur la corrige au bouton H.
 */
object EstimationHauteur {

    /** Hauteurs plausibles pour un arbre sur pied : au-delà, l'attribut est suspect, on l'ignore. */
    private val PLAUSIBLE = 1.0..70.0

    /**
     * Hauteur estimée (m, arrondie) pour la position `p`, ou `null` si elle n'est dans aucun
     * houppier — cas normal en trouée, en bord de peuplement, et pour toute tige **dominée**
     * (sous couvert, elle n'apparaît pas dans le MNH).
     *
     * Les houppiers issus d'une segmentation par enveloppe convexe **se recouvrent** : quand
     * plusieurs contiennent le point, on retient le **plus haut**, celui dont l'apex domine
     * physiquement l'opérateur.
     */
    fun pour(houppiers: List<Houppier>, p: Position): Int? =
        houppiers
            .filter { it.hauteurM in PLAUSIBLE && AttributionSpatiale.contient(it.anneaux, p) }
            .maxByOrNull { it.hauteurM }
            ?.hauteurM
            ?.roundToInt()

    /** Texte de hauteur au format de la saisie manuelle (`HauteurParser`), ou `null`. */
    fun texte(houppiers: List<Houppier>, p: Position): String? = pour(houppiers, p)?.toString()
}
