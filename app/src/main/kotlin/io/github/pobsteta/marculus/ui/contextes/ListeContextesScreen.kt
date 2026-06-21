package io.github.pobsteta.marculus.ui.contextes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.export.ExportCsv
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.data.ResumeContexte
import io.github.pobsteta.marculus.ui.libelle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeContextesScreen(
    repository: MartelageRepository,
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
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                repository.marquerExporte(id)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marculus") },
                actions = {
                    Box {
                        IconButton(onClick = { menuAppli = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(expanded = menuAppli, onDismissRequest = { menuAppli = false }) {
                            DropdownMenuItem(
                                text = { Text("Référentiels") },
                                onClick = { menuAppli = false; onReferentiels() },
                            )
                            DropdownMenuItem(
                                text = { Text("Paramètres") },
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
                Icon(Icons.Filled.Add, contentDescription = "Nouveau contexte")
            }
        },
    ) { padding ->
        if (resumes.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Aucun contexte. Touchez + pour créer une opération de martelage.",
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
                    )
                }
            }
        }
    }

    aSupprimer?.let { cible ->
        AlertDialog(
            onDismissRequest = { aSupprimer = null },
            title = { Text("Supprimer le contexte ?") },
            text = { Text("« ${cible.contexte.nom} » et tout son historique de martelage seront supprimés.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.supprimerContexte(cible.contexte.id) }
                    aSupprimer = null
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { aSupprimer = null }) { Text("Annuler") } },
        )
    }

    aLire?.let { cible ->
        val c = cible.contexte
        AlertDialog(
            onDismissRequest = { aLire = null },
            title = { Text(c.nom) },
            text = {
                Column {
                    Text("Mesure : ${c.mode.libelle()}")
                    Text("Classes : ${c.axe.min}–${c.axe.max} (pas ${c.axe.pas})")
                    Text("Incrément : ${c.increment}")
                    Text("Essences : ${c.essencesNoms.joinToString(", ")}")
                    Text("Tiges enregistrées : ${cible.nbEvenements}")
                    Text("Exporté : ${if (c.exporte) "oui" else "non"}")
                    c.commentaire?.takeIf { it.isNotBlank() }?.let { Text("Commentaire : $it") }
                    if (cible.verrouille) {
                        Text(
                            "Verrouillé : lecture seule tant que le contexte n'est pas exporté.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { aLire = null }) { Text("Fermer") } },
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
                    "${contexte.mode.libelle()} · classes ${contexte.axe.min}–${contexte.axe.max} " +
                        "(pas ${contexte.axe.pas}) · ${contexte.essences.size} essences · ${resume.nbEvenements} tiges" +
                        if (resume.verrouille) " · 🔒" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Box {
                IconButton(onClick = { menuOuvert = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options du contexte")
                }
                DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                    DropdownMenuItem(text = { Text("Lire") }, onClick = { menuOuvert = false; onLire() })
                    DropdownMenuItem(text = { Text("Dupliquer") }, onClick = { menuOuvert = false; onDupliquer() })
                    DropdownMenuItem(text = { Text("Exporter (CSV)") }, onClick = { menuOuvert = false; onExporter() })
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        enabled = !resume.verrouille,
                        onClick = { menuOuvert = false; onModifier() },
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer") },
                        enabled = !resume.verrouille,
                        onClick = { menuOuvert = false; onSupprimer() },
                    )
                }
            }
        }
    }
}
