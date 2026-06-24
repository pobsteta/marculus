package io.github.pobsteta.marculus.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.marculus.core.model.ConfigRtk
import fr.marculus.core.model.Reglages
import fr.marculus.core.model.TransportRtk
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
        val gnssPonctuel = booleanPreferencesKey("gnss_ponctuel")
        val afficherCodeEssence = booleanPreferencesKey("afficher_code_essence")
        val vueKanban = booleanPreferencesKey("vue_kanban")
        val annonceAvisMoins = booleanPreferencesKey("annonce_avis_moins")
        val annonceAvisPlus = booleanPreferencesKey("annonce_avis_plus")
        val rtkActif = booleanPreferencesKey("rtk_actif")
        val rtkTransport = stringPreferencesKey("rtk_transport")
        val rtkAppareilBt = stringPreferencesKey("rtk_appareil_bt")
        val rtkAppareilBtNom = stringPreferencesKey("rtk_appareil_bt_nom")
        val rtkHoteTcp = stringPreferencesKey("rtk_hote_tcp")
        val rtkPortTcp = intPreferencesKey("rtk_port_tcp")
        val rtkPontNtrip = booleanPreferencesKey("rtk_pont_ntrip")
        val rtkCasterHote = stringPreferencesKey("rtk_caster_hote")
        val rtkCasterPort = intPreferencesKey("rtk_caster_port")
        val rtkMountpoint = stringPreferencesKey("rtk_mountpoint")
        val rtkUtilisateur = stringPreferencesKey("rtk_utilisateur")
        val rtkMotDePasse = stringPreferencesKey("rtk_motdepasse")
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
            gnssPonctuel = p[Cles.gnssPonctuel] ?: false,
            afficherCodeEssence = p[Cles.afficherCodeEssence] ?: false,
            vueKanban = p[Cles.vueKanban] ?: false,
            annonceAvisMoins = p[Cles.annonceAvisMoins] ?: false,
            annonceAvisPlus = p[Cles.annonceAvisPlus] ?: false,
            rtk = ConfigRtk(
                actif = p[Cles.rtkActif] ?: false,
                transport = if (p[Cles.rtkTransport] == "TCP") TransportRtk.TCP else TransportRtk.BLUETOOTH,
                appareilBt = p[Cles.rtkAppareilBt],
                appareilBtNom = p[Cles.rtkAppareilBtNom],
                hoteTcp = p[Cles.rtkHoteTcp] ?: "",
                portTcp = p[Cles.rtkPortTcp] ?: 5000,
                pontNtrip = p[Cles.rtkPontNtrip] ?: false,
                casterHote = p[Cles.rtkCasterHote] ?: "caster.centipede.fr",
                casterPort = p[Cles.rtkCasterPort] ?: 2101,
                mountpoint = p[Cles.rtkMountpoint] ?: "",
                utilisateur = p[Cles.rtkUtilisateur] ?: "centipede",
                motDePasse = p[Cles.rtkMotDePasse] ?: "centipede",
            ),
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
            p[Cles.gnssPonctuel] = r.gnssPonctuel
            p[Cles.afficherCodeEssence] = r.afficherCodeEssence
            p[Cles.vueKanban] = r.vueKanban
            p[Cles.annonceAvisMoins] = r.annonceAvisMoins
            p[Cles.annonceAvisPlus] = r.annonceAvisPlus
            p[Cles.rtkActif] = r.rtk.actif
            p[Cles.rtkTransport] = r.rtk.transport.name
            r.rtk.appareilBt.let { if (it == null) p.remove(Cles.rtkAppareilBt) else p[Cles.rtkAppareilBt] = it }
            r.rtk.appareilBtNom.let { if (it == null) p.remove(Cles.rtkAppareilBtNom) else p[Cles.rtkAppareilBtNom] = it }
            p[Cles.rtkHoteTcp] = r.rtk.hoteTcp
            p[Cles.rtkPortTcp] = r.rtk.portTcp
            p[Cles.rtkPontNtrip] = r.rtk.pontNtrip
            p[Cles.rtkCasterHote] = r.rtk.casterHote
            p[Cles.rtkCasterPort] = r.rtk.casterPort
            p[Cles.rtkMountpoint] = r.rtk.mountpoint
            p[Cles.rtkUtilisateur] = r.rtk.utilisateur
            p[Cles.rtkMotDePasse] = r.rtk.motDePasse
        }
    }

    /** Mémorise le dernier contexte ouvert (sans toucher aux autres réglages). */
    suspend fun enregistrerDernierContexte(id: String) {
        ds.edit { it[Cles.dernierContexte] = id }
    }
}
