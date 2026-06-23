package fr.marculus.core

import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CubageTest {

    private fun proche(attendu: Double, obtenu: Double, tol: Double = 0.05) =
        assertTrue(kotlin.math.abs(attendu - obtenu) <= tol, "attendu ~$attendu, obtenu $obtenu")

    private fun contexte(
        tarif: TarifCubage,
        mode: ModeMesure = ModeMesure.DIAMETRE,
        numero: Int = 0,
        coefForme: Double = 0.5,
    ) = Contexte(
        id = "t", nom = "t", mode = mode, axe = AxeClasses(20, 80, 5),
        essences = emptyList(), tarif = tarif, tarifNumero = numero, coefficientForme = coefForme,
    )

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

    @Test
    fun `emerge trois compartiments coherents`() {
        val v = Cubage.volumesUnitaire(contexte(TarifCubage.EMERGE), "Chêne", 40, "20")
        assertTrue(v.tige > 0.0, "tige > 0, obtenu ${v.tige}")
        assertTrue(v.houppier > 0.0, "houppier > 0, obtenu ${v.houppier}")
        proche(v.tige + v.houppier, v.total, 1e-6)
        assertTrue(v.total > v.tige, "total aérien > tige")
    }

    @Test
    fun `volumesUnitaire schaeffer sans houppier`() {
        val v = Cubage.volumesUnitaire(contexte(TarifCubage.SCHAEFFER_RAPIDE, numero = 8), "Chêne", 40, null)
        assertTrue(v.tige > 0.0)
        assertEquals(0.0, v.houppier)
        assertEquals(v.tige, v.total)
    }

    @Test
    fun `routage volumeUnitaireTige selon le tarif`() {
        val sch = contexte(TarifCubage.SCHAEFFER_RAPIDE, numero = 8)
        // centre de classe 40 (pas 5) = 42,5 cm en diamètre.
        assertEquals(Cubage.volume(TarifCubage.SCHAEFFER_RAPIDE, 8, 42.5), Cubage.volumeUnitaireTige(sch, "Chêne", 40, null))
        // EMERGE sans hauteur → non cubable.
        assertEquals(0.0, Cubage.volumeUnitaireTige(contexte(TarifCubage.EMERGE), "Chêne", 40, null))
        // EMERGE + essence inconnue + hauteur → repli coefficient de forme (> 0).
        assertTrue(Cubage.volumeUnitaireTige(contexte(TarifCubage.EMERGE), "Inconnue", 40, "20") > 0.0)
        // AUCUN → 0.
        assertEquals(0.0, Cubage.volumeUnitaireTige(contexte(TarifCubage.AUCUN), "Chêne", 40, "20"))
    }

    @Test
    fun `surface terriere diametre et circonference`() {
        // Diamètre : centre 42,5 cm → g = π/4·0,425² ≈ 0,1419 m².
        proche(0.1419, Cubage.surfaceTerriereUnitaire(contexte(TarifCubage.AUCUN), 40), 0.001)
        // Circonférence : centre 42,5 cm de circonférence → D = 42,5/π → g ≈ 0,01437 m².
        proche(0.01437, Cubage.surfaceTerriereUnitaire(contexte(TarifCubage.AUCUN, ModeMesure.CIRCONFERENCE), 40), 0.0005)
    }

    @Test
    fun `code essence ONF`() {
        assertEquals("HET", Cubage.codeEssence("Hêtre"))
        assertEquals("DOU", Cubage.codeEssence("Douglas"))
        assertNull(Cubage.codeEssence("Xyzzy"))
    }

    @Test
    fun `volume forme respecte le coefficient`() {
        proche(2.0 * Cubage.volumeForme(40.0, 20.0, 0.5), Cubage.volumeForme(40.0, 20.0, 1.0), 1e-6)
        assertEquals(0.0, Cubage.volumeForme(0.0, 20.0))
        assertEquals(0.0, Cubage.volumeForme(40.0, 0.0))
    }
}
