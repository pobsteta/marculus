package io.github.pobsteta.marculus.data

import io.github.pobsteta.marculus.data.db.ContexteEntity
import io.github.pobsteta.marculus.data.db.TigeEntity

/**
 * Réglages **du terrain** : ce que l'opérateur règle sur le téléphone et qu'un émetteur peut
 * ignorer. Nemeton réémet chaque contexte de son plan, plus récent, sans tarif ni affouage. Un
 * Marculus plus ancien repartage des tiges sans `lot`. La fusion remplace la ligne entière :
 * sans ce garde-fou, chaque réglage absent du fichier retomberait à sa valeur par défaut.
 *
 * Règle : une clé **absente** du fichier garde la valeur locale ; une clé **présente** l'emporte,
 * y compris pour remettre une valeur par défaut. Sans ligne locale, les défauts s'appliquent.
 */
internal object ReglagesTerrain {

    /** Clés JSON d'un contexte conservées quand le fichier ne les porte pas. */
    val CLES_CONTEXTE = setOf(
        "affouage", "volumeMaxLotM3", "partHouppier", "tarif", "tarifNumero", "coefficientForme",
        "cheminGpkg", "tarifsEssence", "steresParM3", "communeInsee", "ser", "millesimeTarifs",
    )

    /** Clé JSON d'une tige conservée quand le fichier ne la porte pas (lot figé au martelage). */
    const val CLE_LOT = "lot"

    fun completer(entrant: ContexteEntity, local: ContexteEntity?, clesPresentes: Set<String>): ContexteEntity {
        if (local == null) return entrant
        fun absent(cle: String) = cle !in clesPresentes
        return entrant.copy(
            affouage = if (absent("affouage")) local.affouage else entrant.affouage,
            volumeMaxLotM3 = if (absent("volumeMaxLotM3")) local.volumeMaxLotM3 else entrant.volumeMaxLotM3,
            partHouppier = if (absent("partHouppier")) local.partHouppier else entrant.partHouppier,
            tarif = if (absent("tarif")) local.tarif else entrant.tarif,
            tarifNumero = if (absent("tarifNumero")) local.tarifNumero else entrant.tarifNumero,
            coefficientForme = if (absent("coefficientForme")) local.coefficientForme else entrant.coefficientForme,
            cheminGpkg = if (absent("cheminGpkg")) local.cheminGpkg else entrant.cheminGpkg,
            tarifsEssence = if (absent("tarifsEssence")) local.tarifsEssence else entrant.tarifsEssence,
            steresParM3 = if (absent("steresParM3")) local.steresParM3 else entrant.steresParM3,
            communeInsee = if (absent("communeInsee")) local.communeInsee else entrant.communeInsee,
            ser = if (absent("ser")) local.ser else entrant.ser,
            millesimeTarifs = if (absent("millesimeTarifs")) local.millesimeTarifs else entrant.millesimeTarifs,
        )
    }

    fun completer(entrant: TigeEntity, local: TigeEntity?, clesPresentes: Set<String>): TigeEntity =
        if (local != null && CLE_LOT !in clesPresentes) entrant.copy(lot = local.lot) else entrant
}
