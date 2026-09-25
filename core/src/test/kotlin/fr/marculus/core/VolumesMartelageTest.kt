package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VolumesMartelageTest {
    private val eps = 1e-12

    private fun contexte(tarif: TarifCubage, numero: Int = 0) = Contexte(
        id = "c1", nom = "P", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
        essences = listOf(EssenceColonne("Hêtre", 0, 0)), tarif = tarif, tarifNumero = numero,
    )

    private fun tige(
        uuid: String, t: Long, action: ActionTige = ActionTige.PLUS, classe: Int = 40,
        hauteur: String? = null, essence: String = "Hêtre", quantite: Int = 1,
    ) = Tige(uuid, "c1", essence, classe, action, horodatage = t, quantite = quantite, hauteurTexte = hauteur)

    @Test
    fun `schaeffer rapide tarif 8 au centre de classe`() {
        val ctx = contexte(TarifCubage.SCHAEFFER_RAPIDE, 8)
        val c = VolumesMartelage.cubage(ctx, tige("a", 1))
        assertEquals(Cubage.volume(TarifCubage.SCHAEFFER_RAPIDE, 8, 42.5), c.volumeTigeM3, eps)
        assertEquals(c.volumeTigeM3, c.volumeTotalM3, eps)
        assertEquals(0.0, c.volumeHouppierM3, eps)
        assertEquals("SCHAEFFER_RAPIDE:8", c.cubage)
        assertEquals(Math.PI / 4 * 0.425 * 0.425, c.surfaceTerriereM2, eps)
    }

    @Test
    fun `emerge avec hauteur - total = tige + houppier`() {
        val c = VolumesMartelage.cubage(contexte(TarifCubage.EMERGE), tige("a", 1, hauteur = "27"))
        assertEquals("EMERGE", c.cubage)
        assertEquals(c.volumeTigeM3 + c.volumeHouppierM3, c.volumeTotalM3, eps)
        assertTrue(c.volumeTigeM3 > 0.0)
    }

    @Test
    fun `emerge sans hauteur - non cubable, volumes nuls, compte dans les non cubees`() {
        val ctx = contexte(TarifCubage.EMERGE)
        val c = VolumesMartelage.cubage(ctx, tige("a", 1))
        assertEquals("NON_CUBABLE", c.cubage)
        assertEquals(0.0, c.volumeTotalM3, eps)
        assertEquals(1, VolumesMartelage.totaux(ctx, listOf(tige("a", 1), tige("b", 2, hauteur = "25"))).nbTigesNonCubees)
    }

    @Test
    fun `essence hors emerge - repli coefficient de forme`() {
        val c = VolumesMartelage.cubage(contexte(TarifCubage.EMERGE), tige("a", 1, hauteur = "20", essence = "Zzz inconnue"))
        assertEquals("FORME:0.5", c.cubage)
    }

    @Test
    fun `tarif aucun`() {
        val c = VolumesMartelage.cubage(contexte(TarifCubage.AUCUN), tige("a", 1, hauteur = "20"))
        assertEquals("AUCUN", c.cubage)
        assertEquals(0.0, c.volumeTotalM3, eps)
    }

    @Test
    fun `une annulation retire le volume de la derniere tige de sa case`() {
        // L'annulation est saisie sans hauteur : c'est le volume de la tige annulée qui part.
        val ctx = contexte(TarifCubage.EMERGE)
        val a = tige("a", 1, hauteur = "25")
        val b = tige("b", 2, hauteur = "30")
        val annul = tige("x", 3, action = ActionTige.ANNULATION)
        val t = VolumesMartelage.totaux(ctx, listOf(a, b, annul))
        val va = VolumesMartelage.cubage(ctx, a)
        assertEquals(va.volumeTigeM3, t.volumeTigeM3, eps)
        assertEquals(va.volumeTotalM3, t.volumeTotalM3, eps)
        assertEquals(va.surfaceTerriereM2, t.surfaceTerriereM2, eps)
        assertEquals(0, t.nbTigesNonCubees)
    }

    @Test
    fun `annuler une tige non cubee la retire du compte`() {
        val ctx = contexte(TarifCubage.EMERGE)
        val t = VolumesMartelage.totaux(ctx, listOf(tige("a", 1), tige("x", 2, action = ActionTige.ANNULATION)))
        assertEquals(0, t.nbTigesNonCubees)
        assertEquals(0.0, t.volumeTotalM3, eps)
        assertEquals(0.0, t.surfaceTerriereM2, eps)
    }

    @Test
    fun `annulation d une autre case ou sans tige a retirer`() {
        val ctx = contexte(TarifCubage.SCHAEFFER_LENT, 5)
        // Classe 40 comptée ; annulation en classe 50 sans tige : elle vaut sa propre saisie.
        val t = VolumesMartelage.totaux(
            ctx,
            listOf(tige("a", 1, classe = 40), tige("x", 2, action = ActionTige.ANNULATION, classe = 50)),
        )
        val v40 = Cubage.volume(TarifCubage.SCHAEFFER_LENT, 5, 42.5)
        val v50 = Cubage.volume(TarifCubage.SCHAEFFER_LENT, 5, 52.5)
        assertEquals(v40 - v50, t.volumeTigeM3, eps)
    }

    @Test
    fun `quantites - une annulation de 2 retire les deux dernieres unites`() {
        val ctx = contexte(TarifCubage.EMERGE)
        val a = tige("a", 1, hauteur = "25", quantite = 2)
        val b = tige("b", 2, hauteur = "30")
        val t = VolumesMartelage.totaux(ctx, listOf(a, b, tige("x", 3, action = ActionTige.ANNULATION, quantite = 2)))
        // Il reste une unité de « a ».
        assertEquals(VolumesMartelage.cubage(ctx, a).volumeTotalM3, t.volumeTotalM3, eps)
    }
}
