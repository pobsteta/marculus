package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.TarifEssence
import fr.marculus.core.model.Tige
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mission 022 : une seule règle d'annulation. Oracle : les totaux de [VolumesMartelage.totaux]
 * égalent la somme des cubages ([VolumesMartelage.cubage]) des tiges rendues par
 * [JournalNet.tiges] ; une annulation orpheline (gardée par JournalNet) retire le cubage de sa
 * propre saisie. Filet de la refactorisation : verts avant comme après.
 */
class UneRegleAnnulationTest {
    private val eps = 1e-9

    private fun ctx(
        tarif: TarifCubage = TarifCubage.AUCUN, numero: Int = 0,
        part: Map<String, Double> = emptyMap(), parEssence: Map<String, TarifEssence> = emptyMap(),
    ) = Contexte(
        id = "c1", nom = "P", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
        essences = listOf(EssenceColonne("Hêtre", 0, 0), EssenceColonne("Chêne", 0, 0)),
        tarif = tarif, tarifNumero = numero, partHouppier = part, tarifsEssence = parEssence,
    )

    private fun plus(u: String, t: Long, e: String = "Hêtre", c: Int = 40, q: Int = 1, h: String? = null) =
        Tige(u, "c1", e, c, ActionTige.PLUS, horodatage = t, quantite = q, hauteurTexte = h)

    private fun moins(u: String, t: Long, e: String = "Hêtre", c: Int = 40, q: Int = 1, h: String? = null) =
        Tige(u, "c1", e, c, ActionTige.ANNULATION, horodatage = t, quantite = q, hauteurTexte = h)

    /** Somme des cubages des tiges de JournalNet ; une annulation rendue (orpheline) soustrait. */
    private fun oracle(c: Contexte, j: List<Tige>): VolumesMartelage.Totaux {
        var vt = 0.0; var vh = 0.0; var vtot = 0.0; var g = 0.0; var nc = 0
        JournalNet.tiges(j).forEach { t ->
            val s = if (t.action == ActionTige.PLUS) 1 else -1
            val cu = VolumesMartelage.cubage(c, t)
            vt += s * cu.volumeTigeM3 * t.quantite
            vh += s * cu.volumeHouppierM3 * t.quantite
            vtot += s * cu.volumeTotalM3 * t.quantite
            g += s * cu.surfaceTerriereM2 * t.quantite
            if (cu.nonCubee) nc += s * t.quantite
        }
        return VolumesMartelage.Totaux(vt, vh, vtot, g, nc.coerceAtLeast(0))
    }

    private fun verifie(c: Contexte, j: List<Tige>) {
        val a = VolumesMartelage.totaux(c, j)
        val b = oracle(c, j)
        assertEquals(b.volumeTigeM3, a.volumeTigeM3, eps, "tige")
        assertEquals(b.volumeHouppierM3, a.volumeHouppierM3, eps, "houppier")
        assertEquals(b.volumeTotalM3, a.volumeTotalM3, eps, "total")
        assertEquals(b.surfaceTerriereM2, a.surfaceTerriereM2, eps, "surface terrière")
        assertEquals(b.nbTigesNonCubees, a.nbTigesNonCubees, "non cubées")
    }

    private val schaeffer = ctx(TarifCubage.SCHAEFFER_RAPIDE, 8)
    private val schaefferHouppier = ctx(TarifCubage.SCHAEFFER_LENT, 6, part = mapOf("Hêtre" to 0.25, "Chêne" to 0.4))
    private val emerge = ctx(TarifCubage.EMERGE)
    private val sansTarif = ctx()
    private val tous = listOf(schaeffer, schaefferHouppier, emerge, sansTarif)

    @Test
    fun `journal vide`() = tous.forEach { verifie(it, emptyList()) }

    @Test
    fun `nominal - ajouts et une annulation`() = tous.forEach {
        verifie(it, listOf(plus("a", 1, h = "25"), plus("b", 2, h = "27"), moins("x", 3)))
    }

    @Test
    fun `saisies dans le desordre`() = tous.forEach {
        verifie(it, listOf(moins("x", 5), plus("b", 3, h = "27"), plus("a", 1, h = "20"), plus("c", 4, h = "30")))
    }

    @Test
    fun `egalites d'horodatage departagees par uuid`() = tous.forEach {
        // Même horodatage : l'ordre est celui des uuid, quel que soit l'ordre dans la liste.
        val j = listOf(moins("m", 1, c = 40), plus("z", 1, c = 40, h = "30"), plus("a", 1, c = 40, h = "20"))
        verifie(it, j)
        verifie(it, j.reversed())
        assertEquals(VolumesMartelage.totaux(it, j), VolumesMartelage.totaux(it, j.reversed()))
    }

    @Test
    fun `paquets partiellement annules`() = tous.forEach {
        verifie(it, listOf(
            plus("a", 1, q = 5, h = "22"), plus("b", 2, q = 3, h = "28"),
            moins("x", 3, q = 4), moins("y", 4, q = 2),
        ))
    }

    @Test
    fun `annulation plus grosse que le paquet - deborde sur le paquet precedent`() = tous.forEach {
        verifie(it, listOf(plus("a", 1, q = 2, h = "22"), plus("b", 2, q = 2, h = "28"), moins("x", 3, q = 3)))
    }

    @Test
    fun `annulation orpheline seule`() = tous.forEach {
        verifie(it, listOf(moins("x", 1, q = 2, h = "25")))
    }

    @Test
    fun `annulation orpheline partielle apres epuisement de la case`() = tous.forEach {
        verifie(it, listOf(plus("a", 1, q = 2, h = "25"), moins("x", 2, q = 5, h = "25")))
    }

    @Test
    fun `annulation d'une autre case reste orpheline`() = tous.forEach {
        verifie(it, listOf(plus("a", 1, c = 40, h = "25"), moins("x", 2, c = 45, h = "25"), moins("y", 3, e = "Chêne", c = 40)))
    }

    @Test
    fun `plusieurs cases essence x classe`() = tous.forEach {
        verifie(it, listOf(
            plus("a", 1, "Hêtre", 30, 2, "20"), plus("b", 2, "Chêne", 30, 3, "21"),
            plus("c", 3, "Hêtre", 50, 1, "30"), plus("d", 4, "Chêne", 50, 4, "31"),
            moins("w", 5, "Chêne", 30), moins("x", 6, "Hêtre", 50), moins("y", 7, "Chêne", 50, 2),
            plus("e", 8, "Hêtre", 30, 1, "20"), moins("z", 9, "Hêtre", 30, 2),
        ))
    }

    @Test
    fun `tarif par essence mixte et essence inconnue`() {
        val c = ctx(TarifCubage.EMERGE, parEssence = mapOf(
            "Chêne" to TarifEssence(TarifCubage.SCHAEFFER_INTERMEDIAIRE, 5),
            "Hêtre" to TarifEssence(TarifCubage.AUCUN),
        ), part = mapOf("Chêne" to 0.3))
        verifie(c, listOf(
            plus("a", 1, "Chêne", 40, 3, "25"), plus("b", 2, "Hêtre", 40, 2, "25"),
            plus("c", 3, "Zzz inconnue", 40, 2, "20"), moins("x", 4, "Chêne", 40), moins("y", 5, "Zzz inconnue", 40),
        ))
    }

    @Test
    fun `emerge - tige sans hauteur non cubable annulee`() {
        verifie(emerge, listOf(plus("a", 1, q = 3), plus("b", 2, q = 1, h = "26"), moins("x", 3, q = 2)))
        val t = VolumesMartelage.totaux(emerge, listOf(plus("a", 1, q = 3), moins("x", 2, q = 2)))
        assertEquals(1, t.nbTigesNonCubees)
    }

    @Test
    fun `emerge - l'annulation sans hauteur retire le volume de la tige annulee`() {
        val j = listOf(plus("a", 1, h = "27"), moins("x", 2))
        assertEquals(0.0, VolumesMartelage.totaux(emerge, j).volumeTotalM3, eps)
        verifie(emerge, j)
    }

    @Test
    fun `schaeffer avec part de houppier - houppier non nul et total coherent`() {
        val j = listOf(plus("a", 1, q = 4), moins("x", 2, q = 1))
        val t = VolumesMartelage.totaux(schaefferHouppier, j)
        assertTrue(t.volumeHouppierM3 > 0.0)
        assertEquals(t.volumeTigeM3 + t.volumeHouppierM3, t.volumeTotalM3, eps)
        verifie(schaefferHouppier, j)
    }

    @Test
    fun `contexte sans tarif - volumes nuls, surface terriere nette`() {
        val t = VolumesMartelage.totaux(sansTarif, listOf(plus("a", 1, q = 3), moins("x", 2)))
        assertEquals(0.0, t.volumeTotalM3, eps)
        assertEquals(2 * Cubage.surfaceTerriereUnitaire(sansTarif, 40), t.surfaceTerriereM2, eps)
    }

    @Test
    fun `journaux aleatoires reproductibles - 300 journaux dans le desordre`() {
        val essences = listOf("Hêtre", "Chêne", "Zzz inconnue")
        val rnd = Random(20261110)
        repeat(300) { n ->
            val taille = rnd.nextInt(0, 25)
            val j = List(taille) { i ->
                val e = essences[rnd.nextInt(essences.size)]
                val cl = 30 + 5 * rnd.nextInt(0, 4)
                val q = rnd.nextInt(1, 5)
                val t = rnd.nextLong(0, 8) // beaucoup d'égalités d'horodatage
                val h = if (rnd.nextInt(3) == 0) null else (15 + rnd.nextInt(20)).toString()
                if (rnd.nextInt(10) < 6) plus("u$n-$i", t, e, cl, q, h) else moins("u$n-$i", t, e, cl, q, h)
            }.shuffled(rnd)
            tous.forEach { verifie(it, j) }
        }
    }
}
