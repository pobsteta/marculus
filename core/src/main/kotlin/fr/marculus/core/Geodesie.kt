package fr.marculus.core

import fr.marculus.core.model.Position
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/**
 * Aire d'une (multi)parcelle décrite par ses anneaux WGS84 (longitude/latitude en degrés).
 * Projection équirectangulaire locale (centrée sur la latitude moyenne) puis formule du lacet :
 * précis à l'échelle d'une parcelle forestière. La somme signée des anneaux gère les trous
 * (anneau extérieur et trous d'orientations opposées selon la norme OGC).
 */
object Geodesie {
    private const val R = 6378137.0 // rayon terrestre moyen (m)

    /** Aire en mètres carrés (toujours positive). */
    fun aireM2(anneaux: List<List<Position>>): Double {
        val points = anneaux.flatten()
        if (points.size < 3) return 0.0
        val lat0 = points.sumOf { it.latitude } / points.size
        val cosLat = cos(lat0 * PI / 180.0)
        var somme = 0.0
        for (anneau in anneaux) {
            if (anneau.size < 3) continue
            var aire = 0.0
            for (i in anneau.indices) {
                val a = anneau[i]
                val b = anneau[(i + 1) % anneau.size]
                val xa = a.longitude * PI / 180.0 * R * cosLat
                val ya = a.latitude * PI / 180.0 * R
                val xb = b.longitude * PI / 180.0 * R * cosLat
                val yb = b.latitude * PI / 180.0 * R
                aire += xa * yb - xb * ya
            }
            somme += aire / 2.0
        }
        return abs(somme)
    }

    /** Aire en hectares. */
    fun aireHa(anneaux: List<List<Position>>): Double = aireM2(anneaux) / 10_000.0
}
