package io.github.pobsteta.marculus.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "contexte")
data class ContexteEntity(
    @PrimaryKey val id: String,
    val nom: String,
    val mode: String,            // ModeMesure.name
    val classeMin: Int,
    val classeMax: Int,
    val classePas: Int,
    val essences: String,        // encodé : nom US fond US texte, enregistrements séparés par RS
    val commentaire: String?,
    val increment: Int,
    val exporte: Boolean,
    val dateCreation: Long,
    val operateur: String?,
    val cheminGpkg: String? = null,
)

@Entity(
    tableName = "tige",
    indices = [Index(value = ["contexteId", "essence", "classe"])],
)
data class TigeEntity(
    @PrimaryKey val uuid: String,
    val contexteId: String,
    val essence: String,
    val classe: Int,
    val action: String,          // ActionTige.name
    val horodatage: Long,
    val quantite: Int,
    val hauteurTexte: String?,
    val qualiteArbre: String?,
    val latitude: Double?,
    val longitude: Double?,
    val operateur: String?,
)

/** Réglages par compteur (cellule) d'un contexte : avis si plus / si moins. */
@Entity(tableName = "compteur_config", primaryKeys = ["contexteId", "essence", "classe"])
data class CompteurConfigEntity(
    val contexteId: String,
    val essence: String,
    val classe: Int,
    val avisSiPlus: Int?,
    val avisSiMoins: Int?,
)
