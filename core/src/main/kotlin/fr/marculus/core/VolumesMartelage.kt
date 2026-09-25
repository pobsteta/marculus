package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige

/**
 * Volumes d'un martelage, tels que l'écran Statut les affiche et que les exports (`.marsync`,
 * CSV format 3) les transmettent : Marculus est la **seule source** de ces chiffres.
 *
 * **Annulation** : elle retire, dans la même case (essence × classe), les dernières tiges
 * comptées avant elle et pas encore annulées — donc **leur** volume. Elle est saisie sans
 * hauteur : en EMERGE, lui calculer un volume propre retirerait 0 m³ et laisserait le volume de
 * la tige annulée dans le total. Une annulation sans tige à retirer (journal fusionné dans le
 * désordre) retire le volume calculé sur sa propre saisie.
 */
object VolumesMartelage {

    /** Méthode de cubage effectivement appliquée à une tige (clé `cubage` des exports). */
    object Methode {
        const val AUCUN = "AUCUN"
        const val EMERGE = "EMERGE"
        const val NON_CUBABLE = "NON_CUBABLE"
        fun schaeffer(tarif: TarifCubage, numero: Int) = "${tarif.name}:$numero"
        fun forme(f: Double) = "FORME:$f"
    }

    /** Valeurs **unitaires** d'une tige (pour une tige, avant multiplication par `quantite`). */
    data class CubageTige(
        val volumeTigeM3: Double,
        val volumeHouppierM3: Double,
        val volumeTotalM3: Double,
        val surfaceTerriereM2: Double,
        val cubage: String,
    ) {
        val nonCubee: Boolean get() = cubage == Methode.NON_CUBABLE
    }

    /** Totaux **nets** d'un contexte, annulations déduites. */
    data class Totaux(
        val volumeTigeM3: Double,
        val volumeHouppierM3: Double,
        val volumeTotalM3: Double,
        val surfaceTerriereM2: Double,
        val nbTigesNonCubees: Int,
    )

    /** Cubage d'une tige sur **sa propre saisie** (classe, essence, hauteur), avec le tarif du contexte. */
    fun cubage(contexte: Contexte, tige: Tige): CubageTige {
        val g = Cubage.surfaceTerriereUnitaire(contexte, tige.classe)
        val methode = when (contexte.tarif) {
            TarifCubage.AUCUN -> Methode.AUCUN
            TarifCubage.SCHAEFFER_RAPIDE, TarifCubage.SCHAEFFER_LENT ->
                Methode.schaeffer(contexte.tarif, contexte.tarifNumero)
            TarifCubage.EMERGE -> when {
                HauteurParser.parse(tige.hauteurTexte ?: "").hauteurTotale == null -> Methode.NON_CUBABLE
                Cubage.estCouverteEmerge(tige.essence) -> Methode.EMERGE
                else -> Methode.forme(contexte.coefficientForme)
            }
        }
        val v = Cubage.volumesUnitaire(contexte, tige.essence, tige.classe, tige.hauteurTexte)
        return CubageTige(v.tige, v.houppier, v.total, g, methode)
    }

    /** Totaux nets du journal : chaque annulation retire les dernières tiges de sa case (voir en-tête). */
    fun totaux(contexte: Contexte, journal: List<Tige>): Totaux {
        var vTige = 0.0
        var vHoup = 0.0
        var vTot = 0.0
        var g = 0.0
        var nonCubees = 0
        fun ajouter(c: CubageTige, n: Int) {
            vTige += c.volumeTigeM3 * n
            vHoup += c.volumeHouppierM3 * n
            vTot += c.volumeTotalM3 * n
            g += c.surfaceTerriereM2 * n
            if (c.nonCubee) nonCubees += n
        }
        // Pile, par case, des tiges comptées et pas encore annulées : [cubage, quantité restante].
        val piles = mutableMapOf<CompteurCle, ArrayDeque<Pair<CubageTige, Int>>>()
        journal.sortedWith(compareBy<Tige> { it.horodatage }.thenBy { it.uuid }).forEach { t ->
            val pile = piles.getOrPut(CompteurCle(t.essence, t.classe)) { ArrayDeque() }
            val c = cubage(contexte, t)
            if (t.action == ActionTige.PLUS) {
                ajouter(c, t.quantite)
                pile.addLast(c to t.quantite)
            } else {
                var reste = t.quantite
                while (reste > 0 && pile.isNotEmpty()) {
                    val (annulee, n) = pile.removeLast()
                    val retirees = minOf(n, reste)
                    ajouter(annulee, -retirees)
                    if (n > retirees) pile.addLast(annulee to n - retirees)
                    reste -= retirees
                }
                // Rien à retirer dans la case : l'annulation vaut sa propre saisie.
                if (reste > 0) ajouter(c, -reste)
            }
        }
        return Totaux(vTige, vHoup, vTot, g, nonCubees.coerceAtLeast(0))
    }
}
