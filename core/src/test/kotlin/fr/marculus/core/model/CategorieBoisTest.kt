package fr.marculus.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CategorieBoisTest {

    @Test
    fun `classement par diametre`() {
        assertEquals(CategorieBois.PB, CategorieBois.pour(20, ModeMesure.DIAMETRE))
        assertEquals(CategorieBois.PB, CategorieBois.pour(25, ModeMesure.DIAMETRE))
        assertEquals(CategorieBois.BM, CategorieBois.pour(30, ModeMesure.DIAMETRE))
        assertEquals(CategorieBois.BM, CategorieBois.pour(45, ModeMesure.DIAMETRE))
        assertEquals(CategorieBois.GB, CategorieBois.pour(50, ModeMesure.DIAMETRE))
        assertEquals(CategorieBois.GB, CategorieBois.pour(65, ModeMesure.DIAMETRE))
        assertEquals(CategorieBois.TGB, CategorieBois.pour(70, ModeMesure.DIAMETRE))
        assertEquals(CategorieBois.TGB, CategorieBois.pour(90, ModeMesure.DIAMETRE))
    }

    @Test
    fun `classement par circonference (converti en diametre)`() {
        // 90 cm de circonférence ≈ 28,6 cm de diamètre → bois moyens.
        assertEquals(CategorieBois.BM, CategorieBois.pour(90, ModeMesure.CIRCONFERENCE))
        // 60 cm de circonférence ≈ 19 cm de diamètre → petits bois.
        assertEquals(CategorieBois.PB, CategorieBois.pour(60, ModeMesure.CIRCONFERENCE))
    }
}
