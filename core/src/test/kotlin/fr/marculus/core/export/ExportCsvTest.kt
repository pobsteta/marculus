package fr.marculus.core.export

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.EtatKanban
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.Position
import fr.marculus.core.model.QualiteFix
import fr.marculus.core.model.Tige
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportCsvTest {

    private val contexte = Contexte(
        id = "c1",
        nom = "Parcelle 12",
        mode = ModeMesure.DIAMETRE,
        axe = AxeClasses(20, 30, 5),
        essences = listOf(EssenceColonne("Chêne", 0, 0), EssenceColonne("Hêtre", 0, 0)),
        increment = 1,
    )

    private fun tige(essence: String, classe: Int, action: ActionTige, t: Long) =
        Tige("u$t", "c1", essence, classe, action, horodatage = t, quantite = 1)

    @Test
    fun `le csv contient les sections totaux et journal`() {
        val journal = listOf(
            tige("Chêne", 20, ActionTige.PLUS, 1000L),
            tige("Chêne", 20, ActionTige.PLUS, 2000L),
            tige("Hêtre", 25, ActionTige.PLUS, 3000L),
        )
        val csv = ExportCsv.contexteCsv(contexte, journal)
        assertTrue(csv.contains("TOTAUX"))
        assertTrue(csv.contains("JOURNAL"))
        assertTrue(csv.contains("Chêne;20;2")) // total dérivé
        assertTrue(csv.contains("Parcelle 12"))
    }

    @Test
    fun `le journal exporte la qualite de fix et la precision`() {
        val tige = Tige(
            uuid = "u1", contexteId = "c1", essence = "Chêne", classe = 20,
            action = ActionTige.PLUS, horodatage = 1000L,
            position = Position(47.0, 8.0), qualiteFix = QualiteFix.RTK_FIXE, precisionM = 0.02,
        )
        val csv = ExportCsv.contexteCsv(contexte, listOf(tige))
        assertTrue(csv.contains("QualiteFix${";"}Precision_m"))
        assertTrue(csv.contains("RTK fixe"))
        assertTrue(csv.contains("0.02"))
    }

    @Test
    fun `un champ contenant le separateur est entoure de guillemets`() {
        val ctx = contexte.copy(nom = "Bois; du Roi")
        val csv = ExportCsv.contexteCsv(ctx, emptyList())
        assertTrue(csv.contains("\"Bois; du Roi\""))
    }

    // --- Format 2 : réimportable par Nemeton (brief nemetonshiny 2026-09-25) ---

    private val colonnesFormat1 = listOf(
        "Horodatage", "Essence", "Classe", "Action", "Quantite", "Hauteur", "QualiteArbre",
        "Latitude", "Longitude", "Operateur", "QualiteFix", "Precision_m",
    )

    /** Découpe une ligne CSV `;` en respectant les guillemets (échappement de `champ()`). */
    private fun cellules(ligne: String): List<String> {
        val res = mutableListOf<String>()
        val cour = StringBuilder()
        var entre = false
        var i = 0
        while (i < ligne.length) {
            val c = ligne[i]
            when {
                entre && c == '"' && ligne.getOrNull(i + 1) == '"' -> { cour.append('"'); i++ }
                c == '"' -> entre = !entre
                c == ';' && !entre -> { res.add(cour.toString()); cour.clear() }
                else -> cour.append(c)
            }
            i++
        }
        res.add(cour.toString())
        return res
    }

    private fun lignesJournal(csv: String): List<List<String>> {
        val lignes = csv.split("\n")
        val debut = lignes.indexOf("JOURNAL") + 2
        return lignes.drop(debut).filter { it.isNotEmpty() }.map(::cellules)
    }

    @Test
    fun `les cinq lignes d en-tete du format 2 suivent la ligne Contexte, dans l ordre`() {
        val ctx = contexte.copy(
            id = "act_20260923155141_xyz123",
            statut = EtatKanban.REALISEE,
            dateMartelage = 1_823_558_400_000L, // 2027-10-15T00:00:00Z
            modifie = 1_790_000_000_000L,
        )
        val lignes = ExportCsv.contexteCsv(ctx, emptyList()).split("\n")
        assertEquals(
            listOf(
                "Contexte;Parcelle 12",
                "FormatCsv;2",
                "ContexteId;act_20260923155141_xyz123",
                "Statut;REALISEE",
                "DateMartelage;2027-10-15",
                "Modifie;1790000000000",
                "Mode;DIAMETRE",
                "Increment;1",
            ),
            lignes.take(8),
        )
    }

    @Test
    fun `date de martelage vide quand le contexte n en a pas`() {
        val lignes = ExportCsv.contexteCsv(contexte, emptyList()).split("\n")
        assertTrue("DateMartelage;" in lignes)
        assertTrue("Statut;PROPOSEE" in lignes)
    }

    @Test
    fun `le journal compte 15 colonnes dont les 12 premieres inchangees`() {
        val tige = Tige(
            uuid = "6f1c-uuid", contexteId = "c1", essence = "Chêne", classe = 20,
            action = ActionTige.PLUS, horodatage = 1000L, hauteurTexte = "27-6AB", qualiteArbre = "B",
            position = Position(47.912345, 1.905432), operateur = "PO",
            parcelle = "B 7", qualiteFix = QualiteFix.RTK_FIXE, precisionM = 0.02, modifie = 1_790_000_000_000L,
        )
        val csv = ExportCsv.contexteCsv(contexte, listOf(tige))
        val lignes = csv.split("\n")
        val entete = cellules(lignes[lignes.indexOf("JOURNAL") + 1])
        assertEquals(15, entete.size)
        assertEquals(colonnesFormat1, entete.take(12))
        assertEquals(listOf("Uuid", "Parcelle", "Modifie"), entete.drop(12))
        val ligne = lignesJournal(csv).single()
        assertEquals(15, ligne.size)
        assertEquals(
            listOf(
                "1970-01-01T00:00:01Z", "Chêne", "20", "PLUS", "1", "27-6AB", "B",
                "47.912345", "1.905432", "PO", "RTK fixe", "0.02",
                "6f1c-uuid", "B 7", "1790000000000",
            ),
            ligne,
        )
    }

    @Test
    fun `aller-retour - les tiges du csv sont exactement celles du journal`() {
        // Le .marsync sérialise ce même journal (tige par tige, même uuid) : le CSV doit en porter
        // exactement les mêmes tiges, annulations comprises.
        val journal = listOf(
            Tige("u-a", "c1", "Chêne", 20, ActionTige.PLUS, horodatage = 3000L, quantite = 1),
            Tige("u-b", "c1", "Hêtre", 25, ActionTige.PLUS, horodatage = 1000L, quantite = 2),
            Tige("u-c", "c1", "Chêne", 20, ActionTige.ANNULATION, horodatage = 2000L, quantite = 1),
        )
        val duCsv = lignesJournal(ExportCsv.contexteCsv(contexte, journal))
            .map { c -> listOf(c[12], c[1], c[2], c[3], c[4]) }
            .toSet()
        val duJournal = journal.map { listOf(it.uuid, it.essence, it.classe.toString(), it.action.name, it.quantite.toString()) }
            .toSet()
        assertEquals(duJournal, duCsv)
    }

    @Test
    fun `un nom de contexte avec separateur et guillemet reste lisible`() {
        val ctx = contexte.copy(nom = "Bois; du \"Roi\"")
        val premiere = ExportCsv.contexteCsv(ctx, emptyList()).split("\n").first()
        assertEquals(listOf("Contexte", "Bois; du \"Roi\""), cellules(premiere))
    }
}
