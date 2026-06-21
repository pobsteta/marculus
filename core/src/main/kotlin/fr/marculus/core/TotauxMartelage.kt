package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.Tige

/**
 * Calcule les totaux dérivés d'un journal append-only : pour chaque cellule
 * (essence × classe), nombre de PLUS moins nombre d'ANNULATION. Le journal n'est jamais
 * modifié — l'annulation est un événement conservé, pas une suppression.
 */
class TotauxMartelage(private val journal: List<Tige>) {

    private fun Tige.delta(): Int = if (action == ActionTige.PLUS) 1 else -1

    /** Total d'une cellule donnée. */
    fun totalPour(essence: String, classe: Int): Int =
        journal.filter { it.essence == essence && it.classe == classe }.sumOf { it.delta() }

    /** Total de chaque cellule présente dans le journal. */
    fun totaux(): Map<CompteurCle, Int> =
        journal.groupBy { CompteurCle(it.essence, it.classe) }
            .mapValues { (_, tiges) -> tiges.sumOf { it.delta() } }

    /** Total de tiges sur l'ensemble du contexte. */
    fun totalContexte(): Int = journal.sumOf { it.delta() }
}
