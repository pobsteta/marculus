package fr.marculus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnnonceHauteurTest {

    @Test
    fun `hauteur seule`() {
        assertEquals("12", AnnonceHauteur.totale("12"))
        assertNull(AnnonceHauteur.decoupe("12"))
    }

    @Test
    fun `la decoupe est dite, segment par segment`() {
        assertEquals("12", AnnonceHauteur.totale("12-6A3AB"))
        assertEquals("6 A, 3 A B", AnnonceHauteur.decoupe("12-6A3AB"))
    }

    @Test
    fun `les lettres sont detachees pour etre entendues`() {
        // « AB » lu d'un bloc devient « ab » à la synthèse : inutilisable pour contrôler.
        assertEquals("6 A B", AnnonceHauteur.decoupe("27-6AB"))
    }

    @Test
    fun `une longueur entiere ne se dit pas avec des decimales`() {
        assertEquals("6 A", AnnonceHauteur.decoupe("27-6A"))
        assertEquals("6.5 A", AnnonceHauteur.decoupe("27-6,5A"))
    }

    @Test
    fun `une decoupe illisible est dite telle quelle, jamais tue`() {
        assertEquals("bidule", AnnonceHauteur.decoupe("27-bidule"))
    }

    @Test
    fun `un tiret sans decoupe ne dit rien`() {
        assertNull(AnnonceHauteur.decoupe("27-"))
        assertEquals("27", AnnonceHauteur.totale("27-"))
    }
}
