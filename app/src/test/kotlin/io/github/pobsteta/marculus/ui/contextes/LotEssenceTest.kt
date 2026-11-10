package io.github.pobsteta.marculus.ui.contextes

import fr.marculus.core.model.PartsLot
import fr.marculus.core.model.TarifCubage
import kotlin.test.Test
import kotlin.test.assertEquals

class LotEssenceTest {
    private val deux = PartsLot()
    private val tigeSeule = PartsLot(tige = true, houppier = false)
    private val houppierSeul = PartsLot(tige = false, houppier = true)
    private val aucune = PartsLot(tige = false, houppier = false)
    private val sch = TarifCubage.SCHAEFFER_RAPIDE

    @Test fun `tarif Aucun ne compte jamais`() {
        listOf(deux, tigeSeule, houppierSeul, aucune).forEach {
            assertEquals(false, compteAuLot("Chêne sessile", TarifCubage.AUCUN, it, "30"))
        }
    }

    @Test fun `les deux cases decochees ne comptent pas`() {
        assertEquals(false, compteAuLot("Chêne sessile", sch, aucune, "30"))
    }

    @Test fun `la tige cochee compte avec un tarif`() {
        assertEquals(true, compteAuLot("Chêne sessile", sch, tigeSeule, ""))
        assertEquals(true, compteAuLot("Chêne sessile", sch, deux, ""))
    }

    @Test fun `houppier seul en Schaeffer compte si la part est positive`() {
        assertEquals(true, compteAuLot("Chêne sessile", sch, houppierSeul, "30"))
        assertEquals(true, compteAuLot("Chêne sessile", sch, houppierSeul, "6,5"))
        assertEquals(false, compteAuLot("Chêne sessile", sch, houppierSeul, "0"))
        assertEquals(false, compteAuLot("Chêne sessile", sch, houppierSeul, ""))
    }

    @Test fun `houppier seul en EMERGE compte seulement si l'essence est couverte`() {
        assertEquals(true, compteAuLot("Chêne sessile", TarifCubage.EMERGE, houppierSeul, ""))
        assertEquals(false, compteAuLot("Zzzz inconnue", TarifCubage.EMERGE, houppierSeul, ""))
        assertEquals(true, compteAuLot("Zzzz inconnue", TarifCubage.EMERGE, tigeSeule, ""))
    }
}
