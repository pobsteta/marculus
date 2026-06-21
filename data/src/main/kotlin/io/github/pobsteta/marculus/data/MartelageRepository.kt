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
import fr.marculus.core.model.Tige
import io.github.pobsteta.marculus.data.db.CompteurConfigDao
import io.github.pobsteta.marculus.data.db.CompteurConfigEntity
import io.github.pobsteta.marculus.data.db.ContexteDao
import io.github.pobsteta.marculus.data.db.ContexteEntity
import io.github.pobsteta.marculus.data.db.TigeDao
import io.github.pobsteta.marculus.data.db.TigeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

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

    suspend fun contexte(id: String): Contexte? = contexteDao.parId(id)?.versDomaine()

    suspend fun creerContexte(
        nom: String,
        mode: ModeMesure,
        axe: AxeClasses,
        essences: List<EssenceColonne>,
        commentaire: String? = null,
        increment: Int = 1,
        operateur: String? = null,
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
                dateCreation = horloge(),
                operateur = operateur,
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
            ),
        )
    }

    suspend fun supprimerContexte(id: String) {
        tigeDao.supprimerParContexte(id)
        configDao.supprimerParContexte(id)
        contexteDao.supprimer(id)
    }

    // --- Journal des tiges ---

    fun journal(contexteId: String): Flow<List<Tige>> =
        tigeDao.observerParContexte(contexteId).map { liste -> liste.map { it.versDomaine() } }

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
            ),
        )
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
    }

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

    suspend fun definirAvis(
        contexteId: String,
        essence: String,
        classe: Int,
        avisSiPlus: String?,
        avisSiMoins: String?,
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
    )
}
