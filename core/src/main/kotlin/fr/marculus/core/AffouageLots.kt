package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.Tige

/**
 * Lots d'affouage : les tiges sont rangées, dans l'ordre du martelage, dans des lots dont le
 * volume (bois fort tige) est borné par [Contexte.volumeMaxLotM3]. La tige qui atteint ou dépasse
 * la borne **ferme** son lot ; la suivante ouvre le lot d'après.
 *
 * Le lot d'une tige est **figé** sur elle au martelage (il a été dit à voix haute et marqué sur
 * l'arbre) : rien ici ne renumérote le passé. Seul l'état courant — lot ouvert et son cumul — est
 * dérivé du journal, avec les volumes et les annulations d'aujourd'hui.
 */
object AffouageLots {

    /** Lot ouvert et volume (m³) déjà cumulé dedans. */
    data class Etat(val lot: Int, val cumulM3: Double)

    /**
     * Lot courant : le plus grand lot porté par une tige comptée (1 si aucun) et le volume de ses
     * tiges encore vivantes. Les annulations retirent, comme dans [VolumesMartelage], les
     * dernières tiges de leur case — un − juste après la tige qui a fermé un lot le rouvre.
     */
    fun etat(contexte: Contexte, journal: List<Tige>): Etat {
        // Pile, par case, des tiges comptées et pas encore annulées : [tige, quantité restante].
        val piles = mutableMapOf<CompteurCle, ArrayDeque<Pair<Tige, Int>>>()
        var lot = 1
        journal.sortedWith(compareBy<Tige> { it.horodatage }.thenBy { it.uuid }).forEach { t ->
            val pile = piles.getOrPut(CompteurCle(t.essence, t.classe)) { ArrayDeque() }
            if (t.action == ActionTige.PLUS) {
                t.lot?.let { lot = maxOf(lot, it) }
                pile.addLast(t to t.quantite)
            } else {
                var reste = t.quantite
                while (reste > 0 && pile.isNotEmpty()) {
                    val (annulee, n) = pile.removeLast()
                    val retirees = minOf(n, reste)
                    if (n > retirees) pile.addLast(annulee to n - retirees)
                    reste -= retirees
                }
            }
        }
        val cumul = piles.values.sumOf { pile ->
            pile.filter { (t, _) -> t.lot == lot }
                .sumOf { (t, n) -> VolumesMartelage.cubage(contexte, t).volumeTigeM3 * n }
        }
        return Etat(lot, cumul)
    }

    /**
     * La tige [nouvelle] (pas encore au journal, son lot déjà fixé) ferme-t-elle son lot ?
     * C'est elle qui fait atteindre ou dépasser la borne : on annonce alors « lot N complet ».
     */
    fun fermeLot(contexte: Contexte, journal: List<Tige>, nouvelle: Tige): Boolean {
        val lot = nouvelle.lot ?: return false
        if (contexte.volumeMaxLotM3 <= 0.0) return false
        val e = etat(contexte, journal + nouvelle)
        return e.lot == lot && e.cumulM3 >= contexte.volumeMaxLotM3
    }

    /** Lot où ira la prochaine tige : le lot courant, ou le suivant s'il a atteint la borne. */
    fun prochainLot(contexte: Contexte, journal: List<Tige>): Int {
        val e = etat(contexte, journal)
        return if (contexte.volumeMaxLotM3 > 0.0 && e.cumulM3 >= contexte.volumeMaxLotM3) e.lot + 1 else e.lot
    }
}
