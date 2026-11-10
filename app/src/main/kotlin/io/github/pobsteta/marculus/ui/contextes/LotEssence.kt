package io.github.pobsteta.marculus.ui.contextes

import fr.marculus.core.Cubage
import fr.marculus.core.model.PartsLot
import fr.marculus.core.model.TarifCubage

/**
 * Les cases cochées d'une essence peuvent-elles ajouter du volume au lot ? Faux : l'écran dit
 * « Cette essence ne compte pas dans le lot » (mission 027, décision de Pascal du 2026-11-10).
 * Même lecture que [fr.marculus.core.AffouageLots.volumes] : la tige vaut 0 en tarif Aucun, et le
 * houppier vaut 0 en Schaeffer à part 0 % ou vide, en EMERGE sur une essence non couverte
 * ([Cubage.estCouverteEmerge]), en Aucun. [partPourcent] : la saisie, en %, telle qu'à l'écran.
 */
internal fun compteAuLot(essence: String, tarif: TarifCubage, parts: PartsLot, partPourcent: String): Boolean {
    if (tarif == TarifCubage.AUCUN) return false
    if (parts.tige) return true
    if (!parts.houppier) return false
    return when {
        tarif == TarifCubage.EMERGE -> Cubage.estCouverteEmerge(essence)
        else -> {
            val p = partPourcent.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
            p / 100.0 > 0.0
        }
    }
}
