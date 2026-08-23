package fr.marculus.core

import fr.marculus.core.LotMartelage.ContexteDuLot
import fr.marculus.core.LotMartelage.Rattachement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LotMartelageTest {

    @Test
    fun `une entree plate est acceptee`() {
        assertTrue(LotMartelage.entreeAcceptee("ForetAccess.marsync"))
        assertTrue(LotMartelage.entreeAcceptee("ForetAccess_-_ug_1_-_eclaircie.gpkg"))
    }

    @Test
    fun `zip slip refuse, jamais assaini`() {
        // Le lot est plat : un séparateur de chemin est déjà anormal, et un « .. » réécrit en
        // silence écrirait hors du répertoire d'extraction.
        assertFalse(LotMartelage.entreeAcceptee("../../etc/passwd"))
        assertFalse(LotMartelage.entreeAcceptee("/etc/passwd"))
        assertFalse(LotMartelage.entreeAcceptee("dossier/fichier.gpkg"))
        assertFalse(LotMartelage.entreeAcceptee("dossier\\fichier.gpkg"))
        assertFalse(LotMartelage.entreeAcceptee(".."))
        assertFalse(LotMartelage.entreeAcceptee("   "))
    }

    @Test
    fun `extensions reconnues sans egard a la casse`() {
        assertTrue(LotMartelage.estMarsync("Lot.MARSYNC"))
        assertTrue(LotMartelage.estGpkg("chantier.GPKG"))
        assertFalse(LotMartelage.estGpkg("marculus.json"))
        assertFalse(LotMartelage.estMarsync("chantier.gpkg"))
    }

    @Test
    fun `le nom local est deterministe, donc reimportable sans doublon`() {
        assertEquals("lot-ForetAccess_-_ug_1_-_eclaircie.gpkg", LotMartelage.nomLocal("ForetAccess_-_ug_1_-_eclaircie.gpkg"))
        // Deux fois le même lot → deux fois le même nom → écrasement, pas « fichier(1).gpkg ».
        assertEquals(LotMartelage.nomLocal("a.gpkg"), LotMartelage.nomLocal("a.gpkg"))
    }

    @Test
    fun `le nom local neutralise ce qui n est pas annonce`() {
        // `gpkgNom` est annoncé ASCII, mais l'archive n'est pas forcément celle qu'on croit.
        // Neutralisé, pas translittéré : le nom local n'a pas à être lisible, il a à être sûr.
        assertEquals("lot-ch_ne_vert.gpkg", LotMartelage.nomLocal("chêne vert.gpkg"))
        assertEquals("lot-_.._etc.gpkg", LotMartelage.nomLocal("/../etc.gpkg"))
    }

    @Test
    fun `appariement nominal`() {
        val appariement = LotMartelage.apparier(
            listOf(
                ContexteDuLot("1", "ug 1", "ug_1.gpkg"),
                ContexteDuLot("2", "ug 2", "ug_2.gpkg"),
            ),
            setOf("ug_1.gpkg", "ug_2.gpkg"),
        )
        assertEquals(
            listOf(Rattachement("1", "ug_1.gpkg"), Rattachement("2", "ug_2.gpkg")),
            appariement.rattachements,
        )
        assertTrue(appariement.contextesSansGpkg.isEmpty())
    }

    @Test
    fun `lot ampute, les autres passent et l ecart est nomme`() {
        val appariement = LotMartelage.apparier(
            listOf(
                ContexteDuLot("1", "ug 1", "ug_1.gpkg"),
                ContexteDuLot("2", "ug 2", "ug_2.gpkg"),
            ),
            setOf("ug_1.gpkg"),
        )
        assertEquals(listOf(Rattachement("1", "ug_1.gpkg")), appariement.rattachements)
        assertEquals(listOf("ug 2"), appariement.contextesSansGpkg)
    }

    @Test
    fun `un contexte sans gpkgNom n attend pas de carte`() {
        // Émis par une version antérieure du champ : ce n'est pas un manque, rien à signaler.
        val appariement = LotMartelage.apparier(listOf(ContexteDuLot("1", "ug 1", null)), emptySet())
        assertTrue(appariement.rattachements.isEmpty())
        assertTrue(appariement.contextesSansGpkg.isEmpty())
    }
}
