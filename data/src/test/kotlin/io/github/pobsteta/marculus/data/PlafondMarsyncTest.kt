package io.github.pobsteta.marculus.data

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlafondMarsyncTest {
    @Test
    fun `taille annoncee au plafond acceptee, au dessus refusee, inconnue acceptee`() {
        assertTrue(FichierMarsync.taillePermise(FichierMarsync.TAILLE_MAX_OCTETS))
        assertFalse(FichierMarsync.taillePermise(FichierMarsync.TAILLE_MAX_OCTETS + 1))
        assertTrue(FichierMarsync.taillePermise(null))
        assertTrue(FichierMarsync.taillePermise(-1))
    }

    @Test
    fun `lecture bornee rend le texte sous le plafond et null au dessus`() {
        assertEquals("{\"tiges\":[]}", FichierMarsync.lireBorne(ByteArrayInputStream("{\"tiges\":[]}".toByteArray()), 100))
        assertEquals("abc", FichierMarsync.lireBorne(ByteArrayInputStream("abc".toByteArray()), 3))
        assertNull(FichierMarsync.lireBorne(ByteArrayInputStream("abcd".toByteArray()), 3))
    }

    @Test
    fun `lecture bornee ne lit jamais plus que le plafond plus un octet`() {
        var lus = 0L
        val infini = object : InputStream() {
            override fun read(): Int { lus++; return 'a'.code }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                lus += len
                b.fill('a'.code.toByte(), off, off + len)
                return len
            }
        }
        assertNull(FichierMarsync.lireBorne(infini, 1000))
        assertEquals(1001L, lus)
    }

    @Test
    fun `lecture bornee conserve les accents UTF-8`() {
        assertEquals("hêtre", FichierMarsync.lireBorne(ByteArrayInputStream("hêtre".toByteArray()), 100))
    }
}
