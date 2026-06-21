package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.Tige
import kotlin.test.Test
import kotlin.test.assertEquals

class TotauxMartelageTest {

    private var seq = 0
    private fun tige(essence: String, classe: Int, action: ActionTige) = Tige(
        uuid = "u${seq++}",
        contexteId = "ctx",
        essence = essence,
        classe = classe,
        action = action,
        horodatage = 0L,
    )

    @Test
    fun `le total d'une cellule = nombre de plus moins annulations`() {
        val journal = listOf(
            tige("Épicéa", 60, ActionTige.PLUS),
            tige("Épicéa", 60, ActionTige.PLUS),
            tige("Épicéa", 60, ActionTige.ANNULATION),
            tige("Chêne", 25, ActionTige.PLUS),
        )
        val totaux = TotauxMartelage(journal)
        assertEquals(1, totaux.totalPour("Épicéa", 60))
        assertEquals(1, totaux.totalPour("Chêne", 25))
        assertEquals(0, totaux.totalPour("Hêtre", 30))
    }

    @Test
    fun `le journal reste append-only - l'annulation n'efface rien`() {
        val journal = listOf(
            tige("Épicéa", 60, ActionTige.PLUS),
            tige("Épicéa", 60, ActionTige.ANNULATION),
        )
        // Les deux événements sont conservés…
        assertEquals(2, journal.size)
        // …mais le total dérivé revient à zéro.
        assertEquals(0, TotauxMartelage(journal).totalPour("Épicéa", 60))
    }

    @Test
    fun `totaux agrege par essence et classe`() {
        val journal = listOf(
            tige("Épicéa", 60, ActionTige.PLUS),
            tige("Épicéa", 60, ActionTige.PLUS),
            tige("Chêne", 25, ActionTige.PLUS),
        )
        val map = TotauxMartelage(journal).totaux()
        assertEquals(2, map[CompteurCle("Épicéa", 60)])
        assertEquals(1, map[CompteurCle("Chêne", 25)])
        assertEquals(2, map.size)
    }

    @Test
    fun `total du contexte = somme de toutes les cellules`() {
        val journal = listOf(
            tige("Épicéa", 60, ActionTige.PLUS),
            tige("Chêne", 25, ActionTige.PLUS),
            tige("Chêne", 25, ActionTige.ANNULATION),
            tige("Hêtre", 30, ActionTige.PLUS),
        )
        assertEquals(2, TotauxMartelage(journal).totalContexte())
    }
}
