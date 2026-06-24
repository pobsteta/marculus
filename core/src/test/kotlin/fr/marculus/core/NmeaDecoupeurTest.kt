package fr.marculus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NmeaDecoupeurTest {

    @Test
    fun `une ligne complete CRLF donne une trame`() {
        assertEquals(listOf("\$GPGGA,1,2,3"), NmeaDecoupeur().pousser("\$GPGGA,1,2,3\r\n"))
    }

    @Test
    fun `plusieurs lignes dans un meme morceau`() {
        val trames = NmeaDecoupeur().pousser("\$A,1\r\n\$B,2\r\n")
        assertEquals(listOf("\$A,1", "\$B,2"), trames)
    }

    @Test
    fun `un fragment incomplet est conserve puis assemble`() {
        val d = NmeaDecoupeur()
        assertTrue(d.pousser("\$GPGGA,12").isEmpty())
        assertTrue(d.pousser("3,45").isEmpty())
        assertEquals(listOf("\$GPGGA,123,45"), d.pousser("\r\n"))
    }

    @Test
    fun `les lignes vides et delimiteurs multiples sont ignores`() {
        assertEquals(listOf("\$X,1"), NmeaDecoupeur().pousser("\r\n\n\$X,1\r\r\n"))
    }

    @Test
    fun `garde-fou anti-emballement vide le tampon au dela de la taille max`() {
        val d = NmeaDecoupeur(tailleMax = 8)
        d.pousser("0123456789ABCDEF") // dépasse 8 sans fin de ligne → abandonné
        // après réinitialisation, une trame normale repart proprement
        assertEquals(listOf("\$OK,1"), d.pousser("\$OK,1\r\n"))
    }
}
