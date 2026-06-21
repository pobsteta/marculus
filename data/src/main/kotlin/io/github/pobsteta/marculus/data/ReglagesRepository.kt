package io.github.pobsteta.marculus.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import fr.marculus.core.model.Reglages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "reglages")

/** Persistance des réglages via Preferences DataStore. */
class ReglagesRepository(context: Context) {
    private val ds = context.applicationContext.dataStore

    private object Cles {
        val antiVeille = booleanPreferencesKey("anti_veille")
        val pleinEcran = booleanPreferencesKey("plein_ecran")
        val vibration = booleanPreferencesKey("vibration")
        val sonClic = booleanPreferencesKey("son_clic")
        val themeSombre = booleanPreferencesKey("theme_sombre")
        val capturePosition = booleanPreferencesKey("capture_position")
    }

    val reglages: Flow<Reglages> = ds.data.map { p ->
        Reglages(
            antiVeille = p[Cles.antiVeille] ?: false,
            pleinEcran = p[Cles.pleinEcran] ?: false,
            vibration = p[Cles.vibration] ?: false,
            sonClic = p[Cles.sonClic] ?: false,
            themeSombre = p[Cles.themeSombre] ?: false,
            capturePosition = p[Cles.capturePosition] ?: false,
        )
    }

    suspend fun enregistrer(r: Reglages) {
        ds.edit { p ->
            p[Cles.antiVeille] = r.antiVeille
            p[Cles.pleinEcran] = r.pleinEcran
            p[Cles.vibration] = r.vibration
            p[Cles.sonClic] = r.sonClic
            p[Cles.themeSombre] = r.themeSombre
            p[Cles.capturePosition] = r.capturePosition
        }
    }
}
