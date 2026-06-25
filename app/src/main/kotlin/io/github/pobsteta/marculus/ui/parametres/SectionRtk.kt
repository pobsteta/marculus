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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.StatutNtrip
import fr.marculus.core.model.ConfigRtk
import fr.marculus.core.model.OrigineFix
import fr.marculus.core.model.Reglages
import fr.marculus.core.model.TransportRtk
import fr.marculus.core.EntreeSourcetable
import io.github.pobsteta.marculus.R
import io.github.pobsteta.marculus.gnss.ClientNtrip
import io.github.pobsteta.marculus.gnss.ServiceGnssRtk
import io.github.pobsteta.marculus.ui.gnss.BadgeFix
import io.github.pobsteta.marculus.ui.gnss.DialogueEtatGnss
import kotlinx.coroutines.launch

/** Section « GNSS externe (RTK) » de l'écran Paramètres : transport, caster, test en direct. */
@Composable
fun SectionRtk(reglages: Reglages, onMaj: (Reglages) -> Unit) {
    val context = LocalContext.current
    val rtk = reglages.rtk
    fun majRtk(nouveau: ConfigRtk) = onMaj(reglages.copy(rtk = nouveau))
    val fix by ServiceGnssRtk.fixCourant.collectAsStateWithLifecycle()
    val etat by ServiceGnssRtk.etat.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var choixAppareil by remember { mutableStateOf(false) }
    var mountpoints by remember { mutableStateOf<List<EntreeSourcetable>>(emptyList()) }
    var menuMountpoints by remember { mutableStateOf(false) }
    var chargementMountpoints by remember { mutableStateOf(false) }
    var etatGnssOuvert by remember { mutableStateOf(false) }

    val demandePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) choixAppareil = true
    }

    Text(stringResource(R.string.rtk_section), style = MaterialTheme.typography.titleMedium)
    LigneSwitch(stringResource(R.string.rtk_actif_titre), stringResource(R.string.rtk_actif_desc), rtk.actif) {
        majRtk(rtk.copy(actif = it))
        if (!it) ServiceGnssRtk.arreter(context) // décocher → couper la connexion en cours
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
                ChampPersistant(rtk.hoteTcp, { majRtk(rtk.copy(hoteTcp = it)) }, stringResource(R.string.rtk_hote), Modifier.weight(2f))
                ChampPersistant(
                    rtk.portTcp.toString(),
                    { majRtk(rtk.copy(portTcp = it.toIntOrNull() ?: rtk.portTcp)) },
                    stringResource(R.string.rtk_port),
                    Modifier.weight(1f),
                )
            }
        }

        // Source des corrections : pont NTRIP par l'application, ou récepteur autonome.
        LigneSwitch(stringResource(R.string.rtk_source_titre), stringResource(R.string.rtk_source_desc), rtk.pontNtrip) {
            majRtk(rtk.copy(pontNtrip = it))
        }
        if (rtk.pontNtrip) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChampPersistant(rtk.casterHote, { majRtk(rtk.copy(casterHote = it)) }, stringResource(R.string.rtk_caster), Modifier.weight(2f))
                ChampPersistant(
                    rtk.casterPort.toString(),
                    { majRtk(rtk.copy(casterPort = it.toIntOrNull() ?: rtk.casterPort)) },
                    stringResource(R.string.rtk_port),
                    Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ChampPersistant(rtk.mountpoint, { majRtk(rtk.copy(mountpoint = it)) }, stringResource(R.string.rtk_mountpoint), Modifier.weight(1f))
                Box {
                    OutlinedButton(
                        enabled = !chargementMountpoints,
                        onClick = {
                            chargementMountpoints = true
                            scope.launch {
                                mountpoints = ClientNtrip.chargerMountpoints(rtk.casterHote, rtk.casterPort)
                                chargementMountpoints = false
                                menuMountpoints = mountpoints.isNotEmpty()
                            }
                        },
                    ) { Text(stringResource(R.string.rtk_charger_mountpoints)) }
                    DropdownMenu(expanded = menuMountpoints, onDismissRequest = { menuMountpoints = false }) {
                        mountpoints.forEach { e ->
                            DropdownMenuItem(
                                text = { Text("${e.mountpoint} · ${e.format}") },
                                onClick = { majRtk(rtk.copy(mountpoint = e.mountpoint)); menuMountpoints = false },
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChampPersistant(rtk.utilisateur, { majRtk(rtk.copy(utilisateur = it)) }, stringResource(R.string.rtk_utilisateur), Modifier.weight(1f))
                ChampPersistant(rtk.motDePasse, { majRtk(rtk.copy(motDePasse = it)) }, stringResource(R.string.rtk_motdepasse), Modifier.weight(1f))
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
            BadgeFix(fix) { etatGnssOuvert = true }
        }

        // Diagnostic du lien : permet de distinguer « BT non connecté » / « connecté mais aucune
        // donnée » / « données non‑NMEA » / « communication OK ».
        val diagnostic = when {
            etat.erreur != null -> stringResource(R.string.rtk_diag_erreur, etat.erreur ?: "")
            etat.octetsRecus == 0L -> stringResource(R.string.rtk_diag_connexion)
            etat.tramesRecues == 0L -> stringResource(R.string.rtk_diag_octets_sans_nmea, etat.octetsRecus)
            else -> stringResource(R.string.rtk_diag_trames, etat.tramesRecues, etat.octetsRecus)
        }
        Text(diagnostic, style = MaterialTheme.typography.bodySmall)
        etat.derniereTrame?.let {
            Text(stringResource(R.string.rtk_diag_derniere, it), style = MaterialTheme.typography.labelSmall)
        }
        // Sens téléphone → GNSS : RTCM renvoyé au récepteur. Toujours visible en mode pont NTRIP
        // (diagnostic de ce qui PART du téléphone, indépendant de l'état du fix).
        if (rtk.pontNtrip) {
            // Sens téléphone → caster : GGA envoyée (position du rover), indispensable pour qu'un
            // mountpoint NEAR/VRS sélectionne une base et commence à streamer du RTCM.
            Text(stringResource(R.string.rtk_diag_gga, etat.ggaEnvoye), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.rtk_diag_rtcm, etat.rtcmEnvoye), style = MaterialTheme.typography.bodySmall)
            // Statut du caster NTRIP : rend visibles les échecs (401, mountpoint inconnu, caster
            // injoignable) au lieu de laisser deviner devant un compteur RTCM à 0.
            val statutNtrip = when {
                etat.ntripErreur != null -> stringResource(R.string.rtk_ntrip_erreur, etat.ntripErreur ?: "")
                etat.ntripStatut == StatutNtrip.OK -> stringResource(R.string.rtk_ntrip_ok)
                etat.ntripStatut == StatutNtrip.NON_AUTORISE -> stringResource(R.string.rtk_ntrip_401)
                etat.ntripStatut == StatutNtrip.SOURCETABLE -> stringResource(R.string.rtk_ntrip_sourcetable)
                etat.ntripStatut == StatutNtrip.INCONNU -> stringResource(R.string.rtk_ntrip_inconnu)
                else -> stringResource(R.string.rtk_ntrip_connexion)
            }
            Text(statutNtrip, style = MaterialTheme.typography.bodySmall)
        }
        // Sens GNSS → téléphone : âge des corrections reçues. Affiché uniquement quand le récepteur
        // EXTERNE fournit réellement un fix corrigé (âge présent). Un fix interne (origine INTERNE)
        // ou autonome n'a pas d'âge : masqué, pour ne pas laisser croire à un RTK NTRIP inexistant.
        val age = fix?.ageCorrectionsS
        if (rtk.pontNtrip && fix?.origine == OrigineFix.EXTERNE && age != null) {
            Text(stringResource(R.string.rtk_corrections_ok, age.toInt().toString()), style = MaterialTheme.typography.bodySmall)
        }
        // Station de référence sélectionnée (NEAR/VRS : la base la plus proche), issue du champ 14
        // de la trame GGA dès que le récepteur calcule une solution corrigée.
        val station = fix?.stationRef
        if (rtk.pontNtrip && fix?.origine == OrigineFix.EXTERNE && !station.isNullOrBlank()) {
            Text(stringResource(R.string.rtk_station_connectee, station), style = MaterialTheme.typography.bodySmall)
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

    if (etatGnssOuvert) {
        DialogueEtatGnss(fix) { etatGnssOuvert = false }
    }
}

/**
 * Champ texte piloté par un état local (le curseur ne saute pas malgré l'aller-retour
 * asynchrone par DataStore) ; resynchronisé depuis [valeur] uniquement hors édition.
 */
@Composable
private fun ChampPersistant(valeur: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    var texte by remember { mutableStateOf(valeur) }
    var enEdition by remember { mutableStateOf(false) }
    LaunchedEffect(valeur, enEdition) { if (!enEdition) texte = valeur }
    OutlinedTextField(
        value = texte,
        onValueChange = { texte = it; onChange(it) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.onFocusChanged { enEdition = it.isFocused },
    )
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
