package io.github.pobsteta.marculus.ui.contextes

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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.model.Contexte
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.ui.libelle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeContextesScreen(
    repository: MartelageRepository,
    onCreer: () -> Unit,
    onOuvrir: (String) -> Unit,
    onModifier: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val contextes by repository.contextes().collectAsStateWithLifecycle(emptyList())
    var aSupprimer by remember { mutableStateOf<Contexte?>(null) }
    var aLire by remember { mutableStateOf<Contexte?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marculus") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreer) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) { padding ->
        if (contextes.isEmpty()) {
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
                items(contextes, key = { it.id }) { contexte ->
                    CarteContexte(
                        contexte = contexte,
                        onOuvrir = { onOuvrir(contexte.id) },
                        onModifier = { onModifier(contexte.id) },
                        onSupprimer = { aSupprimer = contexte },
                        onLire = { aLire = contexte },
                    )
                }
            }
        }
    }

    aSupprimer?.let { cible ->
        AlertDialog(
            onDismissRequest = { aSupprimer = null },
            title = { Text("Supprimer le contexte ?") },
            text = { Text("« ${cible.nom} » et tout son historique de martelage seront supprimés.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.supprimerContexte(cible.id) }
                    aSupprimer = null
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { aSupprimer = null }) { Text("Annuler") } },
        )
    }

    aLire?.let { cible ->
        AlertDialog(
            onDismissRequest = { aLire = null },
            title = { Text(cible.nom) },
            text = {
                Column {
                    Text("Mesure : ${cible.mode.libelle()}")
                    Text("Classes : ${cible.axe.min}–${cible.axe.max} (pas ${cible.axe.pas})")
                    Text("Incrément : ${cible.increment}")
                    Text("Essences : ${cible.essencesNoms.joinToString(", ")}")
                    cible.commentaire?.takeIf { it.isNotBlank() }?.let {
                        Text("Commentaire : $it")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { aLire = null }) { Text("Fermer") } },
        )
    }
}

@Composable
private fun CarteContexte(
    contexte: Contexte,
    onOuvrir: () -> Unit,
    onModifier: () -> Unit,
    onSupprimer: () -> Unit,
    onLire: () -> Unit,
) {
    var menuOuvert by remember { mutableStateOf(false) }
    ElevatedCard(onClick = onOuvrir, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(contexte.nom, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${contexte.mode.libelle()} · classes ${contexte.axe.min}–${contexte.axe.max} " +
                        "(pas ${contexte.axe.pas}) · ${contexte.essences.size} essences",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Box {
                IconButton(onClick = { menuOuvert = true }) {
                    Text("⋮", style = MaterialTheme.typography.titleLarge)
                }
                DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                    DropdownMenuItem(text = { Text("Lire") }, onClick = { menuOuvert = false; onLire() })
                    DropdownMenuItem(text = { Text("Modifier") }, onClick = { menuOuvert = false; onModifier() })
                    DropdownMenuItem(text = { Text("Supprimer") }, onClick = { menuOuvert = false; onSupprimer() })
                }
            }
        }
    }
}
