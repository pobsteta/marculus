package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.Tige
import java.util.Locale

/** Parcelle foncière telle que le CSV foncier la décrit (indépendante du GeoPackage). */
data class ParcelleFonciere(
    val id: Long,
    val proprietaire: String?,
    val foret: String?,
    val commune: String?,
    val parcelleNom: String?,
    val surfaceHa: Double,
)

object CsvFoncier {

    /**
     * Construit le CSV « par foncier » (propriétaire/forêt/commune/parcelle × essence × classe) sur les
     * tiges **nettes** ([JournalNet]) ; seules les tiges avec position y figurent.
     */
    fun construire(
        contexte: Contexte,
        journal: List<Tige>,
        parcelleDe: (Tige) -> ParcelleFonciere?,
        locale: Locale,
    ): String {
        val plus = JournalNet.tiges(journal).filter { it.action == ActionTige.PLUS && it.position != null }
        fun champ(s: String) = s.replace(';', ' ').replace('\n', ' ')
        val sb = StringBuilder()
        val tarif = Cubage.aTarif(contexte)
        // Houppier : modèle EMERGE, ou part de houppier en Schaeffer.
        val avecHouppier = Cubage.aHouppier(contexte)
        sb.append("Proprietaire;Foret;Commune;Parcelle;Surface_ha;Essence;Classe;Nombre;Tiges_ha;G_m2;G_ha;Volume_m3;Volume_ha;Houppier_m3;Total_m3\n")
        plus.groupBy(parcelleDe).forEach { (pcl, tiges) ->
            val ha = pcl?.surfaceHa ?: 0.0
            val totalParcelle = tiges.sumOf { it.quantite }
            val densite = if (ha > 0.0) String.format(locale, "%.1f", totalParcelle / ha) else ""
            val surface = if (ha > 0.0) String.format(locale, "%.4f", ha) else ""
            val gParcelle = tiges.sumOf { Cubage.surfaceTerriereUnitaire(contexte, it.classe) * it.quantite }
            val gHaStr = if (ha > 0.0) String.format(locale, "%.2f", gParcelle / ha) else ""
            val volumeParcelle = if (tarif) tiges.sumOf { Cubage.volumeUnitaireTige(contexte, it.essence, it.classe, it.hauteurTexte) * it.quantite } else 0.0
            val volHaStr = if (tarif && ha > 0.0) String.format(locale, "%.2f", volumeParcelle / ha) else ""
            for (essence in contexte.essencesNoms) {
                for (classe in contexte.axe.classes()) {
                    val lot = tiges.filter { it.essence == essence && it.classe == classe }
                    val n = lot.sumOf { it.quantite }
                    if (n > 0) {
                        val gRow = String.format(locale, "%.3f", lot.sumOf { Cubage.surfaceTerriereUnitaire(contexte, it.classe) * it.quantite })
                        var vTige = 0.0; var vHoup = 0.0; var vTot = 0.0
                        if (tarif) {
                            lot.forEach {
                                val v = Cubage.volumesUnitaire(contexte, it.essence, it.classe, it.hauteurTexte)
                                vTige += v.tige * it.quantite; vHoup += v.houppier * it.quantite; vTot += v.total * it.quantite
                            }
                        }
                        val volRow = if (tarif) String.format(locale, "%.3f", vTige) else ""
                        val houpRow = if (avecHouppier) String.format(locale, "%.3f", vHoup) else ""
                        val totRow = if (avecHouppier) String.format(locale, "%.3f", vTot) else ""
                        sb.append(champ(pcl?.proprietaire ?: "")).append(';')
                            .append(champ(pcl?.foret ?: "")).append(';')
                            .append(champ(pcl?.commune ?: "")).append(';')
                            .append(champ(pcl?.parcelleNom ?: pcl?.id?.toString() ?: "")).append(';')
                            .append(surface).append(';')
                            .append(champ(essence)).append(';')
                            .append(classe).append(';')
                            .append(n).append(';')
                            .append(densite).append(';')
                            .append(gRow).append(';')
                            .append(gHaStr).append(';')
                            .append(volRow).append(';')
                            .append(volHaStr).append(';')
                            .append(houpRow).append(';')
                            .append(totRow).append('\n')
                    }
                }
            }
        }
        return sb.toString()
    }
}
