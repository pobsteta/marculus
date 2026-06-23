package fr.marculus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReferentielsTest {

    @Test
    fun `listes par defaut non vides`() {
        assertTrue(Referentiels.ESSENCES_DEFAUT.contains("Chêne"))
        assertEquals(6, Referentiels.ESSENCES_DEFAUT.size)
        assertTrue(Referentiels.QUALITE_ARBRE_DEFAUT.isNotEmpty())
        assertTrue(Referentiels.QUALITE_BOIS_DEFAUT.containsAll(listOf("A", "AB", "CD")))
        assertEquals(Referentiels.ESSENCES_DEFAUT.size, Referentiels.COULEURS_ESSENCES_DEFAUT.size)
        assertTrue(Referentiels.PALETTE.size >= 16)
    }

    @Test
    fun `couleur de fond cyclee et bornee`() {
        // Index aligné sur l'ordre des essences.
        assertEquals(Referentiels.COULEURS_ESSENCES_DEFAUT[0], Referentiels.couleurFondDefaut(0))
        assertEquals(Referentiels.COULEURS_ESSENCES_DEFAUT[2], Referentiels.couleurFondDefaut(2))
        // Cyclage au-delà de la taille.
        val n = Referentiels.COULEURS_ESSENCES_DEFAUT.size
        assertEquals(Referentiels.couleurFondDefaut(0), Referentiels.couleurFondDefaut(n))
        // Index négatif ramené à 0 (pas d'exception).
        assertEquals(Referentiels.couleurFondDefaut(0), Referentiels.couleurFondDefaut(-3))
    }

    @Test
    fun `couleurs de base`() {
        assertEquals(0xFF1FA0EC.toInt(), Referentiels.COULEUR_FOND_DEFAUT)
        assertEquals(0xFFFFFFFF.toInt(), Referentiels.COULEUR_TEXTE_DEFAUT)
    }
}
