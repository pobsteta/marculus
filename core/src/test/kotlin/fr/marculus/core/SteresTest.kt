package fr.marculus.core

import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mission 017 : conversion m³ -> stères et condition d'affichage.
 * Signatures imposées (objet `fr.marculus.core.Steres`) :
 *   fun actif(contexte: Contexte): Boolean
 *   fun deM3(volumeM3: Double, contexte: Contexte): Double   // non arrondi
 */
class SteresTest {

    private val base = Contexte(
        id = "c1", nom = "Affouage", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
        essences = listOf(EssenceColonne("Chêne", 0, 0)),
        affouage = true, volumeMaxLotM3 = 10.0,
    )

    @Test
    fun `10 m3 au coefficient par defaut 1,5 font 15 steres`() {
        assertEquals(15.0, Steres.deM3(10.0, base), 1e-12)
        assertEquals("15,00", String.format(Locale.FRANCE, "%.2f", Steres.deM3(10.0, base)))
    }

    @Test
    fun `le coefficient regle sur le contexte est respecte`() {
        val c = base.copy(steresParM3 = 1.4)
        assertEquals(14.0, Steres.deM3(10.0, c), 1e-12)
        assertEquals(0.0, Steres.deM3(0.0, c), 0.0)
    }

    @Test
    fun `la conversion ne s'arrondit pas elle-meme`() {
        // 0,0049 m3 x 1,5 = 0,00735 st (affiche 0,01) ; en arrondissant d'abord les m3 (0,00) on aurait 0.
        assertEquals(0.00735, Steres.deM3(0.0049, base), 1e-12)
        assertEquals(18.5055, Steres.deM3(12.337, base), 1e-9)
    }

    @Test
    fun `affouage avec maximum positif et coefficient positif active les steres`() {
        assertTrue(Steres.actif(base))
        assertTrue(Steres.actif(base.copy(volumeMaxLotM3 = 0.01)))
    }

    @Test
    fun `affouage sans maximum n'active pas les steres`() {
        assertFalse(Steres.actif(base.copy(volumeMaxLotM3 = 0.0)))
    }

    @Test
    fun `maximum sans affouage n'active pas les steres`() {
        assertFalse(Steres.actif(base.copy(affouage = false, volumeMaxLotM3 = 10.0)))
    }

    @Test
    fun `contexte ordinaire n'active pas les steres`() {
        assertFalse(Steres.actif(base.copy(affouage = false, volumeMaxLotM3 = 0.0)))
    }

    @Test
    fun `coefficient nul ou negatif n'active pas les steres`() {
        assertFalse(Steres.actif(base.copy(steresParM3 = 0.0)))
        assertFalse(Steres.actif(base.copy(steresParM3 = -1.5)))
    }
}
