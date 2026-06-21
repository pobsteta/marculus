package io.github.pobsteta.marculus.ui.parametres

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Écran Paramètres — placeholder qui prépare la Tranche 3 (réglages persistés via DataStore).
 * Liste les réglages prévus ; le câblage viendra ensuite.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametresScreen(onRetour: () -> Unit) {
    val prevus = listOf(
        "Plein écran" to "Masquer les barres de statut et de navigation.",
        "Empêcher la mise en veille" to "Garder l'écran allumé pendant le martelage.",
        "Vibration" to "Vibrer brièvement à chaque comptage.",
        "Son de clic" to "Émettre un son au comptage.",
        "Annonce vocale" to "Lire le nombre / l'étiquette à voix haute.",
        "Boutons de volume" to "Compter avec les boutons de volume.",
        "Thème sombre" to "Forcer le thème sombre.",
        "Langue" to "Choisir la langue de l'application.",
        "Export / Import" to "Sauvegarde ZIP, export CSV, et déverrouillage des contextes exportés.",
    )

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
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Réglages à venir (Tranche 3). Cet écran prépare le câblage des paramètres.",
                style = MaterialTheme.typography.bodyMedium,
            )
            HorizontalDivider()
            prevus.forEach { (titre, desc) ->
                Column {
                    Text(titre, style = MaterialTheme.typography.titleMedium)
                    Text(desc, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
