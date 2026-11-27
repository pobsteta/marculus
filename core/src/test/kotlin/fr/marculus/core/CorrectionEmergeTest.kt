package fr.marculus.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Non-régression de la correction EMERGE (brief `docs/specs/2026-11-27-correction-emerge-volumes.md`) :
 * les valeurs « publié » et « attendu » sont celles du brief (tableaux « Arbres tests »), écrites avant
 * le code qu'elles contrôlent. Circonférences C130 en cm, hauteurs en m, volumes en m³.
 */
class CorrectionEmergeTest {

    private fun aerien(essence: String, c130: Double, h: Double): Double =
        assertNotNull(Cubage.volumesEmerge(essence, c130, h), "$essence : coefficients EMERGE attendus").volumeAerienTotal

    private fun dansLes(pourcent: Double, publie: Double, obtenu: Double, msg: String) =
        assertTrue(abs(obtenu - publie) <= pourcent / 100.0 * publie, "$msg : publié $publie, obtenu $obtenu (écart > $pourcent %)")

    // --- le volume aérien total (formTot · volCyl) contre le publié, ±2 % --------------------------------

    @Test
    fun `hetre 62,8 cm 10 m - volume aerien total`() {
        dansLes(2.0, 0.2332, aerien("Hêtre", 62.8, 10.0), "hêtre 62,8 / 10")
        assertEquals(0.2365, aerien("Hêtre", 62.8, 10.0), 5e-4) // « après » du brief
    }

    @Test
    fun `Chene 125 cm 20 m rattache au chene sessile - volume aerien total`() {
        dansLes(2.0, 1.6099, aerien("Chêne", 125.0, 20.0), "« Chêne » 125 / 20")
        assertEquals(1.5941, aerien("Chêne", 125.0, 20.0), 5e-4)
    }

    @Test
    fun `quatre arbres de la mission 003 - volume aerien total`() {
        dansLes(2.0, 0.034809, aerien("Hêtre", 20.0, 2.0), "A hêtre")
        dansLes(2.0, 0.029909, aerien("Épicéa commun", 25.0, 3.0), "B épicéa")
        dansLes(2.0, 0.039241, aerien("Chêne sessile", 30.0, 5.0), "C chêne sessile")
        dansLes(2.0, 0.135670, aerien("Pin sylvestre", 50.0, 10.0), "D pin sylvestre")
    }

    @Test
    fun `chene rouge saisi explicitement garde sa propre ligne`() {
        assertEquals(1.4835, aerien("Chêne rouge", 125.0, 20.0), 5e-4)
        dansLes(2.0, 1.4677, aerien("Chêne rouge", 125.0, 20.0), "chêne rouge 125 / 20")
    }

    // --- les quatre volumes, chacun sous son nom ----------------------------------------------------------

    @Test
    fun `hetre 62,8 cm 10 m - bois fort tige houppier bois fort total et aerien total`() {
        val v = assertNotNull(Cubage.volumesEmerge("Hêtre", 62.8, 10.0))
        assertEquals(0.1557, v.tige, 5e-4)
        assertEquals(0.0251, v.houppier, 5e-4)
        assertEquals(0.1808, v.boisFortTotal, 5e-4)
        assertEquals(v.tige + v.houppier, v.boisFortTotal, 1e-12)
        assertEquals(0.2365, v.volumeAerienTotal, 5e-4)
        assertTrue(v.volumeAerienTotal > v.boisFortTotal, "le menu bois ne compte pas dans le bois fort")
    }

    @Test
    fun `Chene 125 cm 20 m - tige et houppier du chene sessile`() {
        val v = assertNotNull(Cubage.volumesEmerge("Chêne", 125.0, 20.0))
        assertEquals(1.2574, v.tige, 5e-4)
        assertEquals(0.1822, v.houppier, 5e-4)
        assertEquals(1.4395, v.boisFortTotal, 5e-4)
    }

    // --- rattachement explicite des noms d'essence --------------------------------------------------------

    @Test
    fun `Chene ne retombe plus sur le chene rouge`() {
        val generique = assertNotNull(Cubage.volumesEmerge("Chêne", 125.0, 20.0))
        val rouge = assertNotNull(Cubage.volumesEmerge("Chêne rouge", 125.0, 20.0))
        val sessile = assertNotNull(Cubage.volumesEmerge("Chêne sessile", 125.0, 20.0))
        assertNotEquals(rouge.tige, generique.tige, 1e-6)
        assertEquals(sessile.tige, generique.tige, 1e-12)
        assertEquals(sessile.volumeAerienTotal, generique.volumeAerienTotal, 1e-12)
        assertEquals("CHS", Cubage.codeEssence("Chêne")) // le code suit le même rattachement
        assertEquals("CHR", Cubage.codeEssence("Chêne rouge"))
    }

    @Test
    fun `les essences par defaut de l'application restent couvertes`() {
        for (e in listOf("Chêne", "Hêtre", "Sapin", "Épicéa", "Douglas", "Pin sylvestre", "Autres feuillus", "Autres résineux")) {
            assertTrue(Cubage.estCouverteEmerge(e), "$e doit avoir des coefficients EMERGE")
        }
    }

    @Test
    fun `un nom generique inconnu ne retombe sur aucune essence`() {
        for (e in listOf("Arbre", "Essence inconnue", "Xyzzy", "Chen", "Hetres", "Pins", "")) {
            assertNull(Cubage.volumesEmerge(e, 125.0, 20.0), "« $e » ne doit pas recevoir une essence arbitraire")
            assertFalse(Cubage.estCouverteEmerge(e), "« $e »")
            assertEquals(0.0, Cubage.volumeEmergeTige(e, 125.0, 20.0))
        }
        assertNull(Cubage.codeEssence("Arbre"))
        assertNull(Cubage.codeEssence("Hetres"))
    }

    @Test
    fun `sous la hauteur de 1,4 m les quatre volumes sont nuls`() {
        val v = assertNotNull(Cubage.volumesEmerge("Hêtre", 62.8, 1.4))
        assertEquals(0.0, v.volumeAerienTotal)
        assertEquals(0.0, v.boisFortTotal)
    }
}
