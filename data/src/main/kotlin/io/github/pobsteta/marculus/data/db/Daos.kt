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

    @Query("DELETE FROM contexte WHERE id = :id")
    suspend fun supprimer(id: String)
}

@Dao
interface TigeDao {
    @Insert
    suspend fun inserer(tige: TigeEntity)

    @Query("SELECT * FROM tige WHERE contexteId = :contexteId ORDER BY horodatage ASC")
    fun observerParContexte(contexteId: String): Flow<List<TigeEntity>>

    @Query("SELECT * FROM tige WHERE contexteId = :contexteId")
    suspend fun listeParContexte(contexteId: String): List<TigeEntity>

    @Query("SELECT contexteId, COUNT(*) AS n FROM tige GROUP BY contexteId")
    fun observerComptes(): Flow<List<CompteContexte>>

    /** Dernière tige PLUS d'une cellule (pour l'annotation hauteur/qualité a posteriori). */
    @Query(
        "SELECT * FROM tige WHERE contexteId = :contexteId AND essence = :essence " +
            "AND classe = :classe AND action = 'PLUS' ORDER BY horodatage DESC LIMIT 1",
    )
    suspend fun dernierePlus(contexteId: String, essence: String, classe: Int): TigeEntity?

    @Query("UPDATE tige SET hauteurTexte = :hauteur, qualiteArbre = :qualite WHERE uuid = :uuid")
    suspend fun annoter(uuid: String, hauteur: String?, qualite: String?)

    @Query("UPDATE tige SET hauteurTexte = :hauteur WHERE uuid = :uuid")
    suspend fun majHauteur(uuid: String, hauteur: String?)

    @Query("UPDATE tige SET qualiteArbre = :qualite WHERE uuid = :uuid")
    suspend fun majQualite(uuid: String, qualite: String?)

    @Query("DELETE FROM tige WHERE contexteId = :contexteId")
    suspend fun supprimerParContexte(contexteId: String)
}

/** Projection : nombre d'événements (tiges) par contexte. */
data class CompteContexte(val contexteId: String, val n: Int)

@Dao
interface CompteurConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: CompteurConfigEntity)

    @Query(
        "SELECT * FROM compteur_config WHERE contexteId = :contexteId AND essence = :essence AND classe = :classe",
    )
    suspend fun parCle(contexteId: String, essence: String, classe: Int): CompteurConfigEntity?

    @Query("SELECT * FROM compteur_config WHERE contexteId = :contexteId")
    suspend fun listeParContexte(contexteId: String): List<CompteurConfigEntity>

    @Query("SELECT * FROM compteur_config WHERE contexteId = :contexteId")
    fun observerParContexte(contexteId: String): Flow<List<CompteurConfigEntity>>

    @Query("DELETE FROM compteur_config WHERE contexteId = :contexteId")
    suspend fun supprimerParContexte(contexteId: String)
}
