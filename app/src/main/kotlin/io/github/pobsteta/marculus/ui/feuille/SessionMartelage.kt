package io.github.pobsteta.marculus.ui.feuille

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.AnnonceHauteur
import fr.marculus.core.AttributionSpatiale
import fr.marculus.core.EstimationHauteur
import fr.marculus.core.Houppier
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.ConfigCompteur
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.FixGnss
import fr.marculus.core.model.OrigineFix
import fr.marculus.core.model.Position
import fr.marculus.core.model.QualiteFix
import fr.marculus.core.model.Reglages
import io.github.pobsteta.marculus.Appareil
import io.github.pobsteta.marculus.Langue
import io.github.pobsteta.marculus.R
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.data.ParcelleGpkg
import io.github.pobsteta.marculus.gnss.ServiceGnssRtk
import io.github.pobsteta.marculus.ui.ToucheVolume
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.launch

/** Convertit une position Android (GNSS interne) en FixGnss : qualité dérivée de la précision. */
private fun fixDepuisLocation(loc: Location): FixGnss {
    val precision = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null
    return FixGnss(
        position = Position(loc.latitude, loc.longitude),
        qualite = QualiteFix.depuisPrecision(precision),
        nbSatellites = 0,
        hdop = null,
        altitudeM = if (loc.hasAltitude()) loc.altitude else null,
        ageCorrectionsS = null,
        precisionHorizontaleM = precision,
        origine = OrigineFix.INTERNE,
    )
}

/**
 * Applique la voix TTS choisie ([voixNom]) si elle existe dans le moteur, sinon revient à la langue
 * [localeDefaut]. Le repli évite de rester bloqué sur une voix précédemment sélectionnée quand le
 * réglage est repassé sur « voix par défaut ».
 */
private fun appliquerVoix(tts: TextToSpeech, voixNom: String?, localeDefaut: Locale) {
    val voix = voixNom?.let { nom -> tts.voices?.firstOrNull { it.name == nom } }
    if (voix != null) tts.voice = voix else tts.language = localeDefaut
}

/** Fix GNSS interne courant du téléphone (null si inactif ou non autorisé). */
@Composable
private fun positionActuelle(active: Boolean): FixGnss? {
    val context = LocalContext.current
    val etat = remember { mutableStateOf<FixGnss?>(null) }
    DisposableEffect(active) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val autorise = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        var listener: LocationListener? = null
        if (active && lm != null && autorise) {
            val l = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    etat.value = fixDepuisLocation(location)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                // Localisation coupée : la dernière position deviendrait celle de toutes les
                // tiges suivantes. Mieux vaut une tige sans position qu'une position périmée.
                override fun onProviderDisabled(provider: String) { etat.value = null }
            }
            listener = l
            runCatching {
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { etat.value = fixDepuisLocation(it) }
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 1f, l)
            }
        } else {
            etat.value = null
        }
        onDispose { listener?.let { l -> runCatching { lm?.removeUpdates(l) } } }
    }
    return etat.value
}

/**
 * Localisation du téléphone activée ? Suivi en direct (réglages rapides), y compris en mode
 * ponctuel où aucune écoute GNSS ne tourne entre deux tiges.
 */
@Composable
private fun localisationActivee(): Boolean {
    val context = LocalContext.current
    val lm = remember { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }
    fun lire() = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
    var activee by remember { mutableStateOf(lire()) }
    DisposableEffect(lm) {
        val recepteur = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) { activee = lire() }
        }
        ContextCompat.registerReceiver(
            context,
            recepteur,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        activee = lire()
        onDispose { runCatching { context.unregisterReceiver(recepteur) } }
    }
    return activee
}

/** Capture un fix GNSS interne unique (one-shot) — acquisition ponctuelle au clic. */
private fun capturerPositionPonctuelle(context: Context, onResult: (FixGnss?) -> Unit) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    val autorise = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (lm == null || !autorise) {
        onResult(null)
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching {
            lm.getCurrentLocation(LocationManager.GPS_PROVIDER, null, context.mainExecutor) { loc ->
                onResult(loc?.let(::fixDepuisLocation))
            }
        }.onFailure { onResult(null) }
    } else {
        val loc = runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
        onResult(loc?.let(::fixDepuisLocation))
    }
}

private fun vibrer(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
}

/** Vibration de rejet : deux impulsions, volontairement distinctes du tick de comptage. */
private fun vibrerDouble(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 60, 90, 60), -1))
}

/**
 * Hauteur telle qu'elle s'annonce : « hauteur 12, découpe 6 A, 3 A B ». La découpe est dite
 * **dans tous les cas** — dictée dans le même énoncé que la tige, ajoutée après coup, ou saisie.
 */
private fun annonceHauteur(context: Context, texte: String): String {
    val totale = context.getString(R.string.voix_hauteur_annonce, AnnonceHauteur.totale(texte))
    val decoupe = AnnonceHauteur.decoupe(texte) ?: return totale
    return totale + ", " + context.getString(R.string.voix_decoupe_annonce, decoupe)
}

/**
 * Hauteur estimée depuis les houppiers du MNH, ou `null` : réglage décoché, position absente,
 * couche `houppier` absente, ou position dans aucun houppier (trouée, bord, tige dominée).
 * Elle ne complète **que** les tiges sans hauteur mesurée — dictée ou saisie priment toujours.
 */
private fun hauteurEstimee(actif: Boolean, houppiers: List<Houppier>, p: Position?): String? =
    if (actif && p != null) EstimationHauteur.texte(houppiers, p) else null

/** Dernière tige saisie (la seule annotable par H/Q, par la voix ou les boutons de volume). */
data class DerniereSaisie(val uuid: String, val essence: String, val classe: Int)

/**
 * Ce qu'un écran de martelage (feuille, carte) offre pour compter des tiges : les totaux, le fix
 * retenu, la dernière tige et les gestes de comptage. Une tige comptée depuis la carte est la même
 * que depuis la feuille — mêmes UUID, rattachement GNSS, estimation MNH, annonces et journal.
 */
class SessionMartelage internal constructor(
    val dictee: EtatDictee,
    val totaux: Map<CompteurCle, Int>,
    val configs: Map<CompteurCle, ConfigCompteur>,
    /** Fix retenu pour figer la tige : null si la capture est coupée. */
    val fixTige: FixGnss?,
    val rtkActif: Boolean,
    /** Capture sur le GNSS du téléphone alors que sa localisation est coupée : aucune position. */
    val gnssCoupe: Boolean,
    /** Sans fix, les tiges prennent la position pointée (centre de la carte), en « Manuel ». */
    val pointage: Boolean,
    val derniereSaisie: DerniereSaisie?,
    /** Compte une tige : retour sensoriel, annonce, journal, GNSS. Sans effet tant que le contexte charge. */
    val ajouter: (essence: String, classe: Int) -> Unit,
    /** Retire une tige par événement d'annulation, jamais sous zéro. */
    val retirer: (essence: String, classe: Int) -> Unit,
    /** Saisie hors grille (dialogue de saisie libre) : même journal, sans annonce. */
    val saisirLibre: (
        action: ActionTige,
        essence: String,
        classe: Int,
        quantite: Int,
        hauteur: String?,
        qualite: String?,
    ) -> Unit,
)

/**
 * Mécanique de comptage partagée par les écrans de martelage : synthèse vocale, capture GNSS,
 * rattachement à la parcelle, dictée vocale et son relais sur l'appui long du volume bas.
 *
 * @param parcelles parcelles du GeoPackage, pour figer le rattachement spatial dans la tige
 * @param houppiers houppiers du MNH, lus seulement si l'estimation de hauteur est réglée
 * @param positionPointee position de repli quand la capture est réglée mais qu'aucun fix n'est
 *   disponible (GNSS coupé, en recherche, récepteur absent) : le centre de la carte. La tige est
 *   alors marquée [QualiteFix.MANUEL], jamais confondue avec une mesure GNSS.
 */
@Composable
fun sessionMartelage(
    repository: MartelageRepository,
    contexteId: String,
    contexte: Contexte?,
    reglages: Reglages,
    qualitesArbre: List<String>,
    qualitesBois: List<String>,
    parcelles: List<ParcelleGpkg>,
    houppiers: List<Houppier>,
    positionPointee: (() -> Position?)? = null,
): SessionMartelage {
    val scope = rememberCoroutineScope()
    val androidContext = LocalContext.current
    // Opérateur : nom saisi, sinon identité d'appareil (UUID) garantissant l'unicité.
    val operateurEffectif = reglages.operateur?.takeIf { it.isNotBlank() } ?: Appareil.id(androidContext)
    val toneGen = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 90) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { toneGen?.release() } }
    // Langue par défaut des annonces vocales = langue de l'application (fr/en ; « système » →
    // locale de l'appareil, repli fr). Une voix précise choisie dans les Paramètres prime dessus.
    val localeAnnonce = remember {
        val code = when (Langue.code(androidContext)) {
            "fr" -> "fr"
            "en" -> "en"
            else -> Locale.getDefault().language.takeIf { it == "fr" || it == "en" } ?: "fr"
        }
        Locale(code)
    }
    // Synthèse vocale (annonce du nombre / de l'étiquette), dans la langue de l'app.
    val tts = remember {
        lateinit var moteur: TextToSpeech
        moteur = TextToSpeech(androidContext.applicationContext) { statut ->
            if (statut == TextToSpeech.SUCCESS) moteur.language = localeAnnonce
        }
        moteur
    }
    DisposableEffect(Unit) { onDispose { tts.stop(); tts.shutdown() } }
    fun retourSensoriel() {
        if (reglages.vibration) vibrer(androidContext)
        if (reglages.sonClic) toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
    }
    // Nombre d'annonces encore en file : le micro ne se rouvre qu'une fois la dernière terminée.
    val annoncesEnCours = remember { AtomicInteger(0) }
    fun dire(texte: String, identifiant: String, remplacer: Boolean) {
        appliquerVoix(tts, reglages.voixTts, localeAnnonce)
        annoncesEnCours.incrementAndGet()
        val mode = if (remplacer) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        // Moteur TTS indisponible : sans callback de fin, le micro resterait fermé pour toujours.
        if (tts.speak(texte, mode, null, identifiant) != TextToSpeech.SUCCESS) {
            annoncesEnCours.decrementAndGet()
        }
    }
    /**
     * Annonce du comptage. [forcer] impose l'étiquette même si le réglage est décoché : une tige
     * dictée doit toujours être confirmée à l'oreille, c'est ce qui ferme la boucle sans écran.
     */
    fun annoncer(
        essence: String,
        classe: Int,
        total: Int,
        forcer: Boolean = false,
        qualite: String? = null,
        hauteur: String? = null,
    ) {
        val parties = buildList {
            if (reglages.annonceEtiquette || forcer) {
                add(
                    "$essence $classe" + (qualite?.let { " $it" } ?: "") +
                        // La découpe fait partie de ce qui a été dit : elle se relit aussi, sans
                        // quoi rien ne permet de vérifier à l'oreille ce que le décodeur a compris.
                        (hauteur?.let { " " + annonceHauteur(androidContext, it) } ?: ""),
                )
            }
            if (reglages.annonceNombre) add(total.toString())
        }
        if (parties.isNotEmpty()) dire(parties.joinToString(", "), "tige", remplacer = true)
    }
    // Annonce vocale d'avis : limite inférieure non atteinte / limite supérieure dépassée.
    fun annoncerAvis(cfg: ConfigCompteur, total: Int) {
        val messages = buildList {
            if (reglages.annonceAvisPlus && cfg.alertePlus(total)) add(androidContext.getString(R.string.avis_annonce_plus))
            if (reglages.annonceAvisMoins && cfg.alerteMoins(total)) add(androidContext.getString(R.string.avis_annonce_moins))
        }
        messages.forEach { dire(it, "avis", remplacer = false) }
    }
    val totaux by repository.totaux(contexteId).collectAsStateWithLifecycle(emptyMap())
    val configs by repository.configs(contexteId).collectAsStateWithLifecycle(emptyMap())
    // Demande la permission de localisation à l'exécution dès que la capture GNSS est activée.
    val permLocalisation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    // Permission de localisation : seulement pour le GNSS INTERNE. Le récepteur RTK externe
    // (Bluetooth/réseau, service « connectedDevice ») n'en a pas besoin.
    LaunchedEffect(reglages.capturePosition) {
        if (reglages.capturePosition && !reglages.rtk.actif &&
            ContextCompat.checkSelfPermission(androidContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            permLocalisation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    // Écoute continue seulement si la capture est active ET pas en mode ponctuel.
    val position = positionActuelle(reglages.capturePosition && !reglages.gnssPonctuel)
    // Source RTK externe (service de premier plan) : prioritaire quand elle est activée.
    val fixRtk by ServiceGnssRtk.fixCourant.collectAsStateWithLifecycle()
    val rtkActif = reglages.rtk.actif
    LaunchedEffect(rtkActif) {
        if (rtkActif) ServiceGnssRtk.demarrerDepuis(androidContext, reglages.rtk)
    }
    // Fix retenu pour figer la tige (position + qualité + précision). « Enregistrer la position
    // GNSS » est le maître-interrupteur : décoché → AUCUN fix, même avec un RTK connecté.
    // Coché → RTK si actif, sinon GNSS interne. La qualité est enregistrée dans les deux cas.
    val gnssCoupe = reglages.capturePosition && !rtkActif && !localisationActivee()
    val fixTige = when {
        !reglages.capturePosition -> null
        gnssCoupe -> null
        rtkActif -> fixRtk
        else -> position
    }
    // Sans fix, la position pointée (centre de la carte) tient lieu de position, en « Manuel ».
    // Sauf en ponctuel sur le GNSS du téléphone allumé : le fix n'y existe qu'à la tige, et c'est
    // lui qui la placera.
    val ponctuelInterne = !rtkActif && reglages.gnssPonctuel && !gnssCoupe
    val pointage = reglages.capturePosition && fixTige == null && positionPointee != null && !ponctuelInterne
    fun fixEffectif(): FixGnss? = fixTige ?: positionPointee?.takeIf { pointage }?.invoke()?.let {
        FixGnss(
            position = it,
            qualite = QualiteFix.MANUEL,
            nbSatellites = 0,
            hdop = null,
            altitudeM = null,
            ageCorrectionsS = null,
            precisionHorizontaleM = null,
        )
    }
    val estimerMnh = reglages.estimerHauteurMnh
    fun parcelleDe(p: Position?): String? =
        p?.let { pos -> parcelles.firstOrNull { AttributionSpatiale.contient(it.anneaux, pos) }?.label }

    var derniereSaisie by remember { mutableStateOf<DerniereSaisie?>(null) }
    // Parcelle rattachée à la dernière tige : son changement remet le mode rafale à zéro.
    var parcelleCourante by remember { mutableStateOf<String?>(null) }
    var parcelleConnue by remember { mutableStateOf(false) }

    // Dictée vocale : les actions réelles sont posées plus bas, une fois les gestes définis.
    val actionsVocales = remember { ActionsVocales() }
    val dictee = rememberDicteeVocale(
        actif = reglages.pttEcran || reglages.pttVolumeLong,
        essences = contexte?.essencesNoms ?: emptyList(),
        classes = contexte?.axe?.classes() ?: emptyList(),
        qualites = qualitesArbre,
        qualitesBois = qualitesBois,
        tonalites = toneGen,
        onTige = { essence, classe, qualite, hauteur ->
            actionsVocales.tige(essence, classe, qualite, hauteur)
        },
        onHauteur = { texte -> actionsVocales.hauteur(texte) },
        onDecoupe = { segments -> actionsVocales.decoupe(segments) },
        onQualite = { code -> actionsVocales.qualite(code) },
        onAnnule = { actionsVocales.annule() },
        onRepete = { actionsVocales.repete() },
        onRejet = { actionsVocales.rejet() },
    )
    // Anti-larsen : le micro ne se rouvre qu'une fois la DERNIÈRE annonce de la file terminée.
    DisposableEffect(tts, dictee) {
        val principal = Handler(Looper.getMainLooper())
        fun annonceFinie() {
            if (annoncesEnCours.decrementAndGet() <= 0) {
                annoncesEnCours.set(0)
                principal.post { dictee.annonceTerminee() }
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = annonceFinie()
            override fun onStop(utteranceId: String?, interrupted: Boolean) = annonceFinie()

            @Deprecated("Signature héritée, remplacée par onError(String, Int)")
            override fun onError(utteranceId: String?) = annonceFinie()
        })
        onDispose { tts.setOnUtteranceProgressListener(null) }
    }
    // Appui long sur le volume bas : le relais n'est posé que si la dictée est réellement prête,
    // sinon le volume bas resterait consommé pour rien.
    DisposableEffect(reglages.pttVolumeLong, dictee.microPret) {
        ToucheVolume.onPtt = if (reglages.pttVolumeLong && dictee.microPret) {
            { tenu -> if (tenu) dictee.demarrer() else dictee.arreter() }
        } else {
            null
        }
        onDispose { ToucheVolume.onPtt = null }
    }

    /** Écrit une tige PLUS au journal, puis complète sa position en GNSS ponctuel. */
    fun inserer(
        essence: String,
        classe: Int,
        quantite: Int,
        hauteurTexte: String?,
        qualite: String?,
        onInseree: (uuid: String) -> Unit = {},
    ) {
        val fix = fixEffectif()
        val pos = fix?.position
        scope.launch {
            val uuid = repository.ajouterTige(
                contexteId, essence, classe, quantite = quantite,
                // Hauteur dictée ou saisie : elle prime sur toute estimation (MNH), qui ne doit
                // compléter que les tiges sans hauteur mesurée.
                hauteurTexte = hauteurTexte ?: hauteurEstimee(estimerMnh, houppiers, pos),
                qualiteArbre = qualite,
                position = pos, operateur = operateurEffectif, parcelle = parcelleDe(pos),
                qualiteFix = fix?.qualite, precisionM = fix?.precisionHorizontaleM,
            )
            onInseree(uuid)
            if (!rtkActif && reglages.capturePosition && reglages.gnssPonctuel) {
                capturerPositionPonctuelle(androidContext) { fixP ->
                    if (fixP != null) {
                        scope.launch {
                            repository.annoterPosition(
                                uuid, fixP.position, parcelleDe(fixP.position), fixP.qualite, fixP.precisionHorizontaleM,
                            )
                            // En GNSS ponctuel la position n'existe qu'ici : c'est le seul
                            // moment où l'estimation MNH est possible pour cette tige.
                            if (hauteurTexte == null) {
                                hauteurEstimee(estimerMnh, houppiers, fixP.position)
                                    ?.let { repository.annoterHauteur(uuid, it) }
                            }
                        }
                    }
                }
            }
        }
    }

    // Actions de comptage partagées (cellules, boutons de volume, dictée vocale).
    // Une tige dictée est une tige normale : mêmes UUID, rattachement GNSS et journal.
    fun ajouter(
        essence: String,
        classe: Int,
        qualite: String? = null,
        hauteurTexte: String? = null,
        annonceForcee: Boolean = false,
    ) {
        val ctx = contexte ?: return
        retourSensoriel()
        val cle = CompteurCle(essence, classe)
        val nouveauTotal = (totaux[cle] ?: 0) + ctx.increment
        annoncer(
            essence,
            classe,
            nouveauTotal,
            forcer = annonceForcee,
            qualite = qualite,
            hauteur = hauteurTexte,
        )
        configs[cle]?.let { annoncerAvis(it, nouveauTotal) }
        val parcelleLabel = parcelleDe(fixEffectif()?.position)
        // Changement de parcelle rattachée : le mode rafale repart de l'essence dictée.
        if (parcelleConnue && parcelleLabel != parcelleCourante) dictee.reinitialiserRafale()
        parcelleCourante = parcelleLabel
        parcelleConnue = true
        inserer(essence, classe, ctx.increment, hauteurTexte, qualite) { uuid ->
            derniereSaisie = DerniereSaisie(uuid, essence, classe)
        }
    }
    fun retirer(essence: String, classe: Int) {
        val ctx = contexte ?: return
        val total = totaux[CompteurCle(essence, classe)] ?: 0
        if (total > 0) {
            retourSensoriel()
            val q = minOf(ctx.increment, total) // jamais en dessous de zéro
            annoncer(essence, classe, total - q)
            configs[CompteurCle(essence, classe)]?.let { annoncerAvis(it, total - q) }
            scope.launch { repository.annulerTige(contexteId, essence, classe, quantite = q) }
            derniereSaisie = null // un − ferme la saisie en cours
        }
    }

    // Actions déclenchées par la dictée : insertion, annulation, répétition, rejet.
    val messageNonCompris = androidContext.getString(R.string.voix_non_compris)
    val messageAnnule = androidContext.getString(R.string.voix_annule)
    val messageRienAAnnuler = androidContext.getString(R.string.voix_rien_a_annuler)
    SideEffect {
        actionsVocales.tige = { essence, classe, qualite, hauteur ->
            ajouter(
                essence,
                classe,
                qualite = qualite,
                hauteurTexte = hauteur,
                annonceForcee = true,
            )
        }
        // Hauteur dictée : elle annote la dernière tige, exactement comme le bouton H.
        actionsVocales.hauteur = { texte ->
            val cible = derniereSaisie
            if (cible == null) {
                dire(messageRienAAnnuler, "vocal", remplacer = true)
            } else {
                scope.launch { repository.annoterHauteur(cible.uuid, texte) }
                dire(annonceHauteur(androidContext, texte), "vocal", remplacer = true)
            }
        }
        // Découpe dictée seule : elle se greffe sur la hauteur que la tige porte déjà —
        // estimée par le MNH, dictée ou saisie. Sans hauteur, une longueur de billon ne veut
        // rien dire : on refuse plutôt que d'inventer une hauteur totale.
        actionsVocales.decoupe = { segments ->
            val cible = derniereSaisie
            if (cible == null) {
                dire(messageRienAAnnuler, "vocal", remplacer = true)
            } else {
                scope.launch {
                    val actuelle = repository.tige(cible.uuid)?.hauteurTexte?.substringBefore('-')?.trim()
                    if (actuelle.isNullOrEmpty()) {
                        dire(androidContext.getString(R.string.voix_sans_hauteur), "vocal", remplacer = true)
                    } else {
                        // La découpe dite remplace la précédente : redire, c'est corriger.
                        repository.annoterHauteur(cible.uuid, "$actuelle-$segments")
                        dire(
                            androidContext.getString(
                                R.string.voix_decoupe_annonce,
                                AnnonceHauteur.decoupe("0-$segments") ?: segments,
                            ),
                            "vocal",
                            remplacer = true,
                        )
                    }
                }
            }
        }
        // Qualité dictée seule : elle annote la dernière tige, comme le bouton Q.
        actionsVocales.qualite = { code ->
            val cible = derniereSaisie
            if (cible == null) {
                dire(messageRienAAnnuler, "vocal", remplacer = true)
            } else {
                scope.launch { repository.annoterQualite(cible.uuid, code) }
                dire(code, "vocal", remplacer = true)
            }
        }
        actionsVocales.annule = {
            val cible = derniereSaisie
            if (cible == null) {
                dire(messageRienAAnnuler, "vocal", remplacer = true)
            } else {
                retirer(cible.essence, cible.classe) // annulation par événement, jamais d'effacement
                dire(messageAnnule, "vocal", remplacer = false)
            }
        }
        actionsVocales.repete = {
            val cible = derniereSaisie
            if (cible == null) {
                dire(messageRienAAnnuler, "vocal", remplacer = true)
            } else {
                annoncer(
                    cible.essence,
                    cible.classe,
                    totaux[CompteurCle(cible.essence, cible.classe)] ?: 0,
                    forcer = true,
                )
            }
        }
        actionsVocales.rejet = {
            // Rejet ≠ silence : motif de vibration distinct du tick, et on ne devine jamais.
            vibrerDouble(androidContext)
            dire(messageNonCompris, "vocal", remplacer = true)
        }
    }

    return SessionMartelage(
        dictee = dictee,
        totaux = totaux,
        configs = configs,
        fixTige = fixTige,
        gnssCoupe = gnssCoupe,
        pointage = pointage,
        rtkActif = rtkActif,
        derniereSaisie = derniereSaisie,
        ajouter = { essence, classe -> ajouter(essence, classe) },
        retirer = { essence, classe -> retirer(essence, classe) },
        saisirLibre = { action, essence, classe, quantite, hauteur, qualite ->
            if (action == ActionTige.PLUS) {
                inserer(essence, classe, quantite, hauteur, qualite)
            } else {
                scope.launch {
                    repository.annulerTige(contexteId, essence, classe, quantite = quantite, operateur = operateurEffectif)
                }
            }
        },
    )
}
