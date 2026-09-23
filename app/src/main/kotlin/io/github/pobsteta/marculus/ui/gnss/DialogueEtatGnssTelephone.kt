package io.github.pobsteta.marculus.ui.gnss

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import fr.marculus.core.model.FixGnss
import fr.marculus.core.model.OrigineFix
import fr.marculus.core.model.Position
import fr.marculus.core.model.QualiteFix
import fr.marculus.core.model.SatelliteGsv
import io.github.pobsteta.marculus.R
import kotlinx.coroutines.delay

/** Au-delà de cet âge (s), une position n'est plus « en cours » : c'est la dernière connue. */
private const val AGE_FIX_RECENT_S = 10L

/** Identifiant de constellation façon NMEA (talker ID), pour que le skyplot les traite pareil. */
private fun systeme(constellation: Int): String = when (constellation) {
    GnssStatus.CONSTELLATION_GPS -> "GP"
    GnssStatus.CONSTELLATION_GLONASS -> "GL"
    GnssStatus.CONSTELLATION_GALILEO -> "GA"
    GnssStatus.CONSTELLATION_BEIDOU -> "GB"
    GnssStatus.CONSTELLATION_QZSS -> "GQ"
    GnssStatus.CONSTELLATION_SBAS -> "SB"
    else -> "GN"
}

private fun satellites(status: GnssStatus): List<SatelliteGsv> = (0 until status.satelliteCount).map { i ->
    SatelliteGsv(
        prn = status.getSvid(i),
        elevation = status.getElevationDegrees(i).toInt(),
        azimut = status.getAzimuthDegrees(i).toInt(),
        snr = status.getCn0DbHz(i).toInt().takeIf { it > 0 },
        systeme = systeme(status.getConstellationType(i)),
    )
}

private fun fixDepuis(loc: Location): FixGnss {
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
 * État GNSS quand la position vient **du téléphone**. Le panneau du récepteur externe n'a rien à
 * dire tant qu'aucun fix n'arrive (« aucune donnée ») ; ici on sait pourquoi : autorisation
 * refusée, localisation coupée, ou recherche en cours — avec les satellites que la puce voit déjà.
 *
 * Le panneau **allume lui-même le GNSS** tant qu'il est ouvert : en mode ponctuel, la puce ne
 * tourne qu'au moment de la tige, et il n'y aurait sinon rien à montrer.
 */
@SuppressLint("MissingPermission")
@Composable
fun DialogueEtatGnssTelephone(ponctuel: Boolean, onFermer: () -> Unit) {
    val context = LocalContext.current
    val lm = remember { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }
    val autorise = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    var active by remember { mutableStateOf(lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) }
    var location by remember { mutableStateOf<Location?>(null) }
    var sats by remember { mutableStateOf<List<SatelliteGsv>>(emptyList()) }
    var utilises by remember { mutableStateOf(0) }
    var premierFixS by remember { mutableStateOf<Int?>(null) }
    val debut = remember { SystemClock.elapsedRealtime() }
    var maintenant by remember { mutableLongStateOf(debut) }
    LaunchedEffect(Unit) {
        while (true) {
            maintenant = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }

    DisposableEffect(lm, autorise) {
        if (lm == null || !autorise) return@DisposableEffect onDispose { }
        val principal = Handler(Looper.getMainLooper())
        val ecoute = object : LocationListener {
            override fun onLocationChanged(loc: Location) { location = loc }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) { active = true }
            override fun onProviderDisabled(provider: String) { active = false }
        }
        val statut = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                sats = satellites(status)
                utilises = (0 until status.satelliteCount).count { status.usedInFix(it) }
            }
            override fun onFirstFix(ttffMillis: Int) { premierFixS = ttffMillis / 1000 }
        }
        runCatching {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { location = it }
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, ecoute, Looper.getMainLooper())
            lm.registerGnssStatusCallback(statut, principal)
        }
        onDispose {
            runCatching { lm.removeUpdates(ecoute) }
            runCatching { lm.unregisterGnssStatusCallback(statut) }
        }
    }

    // Âge de la position : une « dernière position connue » peut dater d'hier, à l'autre bout du massif.
    val ageS = location?.let { (maintenant - it.elapsedRealtimeNanos / 1_000_000) / 1000 }?.coerceAtLeast(0)
    val rechercheS = (maintenant - debut) / 1000
    val diagnostic = when {
        !autorise -> stringResource(R.string.etat_tel_non_autorise)
        !active -> stringResource(R.string.etat_tel_desactive)
        ageS != null && ageS <= AGE_FIX_RECENT_S ->
            premierFixS?.let { stringResource(R.string.etat_tel_fix_ttff, it) } ?: stringResource(R.string.etat_tel_fix)
        sats.isEmpty() -> stringResource(R.string.etat_tel_recherche_aucun, rechercheS)
        else -> stringResource(R.string.etat_tel_recherche, rechercheS, sats.size, utilises)
    }

    AlertDialog(
        onDismissRequest = onFermer,
        confirmButton = { TextButton(onClick = onFermer) { Text("OK") } },
        title = { Text(stringResource(R.string.etat_gnss_titre)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Ligne(stringResource(R.string.etat_source), stringResource(R.string.etat_source_telephone))
                Ligne(
                    stringResource(R.string.etat_mode),
                    stringResource(if (ponctuel) R.string.etat_mode_ponctuel else R.string.etat_mode_continu),
                )
                Text(
                    diagnostic,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                if (ponctuel) {
                    Text(
                        stringResource(R.string.etat_tel_ponctuel_note),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Ligne(stringResource(R.string.etat_satellites_vus), sats.size.takeIf { it > 0 }?.toString())
                Ligne(stringResource(R.string.etat_satellites_utilises), utilises.takeIf { it > 0 }?.toString())
                location?.let { loc ->
                    Ligne(stringResource(R.string.etat_age_position), ageS?.let { "$it s" })
                    DetailsFix(fixDepuis(loc))
                }
                if (sats.isNotEmpty()) {
                    Text(
                        stringResource(R.string.etat_skyplot),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    Skyplot(sats, Modifier.fillMaxWidth().heightIn(max = 240.dp))
                    BarresSnr(sats, Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        },
    )
}
