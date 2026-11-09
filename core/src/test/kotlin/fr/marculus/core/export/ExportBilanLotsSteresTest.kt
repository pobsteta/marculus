package fr.marculus.core.export

import fr.marculus.core.Cubage
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Mission 017 : colonnes et lignes en stères du CSV du bilan par lot (API `ExportBilanLots.csv` inchangée). */
class ExportBilanLotsSteresTest {

    private val ctx = Contexte(
        id = "c1", nom = "Affouage 2026", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
        essences = listOf(EssenceColonne("Chêne", 0, 0)), tarif = TarifCubage.SCHAEFFER_RAPIDE, tarifNumero = 8,
        affouage = true, volumeMaxLotM3 = 10.0,
    )
    private val v40 = Cubage.volume(TarifCubage.SCHAEFFER_RAPIDE, 8, 42.5)

    private fun plus(uuid: String, t: Long, lot: Int?) =
        Tige(uuid, "c1", "Chêne", 40, ActionTige.PLUS, horodatage = t, lot = lot)

    private val journal = listOf(plus("a", 1, 1), plus("b", 2, 1), plus("c", 3, 2), plus("e", 4, null))

    private fun lignes(c: Contexte, loc: Locale = Locale.FRANCE) =
        ExportBilanLots.csv(c, journal, loc).trimEnd().split("\n")

    private fun m3(v: Double, loc: Locale = Locale.FRANCE) = String.format(loc, "%.3f", v)
    private fun st(v: Double, k: Double, loc: Locale = Locale.FRANCE) = String.format(loc, "%.2f", v * k)

    @Test
    fun `sous condition entete SteresParM3 puis VolumeMaxLot_st apres VolumeMaxLot_m3`() {
        val l = lignes(ctx)
        assertEquals(
            listOf("Contexte;Affouage 2026", "ContexteId;c1", "VolumeMaxLot_m3;10,000", "SteresParM3;1,50", "VolumeMaxLot_st;15,00", ""),
            l.subList(0, 6),
        )
    }

    @Test
    fun `colonnes _st apres VolumeLot_m3 et avant Etat, total compris`() {
        val l = lignes(ctx)
        assertEquals(
            "Lot;Tiges;VolumeTige_m3;VolumeHouppier_m3;VolumeLot_m3;VolumeTige_st;VolumeHouppier_st;VolumeLot_st;Etat",
            l[6],
        )
        assertEquals("1;2;${m3(2 * v40)};0,000;${m3(2 * v40)};${st(2 * v40, 1.5)};0,00;${st(2 * v40, 1.5)};EN_COURS", l[7])
        assertEquals("2;1;${m3(v40)};0,000;${m3(v40)};${st(v40, 1.5)};0,00;${st(v40, 1.5)};EN_COURS", l[8])
        assertEquals(";1;${m3(v40)};0,000;${m3(v40)};${st(v40, 1.5)};0,00;${st(v40, 1.5)};SANS_LOT", l[9])
        assertEquals("Total;4;${m3(4 * v40)};0,000;${m3(4 * v40)};${st(4 * v40, 1.5)};0,00;${st(4 * v40, 1.5)};", l[10])
    }

    @Test
    fun `coefficient regle 1,4 respecte dans l'en-tete et les lignes`() {
        val l = lignes(ctx.copy(steresParM3 = 1.4))
        assertEquals("SteresParM3;1,40", l[3])
        assertEquals("VolumeMaxLot_st;14,00", l[4])
        assertEquals("Total;4;${m3(4 * v40)};0,000;${m3(4 * v40)};${st(4 * v40, 1.4)};0,00;${st(4 * v40, 1.4)};", l[10])
    }

    @Test
    fun `houppier en steres calcule sur la valeur non arrondie`() {
        val c = ctx.copy(partHouppier = mapOf("Chêne" to 0.5))
        val l = ExportBilanLots.csv(c, listOf(plus("a", 1, 1)), Locale.FRANCE).trimEnd().split("\n")
        assertEquals(
            "1;1;${m3(v40)};${m3(v40)};${m3(v40)};${st(v40, 1.5)};${st(v40, 1.5)};${st(v40, 1.5)};EN_COURS",
            l[7],
        )
    }

    @Test
    fun `decimales dans la langue du telephone`() {
        val l = lignes(ctx, Locale.US)
        assertEquals("SteresParM3;1.50", l[3])
        assertEquals("VolumeMaxLot_st;15.00", l[4])
        assertEquals(
            "Total;4;${m3(4 * v40, Locale.US)};0.000;${m3(4 * v40, Locale.US)};" +
                "${st(4 * v40, 1.5, Locale.US)};0.00;${st(4 * v40, 1.5, Locale.US)};",
            l[10],
        )
    }

    private fun sansStere(c: Contexte) {
        val csv = ExportBilanLots.csv(c, journal, Locale.FRANCE)
        assertFalse(csv.contains("SteresParM3"), csv)
        assertFalse(csv.contains("_st"), csv)
    }

    @Test
    fun `affouage sans maximum, pas de stere`() = sansStere(ctx.copy(volumeMaxLotM3 = 0.0))

    @Test
    fun `contexte ordinaire, pas de stere`() = sansStere(ctx.copy(affouage = false, volumeMaxLotM3 = 0.0))

    @Test
    fun `maximum sans affouage, pas de stere`() = sansStere(ctx.copy(affouage = false))

    @Test
    fun `coefficient nul ou negatif, pas de stere`() {
        sansStere(ctx.copy(steresParM3 = 0.0))
        sansStere(ctx.copy(steresParM3 = -1.5))
    }
}
