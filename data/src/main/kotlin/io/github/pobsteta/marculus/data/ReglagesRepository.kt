package io.github.pobsteta.marculus.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val annonceNombre = booleanPreferencesKey("annonce_nombre")
        val annonceEtiquette = booleanPreferencesKey("annonce_etiquette")
        val boutonsVolume = booleanPreferencesKey("boutons_volume")
        val rouvrirDernier = booleanPreferencesKey("rouvrir_dernier")
        val dernierContexte = stringPreferencesKey("dernier_contexte")
        val voixTts = stringPreferencesKey("voix_tts")
        val operateur = stringPreferencesKey("operateur")
    }

    val reglages: Flow<Reglages> = ds.data.map { p ->
        Reglages(
            antiVeille = p[Cles.antiVeille] ?: false,
            pleinEcran = p[Cles.pleinEcran] ?: false,
            vibration = p[Cles.vibration] ?: false,
            sonClic = p[Cles.sonClic] ?: false,
            themeSombre = p[Cles.themeSombre] ?: false,
            capturePosition = p[Cles.capturePosition] ?: false,
            annonceNombre = p[Cles.annonceNombre] ?: false,
            annonceEtiquette = p[Cles.annonceEtiquette] ?: false,
            boutonsVolume = p[Cles.boutonsVolume] ?: false,
            rouvrirDernier = p[Cles.rouvrirDernier] ?: false,
            dernierContexteId = p[Cles.dernierContexte],
            voixTts = p[Cles.voixTts],
            operateur = p[Cles.operateur],
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
            p[Cles.annonceNombre] = r.annonceNombre
            p[Cles.annonceEtiquette] = r.annonceEtiquette
            p[Cles.boutonsVolume] = r.boutonsVolume
            p[Cles.rouvrirDernier] = r.rouvrirDernier
            val id = r.dernierContexteId
            if (id == null) p.remove(Cles.dernierContexte) else p[Cles.dernierContexte] = id
            val voix = r.voixTts
            if (voix == null) p.remove(Cles.voixTts) else p[Cles.voixTts] = voix
            val op = r.operateur
            if (op.isNullOrBlank()) p.remove(Cles.operateur) else p[Cles.operateur] = op
        }
    }

    /** Mémorise le dernier contexte ouvert (sans toucher aux autres réglages). */
    suspend fun enregistrerDernierContexte(id: String) {
        ds.edit { it[Cles.dernierContexte] = id }
    }
}
