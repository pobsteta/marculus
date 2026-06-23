package io.github.pobsteta.marculus.ui.contextes

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.export.ExportCsv
import io.github.pobsteta.marculus.R
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.data.ResumeContexte
import io.github.pobsteta.marculus.data.SauvegardeRepository
import io.github.pobsteta.marculus.ui.libelle
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeContextesScreen(
    repository: MartelageRepository,
    sauvegardeRepository: SauvegardeRepository,
    operateur: String,
    onCreer: () -> Unit,
    onOuvrir: (String) -> Unit,
    onModifier: (String) -> Unit,
    onParametres: () -> Unit,
    onReferentiels: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resumes by repository.resumes().collectAsStateWithLifecycle(emptyList())
    var aSupprimer by remember { mutableStateOf<ResumeContexte?>(null) }
    var aLire by remember { mutableStateOf<ResumeContexte?>(null) }
    var menuAppli by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val id = pendingExport
        pendingExport = null
        if (uri != null && id != null) {
            scope.launch {
                val ctx = repository.contexte(id) ?: return@launch
                val journal = repository.journalInstantane(id)
                val csv = ExportCsv.contexteCsv(ctx, journal)
                context.contentResolver.openOutputStream(uri)?.use {
                    // BOM UTF-8 pour qu'Excel détecte l'encodage et affiche correctement les accents.
                    it.write("﻿$csv".toByteArray(Charsets.UTF_8))
                }
                repository.marquerExporte(id)
            }
        }
    }

    // Partage d'un contexte pour la synchro (fichier .marsync via le partage système).
    val titrePartage = stringResource(R.string.sync_partage_titre)
    fun partagerContexte(id: String, nom: String) {
        scope.launch {
            val json = sauvegardeRepository.exporterContexteJson(id)
            fun assainir(s: String) = s.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val nomCtx = assainir(nom).ifBlank { "contexte" }
            val op = assainir(operateur).take(16)
            val horodatage = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"))
            val fichier = File(context.cacheDir, "$nomCtx-$op-$horodatage.marsync")
            fichier.writeText(json)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichier)
            val envoi = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(envoi, titrePartage))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.liste_app_title)) },
                actions = {
                    Box {
                        IconButton(onClick = { menuAppli = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.liste_menu_content_desc))
                        }
                        DropdownMenu(expanded = menuAppli, onDismissRequest = { menuAppli = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.liste_menu_referentiels)) },
                                onClick = { menuAppli = false; onReferentiels() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.liste_menu_parametres)) },
                                onClick = { menuAppli = false; onParametres() },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreer) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.liste_fab_nouveau_contexte))
            }
        },
    ) { padding ->
        if (resumes.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.liste_vide_message),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(resumes, key = { it.contexte.id }) { resume ->
                    CarteContexte(
                        resume = resume,
                        onOuvrir = { onOuvrir(resume.contexte.id) },
                        onModifier = { onModifier(resume.contexte.id) },
                        onSupprimer = { aSupprimer = resume },
                        onLire = { aLire = resume },
                        onDupliquer = { scope.launch { repository.dupliquerContexte(resume.contexte.id) } },
                        onExporter = {
                            pendingExport = resume.contexte.id
                            exportLauncher.launch("${resume.contexte.nom}.csv")
                        },
                        onPartager = { partagerContexte(resume.contexte.id, resume.contexte.nom) },
                    )
                }
            }
        }
    }

    aSupprimer?.let { cible ->
        AlertDialog(
            onDismissRequest = { aSupprimer = null },
            title = { Text(stringResource(R.string.liste_dialog_supprimer_titre)) },
            text = { Text(stringResource(R.string.liste_dialog_supprimer_texte, cible.contexte.nom)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.supprimerContexte(cible.contexte.id) }
                    aSupprimer = null
                }) { Text(stringResource(R.string.liste_dialog_supprimer_confirmer)) }
            },
            dismissButton = { TextButton(onClick = { aSupprimer = null }) { Text(stringResource(R.string.liste_dialog_annuler)) } },
        )
    }

    aLire?.let { cible ->
        val c = cible.contexte
        AlertDialog(
            onDismissRequest = { aLire = null },
            title = { Text(c.nom) },
            text = {
                Column {
                    Text(stringResource(R.string.liste_detail_mesure, c.mode.libelle()))
                    Text(stringResource(R.string.liste_detail_classes, c.axe.min, c.axe.max, c.axe.pas))
                    Text(stringResource(R.string.liste_detail_increment, c.increment))
                    Text(stringResource(R.string.liste_detail_essences, c.essencesNoms.joinToString(", ")))
                    Text(stringResource(R.string.liste_detail_tiges, cible.nbEvenements))
                    Text(stringResource(R.string.liste_detail_exporte, stringResource(if (c.exporte) R.string.liste_oui else R.string.liste_non)))
                    c.commentaire?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.liste_detail_commentaire, it)) }
                    if (cible.verrouille) {
                        Text(
                            stringResource(R.string.liste_detail_verrouille),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { aLire = null }) { Text(stringResource(R.string.liste_dialog_fermer)) } },
        )
    }
}

@Composable
private fun CarteContexte(
    resume: ResumeContexte,
    onOuvrir: () -> Unit,
    onModifier: () -> Unit,
    onSupprimer: () -> Unit,
    onLire: () -> Unit,
    onDupliquer: () -> Unit,
    onExporter: () -> Unit,
    onPartager: () -> Unit,
) {
    val contexte = resume.contexte
    var menuOuvert by remember { mutableStateOf(false) }
    ElevatedCard(onClick = onOuvrir, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(contexte.nom, style = MaterialTheme.typography.titleLarge)
                contexte.commentaire?.takeIf { it.isNotBlank() }?.let { commentaire ->
                    Text(
                        commentaire,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    stringResource(R.string.liste_carte_resume,
                        contexte.mode.libelle(),
                        contexte.axe.min,
                        contexte.axe.max,
                        contexte.axe.pas,
                        contexte.essences.size,
                        resume.nbEvenements,
                    ) + if (resume.verrouille) stringResource(R.string.liste_carte_resume_verrouille) else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Box {
                IconButton(onClick = { menuOuvert = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.liste_carte_options_content_desc))
                }
                DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.liste_carte_menu_lire)) }, onClick = { menuOuvert = false; onLire() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.liste_carte_menu_dupliquer)) }, onClick = { menuOuvert = false; onDupliquer() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.liste_carte_menu_exporter_csv)) }, onClick = { menuOuvert = false; onExporter() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.liste_carte_menu_partager)) }, onClick = { menuOuvert = false; onPartager() })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.liste_carte_menu_modifier)) },
                        enabled = !resume.verrouille,
                        onClick = { menuOuvert = false; onModifier() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.liste_carte_menu_supprimer)) },
                        enabled = !resume.verrouille,
                        onClick = { menuOuvert = false; onSupprimer() },
                    )
                }
            }
        }
    }
}
