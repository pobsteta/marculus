package fr.marculus.core

import fr.marculus.core.model.TarifCubage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CubageTest {

    private fun proche(attendu: Double, obtenu: Double, tol: Double = 0.05) =
        assertTrue(kotlin.math.abs(attendu - obtenu) <= tol, "attendu ~$attendu, obtenu $obtenu")

    @Test
    fun `rapide conforme aux tables ONF`() {
        proche(0.9, Cubage.volume(TarifCubage.SCHAEFFER_RAPIDE, 1, 45.0)) // M=0,9
        proche(17.1, Cubage.volume(TarifCubage.SCHAEFFER_RAPIDE, 20, 100.0))
        proche(1.8, Cubage.volume(TarifCubage.SCHAEFFER_RAPIDE, 10, 45.0))
    }

    @Test
    fun `lent conforme aux tables ONF`() {
        proche(0.9, Cubage.volume(TarifCubage.SCHAEFFER_LENT, 1, 45.0))
        proche(14.8, Cubage.volume(TarifCubage.SCHAEFFER_LENT, 20, 100.0))
    }

    @Test
    fun `emerge bois fort tige plausible et repli forme`() {
        val v = Cubage.volumeEmergeTige("Chêne", 125.0, 20.0) // ~40 cm DBH, 20 m
        assertTrue(v in 0.9..1.8, "EMERGE bois fort tige chêne attendu ~1,3 m³, obtenu $v")
        assertEquals(0.0, Cubage.volumeEmergeTige("Essence inconnue", 125.0, 20.0)) // non couverte
        assertTrue(Cubage.volumeForme(40.0, 20.0) > 0.0)
    }

    @Test
    fun `aucun tarif donne zero et pas de volume negatif`() {
        assertEquals(0.0, Cubage.volume(TarifCubage.AUCUN, 10, 50.0))
        assertEquals(0.0, Cubage.volume(TarifCubage.SCHAEFFER_RAPIDE, 10, 8.0)) // D<10 → négatif → 0
    }
}
