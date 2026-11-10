package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.Tige

/**
 * Répartition « Par parcelle » des tiges **nettes** ([JournalNet] : annulations retirées avec la
 * règle de Statut). Le rattachement se fait par la position de la tige ; une annulation, saisie
 * sans position, n'est jamais rattachée : c'est la tige qu'elle retire qui porte la parcelle.
 */
object ParParcelle {

    /**
     * [parcelles] : tiges `PLUS` restantes (avec leur quantité restante) par parcelle ; la clé
     * `null` regroupe les tiges avec position mais hors de toute parcelle. [sansPosition] : quantités
     * nettes sans position ; une annulation sans tige à retirer (journal fusionné dans le désordre)
     * y compte en négatif, pour que [total] reste égal à `TotauxMartelage.totalContexte()`.
     */
    data class Repartition(val parcelles: Map<String?, List<Tige>>, val sansPosition: Int) {
        fun nombre(parcelle: String?): Int = parcelles[parcelle]?.sumOf { it.quantite } ?: 0

        /** Toutes lignes comprises : parcelles, hors parcelle, sans position. */
        val total: Int get() = parcelles.values.sumOf { l -> l.sumOf { it.quantite } } + sansPosition
    }

    /** [parcelleDe] n'est appelée que pour les tiges avec position. */
    fun repartir(journal: List<Tige>, parcelleDe: (Tige) -> String?): Repartition {
        val parcelles = linkedMapOf<String?, MutableList<Tige>>()
        var sansPosition = 0
        for (t in JournalNet.tiges(journal)) {
            when {
                t.action != ActionTige.PLUS -> sansPosition -= t.quantite
                t.position == null -> sansPosition += t.quantite
                else -> parcelles.getOrPut(parcelleDe(t)) { mutableListOf() } += t
            }
        }
        return Repartition(parcelles, sansPosition)
    }
}
