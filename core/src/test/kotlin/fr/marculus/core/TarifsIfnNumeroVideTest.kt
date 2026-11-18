package fr.marculus.core

import fr.marculus.core.model.TarifCubage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Mission 018 (brief nemeton 2026-11-15, § 2) : depuis nemeton 2026.11.31, un numéro ≤ 0 est publié
 * **vide**, `cv` vide aussi ; la ligne reste avec `n_arbres`, `millesime` et `part_houppier`.
 */
class TarifsIfnNumeroVideTest {
    private val entete = "niveau;code;espar;essence_libelle;type;numero;n_arbres;cv;millesime;part_houppier\n"
    private val ser = "code;nom;greco\nC20;Plateaux calcaires;C\n"
    private val communes = "insee;nom;departement;ser;part;greco\n21295;Gevrey;21;C20;1.0;C\n"

    private fun table(vararg lignes: String) = TarifsIfn.charger(entete + lignes.joinToString("\n"), communes, ser)

    @Test
    fun `numero vide - pas de tarif propose mais la part de houppier reste`() {
        val t = table(
            "SER;C20;02;Chêne sessile;SCHL;;500;;2011-2025;22.5",
            "SER;C20;02;Chêne sessile;SCHI;;500;;2011-2025;22.5",
        )
        assertNull(t.proposer("C20", "Chêne sessile", TarifCubage.SCHAEFFER_LENT))
        assertNull(t.typeConseille("C20", "Chêne sessile"))
        val p = assertNotNull(t.proposerPartHouppier("C20", "Chêne sessile"))
        assertEquals(22.5, p.pourcent, 1e-9)
        assertEquals(500, p.nArbres)
        assertEquals("2011-2025", t.millesime)
    }

    @Test
    fun `numero vide en SER - la cascade passe au niveau suivant pour le tarif, pas pour la part`() {
        val t = table(
            "SER;C20;02;Chêne sessile;SCHL;;500;;2011-2025;22.5",
            "FRANCE;FR;02;Chêne sessile;SCHL;7.4;9000;40.0;2011-2025;20.0",
        )
        val tarif = assertNotNull(t.proposer("C20", "Chêne sessile", TarifCubage.SCHAEFFER_LENT))
        assertEquals(TarifsIfn.Niveau.FRANCE, tarif.niveau)
        assertEquals(7, tarif.numero)
        assertEquals(TarifsIfn.Niveau.SER, t.proposerPartHouppier("C20", "Chêne sessile")?.niveau)
    }

    @Test
    fun `une ligne sans numero ne masque pas une ligne a numero du meme code ONF`() {
        // deux espèces IFN rapprochées du même code ONF : la plus fournie l'emporte, parmi celles qui ont un numéro
        val t = table(
            "SER;C20;02;Chêne sessile;SCHL;;900;;2011-2025;",
            "SER;C20;03;Chêne sessile;SCHL;6.0;100;30.0;2011-2025;",
        )
        assertEquals(6, t.proposer("C20", "Chêne sessile", TarifCubage.SCHAEFFER_LENT)?.numero)
    }

    @Test
    fun `numero vide dans Autres - ligne ecartee du tarif, gardee pour la part`() {
        val t = table(
            "SER;C20;41;Charme;SCHL;5.0;40;30.0;2011-2025;10.0",
            "SER;C20;23;Saule marsault;SCHL;;400;;2011-2025;30.0",
        )
        val n = assertNotNull(t.proposerAutres("C20", TarifsIfn.Regroupement.AUTRES_FEUILLUS, emptyList(), TarifCubage.SCHAEFFER_LENT))
        assertEquals(40, n.nArbres)
        assertEquals(5.0, n.numeroBrut, 1e-9)
        val p = assertNotNull(t.proposerPartHouppierAutres("C20", TarifsIfn.Regroupement.AUTRES_FEUILLUS, emptyList()))
        assertEquals(440, p.nArbres)
        assertEquals((400.0 + 12000) / 440, p.pourcent, 1e-9)
    }
}
