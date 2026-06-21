package fr.marculus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HauteurParserTest {

    @Test
    fun `hauteur simple sans decoupe`() {
        val h = HauteurParser.parse("27")
        assertEquals(27.0, h.hauteurTotale)
        assertTrue(h.segments.isEmpty())
    }

    @Test
    fun `hauteur avec decoupe et qualites bois combinees`() {
        val h = HauteurParser.parse("27-6AB4CD")
        assertEquals(27.0, h.hauteurTotale)
        assertEquals(
            listOf(SegmentDecoupe(6.0, "AB"), SegmentDecoupe(4.0, "CD")),
            h.segments,
        )
    }

    @Test
    fun `decoupe avec lettres simples`() {
        val h = HauteurParser.parse("27-6A4B")
        assertEquals(
            listOf(SegmentDecoupe(6.0, "A"), SegmentDecoupe(4.0, "B")),
            h.segments,
        )
    }

    @Test
    fun `accepte la virgule comme separateur decimal`() {
        assertEquals(27.5, HauteurParser.parse("27,5").hauteurTotale)
    }

    @Test
    fun `saisie libre non interpretable - conservee sans planter`() {
        val h = HauteurParser.parse("à revoir")
        assertNull(h.hauteurTotale)
        assertTrue(h.segments.isEmpty())
        assertEquals("à revoir", h.brut)
    }
}
