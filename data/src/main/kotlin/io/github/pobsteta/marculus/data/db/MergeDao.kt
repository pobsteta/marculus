package io.github.pobsteta.marculus.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** Projection (uuid/id, horodatage de modification) pour décider du « dernière écriture gagne ». */
data class CleModifie(val cle: String, val modifie: Long)

/**
 * Fusion (synchro multi-opérateurs) dans une seule transaction (tout ou rien).
 * « Dernière écriture gagne » : une donnée entrante remplace la locale seulement si elle est
 * plus récente (modifie supérieur) ; les nouvelles sont insérées ; les avis sont ajoutés si absents.
 */
@Dao
interface MergeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun remplacerContextes(contextes: List<ContexteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun remplacerTiges(tiges: List<TigeEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insererConfigs(configs: List<CompteurConfigEntity>)

    @Query("SELECT id AS cle, modifie FROM contexte")
    suspend fun contextesModifie(): List<CleModifie>

    @Query("SELECT uuid AS cle, modifie FROM tige")
    suspend fun tigesModifie(): List<CleModifie>

    @Transaction
    suspend fun fusionner(
        contextes: List<ContexteEntity>,
        tiges: List<TigeEntity>,
        configs: List<CompteurConfigEntity>,
    ) {
        val cMod = contextesModifie().associate { it.cle to it.modifie }
        remplacerContextes(contextes.filter { (cMod[it.id] ?: Long.MIN_VALUE) < it.modifie })
        val tMod = tigesModifie().associate { it.cle to it.modifie }
        remplacerTiges(tiges.filter { (tMod[it.uuid] ?: Long.MIN_VALUE) < it.modifie })
        insererConfigs(configs)
    }
}
