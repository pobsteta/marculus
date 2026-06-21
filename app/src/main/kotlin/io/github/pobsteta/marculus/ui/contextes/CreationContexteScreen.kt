package io.github.pobsteta.marculus.ui.contextes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import fr.marculus.core.Referentiels
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.ModeMesure
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.ui.libelle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationContexteScreen(
    repository: MartelageRepository,
    onAnnuler: () -> Unit,
    onCree: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var nom by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ModeMesure.DIAMETRE) }
    var min by remember { mutableStateOf("20") }
    var max by remember { mutableStateOf("90") }
    var pas by remember { mutableStateOf("5") }
    val selection = remember { mutableStateListOf(*Referentiels.ESSENCES_DEFAUT.toTypedArray()) }
    var erreur by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouveau contexte") },
                navigationIcon = {
                    IconButton(onClick = onAnnuler) {
                        Text("✕", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it },
                label = { Text("Nom du contexte") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Mode de mesure", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeMesure.entries.forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick = { mode = m },
                        label = { Text(m.libelle()) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChampNombre("Min", min, { min = it }, Modifier.weight(1f))
                ChampNombre("Max", max, { max = it }, Modifier.weight(1f))
                ChampNombre("Pas", pas, { pas = it }, Modifier.weight(1f))
            }

            Text("Essences (colonnes de la feuille)", style = MaterialTheme.typography.titleSmall)
            Referentiels.ESSENCES_DEFAUT.forEach { essence ->
                val coche = essence in selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (coche) selection.remove(essence) else selection.add(essence)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = coche, onCheckedChange = null)
                    Text(essence)
                }
            }

            erreur?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAnnuler, modifier = Modifier.weight(1f)) {
                    Text("Annuler")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val mi = min.toIntOrNull()
                        val ma = max.toIntOrNull()
                        val pa = pas.toIntOrNull()
                        erreur = when {
                            nom.isBlank() -> "Le nom est obligatoire"
                            mi == null || ma == null || pa == null -> "Min, max et pas doivent être des nombres"
                            pa <= 0 -> "Le pas doit être positif"
                            mi > ma -> "Min doit être inférieur ou égal à max"
                            selection.isEmpty() -> "Sélectionnez au moins une essence"
                            else -> {
                                scope.launch {
                                    val id = repository.creerContexte(
                                        nom = nom.trim(),
                                        mode = mode,
                                        axe = AxeClasses(mi, ma, pa),
                                        essencesActives = selection.toList(),
                                    )
                                    onCree(id)
                                }
                                null
                            }
                        }
                    },
                ) {
                    Text("Créer")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChampNombre(
    label: String,
    valeur: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = { saisie -> onChange(saisie.filter { it.isDigit() }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
