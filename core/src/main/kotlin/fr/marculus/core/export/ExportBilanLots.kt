package fr.marculus.core.export

import fr.marculus.core.AffouageLots
import fr.marculus.core.Steres
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.Tige
import java.util.Locale

/**
 * CSV du bilan par lot d'un affouage : le même tableau que l'onglet « Par lot » (tiges, volumes
 * tige et houppier, volume compté au lot, **nets** : [AffouageLots.bilan]). C'est un rapport à lire, comme le CSV foncier :
 * décimaux dans la langue du téléphone (virgule en français, pour le tableur), pas un format
 * d'échange. Sous [Steres.actif], les stères (2 décimales) suivent les m³ (mission 017).
 */
object ExportBilanLots {
    private const val SEP = ";"

    fun csv(contexte: Contexte, journal: List<Tige>, locale: Locale): String {
        val bilan = AffouageLots.bilan(contexte, journal)
        val st = Steres.actif(contexte)
        fun m3(v: Double) = String.format(locale, "%.3f", v)
        fun stere(v: Double) = String.format(locale, "%.2f", Steres.deM3(v, contexte))
        val sb = StringBuilder()
        sb.appendLine("Contexte${SEP}${champ(contexte.nom)}")
        sb.appendLine("ContexteId${SEP}${champ(contexte.id)}")
        sb.appendLine("VolumeMaxLot_m3${SEP}${m3(contexte.volumeMaxLotM3)}")
        if (st) {
            sb.appendLine("SteresParM3${SEP}${String.format(locale, "%.2f", contexte.steresParM3)}")
            sb.appendLine("VolumeMaxLot_st${SEP}${stere(contexte.volumeMaxLotM3)}")
        }
        sb.appendLine()
        val entete = mutableListOf("Lot", "Tiges", "VolumeTige_m3", "VolumeHouppier_m3", "VolumeLot_m3")
        if (st) entete += listOf("VolumeTige_st", "VolumeHouppier_st", "VolumeLot_st")
        entete += "Etat"
        sb.appendLine(entete.joinToString(SEP))
        fun ligne(debut: List<String>, tige: Double, houppier: Double, lot: Double, etat: String): String {
            val c = debut.toMutableList()
            c += listOf(m3(tige), m3(houppier), m3(lot))
            if (st) c += listOf(stere(tige), stere(houppier), stere(lot))
            c += etat
            return c.joinToString(SEP)
        }
        bilan.forEach { b ->
            val etat = when {
                b.lot == null -> "SANS_LOT"
                b.complet -> "COMPLET"
                else -> "EN_COURS"
            }
            sb.appendLine(
                ligne(listOf(b.lot?.toString() ?: "", b.nbTiges.toString()), b.volumeTigeM3, b.volumeHouppierM3, b.volumeM3, etat),
            )
        }
        sb.appendLine(
            ligne(
                listOf("Total", bilan.sumOf { it.nbTiges }.toString()),
                bilan.sumOf { it.volumeTigeM3 }, bilan.sumOf { it.volumeHouppierM3 }, bilan.sumOf { it.volumeM3 }, "",
            ),
        )
        return sb.toString()
    }

    private fun champ(valeur: String): String =
        if (valeur.contains(SEP) || valeur.contains("\"") || valeur.contains("\n")) {
            "\"" + valeur.replace("\"", "\"\"") + "\""
        } else {
            valeur
        }
}
