package io.github.pobsteta.marculus.ui.parametres

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.model.Reglages
import io.github.pobsteta.marculus.Langue
import io.github.pobsteta.marculus.R
import io.github.pobsteta.marculus.data.ReglagesRepository
import io.github.pobsteta.marculus.data.SauvegardeRepository
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val ENTREE_ZIP = "marculus.json"
private val FORMAT_HORODATAGE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametresScreen(
    reglagesRepository: ReglagesRepository,
    sauvegardeRepository: SauvegardeRepository,
    onRetour: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val reglages by reglagesRepository.reglages.collectAsStateWithLifecycle(Reglages())
    fun maj(nouveau: Reglages) = scope.launch { reglagesRepository.enregistrer(nouveau) }

    var message by remember { mutableStateOf<String?>(null) }
    var restaurationUri by remember { mutableStateOf<Uri?>(null) }
    var choixLangue by remember { mutableStateOf(false) }
    var choixVoix by remember { mutableStateOf(false) }
    var ttsPret by remember { mutableStateOf(false) }
    val ttsParam = remember {
        lateinit var moteur: TextToSpeech
        moteur = TextToSpeech(context.applicationContext) { statut -> if (statut == TextToSpeech.SUCCESS) ttsPret = true }
        moteur
    }
    DisposableEffect(Unit) { onDispose { ttsParam.stop(); ttsParam.shutdown() } }
    val phraseTestVoix = stringResource(R.string.param_voix_test)

    val msgSauvegarde = stringResource(R.string.param_msg_sauvegarde)
    val msgRestauration = stringResource(R.string.param_msg_restauration)
    val msgIllisible = stringResource(R.string.param_msg_illisible)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = sauvegardeRepository.exporterJson()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    ZipOutputStream(os).use { zos ->
                        zos.putNextEntry(ZipEntry(ENTREE_ZIP))
                        zos.write(json.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }
                }
                message = msgSauvegarde
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) restaurationUri = uri }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* accordée ou non : si refusée, la position ne sera simplement pas capturée */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.param_titre)) },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.param_retour))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LigneReglage(stringResource(R.string.param_theme_titre), stringResource(R.string.param_theme_desc), reglages.themeSombre) {
                maj(reglages.copy(themeSombre = it))
            }
            LigneReglage(stringResource(R.string.param_veille_titre), stringResource(R.string.param_veille_desc), reglages.antiVeille) {
                maj(reglages.copy(antiVeille = it))
            }
            LigneReglage(stringResource(R.string.param_plein_titre), stringResource(R.string.param_plein_desc), reglages.pleinEcran) {
                maj(reglages.copy(pleinEcran = it))
            }
            LigneReglage(stringResource(R.string.param_vibration_titre), stringResource(R.string.param_vibration_desc), reglages.vibration) {
                maj(reglages.copy(vibration = it))
            }
            LigneReglage(stringResource(R.string.param_son_titre), stringResource(R.string.param_son_desc), reglages.sonClic) {
                maj(reglages.copy(sonClic = it))
            }
            LigneReglage(
                stringResource(R.string.param_gnss_titre),
                stringResource(R.string.param_gnss_desc),
                reglages.capturePosition,
            ) { active ->
                maj(reglages.copy(capturePosition = active))
                if (active &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            LigneReglage(stringResource(R.string.param_annonce_nombre_titre), stringResource(R.string.param_annonce_nombre_desc), reglages.annonceNombre) {
                maj(reglages.copy(annonceNombre = it))
            }
            LigneReglage(stringResource(R.string.param_annonce_etiquette_titre), stringResource(R.string.param_annonce_etiquette_desc), reglages.annonceEtiquette) {
                maj(reglages.copy(annonceEtiquette = it))
            }
            LigneReglage(stringResource(R.string.param_volume_titre), stringResource(R.string.param_volume_desc), reglages.boutonsVolume) {
                maj(reglages.copy(boutonsVolume = it))
            }
            LigneReglage(stringResource(R.string.param_rouvrir_titre), stringResource(R.string.param_rouvrir_desc), reglages.rouvrirDernier) {
                maj(reglages.copy(rouvrirDernier = it))
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            // Langue de l'application.
            val codeLangue = Langue.code(context)
            val libelleLangue = when (codeLangue) {
                "fr" -> stringResource(R.string.param_langue_fr)
                "en" -> stringResource(R.string.param_langue_en)
                else -> stringResource(R.string.param_langue_systeme)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.param_langue_titre), style = MaterialTheme.typography.titleMedium)
                    Text(libelleLangue, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = { choixLangue = true }) { Text(stringResource(R.string.param_langue_changer)) }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.param_voix_titre), style = MaterialTheme.typography.titleMedium)
                    Text(reglages.voixTts ?: stringResource(R.string.param_voix_defaut), style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = { choixVoix = true }) { Text(stringResource(R.string.param_langue_changer)) }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.param_sauvegarde_section), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.param_sauvegarde_desc), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { exportLauncher.launch("marculus-sauvegarde-${LocalDateTime.now().format(FORMAT_HORODATAGE)}.zip") },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.param_sauvegarder))
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.param_restaurer)) }
            }
        }
    }

    if (choixLangue) {
        fun definirLangue(code: String) {
            choixLangue = false
            Langue.definir(context, code)
            (context as? Activity)?.recreate()
        }
        AlertDialog(
            onDismissRequest = { choixLangue = false },
            title = { Text(stringResource(R.string.param_langue_titre)) },
            text = {
                Column {
                    TextButton(onClick = { definirLangue("system") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.param_langue_systeme), style = MaterialTheme.typography.bodyLarge)
                    }
                    TextButton(onClick = { definirLangue("fr") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.param_langue_fr), style = MaterialTheme.typography.bodyLarge)
                    }
                    TextButton(onClick = { definirLangue("en") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.param_langue_en), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choixLangue = false }) { Text(stringResource(R.string.param_annuler)) } },
        )
    }

    if (choixVoix) {
        val voixDispo = remember(ttsPret) {
            ttsParam.voices
                ?.filter { it.locale.language == "fr" || it.locale.language == "en" }
                ?.sortedBy { it.locale.toString() + it.name }
                ?: emptyList()
        }
        fun choisir(v: Voice?) {
            maj(reglages.copy(voixTts = v?.name))
            if (v != null) ttsParam.voice = v else ttsParam.language = Locale.FRENCH
            ttsParam.speak(phraseTestVoix, TextToSpeech.QUEUE_FLUSH, null, "test")
        }
        AlertDialog(
            onDismissRequest = { choixVoix = false },
            title = { Text(stringResource(R.string.param_voix_titre)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TextButton(onClick = { choisir(null) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.param_voix_defaut), modifier = Modifier.fillMaxWidth())
                    }
                    voixDispo.forEach { v ->
                        TextButton(onClick = { choisir(v) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${v.locale.displayLanguage} – ${v.name}", modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (voixDispo.isEmpty()) {
                        Text(stringResource(R.string.param_voix_aucune), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choixVoix = false }) { Text(stringResource(R.string.param_fermer)) } },
        )
    }

    restaurationUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restaurationUri = null },
            title = { Text(stringResource(R.string.param_restaurer_titre)) },
            text = { Text(stringResource(R.string.param_restaurer_texte)) },
            confirmButton = {
                TextButton(onClick = {
                    restaurationUri = null
                    scope.launch {
                        val json = lireJsonDepuisZip(context, uri)
                        message = if (!json.isNullOrBlank()) {
                            sauvegardeRepository.importerJson(json)
                            msgRestauration
                        } else {
                            msgIllisible
                        }
                    }
                }) { Text(stringResource(R.string.param_restaurer)) }
            },
            dismissButton = { TextButton(onClick = { restaurationUri = null }) { Text(stringResource(R.string.param_annuler)) } },
        )
    }

    message?.let { texte ->
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text(stringResource(R.string.param_ok)) } },
            text = { Text(texte) },
        )
    }
}

private fun lireJsonDepuisZip(context: android.content.Context, uri: Uri): String? =
    context.contentResolver.openInputStream(uri)?.use { ins ->
        ZipInputStream(ins).use { zis ->
            var entree = zis.nextEntry
            var contenu: String? = null
            while (entree != null) {
                if (entree.name == ENTREE_ZIP) contenu = zis.readBytes().decodeToString()
                entree = zis.nextEntry
            }
            contenu
        }
    }

@Composable
private fun LigneReglage(titre: String, description: String, valeur: Boolean, onChange: (Boolean) -> Unit) {
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
