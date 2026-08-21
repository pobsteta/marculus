package fr.marculus.core.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ReferentielParleTest {

    private fun formes(vararg noms: String) =
        ReferentielParle.essences(noms.toList()).associate { it.nom to it.spoken }

    @Test
    fun `libelle simple sans conflit forme parlee normalisee`() {
        val f = formes("Hêtre", "Douglas")
        assertEquals(listOf("hetre"), f["Hêtre"])
        assertEquals(listOf("douglas"), f["Douglas"])
    }

    @Test
    fun `libelle multi-mots dicte tel quel`() {
        val f = formes("Autres feuillus", "Autres résineux")
        assertEquals(listOf("autres", "feuillus"), f["Autres feuillus"])
        assertEquals(listOf("autres", "resineux"), f["Autres résineux"])
    }

    @Test
    fun `premier mot partage la forme longue est imposee des deux cotes`() {
        val f = formes("Chêne sessile", "Chêne pédonculé")
        assertEquals(listOf("chene", "sessile"), f["Chêne sessile"])
        assertEquals(listOf("chene", "pedoncule"), f["Chêne pédonculé"])
    }

    @Test
    fun `quasi-homophones chene et frene passent a deux mots`() {
        val f = formes("Chêne", "Frêne")
        assertTrue(f.getValue("Chêne").size >= 2, "« chêne » seul reste confondable avec « frêne »")
        assertTrue(f.getValue("Frêne").size >= 2)
        assertEquals("chene", f.getValue("Chêne").first())
        assertEquals("frene", f.getValue("Frêne").first())
        assertNotEquals(f.getValue("Chêne")[1], f.getValue("Frêne")[1])
    }

    @Test
    fun `quasi-homophones orme charme et aulne orme`() {
        val ormeCharme = formes("Orme", "Charme")
        assertTrue(ormeCharme.getValue("Orme").size >= 2)
        assertTrue(ormeCharme.getValue("Charme").size >= 2)

        val aulneOrme = formes("Aulne", "Orme")
        assertTrue(aulneOrme.getValue("Aulne").size >= 2)
        assertTrue(aulneOrme.getValue("Orme").size >= 2)
    }

    @Test
    fun `essences sans risque acoustique restent en un mot`() {
        val f = formes("Hêtre", "Douglas", "Épicéa")
        assertTrue(f.values.all { it.size == 1 }, "aucune forme longue inutile : $f")
    }

    @Test
    fun `le second mot impose vient de l alphabet radio`() {
        val f = formes("Chêne", "Frêne")
        val radio = ReferentielParle.ALPHABET_RADIO.values.toSet()
        assertTrue(f.getValue("Chêne")[1] in radio)
        assertTrue(f.getValue("Frêne")[1] in radio)
    }

    @Test
    fun `codes uniques et formes parlees distinctes`() {
        val ref = ReferentielParle.essences(
            listOf("Chêne sessile", "Chêne pédonculé", "Hêtre", "Autres feuillus", "Autres résineux"),
        )
        assertEquals(ref.size, ref.map { it.codeOnf }.distinct().size, "codes en collision : $ref")
        assertEquals(ref.size, ref.map { it.spoken }.distinct().size, "formes parlées en collision : $ref")
    }

    @Test
    fun `le code ONF de gftools est repris quand il existe`() {
        val ref = ReferentielParle.essences(listOf("Hêtre", "Chêne sessile")).associateBy { it.nom }
        assertEquals("HET", ref.getValue("Hêtre").codeOnf)
        assertEquals("CHS", ref.getValue("Chêne sessile").codeOnf)
    }

    @Test
    fun `essence hors table gftools code de repli lisible`() {
        val ref = ReferentielParle.essences(listOf("Autres feuillus", "Autres résineux")).associateBy { it.nom }
        assertEquals("AUF", ref.getValue("Autres feuillus").codeOnf)
        assertEquals("AUR", ref.getValue("Autres résineux").codeOnf)
    }

    @Test
    fun `libelles vides ou doublons ignores`() {
        val ref = ReferentielParle.essences(listOf("Hêtre", "", "  ", "Hêtre"))
        assertEquals(1, ref.size)
    }

    @Test
    fun `qualites en code lettres alphabet radio`() {
        val q = ReferentielParle.qualites(listOf("A", "B", "AB", "CD")).associate { it.code to it.spoken }
        assertEquals("alpha", q["A"])
        assertEquals("bravo", q["B"])
        assertEquals("alpha bravo", q["AB"])
        assertEquals("charlie delta", q["CD"])
    }

    @Test
    fun `qualites en toutes lettres libelle normalise`() {
        val q = ReferentielParle.qualites(listOf("Sec", "Chablis", "Malade")).associate { it.code to it.spoken }
        assertEquals("sec", q["Sec"])
        assertEquals("chablis", q["Chablis"])
        assertEquals("malade", q["Malade"])
    }

    @Test
    fun `le code de qualite enregistre reste le libelle d origine`() {
        val q = ReferentielParle.qualites(listOf("Chablis"))
        assertEquals("Chablis", q.single().code)
    }

    @Test
    fun `formes parlees de qualites dedoublonnees`() {
        val q = ReferentielParle.qualites(listOf("Sec", "sec", "SEC"))
        assertEquals(2, q.size) // "Sec"/"sec" → même forme ; "SEC" → épelé en radio
        assertEquals("sierra echo charlie", q.last().spoken)
    }
}
