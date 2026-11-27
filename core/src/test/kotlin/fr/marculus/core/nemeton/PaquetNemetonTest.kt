package fr.marculus.core.nemeton

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaquetNemetonTest {

    // ---- le paquet embarqué est le paquet livré, octet pour octet ----

    @Test
    fun `les trois fichiers embarques ont l'empreinte du manifeste`() {
        assertEquals(PaquetDeTest.hashDuManifeste("constantes.csv"), PaquetDeTest.sha256(PaquetDeTest.constantesOctets))
        assertEquals(PaquetDeTest.hashDuManifeste("jeu_controle.csv"), PaquetDeTest.sha256(PaquetDeTest.jeuOctets))
    }

    @Test
    fun `empreintes du paquet v0_2_2 provisoire`() {
        // SHA-256 relus sur ~/dev/tarif-nemeton/livrable/ le 2026-11-27 (sha256sum).
        assertEquals("b724ee83267c373402af40b1bee574ed471e797a45b687cee44b100b0882cf8a", PaquetDeTest.sha256(PaquetDeTest.constantesOctets))
        assertEquals("0b06dd1f39ff22d608fcfc4879679fc5c9dbda6d42530c42fedea9412e731341", PaquetDeTest.sha256(PaquetDeTest.jeuOctets))
        assertEquals("ea6d82df144957826da9ddcc2af79bb29db5684eaf2bad692dc5c7716ef7dfcb", PaquetDeTest.sha256(PaquetDeTest.manifesteOctets))
    }

    @Test
    fun `le jeu de controle compte 1032 lignes`() {
        assertEquals(1032, PaquetDeTest.jeu.size)
        assertEquals(1032, PaquetDeTest.jeu.map { listOf(it.code, it.d, it.h, it.sortie) }.toSet().size, "doublon dans le jeu")
    }

    // ---- chargement : empreinte ----

    @Test
    fun `le paquet intact se charge et porte la version du manifeste`() {
        assertEquals("0.2.2-provisoire", PaquetDeTest.charger().version)
    }

    @Test
    fun `un octet change dans constantes refuse le chargement`() {
        val altere = PaquetDeTest.constantes.replaceFirst("-0.5308", "-0.5309")
        assertTrue(altere != PaquetDeTest.constantes)
        assertEquals(PaquetDeTest.constantes.length, altere.length)
        assertFails { PaquetNemeton.charger(altere, PaquetDeTest.manifeste) }
    }

    @Test
    fun `un octet ajoute en fin de constantes refuse le chargement`() {
        assertFails { PaquetNemeton.charger(PaquetDeTest.constantes + "\n", PaquetDeTest.manifeste) }
    }

    @Test
    fun `une ligne retiree de constantes refuse le chargement`() {
        val sansDerniere = PaquetDeTest.constantes.trimEnd('\n').substringBeforeLast('\n') + "\n"
        assertFails { PaquetNemeton.charger(sansDerniere, PaquetDeTest.manifeste) }
    }

    @Test
    fun `constantes vide refuse le chargement`() {
        assertFails { PaquetNemeton.charger("", PaquetDeTest.manifeste) }
    }

    @Test
    fun `une empreinte fausse dans le manifeste refuse le chargement`() {
        val vrai = PaquetDeTest.hashDuManifeste("constantes.csv")
        val faux = PaquetDeTest.manifeste.replace(vrai, "0".repeat(64))
        assertTrue(faux != PaquetDeTest.manifeste)
        assertFails { PaquetNemeton.charger(PaquetDeTest.constantes, faux) }
    }

    @Test
    fun `la version est celle du manifeste`() {
        val m = PaquetDeTest.manifeste.replace("\"version\": \"0.2.2-provisoire\"", "\"version\": \"9.9.9-essai\"")
        assertTrue(m != PaquetDeTest.manifeste)
        val tarif = PaquetNemeton.charger(PaquetDeTest.constantes, m)
        assertEquals("9.9.9-essai", tarif.version)
        assertEquals("9.9.9-essai", assertNotNull(tarif.sorties("9", 30.0, 25.0)).version)
    }

    // ---- codes non couverts ----

    @Test
    fun `les codes 68 et 75 ne sont pas couverts`() {
        val t = PaquetDeTest.charger()
        assertNull(t.sorties("68", 30.0, 25.0))
        assertNull(t.sorties("75", 30.0, 25.0))
    }

    @Test
    fun `un code inconnu ou vide n'est pas couvert`() {
        val t = PaquetDeTest.charger()
        for (code in listOf("1", "4", "999", "", "hetre")) assertNull(t.sorties(code, 30.0, 25.0), "code '$code'")
    }

    @Test
    fun `le code non couvert reste nul hors domaine aussi`() {
        assertNull(PaquetDeTest.charger().sorties("68", 300.0, 2.0))
    }

    // ---- hors domaine : calcul et alerte, pas de refus ----
    // Domaines (constantes.csv) : feuillu = [0,9 x D_min_calage ; 1,1 x D_max_calage] et [H_min_calage - 2 ; H_max_calage + 2]
    // (hêtre : D 8-76, H 8-37) ; résineux = H/D de 40 à 160 (H en m, D en m : D 30 cm, H 12 m à 48 m).

    @Test
    fun `hetre dans le domaine n'est pas hors domaine`() {
        val s = assertNotNull(PaquetDeTest.charger().sorties("9", 40.0, 25.0))
        assertFalse(s.horsDomaine)
    }

    @Test
    fun `hetre trop gros est hors domaine mais cube`() {
        val s = assertNotNull(PaquetDeTest.charger().sorties("9", 120.0, 25.0))
        assertTrue(s.horsDomaine)
        assertCalcule(s)
    }

    @Test
    fun `hetre trop haut est hors domaine mais cube`() {
        val s = assertNotNull(PaquetDeTest.charger().sorties("9", 40.0, 60.0))
        assertTrue(s.horsDomaine)
        assertCalcule(s)
    }

    @Test
    fun `hetre trop bas est hors domaine mais cube`() {
        val s = assertNotNull(PaquetDeTest.charger().sorties("9", 40.0, 2.0))
        assertTrue(s.horsDomaine)
        assertCalcule(s)
    }

    @Test
    fun `chene trop fin est hors domaine mais cube`() {
        // D_min_calage 12 : 0,9 x 12 = 10,8 cm ; D = 7,5 est en dessous.
        val s = assertNotNull(PaquetDeTest.charger().sorties("2", 7.5, 14.0))
        assertTrue(s.horsDomaine)
        assertCalcule(s)
    }

    @Test
    fun `epicea dans le domaine n'est pas hors domaine`() {
        // H/D = 25 m / 0,30 m = 83
        assertFalse(assertNotNull(PaquetDeTest.charger().sorties("62", 30.0, 25.0)).horsDomaine)
    }

    @Test
    fun `epicea trop elance est hors domaine mais cube`() {
        // H/D = 30 m / 0,10 m = 300 > 160
        val s = assertNotNull(PaquetDeTest.charger().sorties("62", 10.0, 30.0))
        assertTrue(s.horsDomaine)
        assertCalcule(s)
    }

    @Test
    fun `epicea trop trapu est hors domaine mais cube`() {
        // H/D = 5 m / 0,40 m = 12,5 < 40
        val s = assertNotNull(PaquetDeTest.charger().sorties("62", 40.0, 5.0))
        assertTrue(s.horsDomaine)
        assertCalcule(s)
    }

    @Test
    fun `autres feuillus hors enveloppe est hors domaine mais cube`() {
        for (code in listOf("29", "49")) {
            val s = assertNotNull(PaquetDeTest.charger().sorties(code, 150.0, 25.0), code)
            assertTrue(s.horsDomaine, code)
            assertCalcule(s)
        }
    }

    @Test
    fun `le hors domaine garde la meme liste de sorties que dans le domaine`() {
        val t = PaquetDeTest.charger()
        assertEquals(t.sorties("9", 40.0, 25.0)!!.valeurs.keys, t.sorties("9", 120.0, 25.0)!!.valeurs.keys)
        assertEquals(t.sorties("62", 30.0, 25.0)!!.valeurs.keys, t.sorties("62", 10.0, 30.0)!!.valeurs.keys)
    }

    private fun assertCalcule(s: SortiesNemeton) {
        assertTrue(s.valeurs.isNotEmpty())
        for ((k, v) in s.valeurs) assertTrue(v.isFinite() && v > 0.0, "$k = $v")
    }

    // ---- traçabilité : essence réellement utilisée, version ----

    @Test
    fun `l'essence est le libelle de la ligne utilisee, un code par ligne`() {
        val t = PaquetDeTest.charger()
        val attendu = mapOf(
            "2" to "Quercus_spec", "3" to "Quercus_spec", "9" to "Fagus_sylvatica", "11" to "Carpinus_betulus",
            "12" to "Betula_spec", "15" to "Acer_pseudoplatanus", "17" to "Fraxinus_excelsior",
            "29" to "Autres_feuillus", "49" to "Autres_feuillus", "52" to "Pinus_sylvestris",
            "61" to "Abies_alba", "62" to "Picea_abies", "63" to "Larix_decidua", "64" to "Pseudotsuga_menziesii",
        )
        for ((code, essence) in attendu) {
            val s = assertNotNull(t.sorties(code, 30.0, 25.0), code)
            assertEquals(essence, s.essence, code)
            assertEquals("0.2.2-provisoire", s.version, code)
        }
    }

    @Test
    fun `les deux codes de chene donnent les memes valeurs`() {
        val t = PaquetDeTest.charger()
        assertEquals(t.sorties("2", 40.0, 25.0)!!.valeurs, t.sorties("3", 40.0, 25.0)!!.valeurs)
    }

    @Test
    fun `une sortie absente n'est jamais zero`() {
        val t = PaquetDeTest.charger()
        // feuillu : pas de bois fort tige ; résineux : pas de bois fort total ; autres feuillus : ni biomasse ni carbone.
        assertFalse("V_bois_fort_tige_m3" in t.sorties("9", 40.0, 25.0)!!.valeurs)
        assertFalse("V_bois_fort_total_m3" in t.sorties("62", 30.0, 25.0)!!.valeurs)
        val af = t.sorties("29", 40.0, 25.0)!!.valeurs
        assertEquals(setOf("V_aerien_total_m3"), af.keys)
    }
}
