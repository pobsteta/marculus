package io.github.pobsteta.marculus.ui.gnss

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.marculus.core.model.FixGnss
import fr.marculus.core.model.OrigineFix
import io.github.pobsteta.marculus.R
import java.util.Locale

/**
 * Panneau « État GNSS » (inspiré de l'écran GNSS Status de SW Maps) : détail du fix courant —
 * type, précision, position, satellites, dilutions, âge des corrections, station de référence.
 * Sert à vérifier d'un coup d'œil que le RTK / NTRIP fonctionne.
 */
@Composable
fun DialogueEtatGnss(fix: FixGnss?, onFermer: () -> Unit) {
    AlertDialog(
        onDismissRequest = onFermer,
        confirmButton = { TextButton(onClick = onFermer) { Text("OK") } },
        title = { Text(stringResource(R.string.etat_gnss_titre)) },
        text = {
            if (fix == null) {
                Text(stringResource(R.string.etat_gnss_aucun))
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Ligne(stringResource(R.string.etat_fix), fix.qualite.libelle)
                    Ligne(stringResource(R.string.etat_precision), fix.precisionHorizontaleM?.let(::precision))
                    Ligne(stringResource(R.string.etat_latitude), fmt(fix.position.latitude, 7))
                    Ligne(stringResource(R.string.etat_longitude), fmt(fix.position.longitude, 7))
                    Ligne(stringResource(R.string.etat_altitude), fix.altitudeM?.let { "${fmt(it, 1)} m" })
                    Ligne(stringResource(R.string.etat_satellites_utilises), fix.nbSatellites.takeIf { it > 0 }?.toString())
                    Ligne(stringResource(R.string.etat_satellites_vus), fix.satellites.size.takeIf { it > 0 }?.toString())
                    Ligne(stringResource(R.string.etat_hdop), fix.hdop?.let { fmt(it, 2) })
                    Ligne(stringResource(R.string.etat_pdop), fix.pdop?.let { fmt(it, 2) })
                    Ligne(stringResource(R.string.etat_vdop), fix.vdop?.let { fmt(it, 2) })
                    // Lignes propres au RTK/NTRIP : sans objet pour un fix du GNSS interne.
                    if (fix.origine == OrigineFix.EXTERNE) {
                        Ligne(stringResource(R.string.etat_age), fix.ageCorrectionsS?.let { "${fmt(it, 0)} s" })
                        Ligne(stringResource(R.string.etat_station), fix.stationRef)
                    }
                    if (fix.satellites.isNotEmpty()) {
                        Text(
                            stringResource(R.string.etat_skyplot),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                        Skyplot(fix.satellites, Modifier.fillMaxWidth().heightIn(max = 240.dp))
                        BarresSnr(fix.satellites, Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
            }
        },
    )
}

@Composable
private fun Ligne(libelle: String, valeur: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(libelle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            valeur ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun fmt(v: Double, decimales: Int): String = String.format(Locale.ROOT, "%.${decimales}f", v)

private fun precision(metres: Double): String =
    if (metres < 1.0) "${(metres * 100).toInt()} cm" else "${fmt(metres, 2)} m"
