package fr.marculus.core.export

import fr.marculus.core.Cubage
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.PartsLot
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mission 026, règle 4 : le CSV passe au **format 8** et porte les cases « tige » / « houppier »
 * par essence dans son en-tête.
 *
 * Forme proposée par le testeur (à valider par Pascal, nemetonshiny la lira, mission 028) : une ligne
 * `PartsLot;` placée après `MillesimeTarifs` et avant `Journal`, qui liste **toutes** les essences du
 * contexte dans l'ordre, comme `TarifsEssence` : `essence=tige:1,houppier:0` séparées par `|`
 * (1 = coché, 0 = décoché ; une essence sans réglage : `tige:1,houppier:1`).
 *
 * Valeurs hand-calculées : Schaeffer rapide n° 8, classe 40, part de houppier 0,3 :
 * T = v40 ≈ 1,393 ; H = v40·3/7 ≈ 0,597.
 */
class ExportCsvPartsLotTest {
    private val v40 = Cubage.volume(TarifCubage.SCHAEFFER_RAPIDE, 8, 42.5)
    private val h30 = v40 * 3.0 / 7.0

    private val ctx = Contexte(
        id = "c1", nom = "Affouage", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
        essences = listOf(EssenceColonne("Chêne", 0, 0), EssenceColonne("Hêtre", 0, 0)),
        tarif = TarifCubage.SCHAEFFER_RAPIDE, tarifNumero = 8, affouage = true, volumeMaxLotM3 = 50.0,
        partHouppier = mapOf("Chêne" to 0.3),
    )

    private val chene = Tige("u-a", "c1", "Chêne", 40, ActionTige.PLUS, horodatage = 1000L, lot = 1)

    private fun f4(x: Double) = "%.4f".format(Locale.ROOT, x)

    @Test
    fun `le format est 8`() {
        assertEquals(8, ExportCsv.FORMAT)
        val lignes = ExportCsv.contexteCsv(ctx, emptyList()).split("\n")
        assertEquals("FormatCsv;8", lignes[1])
    }

    @Test
    fun `en-tete - defaut, toutes les essences avec les deux cases cochees`() {
        val lignes = ExportCsv.contexteCsv(ctx, emptyList()).split("\n")
        val attendu = "PartsLot;Chêne=tige:1,houppier:1|Hêtre=tige:1,houppier:1"
        assertTrue(attendu in lignes, lignes.joinToString("\n"))
        assertTrue(lignes.indexOf("MillesimeTarifs;") < lignes.indexOf(attendu))
        assertTrue(lignes.indexOf(attendu) < lignes.indexOf("Journal;COMPLET"))
    }

    @Test
    fun `en-tete - les cases reglees par essence`() {
        val c = ctx.copy(
            partsLot = mapOf("Chêne" to PartsLot(tige = false, houppier = true), "Hêtre" to PartsLot(tige = false, houppier = false)),
        )
        val lignes = ExportCsv.contexteCsv(c, emptyList()).split("\n")
        assertTrue("PartsLot;Chêne=tige:0,houppier:1|Hêtre=tige:0,houppier:0" in lignes, lignes.joinToString("\n"))
    }

    @Test
    fun `AffouageLotTotalM3 et colonne AffouageLotM3 suivent les cases`() {
        for ((p, attendu) in listOf(
            PartsLot(true, true) to v40 + h30,
            PartsLot(true, false) to v40,
            PartsLot(false, true) to h30,
            PartsLot(false, false) to 0.0,
        )) {
            val c = ctx.copy(partsLot = mapOf("Chêne" to p))
            val csv = ExportCsv.contexteCsv(c, listOf(chene))
            val lignes = csv.split("\n")
            assertTrue("AffouageLotTotalM3;${f4(attendu)}" in lignes, "$p : " + lignes.first { it.startsWith("AffouageLotTotalM3") })
            val debut = lignes.indexOf("JOURNAL") + 2
            val colonne = lignes[debut].split(";")
            assertEquals(22, colonne.size)
            assertEquals(attendu, colonne[21].toDouble(), 1e-6, "$p")
            // les volumes tige, houppier et total du journal ne dépendent pas des cases
            assertEquals(v40, colonne[15].toDouble(), 1e-6)
            assertEquals(h30, colonne[16].toDouble(), 1e-6)
        }
    }
}
