package io.github.pobsteta.marculus.ui.parametres

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.model.Reglages
import io.github.pobsteta.marculus.data.ReglagesRepository
import io.github.pobsteta.marculus.data.SauvegardeRepository
import kotlinx.coroutines.launch
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val ENTREE_ZIP = "marculus.json"

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
                message = "Sauvegarde enregistrée."
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) restaurationUri = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
            LigneReglage("Thème sombre", "Force le thème sombre de l'application.", reglages.themeSombre) {
                maj(reglages.copy(themeSombre = it))
            }
            LigneReglage("Empêcher la mise en veille", "Garde l'écran allumé pendant le martelage.", reglages.antiVeille) {
                maj(reglages.copy(antiVeille = it))
            }
            LigneReglage("Plein écran", "Masque les barres de statut et de navigation.", reglages.pleinEcran) {
                maj(reglages.copy(pleinEcran = it))
            }
            LigneReglage("Vibration", "Vibre brièvement à chaque comptage.", reglages.vibration) {
                maj(reglages.copy(vibration = it))
            }
            LigneReglage("Son de clic", "Émet un son au comptage.", reglages.sonClic) {
                maj(reglages.copy(sonClic = it))
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Sauvegarde / Restauration", style = MaterialTheme.typography.titleMedium)
            Text(
                "Enregistre ou restaure toutes les données (contextes, journal, avis, référentiels) dans un fichier ZIP.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exportLauncher.launch("marculus-sauvegarde.zip") }, modifier = Modifier.weight(1f)) {
                    Text("Sauvegarder")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                    modifier = Modifier.weight(1f),
                ) { Text("Restaurer") }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                "À venir : boutons de volume pour compter, annonce vocale, choix de la langue.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    restaurationUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restaurationUri = null },
            title = { Text("Restaurer la sauvegarde ?") },
            text = { Text("Toutes les données actuelles seront remplacées par celles du fichier.") },
            confirmButton = {
                TextButton(onClick = {
                    restaurationUri = null
                    scope.launch {
                        val json = lireJsonDepuisZip(context, uri)
                        if (!json.isNullOrBlank()) {
                            sauvegardeRepository.importerJson(json)
                            message = "Restauration effectuée."
                        } else {
                            message = "Fichier illisible ou invalide."
                        }
                    }
                }) { Text("Restaurer") }
            },
            dismissButton = { TextButton(onClick = { restaurationUri = null }) { Text("Annuler") } },
        )
    }

    message?.let { texte ->
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } },
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
