package fr.marculus.core.nemeton

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests unitaires du moteur (mission 039), en plus des tests d'acceptation de la mission 038. */
class MoteurNemetonTest {

    private val tarif by lazy { PaquetDeTest.charger() }

    // ---- lecteur CSV ----

    @Test
    fun `csv - guillemets, virgules et guillemets doubles dans un champ`() {
        val l = LectureCsvNemeton.lire("a,b,c\n1,\"x, y\",\"il dit \"\"oui\"\"\"\n2,,\n")
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "x, y", "il dit \"oui\""), listOf("2", "", "")), l)
    }

    @Test
    fun `csv - CRLF et derniere ligne sans fin de ligne`() {
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), LectureCsvNemeton.lire("a,b\r\n1,2"))
    }

    @Test
    fun `csv - guillemet non ferme refuse`() {
        assertFailsWith<IllegalArgumentException> { LectureCsvNemeton.lire("a,\"b\n") }
    }

    @Test
    fun `csv - les constantes embarquees ont 248 lignes de 7 champs`() {
        val l = LectureCsvNemeton.lire(PaquetDeTest.constantes)
        assertEquals(248, l.size)
        assertTrue(l.all { it.size == 7 })
    }

    // ---- lecteur JSON ----

    @Test
    fun `json - objets, tableaux, echappements, nombres`() {
        val v = LectureJsonNemeton.lire("""{"a": [1, -2.5e1, true, false, null], "b": "x\"é\n", "c": {}}""") as Map<*, *>
        assertEquals(listOf(1.0, -25.0, true, false, null), v["a"])
        assertEquals("x\"é\n", v["b"])
        assertEquals(emptyMap<String, Any?>(), v["c"])
    }

    @Test
    fun `json - malforme refuse`() {
        for (t in listOf("{", "{\"a\" 1}", "[1,]", "{\"a\":1} x", "\"abc", "{\"a\":1,\"a\":2}")) {
            assertFailsWith<IllegalArgumentException>(t) { LectureJsonNemeton.lire(t) }
        }
    }

    @Test
    fun `json - la version lue est celle de premier niveau, pas une version imbriquee`() {
        val m = PaquetDeTest.manifeste.replaceFirst("\"name\": \"constantes\",", "\"name\": \"constantes\", \"version\": \"0.0.0-leurre\",")
        assertTrue(m != PaquetDeTest.manifeste)
        assertEquals("0.2.2-provisoire", PaquetNemeton.charger(PaquetDeTest.constantes, m).version)
    }

    // ---- manifeste ----

    @Test
    fun `manifeste sans hash de constantes refuse`() {
        val m = PaquetDeTest.manifeste.replace("\"path\": \"constantes.csv\"", "\"path\": \"autre.csv\"")
        assertFailsWith<IllegalArgumentException> { PaquetNemeton.charger(PaquetDeTest.constantes, m) }
    }

    @Test
    fun `manifeste sans version refuse`() {
        val m = PaquetDeTest.manifeste.replace("\"version\": \"0.2.2-provisoire\"", "\"versio\": \"0.2.2-provisoire\"")
        assertFailsWith<IllegalArgumentException> { PaquetNemeton.charger(PaquetDeTest.constantes, m) }
    }

    @Test
    fun `le refus d'empreinte est une IllegalStateException`() {
        assertFailsWith<IllegalStateException> { PaquetNemeton.charger(PaquetDeTest.constantes + "x", PaquetDeTest.manifeste) }
    }

    // ---- validation des constantes au chargement (sans empreinte : construire) ----

    private fun sansLigne(predicat: (String) -> Boolean): String =
        PaquetDeTest.constantes.lines().filterNot(predicat).joinToString("\n")

    @Test
    fun `constantes completes acceptees par construire`() {
        assertEquals(14, PaquetNemeton.construire("v", PaquetDeTest.constantes).codes.size)
    }

    @Test
    fun `constante d'essence manquante refuse le chargement`() {
        val c = sansLigne { it.startsWith("Picea_abies,62,profil_tige_resineux,sx,") }
        assertFailsWith<IllegalArgumentException> { PaquetNemeton.construire("v", c) }
    }

    @Test
    fun `constante de groupe manquante refuse le chargement`() {
        val c = sansLigne { it.startsWith("conifere,,integration_tige_resineux,n_points,") }
        assertFailsWith<IllegalArgumentException> { PaquetNemeton.construire("v", c) }
    }

    @Test
    fun `infradensite presente sans facteur arbre entier refuse le chargement`() {
        val c = sansLigne { it.startsWith("Fagus_sylvatica,9,facteur_arbre_entier,") }
        assertFailsWith<IllegalArgumentException> { PaquetNemeton.construire("v", c) }
    }

    @Test
    fun `constante en double refuse le chargement`() {
        val l = PaquetDeTest.constantes.lines().filter { it.isNotEmpty() }
        val c = (l + l[1]).joinToString("\n", postfix = "\n")
        assertFailsWith<IllegalArgumentException> { PaquetNemeton.construire("v", c) }
    }

    @Test
    fun `deux libelles pour un code refusent le chargement`() {
        val c = PaquetDeTest.constantes.replaceFirst("Fagus_sylvatica,9,volume_feuillu,a,", "Hetre,9,volume_feuillu,a,")
        assertFailsWith<IllegalArgumentException> { PaquetNemeton.construire("v", c) }
    }

    @Test
    fun `valeur non numerique refuse le chargement`() {
        val c = PaquetDeTest.constantes.replaceFirst("-0.5308", "abc")
        assertFailsWith<IllegalArgumentException> { PaquetNemeton.construire("v", c) }
    }

    @Test
    fun `en-tete faux refuse le chargement`() {
        val c = PaquetDeTest.constantes.replaceFirst("essence,code_ifn", "espece,code_ifn")
        assertFailsWith<IllegalArgumentException> { PaquetNemeton.construire("v", c) }
    }

    @Test
    fun `sans fractions de decoupe le bois fort total disparait`() {
        val c = sansLigne { it.startsWith("Fagus_sylvatica,9,fraction_decoupe,") || it.startsWith("Fagus_sylvatica,9,correction_menu_bois,") }
        val s = assertNotNull(PaquetNemeton.construire("v", c).sorties("9", 40.0, 25.0))
        assertEquals(setOf("V_aerien_total_m3", "biomasse_aerienne_t", "carbone_aerien_t"), s.valeurs.keys)
    }

    // ---- calcul ----

    @Test
    fun `hetre 30 cm 25 m recalcule a la main`() {
        // V = π/4 · 0,30² · 25 · exp(a + 0 + 0 + 0,5 σ²), a = -0,5308, σ = 0,1482 (constantes.csv l. 2 et 5).
        val v = PI / 4 * 0.09 * 25 * exp(-0.5308 + 0.5 * 0.1482 * 0.1482)
        val s = assertNotNull(tarif.sorties("9", 30.0, 25.0))
        assertEquals(v, s.valeurs.getValue("V_aerien_total_m3"), 1e-12)
        assertTrue(abs(v - 1.0508) < 1e-3, "V = $v")
        // biomasse : ρ = 613 · 0,923 (D = D_ref, pente sans effet) ; carbone 0,488.
        val b = v * 613 * 0.923 / 1000
        assertEquals(b, s.valeurs.getValue("biomasse_aerienne_t"), 1e-12)
        assertEquals(b * 0.488, s.valeurs.getValue("carbone_aerien_t"), 1e-12)
    }

    @Test
    fun `les sorties sont dans l'ordre du jeu de controle`() {
        assertEquals(
            listOf("V_aerien_total_m3", "V_bois_fort_total_m3", "biomasse_aerienne_t", "carbone_aerien_t"),
            tarif.sorties("9", 30.0, 25.0)!!.valeurs.keys.toList(),
        )
        assertEquals(
            listOf("V_aerien_total_m3", "V_bois_fort_tige_m3", "biomasse_aerienne_t", "carbone_aerien_t"),
            tarif.sorties("62", 30.0, 25.0)!!.valeurs.keys.toList(),
        )
    }

    @Test
    fun `coherence tige, bois fort et aerien`() {
        for (code in tarif.codes) for (d in listOf(10.0, 30.0, 60.0)) {
            val v = tarif.sorties(code, d, 20.0)!!.valeurs
            val aer = v.getValue("V_aerien_total_m3")
            v["V_bois_fort_total_m3"]?.let { assertTrue(it <= aer, "$code D=$d") }
            v["V_bois_fort_tige_m3"]?.let { assertTrue(it <= aer, "$code D=$d") }
        }
    }

    @Test
    fun `bornes du domaine incluses`() {
        // hêtre : D 8×0,9 = 7,2 à 76×1,1 = 83,6 ; H 6 à 39. Épicéa : H/D de 40 à 160.
        assertFalse(tarif.sorties("9", 7.2, 6.0)!!.horsDomaine)
        assertFalse(tarif.sorties("9", 83.6, 39.0)!!.horsDomaine)
        assertTrue(tarif.sorties("9", 83.7, 39.0)!!.horsDomaine)
        assertFalse(tarif.sorties("62", 25.0, 10.0)!!.horsDomaine)
        assertFalse(tarif.sorties("62", 25.0, 40.0)!!.horsDomaine)
        assertTrue(tarif.sorties("62", 25.0, 40.1)!!.horsDomaine)
    }

    // ---- entrées invalides ----

    @Test
    fun `D ou H nul, negatif ou non fini refuse`() {
        for (code in listOf("9", "62")) for ((d, h) in listOf(0.0 to 20.0, -5.0 to 20.0, 30.0 to 0.0, 30.0 to -1.0, Double.NaN to 20.0, 30.0 to Double.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException>("$code D=$d H=$h") { tarif.sorties(code, d, h) }
        }
    }

    @Test
    fun `resineux sous 1,301 m refuse, feuillu calcule`() {
        // Épicéa D = 10, H = 1,3 : la référence Python rendait V = 0,013 m³ sans sens ; le moteur refuse.
        assertFailsWith<IllegalArgumentException> { tarif.sorties("62", 10.0, 1.3) }
        assertFailsWith<IllegalArgumentException> { tarif.sorties("62", 10.0, 1.301) }
        assertNotNull(tarif.sorties("62", 10.0, 1.302))
        assertNotNull(tarif.sorties("9", 10.0, 1.3))
    }

    @Test
    fun `code non couvert reste nul meme avec des entrees invalides`() {
        assertNull(tarif.sorties("68", -1.0, 0.0))
    }
}
