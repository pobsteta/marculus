package io.github.pobsteta.marculus.ui.parametres

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.model.Reglages
import io.github.pobsteta.marculus.data.ReglagesRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametresScreen(
    reglagesRepository: ReglagesRepository,
    onRetour: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reglages by reglagesRepository.reglages.collectAsStateWithLifecycle(Reglages())
    fun maj(nouveau: Reglages) = scope.launch { reglagesRepository.enregistrer(nouveau) }

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
            Text(
                "À venir : boutons de volume pour compter, annonce vocale, choix de la langue, export / import.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
