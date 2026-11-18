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
    fun `parts de houppier de controle - France`() {
        // Valeurs France du brief nemeton 2026.10.9 ; une SER fictive force le repli sur la France.
        fun france(essence: String) = ifn.proposerPartHouppier("Z99", essence)?.pourcent
        assertEquals(12.1, france("Chêne sessile"))
        assertEquals(14.5, france("Chêne pédonculé"))
        assertEquals(12.5, france("Hêtre"))
        assertEquals(0.6, france("Douglas"))
    }

    @Test
    fun `millesime et couverture`() {
        assertEquals("2011-2025", ifn.millesime)
        // Zéro initial et codes corses gardés tels quels.
        assertEquals("L'Abergement-Clémenciat", ifn.commune("01001")?.nom)
        assertEquals(listOf("K11"), ifn.serDeCommune("2A004").map { it.ser })
        assertTrue(ifn.rechercherCommunes("ajaccio").any { it.insee == "2A004" })
    }

    /** Mission 018 : table de nemeton 2026.11.31 (brief du 2026-11-15) : 1 590 numéros vides, aucun <= 0, 29AF/68CE libellés. */
    @Test
    fun `table de nemeton 2026-11-31 - numeros vides, aucun negatif, libelles 29AF et 68CE`() {
        val lignes = table("tarifs_ser.csv").lineSequence().drop(1).filter { it.isNotBlank() }.map { it.split(';') }.toList()
        assertEquals(14752, lignes.size)
        assertEquals(1590, lignes.count { it[5].isEmpty() })
        assertTrue(lignes.none { it[5].isNotEmpty() && it[5].toDouble() <= 0.0 })
        assertTrue(lignes.filter { it[5].isEmpty() }.all { it[7].isEmpty() })
        val exotiques = lignes.filter { it[2] == "29AF" || it[2] == "68CE" }
        assertEquals(44, exotiques.size)
        assertEquals(setOf("Autre feuillu exotique", "Autre conifère exotique"), exotiques.map { it[3] }.toSet())
    }
}
