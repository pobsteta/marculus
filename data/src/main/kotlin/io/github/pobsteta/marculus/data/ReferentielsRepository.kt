package io.github.pobsteta.marculus.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.marculus.core.Referentiels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.referentielsStore by preferencesDataStore(name = "referentiels")
private const val SEP = ""

/** Listes de référence éditables (essences / qualité arbre / qualité bois), persistées via DataStore. */
class ReferentielsRepository(context: Context) {
    private val ds = context.applicationContext.referentielsStore

    private object Cles {
        val essences = stringPreferencesKey("essences")
        val qualitesArbre = stringPreferencesKey("qualites_arbre")
        val qualitesBois = stringPreferencesKey("qualites_bois")
    }

    private fun decode(s: String?, defaut: List<String>): List<String> = when {
        s == null -> defaut
        s.isBlank() -> emptyList()
        else -> s.split(SEP)
    }

    val essences: Flow<List<String>> =
        ds.data.map { decode(it[Cles.essences], Referentiels.ESSENCES_DEFAUT) }
    val qualitesArbre: Flow<List<String>> =
        ds.data.map { decode(it[Cles.qualitesArbre], Referentiels.QUALITE_ARBRE_DEFAUT) }
    val qualitesBois: Flow<List<String>> =
        ds.data.map { decode(it[Cles.qualitesBois], Referentiels.QUALITE_BOIS_DEFAUT) }

    suspend fun enregistrerEssences(liste: List<String>) =
        ds.edit { it[Cles.essences] = liste.joinToString(SEP) }.let {}

    suspend fun enregistrerQualitesArbre(liste: List<String>) =
        ds.edit { it[Cles.qualitesArbre] = liste.joinToString(SEP) }.let {}

    suspend fun enregistrerQualitesBois(liste: List<String>) =
        ds.edit { it[Cles.qualitesBois] = liste.joinToString(SEP) }.let {}
}
