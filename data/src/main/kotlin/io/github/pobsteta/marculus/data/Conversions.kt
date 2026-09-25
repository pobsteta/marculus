package io.github.pobsteta.marculus.data

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.EtatKanban
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.Position
import fr.marculus.core.model.QualiteFix
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige
import io.github.pobsteta.marculus.data.db.ContexteEntity
import io.github.pobsteta.marculus.data.db.TigeEntity

/** Conversions entités Room ↔ modèle du domaine, partagées par les dépôts du module. */

internal const val RS = "" // séparateur d'enregistrements (essences)
internal const val US = "" // séparateur de champs (nom / fond / texte)

internal fun encodeEssences(list: List<EssenceColonne>): String =
    list.joinToString(RS) { "${it.nom}$US${it.couleurFondArgb}$US${it.couleurTexteArgb}" }

internal fun decodeEssences(s: String): List<EssenceColonne> =
    if (s.isEmpty()) {
        emptyList()
    } else {
        s.split(RS).mapNotNull { rec ->
            val p = rec.split(US)
            if (p.size == 3) EssenceColonne(p[0], p[1].toInt(), p[2].toInt()) else null
        }
    }

internal fun ContexteEntity.versDomaine() = Contexte(
    id = id,
    nom = nom,
    mode = ModeMesure.valueOf(mode),
    axe = AxeClasses(min = classeMin, max = classeMax, pas = classePas),
    essences = decodeEssences(essences),
    commentaire = commentaire,
    increment = increment,
    exporte = exporte,
    cheminGpkg = cheminGpkg,
    tarif = runCatching { TarifCubage.valueOf(tarif) }.getOrDefault(TarifCubage.AUCUN),
    tarifNumero = tarifNumero,
    coefficientForme = coefficientForme,
    dateMartelage = dateMartelage,
    statut = runCatching { EtatKanban.valueOf(statut) }.getOrDefault(EtatKanban.PROPOSEE),
    modifie = modifie,
    dejaExporte = dejaExporte,
)

internal fun TigeEntity.versDomaine() = Tige(
    uuid = uuid,
    contexteId = contexteId,
    essence = essence,
    classe = classe,
    action = ActionTige.valueOf(action),
    horodatage = horodatage,
    quantite = quantite,
    hauteurTexte = hauteurTexte,
    qualiteArbre = qualiteArbre,
    position = if (latitude != null && longitude != null) Position(latitude, longitude) else null,
    operateur = operateur,
    parcelle = parcelle,
    qualiteFix = qualiteFix?.let { runCatching { QualiteFix.valueOf(it) }.getOrNull() },
    precisionM = precisionM,
    modifie = modifie,
)
