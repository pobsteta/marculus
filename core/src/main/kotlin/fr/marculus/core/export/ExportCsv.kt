package fr.marculus.core.export

import fr.marculus.core.TotauxMartelage
import fr.marculus.core.VolumesMartelage
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.Tige
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/**
 * Génère le CSV d'un contexte (totaux + journal des tiges). Logique pure, testable en JVM.
 *
 * **Format 2** (`FormatCsv;2`) : réimportable par Nemeton comme un `.marsync`. Il ajoute, sans
 * rien déplacer, les clés d'appariement (id du contexte, uuid des tiges), le statut, la date de
 * martelage et les horodatages `modifie`. Les lecteurs du format 1 lisent toujours les mêmes
 * lignes aux mêmes places.
 *
 * **Format 3** (`FormatCsv;3`) : ajoute les volumes calculés sur le téléphone ([VolumesMartelage]) —
 * tarif et totaux nets en en-tête, cinq colonnes unitaires en fin de journal.
 */
object ExportCsv {
    private const val SEP = ";"

    /** Version du format, lue par Nemeton : son absence désigne le format 1, non réimportable. */
    const val FORMAT = 3

    fun contexteCsv(contexte: Contexte, journal: List<Tige>): String {
        val sb = StringBuilder()
        sb.appendLine("Contexte${SEP}${champ(contexte.nom)}")
        sb.appendLine("FormatCsv${SEP}$FORMAT")
        sb.appendLine("ContexteId${SEP}${champ(contexte.id)}")
        sb.appendLine("Statut${SEP}${contexte.statut.name}")
        // Date civile, calculée en UTC comme dans le .marsync (minuit UTC).
        sb.appendLine("DateMartelage${SEP}${contexte.dateMartelage?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: ""}")
        sb.appendLine("Modifie${SEP}${contexte.modifie}")
        // Format 3 : tarif et totaux nets, tels que l'écran Statut les affiche.
        val totauxVolumes = VolumesMartelage.totaux(contexte, journal)
        sb.appendLine("Tarif${SEP}${contexte.tarif.name}")
        sb.appendLine("TarifNumero${SEP}${contexte.tarifNumero}")
        sb.appendLine("CoefficientForme${SEP}${contexte.coefficientForme}")
        sb.appendLine("VolumeTigeTotalM3${SEP}${decimal(totauxVolumes.volumeTigeM3, DECIMALES_TOTAL)}")
        sb.appendLine("VolumeTotalM3${SEP}${decimal(totauxVolumes.volumeTotalM3, DECIMALES_TOTAL)}")
        sb.appendLine("SurfaceTerriereTotaleM2${SEP}${decimal(totauxVolumes.surfaceTerriereM2, DECIMALES_TOTAL)}")
        sb.appendLine("NbTigesNonCubees${SEP}${totauxVolumes.nbTigesNonCubees}")
        sb.appendLine("Mode${SEP}${contexte.mode}")
        sb.appendLine("Increment${SEP}${contexte.increment}")
        contexte.commentaire?.let { sb.appendLine("Commentaire${SEP}${champ(it)}") }
        sb.appendLine()

        sb.appendLine("TOTAUX")
        sb.appendLine(listOf("Essence", "Classe", "Total").joinToString(SEP))
        val totaux = TotauxMartelage(journal).totaux()
        contexte.essencesNoms.forEach { e ->
            contexte.axe.classes().forEach { c ->
                val t = totaux[CompteurCle(e, c)] ?: 0
                if (t != 0) sb.appendLine(listOf(champ(e), c.toString(), t.toString()).joinToString(SEP))
            }
        }
        sb.appendLine()

        sb.appendLine("JOURNAL")
        sb.appendLine(
            listOf(
                "Horodatage", "Essence", "Classe", "Action", "Quantite",
                "Hauteur", "QualiteArbre", "Latitude", "Longitude", "Operateur",
                "QualiteFix", "Precision_m",
                // Format 2 : ajoutées en fin de ligne, les douze premières colonnes ne bougent pas.
                "Uuid", "Parcelle", "Modifie",
                // Format 3 : valeurs unitaires (une tige), après les quinze colonnes du format 2.
                "VolumeTigeM3", "VolumeHouppierM3", "VolumeTotalM3", "SurfaceTerriereM2", "Cubage",
            ).joinToString(SEP),
        )
        journal.sortedBy { it.horodatage }.forEach { t ->
            val c = VolumesMartelage.cubage(contexte, t)
            sb.appendLine(
                listOf(
                    Instant.ofEpochMilli(t.horodatage).toString(),
                    t.essence,
                    t.classe.toString(),
                    t.action.name,
                    t.quantite.toString(),
                    t.hauteurTexte ?: "",
                    t.qualiteArbre ?: "",
                    t.position?.latitude?.toString() ?: "",
                    t.position?.longitude?.toString() ?: "",
                    t.operateur ?: "",
                    t.qualiteFix?.libelle ?: "",
                    t.precisionM?.toString() ?: "",
                    t.uuid,
                    t.parcelle ?: "",
                    t.modifie.toString(),
                    decimal(c.volumeTigeM3, DECIMALES_UNITAIRE),
                    decimal(c.volumeHouppierM3, DECIMALES_UNITAIRE),
                    decimal(c.volumeTotalM3, DECIMALES_UNITAIRE),
                    decimal(c.surfaceTerriereM2, DECIMALES_UNITAIRE),
                    c.cubage,
                ).joinToString(SEP) { champ(it) },
            )
        }
        return sb.toString()
    }

    /** Un petit bois dépasse rarement 0,05 m³ : six décimales gardent trois chiffres significatifs. */
    private const val DECIMALES_UNITAIRE = 6
    private const val DECIMALES_TOTAL = 4

    /** Décimal à point, quelle que soit la langue du téléphone. */
    private fun decimal(v: Double, decimales: Int): String = String.format(Locale.ROOT, "%.${decimales}f", v)

    /** Échappe un champ CSV (guillemets si séparateur, guillemet ou saut de ligne). */
    private fun champ(valeur: String): String =
        if (valeur.contains(SEP) || valeur.contains("\"") || valeur.contains("\n")) {
            "\"" + valeur.replace("\"", "\"\"") + "\""
        } else {
            valeur
        }
}
