package io.github.pobsteta.marculus.data

import fr.marculus.core.TotauxMartelage
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.Position
import fr.marculus.core.model.Tige
import io.github.pobsteta.marculus.data.db.ContexteDao
import io.github.pobsteta.marculus.data.db.ContexteEntity
import io.github.pobsteta.marculus.data.db.TigeDao
import io.github.pobsteta.marculus.data.db.TigeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private const val SEP = ""

/** Accès aux données du martelage : mappe les entités Room vers le domaine pur `fr.marculus.core`. */
class MartelageRepository(
    private val contexteDao: ContexteDao,
    private val tigeDao: TigeDao,
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
        essencesActives: List<String>,
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
                essencesActives = essencesActives.joinToString(SEP),
                dateCreation = horloge(),
                operateur = operateur,
            ),
        )
        return id
    }

    // --- Journal des tiges ---

    fun journal(contexteId: String): Flow<List<Tige>> =
        tigeDao.observerParContexte(contexteId).map { liste -> liste.map { it.versDomaine() } }

    fun totaux(contexteId: String): Flow<Map<CompteurCle, Int>> =
        journal(contexteId).map { TotauxMartelage(it).totaux() }

    /** Ajoute une tige (PLUS) et renvoie son uuid. */
    suspend fun ajouterTige(
        contexteId: String,
        essence: String,
        classe: Int,
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
    suspend fun annulerTige(contexteId: String, essence: String, classe: Int, operateur: String? = null) {
        tigeDao.inserer(
            TigeEntity(
                uuid = UUID.randomUUID().toString(),
                contexteId = contexteId,
                essence = essence,
                classe = classe,
                action = ActionTige.ANNULATION.name,
                horodatage = horloge(),
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

    // --- Mapping entité -> domaine ---

    private fun ContexteEntity.versDomaine() = Contexte(
        id = id,
        nom = nom,
        mode = ModeMesure.valueOf(mode),
        axe = AxeClasses(min = classeMin, max = classeMax, pas = classePas),
        essencesActives = if (essencesActives.isEmpty()) emptyList() else essencesActives.split(SEP),
    )

    private fun TigeEntity.versDomaine() = Tige(
        uuid = uuid,
        contexteId = contexteId,
        essence = essence,
        classe = classe,
        action = ActionTige.valueOf(action),
        horodatage = horodatage,
        hauteurTexte = hauteurTexte,
        qualiteArbre = qualiteArbre,
        position = if (latitude != null && longitude != null) Position(latitude, longitude) else null,
        operateur = operateur,
    )
}
