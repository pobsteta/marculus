package fr.marculus.core.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Micro de test : journalise les gestes au lieu d'ouvrir un vrai flux audio. */
private class MicroEspion : MicroPtt {
    val gestes = mutableListOf<String>()
    override fun bip() { gestes += "bip" }
    override fun demarrerEcoute() { gestes += "start" }
    override fun arreterEcoute() { gestes += "stop" }
    val demarrages get() = gestes.count { it == "start" }
    val arrets get() = gestes.count { it == "stop" }
}

class NoyauPttTest {

    @Test
    fun `demarrer bipe puis ouvre le micro`() {
        val micro = MicroEspion()
        assertTrue(NoyauPtt(micro).demarrer())
        assertEquals(listOf("bip", "start"), micro.gestes)
    }

    @Test
    fun `demarrer est idempotent deux appuis, une seule session`() {
        val micro = MicroEspion()
        val ptt = NoyauPtt(micro)
        assertTrue(ptt.demarrer())
        assertFalse(ptt.demarrer(), "le garde doit neutraliser le second déclencheur")
        assertEquals(1, micro.demarrages)
        assertEquals(1, micro.gestes.count { it == "bip" })
    }

    @Test
    fun `arreter sans demarrer est sans effet`() {
        val micro = MicroEspion()
        assertFalse(NoyauPtt(micro).arreter())
        assertTrue(micro.gestes.isEmpty())
    }

    @Test
    fun `deux sources tenues simultanement une seule session d ecoute`() {
        val micro = MicroEspion()
        val ptt = NoyauPtt(micro)
        ptt.demarrer() // source externe enfoncée
        ptt.demarrer() // appui long volume par-dessus
        ptt.arreter() // relâchement de la première source
        assertEquals(1, micro.demarrages)
        assertEquals(1, micro.arrets)
        assertFalse(ptt.actif)
    }

    @Test
    fun `sessions successives`() {
        val micro = MicroEspion()
        val ptt = NoyauPtt(micro)
        repeat(3) { ptt.demarrer(); ptt.arreter() }
        assertEquals(3, micro.demarrages)
        assertEquals(3, micro.arrets)
    }

    @Test
    fun `anti-larsen suspendre puis reprendre garde la session ouverte`() {
        val micro = MicroEspion()
        val ptt = NoyauPtt(micro)
        ptt.demarrer()
        ptt.suspendre() // annonce TTS en cours
        ptt.reprendre() // onDone
        assertTrue(ptt.actif)
        assertEquals(listOf("bip", "start", "stop", "start"), micro.gestes)
    }

    @Test
    fun `reprendre apres relachement ne rouvre pas le micro`() {
        val micro = MicroEspion()
        val ptt = NoyauPtt(micro)
        ptt.demarrer()
        ptt.suspendre()
        ptt.arreter() // l'opérateur a lâché le bouton pendant l'annonce
        ptt.reprendre()
        assertFalse(ptt.actif)
        assertEquals(1, micro.demarrages)
    }
}
