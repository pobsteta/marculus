package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.PartsLot
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mission 026 (fiche `pilotage/missions/026-lot-tige-houppier-par-essence.md`, branche
 * `docs/024-archivage-016-023`) : le volume qu'une tige ajoute à son lot est la somme des parties
 * cochées de son essence, « tige » et « houppier ». Défaut : les deux cochées. Les deux décochées :
 * permis, 0 m³.
 *
 * Champs attendus (nommés par le testeur, à créer par le dev avec l'accord de Pascal) :
 * `Contexte.partsLot: Map<String, PartsLot>` et `data class PartsLot(val tige: Boolean = true,
 * val houppier: Boolean = true)` (paquet `fr.marculus.core.model`). Une essence absente de la table
 * prend les deux cases cochées.
 *
 * Valeurs attendues calculées à la main à partir de `v40`, le volume Schaeffer rapide n° 8 de la
 * classe 40 (≈ 1,393 m³, voir `AffouageLotsTest`) : avec une part de houppier p, le tarif donne la
 * tige T = v40 et le houppier H = T·p/(1 − p) (KDoc de `Cubage.volumesUnitaire`).
 *  - p = 0,3 : H = v40·3/7 ≈ 0,597 ; T + H = v40/0,7 ≈ 1,990.
 */
class AffouageLotsPartsTest {
    private val eps = 1e-9
    private val v40 = Cubage.volume(TarifCubage.SCHAEFFER_RAPIDE, 8, 42.5)
    private val h30 = v40 * 3.0 / 7.0 // houppier à p = 0,3

    private fun contexte(
        max: Double = 100.0,
        tarif: TarifCubage = TarifCubage.SCHAEFFER_RAPIDE,
        part: Double? = 0.3,
        parts: Map<String, PartsLot> = emptyMap(),
    ) = Contexte(
        id = "c1", nom = "Affouage", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
        essences = listOf(EssenceColonne("Chêne", 0, 0), EssenceColonne("Hêtre", 0, 0)),
        tarif = tarif, tarifNumero = 8, affouage = true, volumeMaxLotM3 = max,
        partHouppier = if (part == null) emptyMap() else mapOf("Chêne" to part),
        partsLot = parts,
    )

    private fun plus(uuid: String, t: Long, lot: Int?, essence: String = "Chêne", quantite: Int = 1, h: String? = null) =
        Tige(uuid, "c1", essence, 40, ActionTige.PLUS, horodatage = t, quantite = quantite, lot = lot, hauteurTexte = h)

    private val cases = listOf(
        "tige + houppier" to PartsLot(tige = true, houppier = true),
        "tige seule" to PartsLot(tige = true, houppier = false),
        "houppier seul" to PartsLot(tige = false, houppier = true),
        "aucune" to PartsLot(tige = false, houppier = false),
    )

    private fun attendu(p: PartsLot, tige: Double, houppier: Double) =
        (if (p.tige) tige else 0.0) + (if (p.houppier) houppier else 0.0)

    // ---------- AffouageLots.volumes : 4 combinaisons x 3 cas de tarif ----------

    @Test
    fun `Schaeffer avec part de houppier - le lot est la somme des parties cochees`() {
        for ((nom, p) in cases) {
            val ctx = contexte(parts = mapOf("Chêne" to p))
            val v = AffouageLots.volumes(ctx, plus("a", 1, 1))
            assertEquals(v40, v.tigeM3, eps, nom)
            assertEquals(h30, v.houppierM3, eps, nom)
            assertEquals(attendu(p, v40, h30), v.lotM3, eps, nom)
        }
    }

    @Test
    fun `Schaeffer sans part - cocher houppier n'ajoute rien, houppier seul vaut 0`() {
        for ((nom, p) in cases) {
            val ctx = contexte(part = null, parts = mapOf("Chêne" to p))
            val v = AffouageLots.volumes(ctx, plus("a", 1, 1))
            assertEquals(v40, v.tigeM3, eps, nom)
            assertEquals(0.0, v.houppierM3, eps, nom)
            assertEquals(attendu(p, v40, 0.0), v.lotM3, eps, nom)
        }
        // cas exact : houppier seul sans houppier calculable = 0 m3 (règle 5)
        val seul = contexte(part = null, parts = mapOf("Chêne" to PartsLot(tige = false, houppier = true)))
        assertEquals(0.0, AffouageLots.volumes(seul, plus("a", 1, 1)).lotM3, eps)
    }

    @Test
    fun `EMERGE - le lot est la somme des parties cochees du modele`() {
        for ((nom, p) in cases) {
            val ctx = contexte(tarif = TarifCubage.EMERGE, part = null, parts = mapOf("Chêne" to p))
            val t = plus("a", 1, 1, h = "24")
            val c = VolumesMartelage.cubage(ctx, t) // volumes EMERGE du modèle, hors mission
            assertTrue(c.volumeHouppierM3 > 0.0, "EMERGE couvre le chêne : houppier > 0")
            val v = AffouageLots.volumes(ctx, t)
            assertEquals(c.volumeTigeM3, v.tigeM3, eps, nom)
            assertEquals(c.volumeHouppierM3, v.houppierM3, eps, nom)
            assertEquals(attendu(p, c.volumeTigeM3, c.volumeHouppierM3), v.lotM3, eps, nom)
        }
    }

    @Test
    fun `defaut - sans entree pour l'essence les deux cases sont cochees`() {
        val ctx = contexte() // partsLot vide
        assertEquals(v40 + h30, AffouageLots.volumes(ctx, plus("a", 1, 1)).lotM3, eps)
        // une autre essence réglée ne touche pas l'essence absente de la table
        val autre = contexte(parts = mapOf("Hêtre" to PartsLot(tige = false, houppier = false)))
        assertEquals(v40 + h30, AffouageLots.volumes(autre, plus("a", 1, 1)).lotM3, eps)
        assertEquals(PartsLot(), PartsLot(tige = true, houppier = true))
    }

    @Test
    fun `les reglages sont par essence`() {
        val ctx = contexte(
            parts = mapOf("Chêne" to PartsLot(tige = false, houppier = true), "Hêtre" to PartsLot(tige = true, houppier = false)),
        )
        assertEquals(h30, AffouageLots.volumes(ctx, plus("a", 1, 1, "Chêne")).lotM3, eps)
        assertEquals(v40, AffouageLots.volumes(ctx, plus("b", 2, 1, "Hêtre")).lotM3, eps)
    }

    @Test
    fun `proportions du terrain - houppier seul 2,36 pour tige seule 13,39 et les deux 15,75`() {
        // « Affouage » de Daniel (fiche 026) : part de houppier 0,15 => houppier = 15 % du total.
        // 13,39 + 2,36 = 15,75 ; 2,36 / 15,75 = 0,15 (à l'arrondi du centième près).
        val p = 0.15
        val base = contexte(part = p)
        val total = AffouageLots.volumes(base, plus("a", 1, 1)).lotM3
        val h = AffouageLots.volumes(base.copy(partsLot = mapOf("Chêne" to PartsLot(false, true))), plus("a", 1, 1)).lotM3
        val t = AffouageLots.volumes(base.copy(partsLot = mapOf("Chêne" to PartsLot(true, false))), plus("a", 1, 1)).lotM3
        assertEquals(v40 / (1 - p), total, eps)
        assertEquals(total * p, h, eps)
        assertEquals(total * (1 - p), t, eps)
        assertEquals(total, h + t, eps)
    }

    // ---------- cumul, borne, numéros ----------

    @Test
    fun `bilan - un lot compte la somme des parties cochees, tiges et quantites`() {
        val j = listOf(plus("a", 1, 1, quantite = 2), plus("b", 2, 1, "Hêtre"))
        val ctx = contexte(parts = mapOf("Chêne" to PartsLot(tige = true, houppier = true), "Hêtre" to PartsLot(tige = false, houppier = true)))
        val b = AffouageLots.bilan(ctx, j).single()
        assertEquals(1, b.lot)
        assertEquals(3, b.nbTiges)
        // Chêne x2 : 2·(T + H) ; Hêtre : houppier seul, sans part => 0
        assertEquals(2 * (v40 + h30), b.volumeM3, eps)
    }

    @Test
    fun `les deux decochees - l'essence compte 0 m3 mais ses tiges restent comptees`() {
        val ctx = contexte(parts = mapOf("Chêne" to PartsLot(tige = false, houppier = false)))
        val b = AffouageLots.bilan(ctx, listOf(plus("a", 1, 1), plus("b", 2, 1))).single()
        assertEquals(2, b.nbTiges)
        assertEquals(0.0, b.volumeM3, eps)
        // 0 m3 : jamais de fermeture, quelle que soit la borne positive
        assertEquals(1, AffouageLots.prochainLot(ctx, listOf(plus("a", 1, 1), plus("b", 2, 1))))
        assertEquals(false, AffouageLots.fermeLot(ctx, listOf(plus("a", 1, 1)), plus("b", 2, 1)))
    }

    @Test
    fun `borne 2,5 - avec les deux cases le lot se ferme des la deuxieme tige`() {
        // (T + H) = v40/0,7 ≈ 1,990 < 2,5 ; 2 x 1,990 = 3,980 >= 2,5
        val ctx = contexte(max = 2.5)
        val a = plus("a", 1, 1)
        val b = plus("b", 2, 1)
        assertEquals(false, AffouageLots.fermeLot(ctx, emptyList(), a))
        assertEquals(1, AffouageLots.prochainLot(ctx, listOf(a)))
        assertEquals(true, AffouageLots.fermeLot(ctx, listOf(a), b))
        assertEquals(2, AffouageLots.prochainLot(ctx, listOf(a, b)))
        assertEquals(2 * (v40 + h30), AffouageLots.etat(ctx, listOf(a, b)).cumulM3, eps)
    }

    @Test
    fun `borne 2,5 - houppier seul, il faut cinq tiges pour fermer le lot`() {
        // H ≈ 0,597 : 4 H ≈ 2,39 < 2,5 ; 5 H ≈ 2,99 >= 2,5
        val ctx = contexte(max = 2.5, parts = mapOf("Chêne" to PartsLot(tige = false, houppier = true)))
        val j = (1..4).map { plus("t$it", it.toLong(), 1) }
        assertEquals(1, AffouageLots.prochainLot(ctx, j))
        assertEquals(true, AffouageLots.fermeLot(ctx, j, plus("t5", 5, 1)))
        assertEquals(2, AffouageLots.prochainLot(ctx, j + plus("t5", 5, 1)))
    }

    @Test
    fun `les numeros de lot deja portes par les tiges ne changent pas avec les cases`() {
        val j = listOf(plus("a", 1, 1), plus("b", 2, 1), plus("c", 3, 2), plus("d", 4, 3))
        val defaut = contexte(max = 2.5)
        val houppier = contexte(max = 2.5, parts = mapOf("Chêne" to PartsLot(tige = false, houppier = true)))
        assertEquals(listOf(1, 2, 3), AffouageLots.bilan(defaut, j).map { it.lot })
        assertEquals(listOf(1, 2, 3), AffouageLots.bilan(houppier, j).map { it.lot })
        assertEquals(listOf(2, 1, 1), AffouageLots.bilan(houppier, j).map { it.nbTiges })
        // les volumes, eux, changent : lot 1 = 2 tiges
        assertEquals(2 * (v40 + h30), AffouageLots.bilan(defaut, j).first { it.lot == 1 }.volumeM3, eps)
        assertEquals(2 * h30, AffouageLots.bilan(houppier, j).first { it.lot == 1 }.volumeM3, eps)
        // le lot courant reste le plus grand lot porté : 3
        assertEquals(3, AffouageLots.etat(defaut, j).lot)
    }
}
