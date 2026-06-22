package io.github.pobsteta.marculus.data

import fr.marculus.core.TotauxMartelage
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.ConfigCompteur
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.Position
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige
import io.github.pobsteta.marculus.data.db.CompteurConfigDao
import io.github.pobsteta.marculus.data.db.CompteurConfigEntity
import io.github.pobsteta.marculus.data.db.ContexteDao
import io.github.pobsteta.marculus.data.db.ContexteEntity
import io.github.pobsteta.marculus.data.db.TigeDao
import io.github.pobsteta.marculus.data.db.TigeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Un contexte accompagné de son nombre d'événements (pour l'affichage et le verrouillage). */
data class ResumeContexte(val contexte: Contexte, val nbEvenements: Int) {
    /** Verrouillé (lecture seule) : il contient des tiges et n'a pas encore été exporté. */
    val verrouille: Boolean get() = nbEvenements > 0 && !contexte.exporte
}

private const val RS = "" // séparateur d'enregistrements (essences)
private const val US = "" // séparateur de champs (nom / fond / texte)

/** Accès aux données du martelage : mappe les entités Room vers le domaine pur `fr.marculus.core`. */
class MartelageRepository(
    private val contexteDao: ContexteDao,
    private val tigeDao: TigeDao,
    private val configDao: CompteurConfigDao,
    private val horloge: () -> Long = System::currentTimeMillis,
) {
    // --- Contextes ---

    fun contextes(): Flow<List<Contexte>> =
        contexteDao.observerTous().map { liste -> liste.map { it.versDomaine() } }

    /** Contextes + nombre d'événements (combine la liste et les comptes de tiges). */
    fun resumes(): Flow<List<ResumeContexte>> =
        contexteDao.observerTous().combine(tigeDao.observerComptes()) { contextes, comptes ->
            val parId = comptes.associate { it.contexteId to it.n }
            contextes.map { ResumeContexte(it.versDomaine(), parId[it.id] ?: 0) }
        }

    suspend fun contexte(id: String): Contexte? = contexteDao.parId(id)?.versDomaine()

    /** Duplique un contexte avec toutes ses caractéristiques et ses avis, mais sans les tiges. */
    suspend fun dupliquerContexte(id: String): String? {
        val source = contexteDao.parId(id) ?: return null
        val nouvelId = UUID.randomUUID().toString()
        contexteDao.inserer(
            source.copy(
                id = nouvelId,
                nom = "${source.nom} (copie)",
                exporte = false,
                dateCreation = horloge(),
            ),
        )
        configDao.listeParContexte(id).forEach { configDao.upsert(it.copy(contexteId = nouvelId)) }
        return nouvelId
    }

    /** Marque un contexte comme exporté (débloque modification/suppression). */
    suspend fun marquerExporte(id: String) {
        val existant = contexteDao.parId(id) ?: return
        contexteDao.inserer(existant.copy(exporte = true))
    }

    /** Toute modification du journal repasse le contexte en « non exporté » (re-verrouillage). */
    private suspend fun marquerNonExporte(contexteId: String) {
        val c = contexteDao.parId(contexteId) ?: return
        if (c.exporte) contexteDao.inserer(c.copy(exporte = false))
    }

    suspend fun creerContexte(
        nom: String,
        mode: ModeMesure,
        axe: AxeClasses,
        essences: List<EssenceColonne>,
        commentaire: String? = null,
        increment: Int = 1,
        operateur: String? = null,
        cheminGpkg: String? = null,
        tarif: TarifCubage = TarifCubage.AUCUN,
        tarifNumero: Int = 0,
    ): String {
        val id = UUID.randomUUID().toString()
        contexteDao.inserer(
            ContexteEntity(
                id = id,
                nom = nom,
                mode = mode.name,
                classeMin = axe.min,
                classeMax = axe.max,
                classePas = axe.pas,
                essences = encodeEssences(essences),
                commentaire = commentaire,
                increment = increment,
                exporte = false,
                dateCreation = horloge(),
                operateur = operateur,
                cheminGpkg = cheminGpkg,
                tarif = tarif.name,
                tarifNumero = tarifNumero,
            ),
        )
        return id
    }

    suspend fun modifierContexte(
        id: String,
        nom: String,
        mode: ModeMesure,
        axe: AxeClasses,
        essences: List<EssenceColonne>,
        commentaire: String?,
        increment: Int,
        cheminGpkg: String? = null,
        tarif: TarifCubage = TarifCubage.AUCUN,
        tarifNumero: Int = 0,
    ) {
        val existant = contexteDao.parId(id) ?: return
        contexteDao.inserer(
            existant.copy(
                nom = nom,
                mode = mode.name,
                classeMin = axe.min,
                classeMax = axe.max,
                classePas = axe.pas,
                essences = encodeEssences(essences),
                commentaire = commentaire,
                increment = increment,
                cheminGpkg = cheminGpkg,
                tarif = tarif.name,
                tarifNumero = tarifNumero,
            ),
        )
    }

    /** Met à jour uniquement le GeoPackage rattaché à un contexte (import depuis la carte). */
    suspend fun enregistrerCheminGpkg(contexteId: String, chemin: String?) {
        val existant = contexteDao.parId(contexteId) ?: return
        contexteDao.inserer(existant.copy(cheminGpkg = chemin))
    }

    suspend fun supprimerContexte(id: String) {
        tigeDao.supprimerParContexte(id)
        configDao.supprimerParContexte(id)
        contexteDao.supprimer(id)
    }

    // --- Journal des tiges ---

    fun journal(contexteId: String): Flow<List<Tige>> =
        tigeDao.observerParContexte(contexteId).map { liste -> liste.map { it.versDomaine() } }

    /** Instantané du journal (pour l'export). */
    suspend fun journalInstantane(contexteId: String): List<Tige> =
        tigeDao.listeParContexte(contexteId).map { it.versDomaine() }

    fun totaux(contexteId: String): Flow<Map<CompteurCle, Int>> =
        journal(contexteId).map { TotauxMartelage(it).totaux() }

    /** Ajoute une tige (PLUS) avec la quantité = incrément du contexte, et renvoie son uuid. */
    suspend fun ajouterTige(
        contexteId: String,
        essence: String,
        classe: Int,
        quantite: Int = 1,
        hauteurTexte: String? = null,
        qualiteArbre: String? = null,
        position: Position? = null,
        operateur: String? = null,
        parcelle: String? = null,
    ): String {
        val uuid = UUID.randomUUID().toString()
        tigeDao.inserer(
            TigeEntity(
                uuid = uuid,
                contexteId = contexteId,
                essence = essence,
                classe = classe,
                action = ActionTige.PLUS.name,
                horodatage = horloge(),
                quantite = quantite,
                hauteurTexte = hauteurTexte,
                qualiteArbre = qualiteArbre,
                latitude = position?.latitude,
                longitude = position?.longitude,
                operateur = operateur,
                parcelle = parcelle,
            ),
        )
        marquerNonExporte(contexteId)
        return uuid
    }

    /** Enregistre une annulation (ANNULATION) — conservée au journal, jamais d'effacement. */
    suspend fun annulerTige(
        contexteId: String,
        essence: String,
        classe: Int,
        quantite: Int = 1,
        operateur: String? = null,
    ) {
        tigeDao.inserer(
            TigeEntity(
                uuid = UUID.randomUUID().toString(),
                contexteId = contexteId,
                essence = essence,
                classe = classe,
                action = ActionTige.ANNULATION.name,
                horodatage = horloge(),
                quantite = quantite,
                hauteurTexte = null,
                qualiteArbre = null,
                latitude = null,
                longitude = null,
                operateur = operateur,
            ),
        )
        marquerNonExporte(contexteId)
    }

    /** Corrige une tige (essence/classe/action/quantité/hauteur/qualité) ; la position GNSS et la parcelle restent inchangées. */
    suspend fun modifierTige(
        uuid: String,
        contexteId: String,
        essence: String,
        classe: Int,
        action: ActionTige,
        quantite: Int,
        hauteurTexte: String?,
        qualiteArbre: String?,
    ) {
        tigeDao.majComplet(uuid, essence, classe, action.name, quantite, hauteurTexte, qualiteArbre)
        marquerNonExporte(contexteId)
    }

    /** Annote une tige précise (par uuid) : la hauteur. */
    suspend fun annoterHauteur(uuid: String, hauteurTexte: String?) = tigeDao.majHauteur(uuid, hauteurTexte)

    /** Annote une tige précise (par uuid) : la qualité arbre. */
    suspend fun annoterQualite(uuid: String, qualiteArbre: String?) = tigeDao.majQualite(uuid, qualiteArbre)

    /** Annote la dernière tige PLUS d'une cellule (hauteur et/ou qualité arbre). */
    suspend fun annoterDerniere(
        contexteId: String,
        essence: String,
        classe: Int,
        hauteurTexte: String?,
        qualiteArbre: String?,
    ) {
        val derniere = tigeDao.dernierePlus(contexteId, essence, classe) ?: return
        tigeDao.annoter(
            uuid = derniere.uuid,
            hauteur = hauteurTexte ?: derniere.hauteurTexte,
            qualite = qualiteArbre ?: derniere.qualiteArbre,
        )
    }

    /**
     * Réinitialise toute la fiche à zéro en enregistrant des événements compensatoires
     * (append-only : l'historique est conservé, les totaux retombent à 0).
     */
    suspend fun reinitialiser(contexteId: String) {
        val journal = tigeDao.listeParContexte(contexteId).map { it.versDomaine() }
        val totaux = TotauxMartelage(journal).totaux()
        val t = horloge()
        totaux.forEach { (cle, total) ->
            when {
                total > 0 -> tigeDao.inserer(evenement(contexteId, cle.essence, cle.classe, ActionTige.ANNULATION, total, t))
                total < 0 -> tigeDao.inserer(evenement(contexteId, cle.essence, cle.classe, ActionTige.PLUS, -total, t))
            }
        }
        marquerNonExporte(contexteId)
    }

    private fun evenement(
        contexteId: String,
        essence: String,
        classe: Int,
        action: ActionTige,
        quantite: Int,
        horodatage: Long,
    ) = TigeEntity(
        uuid = UUID.randomUUID().toString(),
        contexteId = contexteId,
        essence = essence,
        classe = classe,
        action = action.name,
        horodatage = horodatage,
        quantite = quantite,
        hauteurTexte = null,
        qualiteArbre = null,
        latitude = null,
        longitude = null,
        operateur = null,
    )

    // --- Réglages par compteur (avis) ---

    suspend fun configCompteur(contexteId: String, essence: String, classe: Int): ConfigCompteur {
        val e = configDao.parCle(contexteId, essence, classe)
        return ConfigCompteur(essence, classe, e?.avisSiPlus, e?.avisSiMoins)
    }

    /** Réglages (avis) de tous les compteurs d'un contexte, réactifs (pour les alertes de la feuille). */
    fun configs(contexteId: String): Flow<Map<CompteurCle, ConfigCompteur>> =
        configDao.observerParContexte(contexteId).map { liste ->
            liste.associate {
                CompteurCle(it.essence, it.classe) to ConfigCompteur(it.essence, it.classe, it.avisSiPlus, it.avisSiMoins)
            }
        }

    suspend fun definirAvis(
        contexteId: String,
        essence: String,
        classe: Int,
        avisSiPlus: Int?,
        avisSiMoins: Int?,
    ) {
        configDao.upsert(CompteurConfigEntity(contexteId, essence, classe, avisSiPlus, avisSiMoins))
    }

    // --- Encodage / mapping ---

    private fun encodeEssences(list: List<EssenceColonne>): String =
        list.joinToString(RS) { "${it.nom}$US${it.couleurFondArgb}$US${it.couleurTexteArgb}" }

    private fun decodeEssences(s: String): List<EssenceColonne> =
        if (s.isEmpty()) {
            emptyList()
        } else {
            s.split(RS).mapNotNull { rec ->
                val p = rec.split(US)
                if (p.size == 3) EssenceColonne(p[0], p[1].toInt(), p[2].toInt()) else null
            }
        }

    private fun ContexteEntity.versDomaine() = Contexte(
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
    )

    private fun TigeEntity.versDomaine() = Tige(
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
    )
}
