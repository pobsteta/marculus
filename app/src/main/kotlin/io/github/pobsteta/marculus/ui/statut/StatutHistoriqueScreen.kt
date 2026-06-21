package io.github.pobsteta.marculus.ui.statut

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.Tige
import io.github.pobsteta.marculus.data.MartelageRepository
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatutHistoriqueScreen(
    repository: MartelageRepository,
    contexteId: String,
    onRetour: () -> Unit,
) {
    val contexte by produceState<Contexte?>(initialValue = null, contexteId) {
        value = repository.contexte(contexteId)
    }
    val totaux by repository.totaux(contexteId).collectAsStateWithLifecycle(emptyMap())
    val journal by repository.journal(contexteId).collectAsStateWithLifecycle(emptyList())
    var onglet by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contexte?.nom ?: "Statut") },
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
        val ctx = contexte
        if (ctx == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = onglet) {
                Tab(selected = onglet == 0, onClick = { onglet = 0 }, text = { Text("Statut") })
                Tab(selected = onglet == 1, onClick = { onglet = 1 }, text = { Text("Historique détaillé") })
            }
            when (onglet) {
                0 -> OngletStatut(ctx, totaux)
                else -> OngletHistorique(ctx, journal)
            }
        }
    }
}

@Composable
private fun OngletStatut(contexte: Contexte, totaux: Map<CompteurCle, Int>) {
    val couleurs = contexte.essences.associate { it.nom to it.couleurFondArgb }
    // Total par essence (pour le donut), dans l'ordre des colonnes.
    val parEssence = contexte.essencesNoms.map { nom ->
        nom to contexte.axe.classes().sumOf { c -> totaux[CompteurCle(nom, c)] ?: 0 }
    }
    val total = parEssence.sumOf { it.second }
    // Cellules non nulles (pour les barres), triées par valeur décroissante.
    val cellules = totaux.filterValues { it > 0 }.entries.sortedByDescending { it.value }
    val maxCellule = cellules.maxOfOrNull { it.value } ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            if (total == 0) {
                Text("Aucune tige enregistrée.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(contentAlignment = Alignment.Center) {
                        Donut(parEssence.filter { it.second > 0 }, couleurs, total)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$total", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("tiges", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        item {
            // Récap par essence.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                parEssence.forEach { (nom, n) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(14.dp).background(Color(couleurs[nom] ?: 0xFF888888.toInt())))
                        Text(nom, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("$n", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (cellules.isNotEmpty()) {
            item { HorizontalDivider() }
            item { Text("Détail par classe", style = MaterialTheme.typography.titleSmall) }
            items(cellules) { (cle, valeur) ->
                BarreCellule(
                    libelle = "${cle.essence} ${cle.classe}",
                    valeur = valeur,
                    fraction = if (maxCellule > 0) valeur.toFloat() / maxCellule else 0f,
                    couleur = Color(couleurs[cle.essence] ?: 0xFF888888.toInt()),
                )
            }
        }
    }
}

@Composable
private fun Donut(parEssence: List<Pair<String, Int>>, couleurs: Map<String, Int>, total: Int) {
    Canvas(Modifier.size(200.dp)) {
        val strokeW = 44.dp.toPx()
        val d = size.minDimension - strokeW
        val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
        var start = -90f
        parEssence.forEach { (nom, n) ->
            val sweep = 360f * n / total
            drawArc(
                color = Color(couleurs[nom] ?: 0xFF888888.toInt()),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(d, d),
                style = Stroke(width = strokeW),
            )
            start += sweep
        }
    }
}

@Composable
private fun BarreCellule(libelle: String, valeur: Int, fraction: Float, couleur: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(libelle, modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodySmall)
        Box(
            Modifier.weight(1f).height(20.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(20.dp).background(couleur))
        }
        Text("$valeur", modifier = Modifier.width(36.dp), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OngletHistorique(contexte: Contexte, journal: List<Tige>) {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", locale) }
    val evenements = journal.sortedByDescending { it.horodatage }

    if (evenements.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Aucun événement.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        items(evenements) { tige ->
            val signe = if (tige.action == ActionTige.PLUS) "+" else "−"
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$signe${tige.quantite}",
                        modifier = Modifier.width(48.dp),
                        fontWeight = FontWeight.Bold,
                        color = if (tige.action == ActionTige.PLUS) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Column(Modifier.weight(1f)) {
                        Text("${tige.essence} ${tige.classe}", style = MaterialTheme.typography.bodyMedium)
                        val details = buildList {
                            tige.hauteurTexte?.takeIf { it.isNotBlank() }?.let { add("h $it") }
                            tige.qualiteArbre?.let { add(it) }
                        }
                        if (details.isNotEmpty()) {
                            Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(format.format(Date(tige.horodatage)), style = MaterialTheme.typography.labelSmall)
                }
                HorizontalDivider(Modifier.padding(top = 6.dp))
            }
        }
    }
}
