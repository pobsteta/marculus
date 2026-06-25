package io.github.pobsteta.marculus.gnss

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import fr.marculus.core.model.FixGnss
import fr.marculus.core.model.OrigineFix
import fr.marculus.core.model.Position
import fr.marculus.core.model.QualiteFix
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Source de position **interne** (GNSS du téléphone via `LocationManager`, sans Play Services).
 * L'API ne fournit pas la qualité de fix : on l'**approche** depuis la précision horizontale
 * (`Location.accuracy`). Mode par défaut quand aucun récepteur externe n'est connecté.
 */
class SourcePositionInterne(private val context: Context) : SourcePosition {

    @SuppressLint("MissingPermission")
    override fun fixs(): Flow<FixGnss?> = callbackFlow {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val autorise = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (lm == null || !autorise) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) { trySend(fixDepuis(location)) }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        runCatching {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { trySend(fixDepuis(it)) }
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener)
        }
        awaitClose { runCatching { lm.removeUpdates(listener) } }
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
            capDeg = if (loc.hasBearing()) loc.bearing.toDouble() else null,
            vitesseMs = if (loc.hasSpeed()) loc.speed.toDouble() else null,
            origine = OrigineFix.INTERNE,
        )
    }
}
