package io.github.pobsteta.marculus.ui.feuille

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.Referentiels
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.ModeMesure
import io.github.pobsteta.marculus.data.MartelageRepository
import kotlinx.coroutines.launch

private val LARGEUR_CELLULE = 120.dp
private val HAUTEUR_CELLULE = 128.dp
private val LARGEUR_LABEL = 64.dp
private val HAUTEUR_ENTETE = 48.dp

private sealed interface Saisie {
    data class Hauteur(val essence: String, val classe: Int) : Saisie
    data class Qualite(val essence: String, val classe: Int) : Saisie
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeuilleMartelageScreen(
    repository: MartelageRepository,
    contexteId: String,
    onRetour: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val contexte by produceState<Contexte?>(initialValue = null, contexteId) {
        value = repository.contexte(contexteId)
    }
    val totaux by repository.totaux(contexteId).collectAsStateWithLifecycle(emptyMap())
    var saisie by remember { mutableStateOf<Saisie?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contexte?.nom ?: "Feuille de martelage") },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
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

        val classes = ctx.axe.classes()
        val essences = ctx.essencesActives
        val uniteLabel = if (ctx.mode == ModeMesure.DIAMETRE) "Ø cm" else "C cm"

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
        ) {
            // En-tête : coin + une colonne par essence.
            Row {
                Entete(uniteLabel, LARGEUR_LABEL, HAUTEUR_ENTETE)
                essences.forEach { essence ->
                    Entete(essence, LARGEUR_CELLULE, HAUTEUR_ENTETE)
                }
            }
            // Une ligne par classe.
            classes.forEach { classe ->
                Row {
                    Entete("$classe", LARGEUR_LABEL, HAUTEUR_CELLULE)
                    essences.forEach { essence ->
                        CelluleCompteur(
                            total = totaux[CompteurCle(essence, classe)] ?: 0,
                            onPlus = { scope.launch { repository.ajouterTige(contexteId, essence, classe) } },
                            onMoins = { scope.launch { repository.annulerTige(contexteId, essence, classe) } },
                            onHauteur = { saisie = Saisie.Hauteur(essence, classe) },
                            onQualite = { saisie = Saisie.Qualite(essence, classe) },
                        )
                    }
                }
            }
        }
    }

    when (val s = saisie) {
        is Saisie.Hauteur -> SaisieHauteurDialog(
            onAnnuler = { saisie = null },
            onValider = { texte ->
                scope.launch {
                    repository.annoterDerniere(contexteId, s.essence, s.classe, hauteurTexte = texte, qualiteArbre = null)
                }
                saisie = null
            },
        )

        is Saisie.Qualite -> ChoixQualiteDialog(
            onAnnuler = { saisie = null },
            onChoisir = { qualite ->
                scope.launch {
                    repository.annoterDerniere(contexteId, s.essence, s.classe, hauteurTexte = null, qualiteArbre = qualite)
                }
                saisie = null
            },
        )

        null -> Unit
    }
}

@Composable
private fun Entete(texte: String, largeur: androidx.compose.ui.unit.Dp, hauteur: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier.width(largeur).height(hauteur).padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texte,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CelluleCompteur(
    total: Int,
    onPlus: () -> Unit,
    onMoins: () -> Unit,
    onHauteur: () -> Unit,
    onQualite: () -> Unit,
) {
    Card(
        modifier = Modifier.width(LARGEUR_CELLULE).height(HAUTEUR_CELLULE).padding(2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("$total", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onMoins, modifier = Modifier.size(36.dp)) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = onPlus, modifier = Modifier.size(36.dp)) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onHauteur) { Text("H") }
                TextButton(onClick = onQualite) { Text("Q") }
            }
        }
    }
}

@Composable
private fun SaisieHauteurDialog(onAnnuler: () -> Unit, onValider: (String) -> Unit) {
    var texte by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Hauteur de la dernière tige") },
        text = {
            Column {
                Text("Format : hauteur, puis « - », puis découpe. Ex. 27-6AB4CD")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onValider(texte) }) { Text("Valider") } },
        dismissButton = { TextButton(onClick = onAnnuler) { Text("Annuler") } },
    )
}

@Composable
private fun ChoixQualiteDialog(onAnnuler: () -> Unit, onChoisir: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Qualité de l'arbre") },
        text = {
            Column {
                Referentiels.QUALITE_ARBRE_DEFAUT.forEach { qualite ->
                    Text(
                        qualite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChoisir(qualite) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onAnnuler) { Text("Fermer") } },
    )
}
