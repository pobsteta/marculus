package io.github.pobsteta.marculus.ui.parametres

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.model.ConfigRtk
import fr.marculus.core.model.Reglages
import fr.marculus.core.model.TransportRtk
import io.github.pobsteta.marculus.R
import io.github.pobsteta.marculus.gnss.ServiceGnssRtk
import io.github.pobsteta.marculus.ui.gnss.BadgeFix

/** Section « GNSS externe (RTK) » de l'écran Paramètres : transport, caster, test en direct. */
@Composable
fun SectionRtk(reglages: Reglages, onMaj: (Reglages) -> Unit) {
    val context = LocalContext.current
    val rtk = reglages.rtk
    fun majRtk(nouveau: ConfigRtk) = onMaj(reglages.copy(rtk = nouveau))
    val fix by ServiceGnssRtk.fixCourant.collectAsStateWithLifecycle()
    var choixAppareil by remember { mutableStateOf(false) }

    val demandePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) choixAppareil = true
    }

    Text(stringResource(R.string.rtk_section), style = MaterialTheme.typography.titleMedium)
    LigneSwitch(stringResource(R.string.rtk_actif_titre), stringResource(R.string.rtk_actif_desc), rtk.actif) {
        majRtk(rtk.copy(actif = it))
    }

    if (rtk.actif) {
        // Transport : Bluetooth ou TCP/WiFi.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            BoutonChoix(stringResource(R.string.rtk_transport_bt), rtk.transport == TransportRtk.BLUETOOTH) {
                majRtk(rtk.copy(transport = TransportRtk.BLUETOOTH))
            }
            BoutonChoix(stringResource(R.string.rtk_transport_tcp), rtk.transport == TransportRtk.TCP) {
                majRtk(rtk.copy(transport = TransportRtk.TCP))
            }
        }

        when (rtk.transport) {
            TransportRtk.BLUETOOTH -> Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    rtk.appareilBtNom ?: stringResource(R.string.rtk_aucun_appareil),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        choixAppareil = true
                    } else {
                        demandePermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                }) { Text(stringResource(R.string.rtk_choisir_appareil)) }
            }

            TransportRtk.TCP -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rtk.hoteTcp,
                    onValueChange = { majRtk(rtk.copy(hoteTcp = it)) },
                    label = { Text(stringResource(R.string.rtk_hote)) },
                    singleLine = true,
                    modifier = Modifier.weight(2f),
                )
                OutlinedTextField(
                    value = rtk.portTcp.toString(),
                    onValueChange = { majRtk(rtk.copy(portTcp = it.toIntOrNull() ?: rtk.portTcp)) },
                    label = { Text(stringResource(R.string.rtk_port)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Source des corrections : pont NTRIP par l'application, ou récepteur autonome.
        LigneSwitch(stringResource(R.string.rtk_source_titre), stringResource(R.string.rtk_source_desc), rtk.pontNtrip) {
            majRtk(rtk.copy(pontNtrip = it))
        }
        if (rtk.pontNtrip) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rtk.casterHote,
                    onValueChange = { majRtk(rtk.copy(casterHote = it)) },
                    label = { Text(stringResource(R.string.rtk_caster)) },
                    singleLine = true,
                    modifier = Modifier.weight(2f),
                )
                OutlinedTextField(
                    value = rtk.casterPort.toString(),
                    onValueChange = { majRtk(rtk.copy(casterPort = it.toIntOrNull() ?: rtk.casterPort)) },
                    label = { Text(stringResource(R.string.rtk_port)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = rtk.mountpoint,
                onValueChange = { majRtk(rtk.copy(mountpoint = it)) },
                label = { Text(stringResource(R.string.rtk_mountpoint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rtk.utilisateur,
                    onValueChange = { majRtk(rtk.copy(utilisateur = it)) },
                    label = { Text(stringResource(R.string.rtk_utilisateur)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = rtk.motDePasse,
                    onValueChange = { majRtk(rtk.copy(motDePasse = it)) },
                    label = { Text(stringResource(R.string.rtk_motdepasse)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Test en direct : démarre le service et affiche le fix courant.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Button(onClick = { ServiceGnssRtk.demarrerDepuis(context, rtk) }) { Text(stringResource(R.string.rtk_tester)) }
            OutlinedButton(onClick = { ServiceGnssRtk.arreter(context) }) { Text(stringResource(R.string.rtk_arreter)) }
            BadgeFix(fix)
        }
    }

    if (choixAppareil) {
        val appareils = remember { appareilsAppaires(context) }
        AlertDialog(
            onDismissRequest = { choixAppareil = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choixAppareil = false }) { Text("OK") } },
            title = { Text(stringResource(R.string.rtk_appareils_titre)) },
            text = {
                Column {
                    if (appareils.isEmpty()) {
                        Text(stringResource(R.string.rtk_permission_bt))
                    } else {
                        appareils.forEach { (nom, adresse) ->
                            TextButton(onClick = {
                                majRtk(rtk.copy(appareilBt = adresse, appareilBtNom = nom))
                                choixAppareil = false
                            }) { Text(nom) }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun BoutonChoix(libelle: String, choisi: Boolean, onClick: () -> Unit) {
    if (choisi) {
        Button(onClick = onClick) { Text(libelle) }
    } else {
        OutlinedButton(onClick = onClick) { Text(libelle) }
    }
}

@Composable
private fun LigneSwitch(titre: String, description: String, valeur: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titre, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = valeur, onCheckedChange = onChange)
    }
}

/** Appareils Bluetooth appairés (nom, adresse). Vide si la permission n'est pas accordée. */
@SuppressLint("MissingPermission")
private fun appareilsAppaires(context: Context): List<Pair<String, String>> {
    val adaptateur = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
    return runCatching {
        adaptateur.bondedDevices.map { (it.name ?: it.address) to it.address }
    }.getOrDefault(emptyList())
}
