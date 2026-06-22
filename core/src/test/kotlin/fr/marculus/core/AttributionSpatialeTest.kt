package fr.marculus.core

import fr.marculus.core.model.Position
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AttributionSpatialeTest {

    // Carré unité [0,1]x[0,1] (longitude, latitude).
    private val carre = listOf(
        listOf(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(1.0, 1.0),
            Position(1.0, 0.0),
            Position(0.0, 0.0),
        ),
    )

    @Test
    fun `point au centre est dedans`() {
        assertTrue(AttributionSpatiale.contient(carre, Position(0.5, 0.5)))
    }

    @Test
    fun `point hors du carre est dehors`() {
        assertFalse(AttributionSpatiale.contient(carre, Position(0.5, 1.5)))
        assertFalse(AttributionSpatiale.contient(carre, Position(2.0, 2.0)))
    }

    @Test
    fun `trou central exclu (regle pair-impair)`() {
        // Anneau extérieur 0..10 + anneau intérieur (trou) 4..6.
        val avecTrou = listOf(
            listOf(
                Position(0.0, 0.0), Position(0.0, 10.0),
                Position(10.0, 10.0), Position(10.0, 0.0), Position(0.0, 0.0),
            ),
            listOf(
                Position(4.0, 4.0), Position(4.0, 6.0),
                Position(6.0, 6.0), Position(6.0, 4.0), Position(4.0, 4.0),
            ),
        )
        assertTrue(AttributionSpatiale.contient(avecTrou, Position(1.0, 1.0))) // dans l'anneau, hors trou
        assertFalse(AttributionSpatiale.contient(avecTrou, Position(5.0, 5.0))) // dans le trou
    }
}
