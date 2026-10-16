package fr.marculus.core

import fr.marculus.core.TarifsIfn.Niveau
import fr.marculus.core.model.TarifCubage
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contrôle des tables embarquées (`data/src/main/assets/tarifs-ifn/`, produites par nemeton) :
 * format lu sans perte et valeurs de contrôle de la spec. À mettre à jour quand nemeton publie
 * un nouveau millésime.
 */
class TarifsIfnReelsTest {

    private fun table(nom: String) = File("../data/src/main/assets/tarifs-ifn/$nom").readText()

    private val ifn = TarifsIfn.charger(table("tarifs_ser.csv"), table("communes_ser.csv"), table("ser.csv"))

    @Test
    fun `communes et SER - Gevrey-Chambertin et Huisseau-sur-Mauves`() {
        assertEquals(listOf("C20", "C51"), ifn.serDeCommune("21295").map { it.ser })
        assertEquals(0.5563, ifn.serDeCommune("21295").first().part, 1e-9)
        assertEquals(listOf("B44", "B70"), ifn.serDeCommune("45167").map { it.ser })
        assertEquals("Gevrey-Chambertin", ifn.commune("21295")?.nom)
        assertEquals("C", ifn.ser("C20")?.greco)
        assertEquals(listOf("21295"), ifn.communesNommees("Gevrey-Chambertin").map { it.insee })
    }

    @Test
    fun `numeros de controle au niveau SER`() {
        val chs = assertNotNull(ifn.proposer("C20", "Chêne sessile", TarifCubage.SCHAEFFER_LENT))
        assertEquals(Niveau.SER, chs.niveau)
        assertEquals(7.8, chs.numeroBrut)
        assertEquals(8, chs.numero)
        assertEquals(8746, chs.nArbres)
        assertEquals(9, ifn.proposer("C20", "Hêtre", TarifCubage.SCHAEFFER_LENT)?.numero) // 8,5
        assertEquals(7, ifn.proposer("B70", "Chêne pédonculé", TarifCubage.SCHAEFFER_RAPIDE)?.numero) // 6,7
        assertEquals(9, ifn.proposer("B70", "Chêne sessile", TarifCubage.SCHAEFFER_RAPIDE)?.numero) // 9,3
    }

    @Test
    fun `millesime et couverture`() {
        assertEquals("2011-2025", ifn.millesime)
        // Zéro initial et codes corses gardés tels quels.
        assertEquals("L'Abergement-Clémenciat", ifn.commune("01001")?.nom)
        assertEquals(listOf("K11"), ifn.serDeCommune("2A004").map { it.ser })
        assertTrue(ifn.rechercherCommunes("ajaccio").any { it.insee == "2A004" })
    }
}
