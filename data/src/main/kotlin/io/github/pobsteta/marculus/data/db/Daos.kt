package io.github.pobsteta.marculus.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContexteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserer(contexte: ContexteEntity)

    @Query("SELECT * FROM contexte ORDER BY dateCreation DESC")
    fun observerTous(): Flow<List<ContexteEntity>>

    @Query("SELECT * FROM contexte WHERE id = :id")
    suspend fun parId(id: String): ContexteEntity?
}

@Dao
interface TigeDao {
    @Insert
    suspend fun inserer(tige: TigeEntity)

    @Query("SELECT * FROM tige WHERE contexteId = :contexteId ORDER BY horodatage ASC")
    fun observerParContexte(contexteId: String): Flow<List<TigeEntity>>

    /** Dernière tige PLUS d'une cellule (pour l'annotation hauteur/qualité a posteriori). */
    @Query(
        "SELECT * FROM tige WHERE contexteId = :contexteId AND essence = :essence " +
            "AND classe = :classe AND action = 'PLUS' ORDER BY horodatage DESC LIMIT 1",
    )
    suspend fun dernierePlus(contexteId: String, essence: String, classe: Int): TigeEntity?

    @Query("UPDATE tige SET hauteurTexte = :hauteur, qualiteArbre = :qualite WHERE uuid = :uuid")
    suspend fun annoter(uuid: String, hauteur: String?, qualite: String?)
}
