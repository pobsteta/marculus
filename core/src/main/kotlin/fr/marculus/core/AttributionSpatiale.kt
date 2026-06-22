package fr.marculus.core

import fr.marculus.core.model.Position

/**
 * Rattachement spatial : test « point dans polygone » par lancer de rayon (ray casting),
 * règle pair-impair sur l'ensemble des anneaux d'une parcelle (les trous sont donc gérés :
 * un point dans un trou traverse l'anneau extérieur puis l'anneau du trou → parité paire → dehors).
 *
 * Les coordonnées sont en WGS84 (longitude = x, latitude = y) ; à l'échelle d'une parcelle,
 * l'erreur due à la non-planéité est négligeable pour un test d'appartenance.
 */
object AttributionSpatiale {

    /** Vrai si la position `p` est contenue dans la parcelle décrite par ses `anneaux`. */
    fun contient(anneaux: List<List<Position>>, p: Position): Boolean {
        var dedans = false
        for (anneau in anneaux) {
            val n = anneau.size
            if (n < 3) continue
            var j = n - 1
            for (i in 0 until n) {
                val xi = anneau[i].longitude
                val yi = anneau[i].latitude
                val xj = anneau[j].longitude
                val yj = anneau[j].latitude
                if (((yi > p.latitude) != (yj > p.latitude)) &&
                    (p.longitude < (xj - xi) * (p.latitude - yi) / (yj - yi) + xi)
                ) {
                    dedans = !dedans
                }
                j = i
            }
        }
        return dedans
    }
}
