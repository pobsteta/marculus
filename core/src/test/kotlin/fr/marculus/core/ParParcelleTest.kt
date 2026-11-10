package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige
import fr.nemeton.terrain.geo.Position
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Mission 019 : « Par parcelle » compte les tiges nettes (annulations retirées avec la règle de
 * Statut). Contrat imposé au dev, dans `:core` (fichier `ParParcelle.kt`, package `fr.marculus.core`) :
 *
 *     object ParParcelle {
 *         data class Repartition(val parcelles: Map<String?, List<Tige>>, val sansPosition: Int) {
 *             fun nombre(parcelle: String?): Int   // somme des quantités de la parcelle, 0 si absente
 *             val total: Int                       // toutes lignes comprises (parcelles, hors parcelle, sans position)
 *         }
 *         fun repartir(journal: List<Tige>, parcelleDe: (Tige) -> String?): Repartition
 *     }
 *
 * Clé `null` = tige avec position mais hors de toute parcelle. `sansPosition` = quantités des
 * tiges nettes sans position. `parcelleDe` n'est appelée que pour les tiges avec position.
 * Les tiges de `parcelles` sont les PLUS restantes, avec leur quantité restante.
 */
class ParParcelleTest {
    private val eps = 1e-9

    private val ctx = Contexte(
        id = "c1", nom = "Affouage", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
        essences = listOf(EssenceColonne("Chêne", 0, 0), EssenceColonne("Hêtre", 0, 0)),
        tarif = TarifCubage.SCHAEFFER_RAPIDE, tarifNumero = 8,
    )

    private val posA = Position(45.0, 5.0)
    private val posB = Position(46.0, 5.0)
    private val posHors = Position(10.0, 5.0)

    /** Parcelle par latitude : 45 -> A, 46 -> B, ailleurs hors parcelle. */
    private val parcelleDe: (Tige) -> String? = { t ->
        when (t.position?.latitude) {
            45.0 -> "A"
            46.0 -> "B"
            else -> null
        }
    }

    private fun plus(
        uuid: String, t: Long, pos: Position? = posA, essence: String = "Chêne", classe: Int = 30,
        quantite: Int = 1,
    ) = Tige(uuid, "c1", essence, classe, ActionTige.PLUS, horodatage = t, quantite = quantite, position = pos)

    private fun moins(uuid: String, t: Long, essence: String = "Chêne", classe: Int = 30, quantite: Int = 1) =
        Tige(uuid, "c1", essence, classe, ActionTige.ANNULATION, horodatage = t, quantite = quantite)

    @Test
    fun `sans annulation - chaque tige dans sa parcelle`() {
        val r = ParParcelle.repartir(listOf(plus("a", 1, posA), plus("b", 2, posB), plus("c", 3, posHors)), parcelleDe)
        assertEquals(1, r.nombre("A"))
        assertEquals(1, r.nombre("B"))
        assertEquals(1, r.nombre(null))
        assertEquals(0, r.sansPosition)
        assertEquals(3, r.total)
    }

    @Test
    fun `l annulation retire la derniere tige de la case, donc celle de la parcelle B`() {
        val r = ParParcelle.repartir(listOf(plus("a", 1, posA), plus("b", 2, posB), moins("x", 3)), parcelleDe)
        assertEquals(1, r.nombre("A"))
        assertEquals(0, r.nombre("B"))
        assertEquals(1, r.total)
    }

    @Test
    fun `la tige retiree est la derniere meme si le journal est donne dans le desordre`() {
        val r = ParParcelle.repartir(listOf(moins("x", 3), plus("b", 2, posB), plus("a", 1, posA)), parcelleDe)
        assertEquals(1, r.nombre("A"))
        assertEquals(0, r.nombre("B"))
    }

    @Test
    fun `a horodatage egal le tri par uuid decide`() {
        // Même horodatage : ordre a, b, x (uuid croissant) : l'annulation x retire b (parcelle B).
        val r = ParParcelle.repartir(listOf(plus("a", 5, posA), plus("b", 5, posB), moins("x", 5)), parcelleDe)
        assertEquals(1, r.nombre("A"))
        assertEquals(0, r.nombre("B"))
    }

    @Test
    fun `paquet de 3 annule de 1 - il en reste 2 dans la parcelle`() {
        val r = ParParcelle.repartir(listOf(plus("a", 1, posA, quantite = 3), moins("x", 2)), parcelleDe)
        assertEquals(2, r.nombre("A"))
        assertEquals(2, r.total)
    }

    @Test
    fun `cases differentes - une annulation de chene 30 ne retire rien au hetre 30`() {
        val r = ParParcelle.repartir(
            listOf(plus("h", 1, posA, essence = "Hêtre"), plus("c", 2, posB, essence = "Chêne"), moins("x", 3, "Chêne")),
            parcelleDe,
        )
        assertEquals(1, r.nombre("A"))
        assertEquals(0, r.nombre("B"))
        assertEquals(listOf("Hêtre"), r.parcelles["A"]!!.map { it.essence })
    }

    @Test
    fun `classes differentes - une annulation de chene 35 ne retire pas le chene 30`() {
        val r = ParParcelle.repartir(listOf(plus("c", 1, posA, classe = 30), moins("x", 2, classe = 35)), parcelleDe)
        assertEquals(1, r.nombre("A"))
    }

    @Test
    fun `une tige sans position annulee disparait des sans position`() {
        val r = ParParcelle.repartir(listOf(plus("a", 1, null), plus("b", 2, null), moins("x", 3)), parcelleDe)
        assertEquals(1, r.sansPosition)
        assertEquals(1, r.total)
    }

    @Test
    fun `une tige posterieure a l annulation n est pas retiree`() {
        val r = ParParcelle.repartir(listOf(plus("a", 1, posA), moins("x", 2), plus("b", 3, posB)), parcelleDe)
        assertEquals(0, r.nombre("A"))
        assertEquals(1, r.nombre("B"))
    }

    @Test
    fun `parcelleDe n est jamais appelee pour une tige sans position`() {
        ParParcelle.repartir(listOf(plus("a", 1, null)), { error("tige sans position") })
    }

    @Test
    fun `la somme de Par parcelle egale le nombre net de Statut`() {
        val j = listOf(
            plus("a", 1, posA, quantite = 3), plus("b", 2, posB), plus("c", 3, posHors), plus("d", 4, null),
            plus("e", 5, posA, "Hêtre", 40), moins("x", 6), moins("y", 7), moins("z", 8, "Hêtre", 40),
            plus("f", 9, posB),
        )
        val r = ParParcelle.repartir(j, parcelleDe)
        assertEquals(TotauxMartelage(j).totalContexte(), r.total)
        // Chêne 30 : pile a(3) b c d, x retire d, y retire c, puis f ; Hêtre 40 : e retiré par z.
        assertEquals(5, r.total)
        assertEquals(3, r.nombre("A"))
        assertEquals(2, r.nombre("B"))
        assertEquals(0, r.nombre(null))
        assertEquals(0, r.sansPosition)
    }

    @Test
    fun `les volumes de Par parcelle sommes egalent VolumesMartelage totaux`() {
        // Net : a (2 restants, classe 30, parcelle A), b (B), c (hors parcelle, hêtre 40 annulé -> retirée),
        // d (sans position, hêtre 25).
        val j = listOf(
            plus("a", 1, posA, classe = 30, quantite = 3), plus("b", 2, posB, classe = 45),
            plus("c", 3, posHors, "Hêtre", 40), plus("d", 4, null, "Hêtre", 25),
            moins("x", 5, classe = 30), moins("y", 6, "Hêtre", 40),
        )
        val r = ParParcelle.repartir(j, parcelleDe)
        val geo = r.parcelles.values.flatten().sumOf {
            Cubage.volumesUnitaire(ctx, it.essence, it.classe, it.hauteurTexte).total * it.quantite
        }
        val sansPosition = Cubage.volumesUnitaire(ctx, "Hêtre", 25, null).total * r.sansPosition
        assertEquals(1, r.sansPosition)
        assertEquals(VolumesMartelage.totaux(ctx, j).volumeTotalM3, geo + sansPosition, eps)
    }
}
