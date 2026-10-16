package fr.marculus.core

import fr.marculus.core.TarifsIfn.Niveau
import fr.marculus.core.model.TarifCubage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Jeux factices : `src/test/resources/tarifs-ifn/` (2 SER de la GRECO C, une SER de B sans tarif). */
class TarifsIfnTest {

    private fun ressource(nom: String) =
        requireNotNull(javaClass.getResource("/tarifs-ifn/$nom")) { nom }.readText()

    private val ifn = TarifsIfn.charger(ressource("tarifs_ser.csv"), ressource("communes_ser.csv"), ressource("ser.csv"))

    @Test
    fun `commune a cheval - SER triees par part decroissante`() {
        val sers = ifn.serDeCommune("21295")
        assertEquals(listOf("C20", "C51"), sers.map { it.ser })
        assertEquals(0.556, sers.first().part, 1e-9)
        assertEquals("C", sers.first().greco)
    }

    @Test
    fun `commune inconnue - aucune SER`() {
        assertTrue(ifn.serDeCommune("99999").isEmpty())
        assertNull(ifn.commune("99999"))
    }

    @Test
    fun `niveau SER retenu quand l echantillon suffit`() {
        val p = assertNotNull(ifn.proposer("C20", "Chêne sessile", TarifCubage.SCHAEFFER_LENT))
        assertEquals(Niveau.SER, p.niveau)
        assertEquals("C20", p.code)
        assertEquals(8, p.numero) // 7,7 arrondi
        assertEquals(7.7, p.numeroBrut)
        assertEquals(500, p.nArbres)
        assertEquals(38.6, p.cv)
        assertEquals("IFN test", p.millesime)
    }

    @Test
    fun `seuil exact - 30 arbres suffisent, 29 non`() {
        val c51 = assertNotNull(ifn.proposer("C51", "Chêne sessile", TarifCubage.SCHAEFFER_LENT))
        assertEquals(Niveau.SER, c51.niveau)
        assertEquals(30, c51.nArbres)
        assertEquals(10, c51.numero) // 9,5 arrondi au-dessus
        // Hêtre en C20 : 29 arbres → repli GRECO.
        val het = assertNotNull(ifn.proposer("C20", "Hêtre", TarifCubage.SCHAEFFER_LENT))
        assertEquals(Niveau.GRECO, het.niveau)
        assertEquals("C", het.code)
        assertEquals(9, het.numero)
        assertEquals(TarifsIfn.SEUIL_ARBRES, 30)
    }

    @Test
    fun `repli jusqu a la France, puis rien`() {
        // GRECO C : 12 arbres → France (40).
        val alt = assertNotNull(ifn.proposer("C20", "Alisier torminal", TarifCubage.SCHAEFFER_LENT))
        assertEquals(Niveau.FRANCE, alt.niveau)
        assertEquals(TarifsIfn.CODE_FRANCE, alt.code)
        // SER B70 sans aucune ligne ni GRECO B : France directement.
        assertEquals(Niveau.FRANCE, ifn.proposer("B70", "Alisier torminal", TarifCubage.SCHAEFFER_LENT)?.niveau)
        // Sorbier : 20 arbres en France, sous le seuil partout → pas de proposition.
        assertNull(ifn.proposer("C20", "Sorbier des oiseleurs", TarifCubage.SCHAEFFER_LENT))
        // Type absent de la table pour l'essence.
        assertNull(ifn.proposer("C20", "Hêtre", TarifCubage.SCHAEFFER_RAPIDE))
    }

    @Test
    fun `arrondi borne a 1 - 20`() {
        assertEquals(20, ifn.proposer("C20", "Robinier faux acacia", TarifCubage.SCHAEFFER_RAPIDE)?.numero)
        assertEquals(24.6, ifn.proposer("C20", "Robinier faux acacia", TarifCubage.SCHAEFFER_RAPIDE)?.numeroBrut)
        assertEquals(1, ifn.proposer("C20", "Tremble", TarifCubage.SCHAEFFER_RAPIDE)?.numero)
    }

    @Test
    fun `essence sans code ONF ou non couverte - pas de proposition`() {
        assertNull(Cubage.codeEssence("Catalpa"))
        assertNull(ifn.proposer("C20", "Catalpa", TarifCubage.SCHAEFFER_LENT))
        assertNull(ifn.proposer("C20", "Essence inventée", TarifCubage.SCHAEFFER_LENT))
    }

    @Test
    fun `AUCUN et EMERGE - pas de proposition`() {
        assertNull(ifn.proposer("C20", "Chêne sessile", TarifCubage.AUCUN))
        assertNull(ifn.proposer("C20", "Chêne sessile", TarifCubage.EMERGE))
    }

    @Test
    fun `essence rapprochee par son nom - casse, accents et nom latin`() {
        assertEquals(8, ifn.proposer("C20", "chene SESSILE", TarifCubage.SCHAEFFER_LENT)?.numero)
        assertEquals(8, ifn.proposer("C20", "Quercus petraea", TarifCubage.SCHAEFFER_LENT)?.numero)
    }

    @Test
    fun `type conseille - plus faible CV`() {
        assertEquals(TarifCubage.SCHAEFFER_INTERMEDIAIRE, ifn.typeConseille("C20", "Chêne sessile"))
        assertNull(ifn.typeConseille("C20", "Sorbier des oiseleurs"))
    }

    @Test
    fun `recherche de communes - accents et casse ignores, debut avant milieu`() {
        assertEquals(listOf("21295"), ifn.rechercherCommunes("gevrey").map { it.insee })
        assertEquals(listOf("45167"), ifn.rechercherCommunes("MAUVES").map { it.insee })
        assertEquals("Dijon", ifn.rechercherCommunes("21231").single().nom)
        assertTrue(ifn.rechercherCommunes("  ").isEmpty())
        assertEquals(listOf("21295"), ifn.communesNommees("GEVREY CHAMBERTIN").map { it.insee })
    }

    @Test
    fun `libelles des SER et millesime`() {
        assertEquals("IFN test", ifn.millesime)
        assertEquals("C", ifn.ser("C51")?.greco)
        assertNull(ifn.ser("Z99"))
    }

    @Test
    fun `lignes illisibles ignorees, BOM accepte`() {
        val t = TarifsIfn.charger(
            "﻿niveau;code;espar;essence_libelle;type;numero;n_arbres;cv;millesime\n" +
                "SER;C20;03;Chêne sessile;SCHL;abc;500;1;m\n" +
                "XXX;C20;03;Chêne sessile;SCHL;7;500;1;m\n" +
                "SER;C20;03;Chêne sessile;SCHL;7.2;500;1;m\n",
            "insee;nom;departement;ser;part;greco\n21231;Dijon;21;C20;x;C\n",
            "",
        )
        assertEquals(7, t.proposer("C20", "Chêne sessile", TarifCubage.SCHAEFFER_LENT)?.numero)
        assertTrue(t.serDeCommune("21231").isEmpty())
    }
}
