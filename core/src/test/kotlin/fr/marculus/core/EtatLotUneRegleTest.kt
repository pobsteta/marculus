package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Mission 022 : filet de la refactorisation « une seule règle d'annulation ».
 * Fixe le comportement actuel de [AffouageLots.etat] ; l'oracle s'exprime avec [JournalNet.tiges]
 * et le cubage au lot ([AffouageLots.volumes]).
 *
 * Comportement fixé : le lot courant est le plus grand lot porté par une tige PLUS du journal,
 * **annulée ou non** (une annulation ne fait donc jamais redescendre le lot) ; 1 si aucune.
 * Le cumul est le volume au lot des tiges nettes (JournalNet) de ce lot.
 */
class EtatLotUneRegleTest {
    private val eps = 1e-9

    private fun contexte(max: Double = 3.0) = Contexte(
        id = "c1", nom = "Affouage", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
        essences = listOf(EssenceColonne("Chêne", 0, 0), EssenceColonne("Hêtre", 0, 0)),
        tarif = TarifCubage.SCHAEFFER_RAPIDE, tarifNumero = 8,
        affouage = true, volumeMaxLotM3 = max,
    )

    private val ctx = contexte()

    private fun plus(
        uuid: String, t: Long, lot: Int?, quantite: Int = 1, essence: String = "Chêne", classe: Int = 40,
    ) = Tige(uuid, "c1", essence, classe, ActionTige.PLUS, horodatage = t, quantite = quantite, lot = lot)

    private fun moins(uuid: String, t: Long, quantite: Int = 1, essence: String = "Chêne", classe: Int = 40) =
        Tige(uuid, "c1", essence, classe, ActionTige.ANNULATION, horodatage = t, quantite = quantite)

    private fun vol(t: Tige) = AffouageLots.volumes(ctx, t).lotM3

    /** Oracle : lot = max des lots des PLUS du journal (1 si aucun) ; cumul = tiges nettes de ce lot. */
    private fun oracle(journal: List<Tige>): AffouageLots.Etat {
        val lot = journal.filter { it.action == ActionTige.PLUS }.mapNotNull { it.lot }.maxOrNull()?.coerceAtLeast(1) ?: 1
        val cumul = JournalNet.tiges(journal)
            .filter { it.action == ActionTige.PLUS && it.lot == lot }
            .sumOf { vol(it) * it.quantite }
        return AffouageLots.Etat(lot, cumul)
    }

    private fun verifie(journal: List<Tige>): AffouageLots.Etat {
        val attendu = oracle(journal)
        val e = AffouageLots.etat(ctx, journal)
        assertEquals(attendu.lot, e.lot)
        assertEquals(attendu.cumulM3, e.cumulM3, eps)
        return e
    }

    @Test
    fun `journal vide - lot 1 cumul 0`() {
        val e = verifie(emptyList())
        assertEquals(1, e.lot)
        assertEquals(0.0, e.cumulM3, eps)
    }

    @Test
    fun `un seul lot`() {
        val j = listOf(plus("a", 1, 1), plus("b", 2, 1, quantite = 3))
        val e = verifie(j)
        assertEquals(1, e.lot)
        assertEquals(4 * vol(j[0]), e.cumulM3, eps)
    }

    @Test
    fun `tiges sans lot - lot 1 et cumul 0`() {
        val j = listOf(plus("a", 1, null), plus("b", 2, null))
        val e = verifie(j)
        assertEquals(1, e.lot)
        assertEquals(0.0, e.cumulM3, eps)
    }

    @Test
    fun `plusieurs lots - le cumul est celui du dernier lot`() {
        val j = listOf(plus("a", 1, 1), plus("b", 2, 1), plus("c", 3, 2), plus("d", 4, 3), plus("e", 5, 3))
        val e = verifie(j)
        assertEquals(3, e.lot)
        assertEquals(2 * vol(j[0]), e.cumulM3, eps)
    }

    @Test
    fun `un moins juste apres la tige qui a ferme un lot le rouvre`() {
        // Lot 1 fermé par b (2 x ~1,39 >= 2) ; le − retire b : le lot 1 est rouvert, cumul d'une tige.
        val ctx2 = contexte(2.0)
        val j = listOf(plus("a", 1, 1), plus("b", 2, 1), moins("m", 3))
        val e = AffouageLots.etat(ctx2, j)
        assertEquals(1, e.lot)
        assertEquals(vol(j[0]), e.cumulM3, eps)
        assertEquals(1, AffouageLots.prochainLot(ctx2, j))
    }

    @Test
    fun `annulation de la derniere tige d'un lot ouvert - le lot courant ne redescend pas`() {
        // Comportement actuel : le lot 2 reste le lot courant même si sa seule tige est annulée.
        val j = listOf(plus("a", 1, 1), plus("b", 2, 1), plus("c", 3, 2), moins("m", 4))
        val e = verifie(j)
        assertEquals(2, e.lot)
        assertEquals(0.0, e.cumulM3, eps)
    }

    @Test
    fun `annulation d'une tige d'un lot ancien - le lot courant et son cumul sont inchanges`() {
        // Essences différentes : le − de Hêtre retire la tige Hêtre du lot 1, pas celle du lot 2.
        val j = listOf(
            plus("a", 1, 1, essence = "Hêtre"), plus("b", 2, 2, essence = "Chêne"),
            moins("m", 3, essence = "Hêtre"),
        )
        val e = verifie(j)
        assertEquals(2, e.lot)
        assertEquals(vol(j[1]), e.cumulM3, eps)
    }

    @Test
    fun `annulations dans le desordre - l'ordre est horodatage puis uuid, pas celui de la liste`() {
        val j = listOf(moins("m", 5), plus("c", 4, 2), plus("a", 1, 1), plus("b", 2, 1))
        val e = verifie(j)
        assertEquals(2, e.lot)
        assertEquals(0.0, e.cumulM3, eps) // m retire c (lot 2)
        assertEquals(e, AffouageLots.etat(ctx, j.reversed()))
    }

    @Test
    fun `egalite d'horodatage departagee par uuid`() {
        // Même instant : "a" avant "b" avant "c" ; le − "c" retire donc "b" (lot 2), pas "a".
        val j = listOf(moins("c", 7), plus("b", 7, 2), plus("a", 7, 1))
        val e = verifie(j)
        assertEquals(2, e.lot)
        assertEquals(0.0, e.cumulM3, eps)
        // Autre départage : le − "a" passe avant les PLUS "b" : orphelin, rien n'est retiré.
        val j2 = listOf(plus("b", 7, 2), moins("a", 7))
        val e2 = verifie(j2)
        assertEquals(2, e2.lot)
        assertEquals(vol(j2[0]), e2.cumulM3, eps)
        assertEquals(e2, AffouageLots.etat(ctx, j2.reversed()))
    }

    @Test
    fun `paquet partiellement annule - il reste dans le lot avec la quantite restante`() {
        val j = listOf(plus("a", 1, 1, quantite = 5), moins("m", 2, quantite = 2))
        val e = verifie(j)
        assertEquals(1, e.lot)
        assertEquals(3 * vol(j[0]), e.cumulM3, eps)
    }

    @Test
    fun `une annulation de plusieurs tiges traverse les paquets`() {
        val j = listOf(
            plus("a", 1, 1, quantite = 2), plus("b", 2, 2, quantite = 2), plus("c", 3, 2, quantite = 2),
            moins("m", 4, quantite = 3),
        )
        val e = verifie(j)
        assertEquals(2, e.lot)
        assertEquals(1 * vol(j[1]), e.cumulM3, eps) // c retiré (2), puis 1 sur b : reste 1 de b
    }

    @Test
    fun `paquet entierement annule - ne compte plus mais son lot reste courant`() {
        val j = listOf(plus("a", 1, 1), plus("b", 2, 2, quantite = 3), moins("m", 3, quantite = 3))
        val e = verifie(j)
        assertEquals(2, e.lot)
        assertEquals(0.0, e.cumulM3, eps)
    }

    @Test
    fun `annulation orpheline - sans tige a retirer, elle est sans effet`() {
        val j = listOf(moins("m", 1))
        val e = verifie(j)
        assertEquals(1, e.lot)
        assertEquals(0.0, e.cumulM3, eps)
    }

    @Test
    fun `annulation orpheline avant la tige - la tige suivante reste comptee`() {
        val j = listOf(moins("m", 1), plus("a", 2, 1))
        val e = verifie(j)
        assertEquals(vol(j[1]), e.cumulM3, eps)
    }

    @Test
    fun `annulation en trop - le surplus est ignore`() {
        val j = listOf(plus("a", 1, 1), moins("m", 2, quantite = 4))
        val e = verifie(j)
        assertEquals(0.0, e.cumulM3, eps)
    }

    @Test
    fun `plusieurs cases essence x classe - chaque annulation vise sa case`() {
        val j = listOf(
            plus("a", 1, 1, essence = "Chêne", classe = 40),
            plus("b", 2, 1, essence = "Hêtre", classe = 40),
            plus("c", 3, 1, essence = "Chêne", classe = 55),
            plus("d", 4, 1, essence = "Chêne", classe = 40),
            moins("m1", 5, essence = "Chêne", classe = 40), // retire d
            moins("m2", 6, essence = "Hêtre", classe = 40), // retire b
        )
        val e = verifie(j)
        assertEquals(1, e.lot)
        assertEquals(vol(j[0]) + vol(j[2]), e.cumulM3, eps)
    }

    @Test
    fun `une annulation d'une case sans tige ne retire pas celle d'une autre case`() {
        val j = listOf(plus("a", 1, 1, classe = 40), moins("m", 2, classe = 55))
        val e = verifie(j)
        assertEquals(vol(j[0]), e.cumulM3, eps)
    }

    @Test
    fun `aleatoire - etat egale l'oracle (graine fixe)`() {
        val rnd = Random(2026_11_10)
        repeat(300) { essai ->
            val n = rnd.nextInt(0, 25)
            val max = listOf(1.0, 2.0, 3.0, 0.0)[rnd.nextInt(4)]
            var lot = 1
            val j = (0 until n).map { i ->
                val essence = listOf("Chêne", "Hêtre")[rnd.nextInt(2)]
                val classe = listOf(30, 40, 55)[rnd.nextInt(3)]
                // Horodatages avec égalités fréquentes ; uuid unique.
                val t = rnd.nextLong(0, 12)
                val uuid = "u%03d".format(rnd.nextInt(1000) * 100 + i)
                if (rnd.nextInt(10) < 3) {
                    moins(uuid, t, quantite = rnd.nextInt(1, 4), essence = essence, classe = classe)
                } else {
                    if (rnd.nextInt(6) == 0) lot += 1
                    val lotTige = if (rnd.nextInt(15) == 0) null else lot
                    plus(uuid, t, lotTige, quantite = rnd.nextInt(1, 4), essence = essence, classe = classe)
                }
            }
            val c = contexte(max)
            val attendu = run {
                val l = j.filter { it.action == ActionTige.PLUS }.mapNotNull { it.lot }.maxOrNull()?.coerceAtLeast(1) ?: 1
                val cumul = JournalNet.tiges(j).filter { it.action == ActionTige.PLUS && it.lot == l }
                    .sumOf { AffouageLots.volumes(c, it).lotM3 * it.quantite }
                l to cumul
            }
            val e = AffouageLots.etat(c, j)
            assertEquals(attendu.first, e.lot, "lot, essai $essai")
            assertEquals(attendu.second, e.cumulM3, eps, "cumul, essai $essai")
            // L'ordre de la liste ne compte pas.
            assertEquals(e, AffouageLots.etat(c, j.shuffled(rnd)), "ordre, essai $essai")
        }
    }
}
