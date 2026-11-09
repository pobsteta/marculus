package io.github.pobsteta.marculus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ResumeMarsyncTest {
    private fun tige(op: String? = null, action: String = "PLUS") =
        """{"uuid":"u","action":"$action"${op?.let { ",\"operateur\":\"$it\"" } ?: ""}}"""

    @Test
    fun `un contexte nomme avec operateur et trois tiges`() {
        val json = """{"version":1,"contextes":[{"id":"a","nom":"Affouage Les Hauts","operateur":"Pascal"}],"tiges":[${tige("Pascal")},${tige()},${tige()}]}"""
        val r = FichierMarsync.resume(json)!!
        assertEquals(1, r.nbContextes)
        assertEquals(listOf("Affouage Les Hauts"), r.noms)
        assertEquals(3, r.nbTiges)
        assertEquals(listOf("Pascal"), r.operateurs)
    }

    @Test
    fun `plusieurs contextes gardent l'ordre des noms et dedoublonnent les operateurs`() {
        val json = """{"contextes":[{"nom":"A","operateur":"Zoe"},{"nom":"B","operateur":"Zoe"},{"nom":"C"}],"tiges":[${tige("Luc")}]}"""
        val r = FichierMarsync.resume(json)!!
        assertEquals(3, r.nbContextes)
        assertEquals(listOf("A", "B", "C"), r.noms)
        assertEquals(listOf("Zoe", "Luc"), r.operateurs)
    }

    @Test
    fun `contexte sans nom ou nom vide`() {
        val r = FichierMarsync.resume("""{"contextes":[{"id":"a"},{"id":"b","nom":"  "}],"tiges":[]}""")!!
        assertEquals(2, r.nbContextes)
        assertEquals(emptyList<String>(), r.noms)
        assertEquals(0, r.nbTiges)
        assertEquals(emptyList<String>(), r.operateurs)
    }

    @Test
    fun `tiges seules sans contexte`() {
        val r = FichierMarsync.resume("""{"tiges":[${tige()},${tige()}]}""")!!
        assertEquals(0, r.nbContextes)
        assertEquals(2, r.nbTiges)
    }

    @Test
    fun `une annulation compte comme une tige`() {
        val r = FichierMarsync.resume("""{"tiges":[${tige()},${tige(action = "ANNULATION")}]}""")!!
        assertEquals(2, r.nbTiges)
    }

    @Test
    fun `contenu non reconnu donne null`() {
        assertNull(FichierMarsync.resume("pas du json"))
        assertNull(FichierMarsync.resume("""{"autre":1}"""))
    }

    @Test
    fun `la proposition expose le resume de son contenu`() {
        val p = ReceptionMarsync.Proposition("x.marsync", """{"contextes":[{"nom":"A"}],"tiges":[${tige()}]}""")
        assertEquals(1, p.resume!!.nbTiges)
    }
}
