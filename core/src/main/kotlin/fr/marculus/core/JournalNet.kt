package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.Tige

/**
 * Journal **net** : les seules tiges à comptabiliser. Chaque annulation retire, comme dans
 * [VolumesMartelage], les dernières tiges de sa case (essence × classe) comptées avant elle ;
 * la tige annulée et l'annulation disparaissent toutes deux. Une tige comptée par paquet
 * (quantité > 1) et annulée en partie reste, avec la quantité qui lui reste.
 *
 * Une annulation sans tige à retirer (journal fusionné dans le désordre) est **gardée** pour ce
 * qu'elle n'a pas pu retirer : la somme du journal net reste égale aux totaux.
 */
object JournalNet {

    fun tiges(journal: List<Tige>): List<Tige> {
        // Pile, par case, des index (dans `restes`) des tiges comptées encore vivantes.
        val piles = mutableMapOf<CompteurCle, ArrayDeque<Int>>()
        val restes = mutableListOf<Pair<Tige, Int>>()
        journal.sortedWith(compareBy<Tige> { it.horodatage }.thenBy { it.uuid }).forEach { t ->
            val pile = piles.getOrPut(CompteurCle(t.essence, t.classe)) { ArrayDeque() }
            if (t.action == ActionTige.PLUS) {
                restes += t to t.quantite
                pile.addLast(restes.lastIndex)
            } else {
                var reste = t.quantite
                while (reste > 0 && pile.isNotEmpty()) {
                    val i = pile.last()
                    val (annulee, n) = restes[i]
                    val retirees = minOf(n, reste)
                    restes[i] = annulee to n - retirees
                    if (n == retirees) pile.removeLast()
                    reste -= retirees
                }
                if (reste > 0) restes += t to reste
            }
        }
        return restes.filter { it.second > 0 }.map { (t, n) -> if (n == t.quantite) t else t.copy(quantite = n) }
    }
}
