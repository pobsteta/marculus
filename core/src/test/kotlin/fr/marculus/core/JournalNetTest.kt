package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.Tige
import kotlin.test.Test
import kotlin.test.assertEquals

class JournalNetTest {

    private fun plus(uuid: String, t: Long, essence: String = "Chêne", classe: Int = 40, quantite: Int = 1) =
        Tige(uuid, "c1", essence, classe, ActionTige.PLUS, horodatage = t, quantite = quantite)

    private fun moins(uuid: String, t: Long, essence: String = "Chêne", classe: Int = 40, quantite: Int = 1) =
        Tige(uuid, "c1", essence, classe, ActionTige.ANNULATION, horodatage = t, quantite = quantite)

    @Test
    fun `sans annulation le journal net est le journal`() {
        val j = listOf(plus("a", 1), plus("b", 2, "Hêtre"))
        assertEquals(j, JournalNet.tiges(j))
    }

    @Test
    fun `l annulation et la derniere tige de sa case disparaissent`() {
        val j = listOf(plus("a", 1), plus("b", 2), moins("x", 3))
        assertEquals(listOf("a"), JournalNet.tiges(j).map { it.uuid })
    }

    @Test
    fun `l annulation ne touche que sa case`() {
        val j = listOf(plus("a", 1), plus("h", 2, "Hêtre"), moins("x", 3))
        assertEquals(listOf("h"), JournalNet.tiges(j).map { it.uuid })
    }

    @Test
    fun `une tige posterieure a l annulation n est pas retiree`() {
        val j = listOf(plus("a", 1), moins("x", 2), plus("b", 3))
        assertEquals(listOf("b"), JournalNet.tiges(j).map { it.uuid })
    }

    @Test
    fun `annulation partielle d un paquet - la tige reste avec sa quantite restante`() {
        val net = JournalNet.tiges(listOf(plus("a", 1, quantite = 3), moins("x", 2)))
        assertEquals(listOf("a" to 2), net.map { it.uuid to it.quantite })
    }

    @Test
    fun `annulation sans tige a retirer - gardee pour son reste`() {
        val net = JournalNet.tiges(listOf(moins("x", 1, quantite = 2), plus("a", 2)))
        assertEquals(listOf("x" to 2, "a" to 1), net.map { it.uuid to it.quantite })
    }

    @Test
    fun `le journal net donne les memes totaux que le journal complet`() {
        val j = listOf(
            plus("a", 1), plus("b", 2, quantite = 2), moins("x", 3, quantite = 3),
            plus("c", 4, "Hêtre", 25), moins("y", 5, "Hêtre", 25), plus("d", 6),
        )
        assertEquals(TotauxMartelage(j).totaux().filterValues { it != 0 }, TotauxMartelage(JournalNet.tiges(j)).totaux())
    }
}
