package fr.marculus.core.export

import fr.marculus.core.TotauxMartelage
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.Tige
import java.time.Instant

/** Génère le CSV d'un contexte (totaux + journal des tiges). Logique pure, testable en JVM. */
object ExportCsv {
    private const val SEP = ";"

    fun contexteCsv(contexte: Contexte, journal: List<Tige>): String {
        val sb = StringBuilder()
        sb.appendLine("Contexte${SEP}${champ(contexte.nom)}")
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
            ).joinToString(SEP),
        )
        journal.sortedBy { it.horodatage }.forEach { t ->
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
                ).joinToString(SEP) { champ(it) },
            )
        }
        return sb.toString()
    }

    /** Échappe un champ CSV (guillemets si séparateur, guillemet ou saut de ligne). */
    private fun champ(valeur: String): String =
        if (valeur.contains(SEP) || valeur.contains("\"") || valeur.contains("\n")) {
            "\"" + valeur.replace("\"", "\"\"") + "\""
        } else {
            valeur
        }
}
