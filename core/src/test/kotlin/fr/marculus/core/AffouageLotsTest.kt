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

class AffouageLotsTest {
    private val eps = 1e-9

    /** Volume Schaeffer rapide n° 8 d'une tige de la classe 40 (centre 42,5 cm) ≈ 1,393 m³. */
    private val v40 = Cubage.volume(TarifCubage.SCHAEFFER_RAPIDE, 8, 42.5)

    private fun contexte(max: Double, tarif: TarifCubage = TarifCubage.SCHAEFFER_RAPIDE) = Contexte(
        id = "c1", nom = "Affouage", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
        essences = listOf(EssenceColonne("Chêne", 0, 0)), tarif = tarif, tarifNumero = 8,
        affouage = true, volumeMaxLotM3 = max,
    )

    private fun plus(uuid: String, t: Long, lot: Int?, quantite: Int = 1) =
        Tige(uuid, "c1", "Chêne", 40, ActionTige.PLUS, horodatage = t, quantite = quantite, lot = lot)

    private fun moins(uuid: String, t: Long) =
        Tige(uuid, "c1", "Chêne", 40, ActionTige.ANNULATION, horodatage = t)

    @Test
    fun `journal vide - lot 1`() {
        assertEquals(1, AffouageLots.prochainLot(contexte(5.0), emptyList()))
        assertEquals(0.0, AffouageLots.etat(contexte(5.0), emptyList()).cumulM3, eps)
    }

    @Test
    fun `le cumul reste dans le lot tant que la borne n'est pas atteinte`() {
        val j = listOf(plus("a", 1, 1), plus("b", 2, 1))
        val e = AffouageLots.etat(contexte(3.0), j)
        assertEquals(1, e.lot)
        assertEquals(2 * v40, e.cumulM3, eps)
        assertEquals(1, AffouageLots.prochainLot(contexte(3.0), j))
    }

    @Test
    fun `borne atteinte a l'egalite - la tige suivante ouvre le lot suivant`() {
        val j = listOf(plus("a", 1, 1), plus("b", 2, 1))
        assertEquals(2, AffouageLots.prochainLot(contexte(2 * v40), j))
    }

    @Test
    fun `la tige qui deborde ferme son lot et le cumul repart a zero`() {
        val j = listOf(plus("a", 1, 1), plus("b", 2, 1), plus("c", 3, 2))
        val ctx = contexte(2.0)
        val e = AffouageLots.etat(ctx, j)
        assertEquals(2, e.lot)
        assertEquals(v40, e.cumulM3, eps)
        assertEquals(2, AffouageLots.prochainLot(ctx, j))
    }

    @Test
    fun `annuler la tige qui a ferme le lot le rouvre`() {
        val ctx = contexte(2.0)
        val j = listOf(plus("a", 1, 1), plus("b", 2, 1), moins("x", 3))
        assertEquals(1, AffouageLots.prochainLot(ctx, j))
    }

    @Test
    fun `un lot vide par annulation garde son numero`() {
        val ctx = contexte(2.0)
        val j = listOf(plus("a", 1, 1), plus("b", 2, 1), plus("c", 3, 2), moins("x", 4))
        val e = AffouageLots.etat(ctx, j)
        assertEquals(2, e.lot)
        assertEquals(0.0, e.cumulM3, eps)
        assertEquals(2, AffouageLots.prochainLot(ctx, j))
    }

    @Test
    fun `la quantite multiplie le volume`() {
        val ctx = contexte(2.0)
        assertEquals(2, AffouageLots.prochainLot(ctx, listOf(plus("a", 1, 1, quantite = 2))))
    }

    @Test
    fun `les tiges sans lot sont ignorees`() {
        val ctx = contexte(2.0)
        val j = listOf(plus("a", 1, null), plus("b", 2, null))
        assertEquals(1, AffouageLots.prochainLot(ctx, j))
    }

    @Test
    fun `sans tarif aucun lot ne se ferme`() {
        val ctx = contexte(1.0, TarifCubage.AUCUN)
        assertEquals(1, AffouageLots.prochainLot(ctx, listOf(plus("a", 1, 1), plus("b", 2, 1))))
    }

    @Test
    fun `la tige qui atteint la borne ferme son lot`() {
        val ctx = contexte(2.0)
        assertEquals(false, AffouageLots.fermeLot(ctx, emptyList(), plus("a", 1, 1)))
        assertEquals(true, AffouageLots.fermeLot(ctx, listOf(plus("a", 1, 1)), plus("b", 2, 1)))
    }

    @Test
    fun `a l egalite la tige ferme son lot, pas la suivante`() {
        val ctx = contexte(2 * v40)
        assertEquals(true, AffouageLots.fermeLot(ctx, listOf(plus("a", 1, 1)), plus("b", 2, 1)))
        val j = listOf(plus("a", 1, 1), plus("b", 2, 1))
        assertEquals(false, AffouageLots.fermeLot(ctx, j, plus("c", 3, 2)))
    }

    @Test
    fun `hors affouage aucune fermeture`() {
        assertEquals(false, AffouageLots.fermeLot(contexte(2.0), listOf(plus("a", 1, 1)), plus("b", 2, null)))
    }

    @Test
    fun `bilan par lot - tiges et volume nets, lot complet signale`() {
        val ctx = contexte(2.0)
        val j = listOf(
            plus("a", 1, 1), plus("b", 2, 1), plus("c", 3, 2), plus("d", 4, 2), moins("x", 5),
            plus("e", 6, null),
        )
        val b = AffouageLots.bilan(ctx, j)
        assertEquals(listOf(1, 2, null), b.map { it.lot })
        assertEquals(listOf(2, 1, 1), b.map { it.nbTiges })
        assertEquals(2 * v40, b[0].volumeM3, eps)
        assertEquals(v40, b[1].volumeM3, eps)
        assertEquals(listOf(true, false, false), b.map { it.complet })
    }

    @Test
    fun `Schaeffer sans part de houppier - l'arbre compte en entier`() {
        val v = AffouageLots.volumes(contexte(5.0), plus("a", 1, 1))
        assertEquals(v40, v.tigeM3, eps)
        assertEquals(0.0, v.houppierM3, eps)
        assertEquals(v40, v.lotM3, eps)
    }

    @Test
    fun `Schaeffer avec 30 pourcent de houppier - seul le houppier compte`() {
        val ctx = contexte(5.0).copy(partHouppierAffouage = mapOf("Chêne" to 0.3))
        val v = AffouageLots.volumes(ctx, plus("a", 1, 1))
        // Le tarif donne la tige, 70 % du total : total = v40 / 0,7, houppier = 30 % du total.
        assertEquals(v40 * 0.3 / 0.7, v.houppierM3, eps)
        assertEquals(v.houppierM3, v.lotM3, eps)
        assertEquals(0.3, v.houppierM3 / (v.tigeM3 + v.houppierM3), eps)
    }

    @Test
    fun `la part de houppier est propre a l'essence`() {
        val ctx = contexte(5.0).copy(partHouppierAffouage = mapOf("Hêtre" to 0.3))
        assertEquals(v40, AffouageLots.volumes(ctx, plus("a", 1, 1)).lotM3, eps)
    }

    @Test
    fun `la part de houppier cumule les lots sur le houppier`() {
        val ctx = contexte(1.0).copy(partHouppierAffouage = mapOf("Chêne" to 0.3))
        val h = v40 * 0.3 / 0.7
        val e = AffouageLots.etat(ctx, listOf(plus("a", 1, 1)))
        assertEquals(h, e.cumulM3, eps)
        assertEquals(1, AffouageLots.prochainLot(ctx, listOf(plus("a", 1, 1))))
        assertEquals(true, AffouageLots.fermeLot(ctx, listOf(plus("a", 1, 1)), plus("b", 2, 1)))
        val b = AffouageLots.bilan(ctx, listOf(plus("a", 1, 1), plus("b", 2, 1)))
        assertEquals(2 * v40, b[0].volumeTigeM3, eps)
        assertEquals(2 * h, b[0].volumeHouppierM3, eps)
        assertEquals(2 * h, b[0].volumeM3, eps)
    }

    @Test
    fun `une part de houppier aberrante est bornee`() {
        val ctx = contexte(5.0).copy(partHouppierAffouage = mapOf("Chêne" to 1.0))
        assertEquals(AffouageLots.PART_HOUPPIER_MAX, AffouageLots.partHouppier(ctx, "Chêne"), eps)
        val neg = contexte(5.0).copy(partHouppierAffouage = mapOf("Chêne" to -0.2))
        assertEquals(v40, AffouageLots.volumes(neg, plus("a", 1, 1)).lotM3, eps)
    }

    @Test
    fun `EMERGE - le houppier du modele compte, la part saisie est ignoree`() {
        val ctx = contexte(5.0, TarifCubage.EMERGE).copy(partHouppierAffouage = mapOf("Chêne" to 0.3))
        val t = plus("a", 1, 1).copy(hauteurTexte = "25")
        val c = VolumesMartelage.cubage(ctx, t)
        val v = AffouageLots.volumes(ctx, t)
        assertEquals(c.volumeTigeM3, v.tigeM3, eps)
        assertEquals(c.volumeHouppierM3, v.houppierM3, eps)
        assertEquals(c.volumeHouppierM3, v.lotM3, eps)
        assert(v.houppierM3 > 0.0)
    }

    @Test
    fun `EMERGE - essence non couverte, sans houppier, compte en entier`() {
        val ctx = contexte(5.0, TarifCubage.EMERGE).copy(essences = listOf(EssenceColonne("Zzz inconnue", 0, 0)))
        val t = Tige("a", "c1", "Zzz inconnue", 40, ActionTige.PLUS, horodatage = 1, hauteurTexte = "25", lot = 1)
        val v = AffouageLots.volumes(ctx, t)
        assertEquals(0.0, v.houppierM3, eps)
        assertEquals(v.tigeM3, v.lotM3, eps)
        assert(v.lotM3 > 0.0)
    }
}
