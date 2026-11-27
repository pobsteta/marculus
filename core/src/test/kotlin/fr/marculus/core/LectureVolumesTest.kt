package fr.marculus.core

import fr.marculus.core.export.ExportCsv
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Décision de Pascal (2026-11-27) : les colonnes sont renommées, l'ancien nom `VolumeTotalM3` est relu comme
 * bois fort total (tige + houppier, découpe 7 cm). Le fichier ci-dessous est un en-tête de CSV format 8, écrit
 * à la main tel que Marculus l'émettait avant la correction.
 */
class LectureVolumesTest {

    private val ancienCsv = """
        Contexte;Parcelle 12
        FormatCsv;8
        ContexteId;c1
        Tarif;EMERGE
        VolumeTigeTotalM3;0.1557
        VolumeTotalM3;0.1808
        SurfaceTerriereTotaleM2;0.3097
        VolumeHouppierTotalM3;0.0251

        JOURNAL
        Horodatage;Essence;VolumeTotalM3
    """.trimIndent()

    @Test
    fun `un ancien csv - VolumeTotalM3 est compris comme bois fort total`() {
        val champs = LectureVolumes.entete(ancienCsv)
        assertEquals(0.1808, LectureVolumes.boisFortTotal(champs))
        assertNull(LectureVolumes.aerienTotal(champs), "un ancien fichier n'a pas de volume aérien total")
    }

    @Test
    fun `une ancienne sauvegarde json - volumeTotalM3 est compris comme bois fort total`() {
        val champs = mapOf("volumeTigeTotalM3" to "0.1557", "volumeTotalM3" to "0.1808", "volumeHouppierTotalM3" to "0.0251")
        assertEquals(0.1808, LectureVolumes.boisFortTotal(champs))
        assertNull(LectureVolumes.aerienTotal(champs))
    }

    @Test
    fun `le nouveau nom prime sur l ancien`() {
        val champs = mapOf("VolumeTotalM3" to "9.0", "VolumeBoisFortTotalM3" to "0.1808", "VolumeAerienTotalM3" to "0.2365")
        assertEquals(0.1808, LectureVolumes.boisFortTotal(champs))
        assertEquals(0.2365, LectureVolumes.aerienTotal(champs))
    }

    @Test
    fun `un csv ecrit aujourd hui se relit sous les nouveaux noms, sans VolumeTotalM3`() {
        val ctx = Contexte(
            id = "c1", nom = "Parcelle 12", mode = ModeMesure.CIRCONFERENCE, axe = AxeClasses(20, 140, 5),
            essences = emptyList(), tarif = TarifCubage.EMERGE,
        )
        val journal = listOf(Tige("u-a", "c1", "Hêtre", 60, ActionTige.PLUS, horodatage = 1000L, hauteurTexte = "10"))
        val csv = ExportCsv.contexteCsv(ctx, journal)
        val champs = LectureVolumes.entete(csv)
        assertTrue("VolumeTotalM3" !in champs, "l'ancien nom n'est plus écrit")
        val attendu = VolumesMartelage.totaux(ctx, journal)
        assertEquals(attendu.volumeBoisFortTotalM3, LectureVolumes.boisFortTotal(champs)!!, 1e-4)
        assertEquals(attendu.volumeAerienTotalM3, LectureVolumes.aerienTotal(champs)!!, 1e-4)
        assertTrue(attendu.volumeAerienTotalM3 > attendu.volumeBoisFortTotalM3)
    }

    @Test
    fun `sans EMERGE le volume aerien total n est pas exporte`() {
        val ctx = Contexte(
            id = "c1", nom = "S", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
            essences = emptyList(), tarif = TarifCubage.SCHAEFFER_RAPIDE, tarifNumero = 8,
        )
        val champs = LectureVolumes.entete(ExportCsv.contexteCsv(ctx, listOf(Tige("u-a", "c1", "Chêne", 40, ActionTige.PLUS, horodatage = 1L))))
        assertEquals("", champs["VolumeAerienTotalM3"])
        assertNull(LectureVolumes.aerienTotal(champs))
    }
}

/** Contexte mixte (décision de Pascal, 2026-11-27) : l'aérien total somme les seules tiges EMERGE, et le dit. */
class ContexteMixteAerienTest {
    private val ctx = Contexte(
        id = "c1", nom = "Mixte", mode = ModeMesure.CIRCONFERENCE, axe = AxeClasses(20, 140, 5),
        essences = emptyList(), tarif = TarifCubage.SCHAEFFER_RAPIDE, tarifNumero = 8,
        tarifsEssence = mapOf("Hêtre" to fr.marculus.core.model.TarifEssence(TarifCubage.EMERGE, 0)),
    )
    private val journal = listOf(
        Tige("a", "c1", "Hêtre", 60, ActionTige.PLUS, horodatage = 1L, quantite = 2, hauteurTexte = "10"),
        Tige("b", "c1", "Chêne", 60, ActionTige.PLUS, horodatage = 2L, quantite = 3),
        Tige("x", "c1", "Hêtre", 60, ActionTige.ANNULATION, horodatage = 3L),
    )

    @Test
    fun `l aerien total ne somme que les tiges EMERGE et compte celles-ci`() {
        val t = VolumesMartelage.totaux(ctx, journal)
        val hetre = VolumesMartelage.cubage(ctx, journal[0])
        assertEquals(1, t.nbTigesAerien) // 2 hêtres moins 1 annulé
        assertEquals(4, t.nbTiges) // 1 hêtre + 3 chênes
        assertEquals(hetre.volumeAerienTotalM3, t.volumeAerienTotalM3, 1e-9)
        assertTrue(t.volumeAerienTotalM3 > 0.0)
        assertTrue(t.nbTigesAerien < t.nbTiges)
    }

    @Test
    fun `l export donne le nombre de tiges couvertes, le journal laisse vide les autres`() {
        val csv = ExportCsv.contexteCsv(ctx, journal)
        val champs = LectureVolumes.entete(csv)
        assertEquals("1", champs["NbTigesVolumeAerien"])
        val lignes = csv.lines()
        val cellules = lignes.drop(lignes.indexOf("JOURNAL") + 2).takeWhile { it.isNotBlank() }.map { it.split(";") }
        assertTrue(cellules.first { it[1] == "Hêtre" && it[3] == "PLUS" }[22].toDouble() > 0.0)
        assertEquals("", cellules.first { it[1] == "Chêne" }[22])
    }

    @Test
    fun `sans EMERGE le nombre de tiges couvertes est vide`() {
        val s = ctx.copy(tarifsEssence = emptyMap())
        assertEquals("", LectureVolumes.entete(ExportCsv.contexteCsv(s, journal))["NbTigesVolumeAerien"])
    }
}
