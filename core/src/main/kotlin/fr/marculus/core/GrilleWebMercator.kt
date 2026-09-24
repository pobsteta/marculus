package fr.marculus.core

import kotlin.math.abs

/** Une matrice de tuiles d'un GeoPackage, réduite à ce qui décide de la grille. */
data class MatriceTuiles(
    val zoom: Int,
    val largeurMatrice: Long,
    val hauteurMatrice: Long,
    val largeurTuile: Long,
    val hauteurTuile: Long,
)

/**
 * Reconnaît une ortho déjà tuilée sur la grille Web Mercator standard (celle d'osmdroid, GDAL
 * `TILING_SCHEME=GoogleMapsCompatible`) : ses tuiles se servent **telles quelles** en XYZ, sans
 * reprojection — qui coûterait des minutes au téléphone pour produire des tuiles identiques.
 */
object GrilleWebMercator {
    /** Demi-étendue du monde en EPSG:3857 (m). */
    const val DEMI_ETENDUE_M = 20037508.3427892

    /** Tolérance sur les bornes : GDAL écrit 20037508.342789244, d'autres arrondissent. */
    private const val TOLERANCE_M = 0.01

    private const val TUILE_PX = 256L

    /**
     * @param organisation organisation du SRS (« EPSG », casse indifférente)
     * @param code code du SRS dans cette organisation
     * @param minX bornes du jeu de matrices, dans le SRS
     * @param matrices matrices **qui contiennent des tuiles** : GDAL déclare aussi les zooms vides
     */
    fun estStandard(
        organisation: String?,
        code: Long,
        minX: Double,
        minY: Double,
        maxX: Double,
        maxY: Double,
        matrices: List<MatriceTuiles>,
    ): Boolean {
        if (!organisation.equals("EPSG", ignoreCase = true) || code != 3857L) return false
        val bornes = listOf(minX to -DEMI_ETENDUE_M, minY to -DEMI_ETENDUE_M, maxX to DEMI_ETENDUE_M, maxY to DEMI_ETENDUE_M)
        if (bornes.any { (v, attendu) -> abs(v - attendu) > TOLERANCE_M }) return false
        if (matrices.isEmpty()) return false
        return matrices.all { m ->
            val cote = 1L shl m.zoom
            m.largeurTuile == TUILE_PX && m.hauteurTuile == TUILE_PX &&
                m.largeurMatrice == cote && m.hauteurMatrice == cote
        }
    }
}
