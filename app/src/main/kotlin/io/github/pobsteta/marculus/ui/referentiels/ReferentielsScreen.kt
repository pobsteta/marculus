package io.github.pobsteta.marculus.ui.referentiels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.Referentiels
import fr.marculus.core.model.SeuilsCategories
import io.github.pobsteta.marculus.data.ReferentielsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferentielsScreen(
    referentielsRepository: ReferentielsRepository,
    onRetour: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val essences by referentielsRepository.essences.collectAsStateWithLifecycle(Referentiels.ESSENCES_DEFAUT)
    val qualitesArbre by referentielsRepository.qualitesArbre.collectAsStateWithLifecycle(Referentiels.QUALITE_ARBRE_DEFAUT)
    val qualitesBois by referentielsRepository.qualitesBois.collectAsStateWithLifecycle(Referentiels.QUALITE_BOIS_DEFAUT)
    val seuils by referentielsRepository.seuils.collectAsStateWithLifecycle(SeuilsCategories.DEFAUT)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Référentiels") },
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
            SectionListe(
                titre = "Essences",
                items = essences,
                onAjouter = { scope.launch { referentielsRepository.enregistrerEssences(essences + it) } },
                onSupprimer = { scope.launch { referentielsRepository.enregistrerEssences(essences - it) } },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            SectionListe(
                titre = "Qualité de l'arbre",
                items = qualitesArbre,
                onAjouter = { scope.launch { referentielsRepository.enregistrerQualitesArbre(qualitesArbre + it) } },
                onSupprimer = { scope.launch { referentielsRepository.enregistrerQualitesArbre(qualitesArbre - it) } },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            SectionListe(
                titre = "Qualité bois",
                items = qualitesBois,
                onAjouter = { scope.launch { referentielsRepository.enregistrerQualitesBois(qualitesBois + it) } },
                onSupprimer = { scope.launch { referentielsRepository.enregistrerQualitesBois(qualitesBois - it) } },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            SectionSeuils(seuils) { scope.launch { referentielsRepository.enregistrerSeuils(it) } }
        }
    }
}

private fun formatSeuil(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

@Composable
private fun SectionSeuils(seuils: SeuilsCategories, onEnregistrer: (SeuilsCategories) -> Unit) {
    var pb by remember(seuils) { mutableStateOf(formatSeuil(seuils.pbBm)) }
    var bm by remember(seuils) { mutableStateOf(formatSeuil(seuils.bmGb)) }
    var gb by remember(seuils) { mutableStateOf(formatSeuil(seuils.gbTgb)) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Catégories de grosseur (seuils de diamètre, cm)", style = MaterialTheme.typography.titleMedium)
        Text(
            "PB < seuil₁ ≤ BM < seuil₂ ≤ GB < seuil₃ ≤ TGB.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChampSeuil("PB→BM", pb, { pb = it }, Modifier.weight(1f))
            ChampSeuil("BM→GB", bm, { bm = it }, Modifier.weight(1f))
            ChampSeuil("GB→TGB", gb, { gb = it }, Modifier.weight(1f))
        }
        Button(onClick = {
            val p = pb.replace(',', '.').toDoubleOrNull()
            val b = bm.replace(',', '.').toDoubleOrNull()
            val g = gb.replace(',', '.').toDoubleOrNull()
            if (p != null && b != null && g != null && p < b && b < g) {
                onEnregistrer(SeuilsCategories(p, b, g))
            }
        }) { Text("Enregistrer les seuils") }
    }
}

@Composable
private fun ChampSeuil(label: String, valeur: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = valeur,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun SectionListe(
    titre: String,
    items: List<String>,
    onAjouter: (String) -> Unit,
    onSupprimer: (String) -> Unit,
) {
    var nouveau by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titre, style = MaterialTheme.typography.titleMedium)
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item, modifier = Modifier.weight(1f))
                IconButton(onClick = { onSupprimer(item) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer $item")
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = nouveau,
                onValueChange = { nouveau = it },
                label = { Text("Ajouter") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                val v = nouveau.trim()
                if (v.isNotBlank() && v !in items) onAjouter(v)
                nouveau = ""
            }) { Text("Ajouter") }
        }
    }
}
