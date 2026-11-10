package io.github.pobsteta.marculus.ui.contextes

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatNombreTest {
    @Test fun `affichage FR avec virgule`() {
        assertEquals("7,41", formatPourcent(7.41, Locale.FRANCE))
        assertEquals("30", formatPourcent(30.0, Locale.FRANCE))
        assertEquals("6,667", formatDecimal(6.6667, 3, Locale.FRANCE))
    }

    @Test fun `affichage EN avec point`() {
        assertEquals("7.41", formatPourcent(7.41, Locale.ENGLISH))
        assertEquals("30", formatPourcent(30.0, Locale.ENGLISH))
    }

    @Test fun `la langue passee gagne sur la locale par defaut`() {
        val avant = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            assertEquals("7,41", formatPourcent(7.41, Locale.FRANCE))
            Locale.setDefault(Locale.FRANCE)
            assertEquals("7.41", formatPourcent(7.41, Locale.ENGLISH))
        } finally {
            Locale.setDefault(avant)
        }
    }

    @Test fun `saisie virgule ou point lue pareil`() {
        assertEquals(7.41, lireDecimal("7,41"))
        assertEquals(7.41, lireDecimal("7.41"))
        assertEquals(null, lireDecimal(""))
    }

    @Test fun `valeur enregistree inchangee - fraction`() {
        val fr = lireDecimal(formatPourcent(7.41, Locale.FRANCE))!! / 100.0
        val en = lireDecimal(formatPourcent(7.41, Locale.ENGLISH))!! / 100.0
        assertEquals(0.0741, fr, 1e-12)
        assertEquals(fr, en, 0.0)
    }
}
