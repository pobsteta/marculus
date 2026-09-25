package io.github.pobsteta.marculus.data

import io.github.pobsteta.marculus.data.db.CompteurConfigDao
import io.github.pobsteta.marculus.data.db.CompteurConfigEntity
import io.github.pobsteta.marculus.data.db.ContexteDao
import io.github.pobsteta.marculus.data.db.ContexteEntity
import io.github.pobsteta.marculus.data.db.MergeDao
import io.github.pobsteta.marculus.data.db.TigeDao
import io.github.pobsteta.marculus.data.db.TigeEntity
import fr.marculus.core.LotMartelage
import kotlinx.coroutines.flow.first
import fr.marculus.core.VolumesMartelage
import org.json.JSONArray
import org.json.JSONObject

/** Sauvegarde / restauration complète des données (sérialisation JSON, enveloppée en ZIP côté app). */
class SauvegardeRepository(
    private val contexteDao: ContexteDao,
    private val tigeDao: TigeDao,
    private val configDao: CompteurConfigDao,
    private val mergeDao: MergeDao,
    private val referentiels: ReferentielsRepository,
) {
    suspend fun exporterJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        val contextes = contexteDao.toutes()
        val tiges = tigeDao.toutes()
        val v = volumes(contextes, tiges)
        root.put("contextes", JSONArray().apply { contextes.forEach { put(it.toJson(v.totaux[it.id])) } })
        root.put("tiges", JSONArray().apply { tiges.forEach { put(it.toJson(v.parTige[it.uuid])) } })
        root.put("configs", JSONArray().apply { configDao.toutes().forEach { put(it.toJson()) } })
        root.put(
            "referentiels",
            JSONObject().apply {
                put("essences", JSONArray(referentiels.essences.first()))
                put("qualitesArbre", JSONArray(referentiels.qualitesArbre.first()))
                put("qualitesBois", JSONArray(referentiels.qualitesBois.first()))
            },
        )
        return root.toString(2)
    }

    /** Remplace toutes les données par celles du JSON. */
    suspend fun importerJson(json: String) {
        val root = JSONObject(json)
        configDao.toutSupprimer()
        tigeDao.toutSupprimer()
        contexteDao.toutSupprimer()
        root.getJSONArray("contextes").objets().forEach { contexteDao.inserer(it.versContexte()) }
        root.getJSONArray("tiges").objets().forEach { tigeDao.inserer(it.versTige()) }
        root.getJSONArray("configs").objets().forEach { configDao.upsert(it.versConfig()) }
        root.optJSONObject("referentiels")?.let { r ->
            referentiels.enregistrerEssences(r.getJSONArray("essences").chaines())
            referentiels.enregistrerQualitesArbre(r.getJSONArray("qualitesArbre").chaines())
            referentiels.enregistrerQualitesBois(r.getJSONArray("qualitesBois").chaines())
        }
    }

    /** Exporte un seul contexte (contexte + ses tiges + ses avis) pour le partager. */
    suspend fun exporterContexteJson(contexteId: String): String {
        val root = JSONObject()
        root.put("version", 1)
        val contextes = listOfNotNull(contexteDao.parId(contexteId))
        val tiges = tigeDao.listeParContexte(contexteId)
        val v = volumes(contextes, tiges)
        root.put("contextes", JSONArray().apply { contextes.forEach { put(it.toJson(v.totaux[it.id])) } })
        root.put("tiges", JSONArray().apply { tiges.forEach { put(it.toJson(v.parTige[it.uuid])) } })
        root.put("configs", JSONArray().apply { configDao.listeParContexte(contexteId).forEach { put(it.toJson()) } })
        return root.toString(2)
    }

    /**
     * Fusionne (synchro multi-opérateurs) le JSON d'un autre appareil : union par UUID des
     * contextes, tiges et avis, sans écraser le local. Atomique (transaction). Les référentiels
     * ne sont pas fusionnés (chaque appareil garde les siens).
     */
    suspend fun fusionnerJson(json: String) {
        val root = JSONObject(json)
        val contextes = root.optJSONArray("contextes").objetsOuVide().map { it.versContexte() }
        val tiges = root.optJSONArray("tiges").objetsOuVide().map { it.versTige() }
        val configs = root.optJSONArray("configs").objetsOuVide().map { it.versConfig() }
        mergeDao.fusionner(contextes, tiges, configs)
    }

    /**
     * Contextes annoncés par un lot, avec le **`gpkgNom`** qui les apparie à leur GeoPackage.
     * Ce champ n'existe que dans les lots : `versContexte()` l'ignore, et `cheminGpkg` reste à
     * renseigner après copie — la machine émettrice ne peut pas connaître nos chemins privés.
     * Lecture seule : cette fonction n'écrit rien, la fusion reste l'affaire de [fusionnerJson].
     */
    fun contextesDuLot(json: String): List<LotMartelage.ContexteDuLot> =
        JSONObject(json).optJSONArray("contextes").objetsOuVide().map {
            LotMartelage.ContexteDuLot(
                id = it.getString("id"),
                nom = it.optString("nom", it.getString("id")),
                gpkgNom = it.texteOuNull("gpkgNom"),
            )
        }

    private fun JSONArray?.objetsOuVide(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

    /** Volumes calculés à l'export, avec le tarif courant de chaque contexte. */
    private class Volumes(
        val totaux: Map<String, VolumesMartelage.Totaux>,
        val parTige: Map<String, VolumesMartelage.CubageTige>,
    )

    private fun volumes(contextes: List<ContexteEntity>, tiges: List<TigeEntity>): Volumes {
        val parContexte = tiges.groupBy { it.contexteId }
        val totaux = mutableMapOf<String, VolumesMartelage.Totaux>()
        val parTige = mutableMapOf<String, VolumesMartelage.CubageTige>()
        contextes.forEach { entite ->
            val contexte = runCatching { entite.versDomaine() }.getOrNull() ?: return@forEach
            val journal = parContexte[entite.id].orEmpty().mapNotNull { runCatching { it.versDomaine() }.getOrNull() }
            totaux[entite.id] = VolumesMartelage.totaux(contexte, journal)
            journal.forEach { parTige[it.uuid] = VolumesMartelage.cubage(contexte, it) }
        }
        return Volumes(totaux, parTige)
    }

    // --- Sérialisation des entités ---

    /**
     * [volumes] : totaux nets calculés sur le téléphone (lecture seule, ignorés à la relecture).
     * Marculus est la seule source des volumes de martelage.
     */
    private fun ContexteEntity.toJson(volumes: VolumesMartelage.Totaux? = null) = JSONObject().apply {
        put("id", id); put("nom", nom); put("mode", mode)
        put("classeMin", classeMin); put("classeMax", classeMax); put("classePas", classePas)
        put("essences", essences); putOpt("commentaire", commentaire); put("increment", increment)
        put("exporte", exporte); put("dateCreation", dateCreation); putOpt("operateur", operateur)
        putOpt("cheminGpkg", cheminGpkg); put("tarif", tarif); put("tarifNumero", tarifNumero)
        put("coefficientForme", coefficientForme); putOpt("dateMartelage", dateMartelage)
        put("statut", statut); put("modifie", modifie); put("dejaExporte", dejaExporte)
        volumes?.let {
            put("volumeTigeTotalM3", it.volumeTigeM3); put("volumeTotalM3", it.volumeTotalM3)
            put("surfaceTerriereTotaleM2", it.surfaceTerriereM2); put("nbTigesNonCubees", it.nbTigesNonCubees)
        }
    }

    private fun JSONObject.versContexte() = ContexteEntity(
        id = getString("id"), nom = getString("nom"), mode = getString("mode"),
        classeMin = getInt("classeMin"), classeMax = getInt("classeMax"), classePas = getInt("classePas"),
        essences = optString("essences", ""), commentaire = texteOuNull("commentaire"),
        increment = getInt("increment"), exporte = getBoolean("exporte"),
        dateCreation = getLong("dateCreation"), operateur = texteOuNull("operateur"),
        cheminGpkg = texteOuNull("cheminGpkg"),
        tarif = if (has("tarif") && !isNull("tarif")) getString("tarif") else "AUCUN",
        tarifNumero = if (has("tarifNumero") && !isNull("tarifNumero")) getInt("tarifNumero") else 0,
        coefficientForme = if (has("coefficientForme") && !isNull("coefficientForme")) getDouble("coefficientForme") else 0.5,
        dateMartelage = if (has("dateMartelage") && !isNull("dateMartelage")) getLong("dateMartelage") else null,
        statut = if (has("statut") && !isNull("statut")) getString("statut") else "PROPOSEE",
        modifie = if (has("modifie") && !isNull("modifie")) getLong("modifie") else 0,
        // Absent des fichiers d'avant la v13 : un contexte exporté l'a forcément été une fois.
        dejaExporte = optBoolean("dejaExporte", getBoolean("exporte")),
    )

    /** [cubage] : valeurs unitaires (une tige) calculées à l'export ; ignorées à la relecture. */
    private fun TigeEntity.toJson(cubage: VolumesMartelage.CubageTige? = null) = JSONObject().apply {
        put("uuid", uuid); put("contexteId", contexteId); put("essence", essence); put("classe", classe)
        put("action", action); put("horodatage", horodatage); put("quantite", quantite)
        putOpt("hauteurTexte", hauteurTexte); putOpt("qualiteArbre", qualiteArbre)
        putOpt("latitude", latitude); putOpt("longitude", longitude); putOpt("operateur", operateur)
        putOpt("parcelle", parcelle); putOpt("qualiteFix", qualiteFix); putOpt("precisionM", precisionM)
        put("modifie", modifie)
        cubage?.let {
            put("volumeTigeM3", it.volumeTigeM3); put("volumeHouppierM3", it.volumeHouppierM3)
            put("volumeTotalM3", it.volumeTotalM3); put("surfaceTerriereM2", it.surfaceTerriereM2)
            put("cubage", it.cubage)
        }
    }

    private fun JSONObject.versTige() = TigeEntity(
        uuid = getString("uuid"), contexteId = getString("contexteId"), essence = getString("essence"),
        classe = getInt("classe"), action = getString("action"), horodatage = getLong("horodatage"),
        quantite = getInt("quantite"), hauteurTexte = texteOuNull("hauteurTexte"),
        qualiteArbre = texteOuNull("qualiteArbre"), latitude = reelOuNull("latitude"),
        longitude = reelOuNull("longitude"), operateur = texteOuNull("operateur"),
        parcelle = texteOuNull("parcelle"),
        qualiteFix = texteOuNull("qualiteFix"), precisionM = reelOuNull("precisionM"),
        modifie = if (has("modifie") && !isNull("modifie")) getLong("modifie") else 0,
    )

    private fun CompteurConfigEntity.toJson() = JSONObject().apply {
        put("contexteId", contexteId); put("essence", essence); put("classe", classe)
        putOpt("avisSiPlus", avisSiPlus); putOpt("avisSiMoins", avisSiMoins)
    }

    private fun JSONObject.versConfig() = CompteurConfigEntity(
        contexteId = getString("contexteId"), essence = getString("essence"), classe = getInt("classe"),
        avisSiPlus = entierOuNull("avisSiPlus"), avisSiMoins = entierOuNull("avisSiMoins"),
    )

    private fun JSONObject.texteOuNull(cle: String): String? =
        if (has(cle) && !isNull(cle)) getString(cle) else null

    private fun JSONObject.reelOuNull(cle: String): Double? =
        if (has(cle) && !isNull(cle)) getDouble(cle) else null

    private fun JSONObject.entierOuNull(cle: String): Int? =
        if (has(cle) && !isNull(cle)) getInt(cle) else null

    private fun JSONArray.objets(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONArray.chaines(): List<String> = (0 until length()).map { getString(it) }
}
