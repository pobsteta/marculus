package fr.marculus.core.export

import fr.marculus.core.AffouageLots
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.Tige
import java.util.Locale

/**
 * CSV du bilan par lot d'un affouage : le même tableau que l'onglet « Par lot » (tiges, volumes
 * tige et houppier, volume compté au lot, **nets** : [AffouageLots.bilan]). C'est un rapport à lire, comme le CSV foncier :
 * décimaux dans la langue du téléphone (virgule en français, pour le tableur), pas un format
 * d'échange.
 */
object ExportBilanLots {
    private const val SEP = ";"

    fun csv(contexte: Contexte, journal: List<Tige>, locale: Locale): String {
        val bilan = AffouageLots.bilan(contexte, journal)
        fun m3(v: Double) = String.format(locale, "%.3f", v)
        val sb = StringBuilder()
        sb.appendLine("Contexte${SEP}${champ(contexte.nom)}")
        sb.appendLine("ContexteId${SEP}${champ(contexte.id)}")
        sb.appendLine("VolumeMaxLot_m3${SEP}${m3(contexte.volumeMaxLotM3)}")
        sb.appendLine()
        sb.appendLine(listOf("Lot", "Tiges", "VolumeTige_m3", "VolumeHouppier_m3", "VolumeLot_m3", "Etat").joinToString(SEP))
        bilan.forEach { b ->
            val etat = when {
                b.lot == null -> "SANS_LOT"
                b.complet -> "COMPLET"
                else -> "EN_COURS"
            }
            sb.appendLine(
                listOf(
                    b.lot?.toString() ?: "", b.nbTiges.toString(),
                    m3(b.volumeTigeM3), m3(b.volumeHouppierM3), m3(b.volumeM3), etat,
                ).joinToString(SEP),
            )
        }
        sb.appendLine(
            listOf(
                "Total", bilan.sumOf { it.nbTiges }.toString(),
                m3(bilan.sumOf { it.volumeTigeM3 }), m3(bilan.sumOf { it.volumeHouppierM3 }), m3(bilan.sumOf { it.volumeM3 }), "",
            ).joinToString(SEP),
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
