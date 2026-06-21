package fr.marculus.core

import fr.marculus.core.model.AxeClasses
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AxeClassesTest {

    @Test
    fun `genere les classes de 20 a 90 par pas de 5 (comme la feuille de reference)`() {
        val axe = AxeClasses(min = 20, max = 90, pas = 5)
        val classes = axe.classes()
        assertEquals(15, classes.size)
        assertEquals(20, classes.first())
        assertEquals(90, classes.last())
        assertEquals(listOf(20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90), classes)
    }

    @Test
    fun `s'arrete a la derniere classe inferieure ou egale a max quand le pas ne tombe pas juste`() {
        val axe = AxeClasses(min = 20, max = 92, pas = 5)
        assertEquals(90, axe.classes().last())
    }

    @Test
    fun `une seule classe quand min egale max`() {
        assertEquals(listOf(30), AxeClasses(min = 30, max = 30, pas = 5).classes())
    }

    @Test
    fun `refuse un pas nul ou negatif`() {
        assertFailsWith<IllegalArgumentException> { AxeClasses(min = 20, max = 90, pas = 0) }
        assertFailsWith<IllegalArgumentException> { AxeClasses(min = 20, max = 90, pas = -5) }
    }

    @Test
    fun `refuse min superieur a max`() {
        assertFailsWith<IllegalArgumentException> { AxeClasses(min = 90, max = 20, pas = 5) }
    }
}
