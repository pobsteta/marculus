package fr.marculus.core.export

import fr.marculus.core.VolumesMartelage
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.EtatKanban
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.Position
import fr.marculus.core.model.QualiteFix
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige
import java.util.Locale
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
    fun `l en-tete du format 3 suit la ligne Contexte, dans l ordre`() {
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
                "FormatCsv;3",
                "ContexteId;act_20260923155141_xyz123",
                "Statut;REALISEE",
                "DateMartelage;2027-10-15",
                "Modifie;1790000000000",
                "Tarif;AUCUN",
                "TarifNumero;0",
                "CoefficientForme;0.5",
                "VolumeTigeTotalM3;0.0000",
                "VolumeTotalM3;0.0000",
                "SurfaceTerriereTotaleM2;0.0000",
                "NbTigesNonCubees;0",
                "Mode;DIAMETRE",
                "Increment;1",
            ),
            lignes.take(15),
        )
    }

    @Test
    fun `date de martelage vide quand le contexte n en a pas`() {
        val lignes = ExportCsv.contexteCsv(contexte, emptyList()).split("\n")
        assertTrue("DateMartelage;" in lignes)
        assertTrue("Statut;PROPOSEE" in lignes)
    }

    @Test
    fun `le journal compte 20 colonnes dont les 15 du format 2 inchangees`() {
        val tige = Tige(
            uuid = "6f1c-uuid", contexteId = "c1", essence = "Chêne", classe = 20,
            action = ActionTige.PLUS, horodatage = 1000L, hauteurTexte = "27-6AB", qualiteArbre = "B",
            position = Position(47.912345, 1.905432), operateur = "PO",
            parcelle = "B 7", qualiteFix = QualiteFix.RTK_FIXE, precisionM = 0.02, modifie = 1_790_000_000_000L,
        )
        val csv = ExportCsv.contexteCsv(contexte, listOf(tige))
        val lignes = csv.split("\n")
        val entete = cellules(lignes[lignes.indexOf("JOURNAL") + 1])
        assertEquals(20, entete.size)
        assertEquals(colonnesFormat1, entete.take(12))
        assertEquals(listOf("Uuid", "Parcelle", "Modifie"), entete.subList(12, 15))
        assertEquals(
            listOf("VolumeTigeM3", "VolumeHouppierM3", "VolumeTotalM3", "SurfaceTerriereM2", "Cubage"),
            entete.drop(15),
        )
        val ligne = lignesJournal(csv).single()
        assertEquals(20, ligne.size)
        assertEquals(
            listOf(
                "1970-01-01T00:00:01Z", "Chêne", "20", "PLUS", "1", "27-6AB", "B",
                "47.912345", "1.905432", "PO", "RTK fixe", "0.02",
                "6f1c-uuid", "B 7", "1790000000000",
            ),
            ligne.take(15),
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

    // --- Format 3 : volumes (brief nemetonshiny 2026-09-25) ---

    @Test
    fun `volumes du csv identiques au calcul partage avec le marsync et l ecran Statut`() {
        val ctx = contexte.copy(tarif = TarifCubage.EMERGE)
        val journal = listOf(
            Tige("u-a", "c1", "Hêtre", 25, ActionTige.PLUS, horodatage = 1000L, hauteurTexte = "22"),
            Tige("u-b", "c1", "Hêtre", 25, ActionTige.PLUS, horodatage = 2000L, hauteurTexte = "26"),
            Tige("u-c", "c1", "Chêne", 30, ActionTige.PLUS, horodatage = 3000L), // sans hauteur
            Tige("u-x", "c1", "Hêtre", 25, ActionTige.ANNULATION, horodatage = 4000L),
        )
        val csv = ExportCsv.contexteCsv(ctx, journal)
        val lignes = csv.split("\n")
        val attendu = VolumesMartelage.totaux(ctx, journal)
        assertTrue("VolumeTigeTotalM3;${"%.4f".format(Locale.ROOT, attendu.volumeTigeM3)}" in lignes)
        assertTrue("VolumeTotalM3;${"%.4f".format(Locale.ROOT, attendu.volumeTotalM3)}" in lignes)
        assertTrue("NbTigesNonCubees;1" in lignes)
        lignesJournal(csv).forEach { c ->
            val t = journal.single { it.uuid == c[12] }
            val v = VolumesMartelage.cubage(ctx, t)
            assertEquals(v.volumeTigeM3, c[15].toDouble(), 1e-6)
            assertEquals(v.volumeHouppierM3, c[16].toDouble(), 1e-6)
            assertEquals(v.volumeTotalM3, c[17].toDouble(), 1e-6)
            assertEquals(v.surfaceTerriereM2, c[18].toDouble(), 1e-6)
            assertEquals(v.cubage, c[19])
        }
        assertEquals("NON_CUBABLE", lignesJournal(csv).single { it[12] == "u-c" }[19])
    }

    @Test
    fun `les decimaux des volumes restent a point meme en francais`() {
        val defaut = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRANCE)
            val ctx = contexte.copy(tarif = TarifCubage.SCHAEFFER_RAPIDE, tarifNumero = 8)
            val csv = ExportCsv.contexteCsv(ctx, listOf(Tige("u", "c1", "Chêne", 30, ActionTige.PLUS, horodatage = 1L)))
            val ligne = lignesJournal(csv).single()
            assertTrue(ligne[15].contains('.') && !ligne[15].contains(','))
            assertEquals("SCHAEFFER_RAPIDE:8", ligne[19])
        } finally {
            Locale.setDefault(defaut)
        }
    }
}
