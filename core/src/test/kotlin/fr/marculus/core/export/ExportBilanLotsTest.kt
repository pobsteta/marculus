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

class ExportBilanLotsTest {

    private val ctx = Contexte(
        id = "c1", nom = "Affouage 2026", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
        essences = listOf(EssenceColonne("Chêne", 0, 0)), tarif = TarifCubage.SCHAEFFER_RAPIDE, tarifNumero = 8,
        affouage = true, volumeMaxLotM3 = 2.0,
    )
    private val v40 = Cubage.volume(TarifCubage.SCHAEFFER_RAPIDE, 8, 42.5)

    private fun plus(uuid: String, t: Long, lot: Int?) =
        Tige(uuid, "c1", "Chêne", 40, ActionTige.PLUS, horodatage = t, lot = lot)

    @Test
    fun `une ligne par lot, etat et total, decimaux de la langue`() {
        val j = listOf(
            plus("a", 1, 1), plus("b", 2, 1), plus("c", 3, 2),
            Tige("x", "c1", "Chêne", 40, ActionTige.ANNULATION, horodatage = 4),
            plus("d", 5, 2), plus("e", 6, null),
        )
        val lignes = ExportBilanLots.csv(ctx, j, Locale.FRANCE).trimEnd().split("\n")
        fun fr(v: Double) = String.format(Locale.FRANCE, "%.3f", v)
        assertEquals(
            listOf(
                "Contexte;Affouage 2026",
                "ContexteId;c1",
                "VolumeMaxLot_m3;2,000",
                "",
                "Lot;Tiges;Volume_m3;Etat",
                "1;2;${fr(2 * v40)};COMPLET",
                "2;1;${fr(v40)};EN_COURS",
                ";1;${fr(v40)};SANS_LOT",
                "Total;4;${fr(4 * v40)};",
            ),
            lignes,
        )
    }
}
