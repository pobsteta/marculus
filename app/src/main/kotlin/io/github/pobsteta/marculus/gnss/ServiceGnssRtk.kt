package io.github.pobsteta.marculus.gnss

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import fr.marculus.core.model.ConfigRtk
import fr.marculus.core.model.FixGnss
import fr.marculus.core.model.TransportRtk
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** État détaillé du lien RTK, pour suivi et diagnostic dans l'UI. */
data class EtatRtk(
    val connecte: Boolean = false,
    val octetsRecus: Long = 0,
    val tramesRecues: Long = 0,
    val derniereTrame: String? = null,
    val rtcmEnvoye: Long = 0,
    val fix: FixGnss? = null,
    val erreur: String? = null,
)

/**
 * Service de **premier plan** maintenant la connexion GNSS/RTK active hors écran et au-delà des
 * rotations. Héberge le [PontRtk] fourni via [demarrer] et publie le dernier [FixGnss] dans
 * [fixCourant], que l'UI observe. Type `location` (notification persistante obligatoire).
 *
 * Le cycle de vie est piloté depuis l'UI (réglages → démarrer/arrêter) ; le câblage du choix de
 * transport/caster reste à brancher en G3.
 */
class ServiceGnssRtk : Service() {

    // Filet de sécurité : une exception non rattrapée (échec BT/TCP/NTRIP) ne doit pas crasher l'app.
    private val gestionnaireErreurs = CoroutineExceptionHandler { _, _ -> _fixCourant.value = null }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + gestionnaireErreurs)
    private var collecte: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val canal = NotificationChannel(CANAL, "GNSS RTK", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ARRET) {
            arreterInterne()
            return START_NOT_STICKY
        }
        // Si le passage en premier plan échoue (ex. permission localisation absente sur Android 14+),
        // on arrête proprement au lieu de laisser l'OS tuer le processus.
        if (!majPremierPlan(null)) {
            arreterInterne()
            return START_NOT_STICKY
        }
        val pont = pontCourant ?: run {
            arreterInterne()
            return START_NOT_STICKY
        }
        _etat.value = EtatRtk()
        collecte?.cancel()
        collecte = scope.launch {
            // runCatching : un échec de connexion (BT/TCP/NTRIP) termine le flux sans crasher.
            runCatching {
                pont.evenements().collect { ev ->
                    when (ev) {
                        is EvenementRtk.Octets ->
                            _etat.update { it.copy(connecte = true, octetsRecus = it.octetsRecus + ev.n) }
                        is EvenementRtk.Trame ->
                            _etat.update { it.copy(tramesRecues = it.tramesRecues + 1, derniereTrame = ev.ligne) }
                        is EvenementRtk.Fix -> {
                            _fixCourant.value = ev.fix
                            _etat.update { it.copy(fix = ev.fix) }
                            majPremierPlan(ev.fix)
                        }
                        is EvenementRtk.Rtcm ->
                            _etat.update { it.copy(rtcmEnvoye = it.rtcmEnvoye + ev.n) }
                    }
                }
            }.onFailure { e -> _etat.update { it.copy(erreur = e.message ?: "erreur de connexion") } }
            _fixCourant.value = null
            stopSelf()
        }
        return START_STICKY
    }

    /** Met à jour la notification de premier plan. Renvoie false si `startForeground` échoue. */
    private fun majPremierPlan(fix: FixGnss?): Boolean {
        val texte = fix?.let { "${it.qualite.libelle}${it.precisionHorizontaleM?.let { p -> " · ${"%.2f".format(p)} m" } ?: ""}" }
            ?: "Connexion au récepteur…"
        val notif: Notification = NotificationCompat.Builder(this, CANAL)
            .setContentTitle("Marculus — GNSS RTK")
            .setContentText(texte)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(ID_NOTIF, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            } else {
                startForeground(ID_NOTIF, notif)
            }
        }.isSuccess
    }

    private fun arreterInterne() {
        collecte?.cancel()
        _fixCourant.value = null
        _etat.value = EtatRtk()
        pontCourant = null
        ServiceCompat_stopForeground(this)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CANAL = "gnss_rtk"
        private const val ID_NOTIF = 1001
        private const val ACTION_ARRET = "io.github.pobsteta.marculus.gnss.ARRET"

        /** Pont à exécuter, posé par l'appelant juste avant [demarrer]. */
        @Volatile
        var pontCourant: PontRtk? = null
            private set

        private val _fixCourant = MutableStateFlow<FixGnss?>(null)

        /** Dernier fix publié par le service (null si arrêté ou pas encore de fix). */
        val fixCourant: StateFlow<FixGnss?> = _fixCourant.asStateFlow()

        private val _etat = MutableStateFlow(EtatRtk())

        /** État détaillé du lien (connexion, octets/trames reçus, fix, erreur) pour le diagnostic. */
        val etat: StateFlow<EtatRtk> = _etat.asStateFlow()

        /** Démarre le service de premier plan avec le pont donné. */
        fun demarrer(contexte: Context, pont: PontRtk) {
            pontCourant = pont
            ContextCompat.startForegroundService(contexte, Intent(contexte, ServiceGnssRtk::class.java))
        }

        /**
         * Construit le pont depuis une [ConfigRtk] (transport BT/TCP + caster éventuel) et démarre
         * le service. Renvoie false si la configuration ne permet pas d'ouvrir un transport.
         */
        @SuppressLint("MissingPermission")
        fun demarrerDepuis(contexte: Context, rtk: ConfigRtk): Boolean {
            val transport: Transport = when (rtk.transport) {
                TransportRtk.BLUETOOTH -> {
                    val adresse = rtk.appareilBt ?: return false
                    val adaptateur = contexte.getSystemService(BluetoothManager::class.java)?.adapter
                    val appareil = runCatching { adaptateur?.getRemoteDevice(adresse) }.getOrNull() ?: return false
                    TransportBluetoothSpp(appareil)
                }
                TransportRtk.TCP -> {
                    if (rtk.hoteTcp.isBlank()) return false
                    TransportTcp(rtk.hoteTcp, rtk.portTcp)
                }
            }
            val ntrip = if (rtk.pontNtrip) {
                ClientNtrip(rtk.casterHote, rtk.casterPort, rtk.mountpoint, rtk.utilisateur, rtk.motDePasse)
            } else {
                null
            }
            demarrer(contexte, PontRtk(transport, ntrip))
            return true
        }

        /** Demande l'arrêt du service. */
        fun arreter(contexte: Context) {
            contexte.startService(
                Intent(contexte, ServiceGnssRtk::class.java).setAction(ACTION_ARRET),
            )
        }
    }
}

/** Arrêt du premier plan compatible toutes versions (retire la notification). */
private fun ServiceCompat_stopForeground(service: Service) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
    } else {
        @Suppress("DEPRECATION")
        service.stopForeground(true)
    }
}
