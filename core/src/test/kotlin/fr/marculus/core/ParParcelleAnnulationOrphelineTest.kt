package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.Tige
import fr.nemeton.terrain.geo.Position
import kotlin.test.Test
import kotlin.test.assertEquals

/** Mission 019 : annulation sans tige à retirer = comptée en négatif dans « sans position ». */
class ParParcelleAnnulationOrphelineTest {
    @Test
    fun `annulation sans tige a retirer - negatif dans sans position, total egal au net de Statut`() {
        val j = listOf(
            Tige("a", "c1", "Chêne", 30, ActionTige.PLUS, horodatage = 1, position = Position(45.0, 5.0)),
            Tige("x", "c1", "Hêtre", 40, ActionTige.ANNULATION, horodatage = 2),
        )
        val r = ParParcelle.repartir(j) { "A" }
        assertEquals(-1, r.sansPosition)
        assertEquals(1, r.nombre("A"))
        assertEquals(TotauxMartelage(j).totalContexte(), r.total)
    }
}
