package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.Contexte
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
    data class Repartition(
        val parcelles: Map<String?, List<Tige>>,
        val sansPosition: Int,
        /** Tiges `PLUS` nettes sans position (comptées dans [sansPosition]). */
        val tigesSansPosition: List<Tige> = emptyList(),
        /** Annulations sans tige à retirer (comptées en négatif dans [sansPosition]). */
        val annulationsOrphelines: List<Tige> = emptyList(),
    ) {
        /**
         * Volumes de « sans position », même calcul que [VolumesMartelage.totaux] : les tiges sans
         * position s'ajoutent, une annulation orpheline retire le cubage de sa propre saisie. Ajouté à
         * ceux des parcelles, il donne les volumes de Statut.
         */
        fun volumesSansPosition(contexte: Contexte): VolumesMartelage.Totaux {
            var vt = 0.0; var vh = 0.0; var vtot = 0.0; var g = 0.0; var nc = 0
            fun ajouter(t: Tige, signe: Int) {
                val c = VolumesMartelage.cubage(contexte, t)
                val n = signe * t.quantite
                vt += c.volumeTigeM3 * n; vh += c.volumeHouppierM3 * n
                vtot += c.volumeTotalM3 * n; g += c.surfaceTerriereM2 * n
                if (c.nonCubee) nc += n
            }
            tigesSansPosition.forEach { ajouter(it, 1) }
            annulationsOrphelines.forEach { ajouter(it, -1) }
            return VolumesMartelage.Totaux(vt, vh, vtot, g, nc)
        }

        fun nombre(parcelle: String?): Int = parcelles[parcelle]?.sumOf { it.quantite } ?: 0

        /** Toutes lignes comprises : parcelles, hors parcelle, sans position. */
        val total: Int get() = parcelles.values.sumOf { l -> l.sumOf { it.quantite } } + sansPosition
    }

    /** [parcelleDe] n'est appelée que pour les tiges avec position. */
    fun repartir(journal: List<Tige>, parcelleDe: (Tige) -> String?): Repartition {
        val parcelles = linkedMapOf<String?, MutableList<Tige>>()
        var sansPosition = 0
        val sans = mutableListOf<Tige>()
        val orphelines = mutableListOf<Tige>()
        for (t in JournalNet.tiges(journal)) {
            when {
                t.action != ActionTige.PLUS -> { sansPosition -= t.quantite; orphelines += t }
                t.position == null -> { sansPosition += t.quantite; sans += t }
                else -> parcelles.getOrPut(parcelleDe(t)) { mutableListOf() } += t
            }
        }
        return Repartition(parcelles, sansPosition, sans, orphelines)
    }
}
