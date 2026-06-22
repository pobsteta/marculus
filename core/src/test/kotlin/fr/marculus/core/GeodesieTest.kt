package fr.marculus.core

import fr.marculus.core.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeodesieTest {

    @Test
    fun `carre d'environ 1 km de cote proche de 100 ha`() {
        // ~1 km à la latitude de Dijon (~47°). 1° lon ≈ 111320*cos(47) m ; 0,0132° ≈ 1 km.
        val dLon = 0.0132
        val dLat = 0.00899 // ≈ 1 km en latitude
        val lat = 47.0
        val lon = 5.0
        val carre = listOf(
            listOf(
                Position(lat, lon),
                Position(lat + dLat, lon),
                Position(lat + dLat, lon + dLon),
                Position(lat, lon + dLon),
                Position(lat, lon),
            ),
        )
        val ha = Geodesie.aireHa(carre)
        assertTrue(ha in 90.0..110.0, "Aire attendue ~100 ha, obtenue $ha")
    }

    @Test
    fun `trou central reduit l'aire`() {
        fun anneau(x0: Double, y0: Double, c: Double) = listOf(
            Position(y0, x0), Position(y0 + c, x0),
            Position(y0 + c, x0 + c), Position(y0, x0 + c), Position(y0, x0),
        )
        val plein = listOf(anneau(5.0, 47.0, 0.01))
        // Trou orienté en sens inverse (norme OGC) → soustrait de l'aire extérieure.
        val avecTrou = listOf(anneau(5.0, 47.0, 0.01), anneau(5.004, 47.004, 0.002).reversed())
        assertTrue(Geodesie.aireM2(avecTrou) < Geodesie.aireM2(plein))
    }

    @Test
    fun `polygone degenere = 0`() {
        assertEquals(0.0, Geodesie.aireM2(listOf(listOf(Position(47.0, 5.0), Position(47.0, 5.0)))))
    }
}
