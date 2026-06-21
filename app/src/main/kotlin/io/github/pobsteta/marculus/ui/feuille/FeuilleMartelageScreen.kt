package io.github.pobsteta.marculus.ui.feuille

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.Referentiels
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import io.github.pobsteta.marculus.data.MartelageRepository
import kotlinx.coroutines.launch

private val LARGEUR_CELLULE = 120.dp
private val HAUTEUR_CELLULE = 132.dp
private val LARGEUR_LABEL = 56.dp
private val HAUTEUR_ENTETE = 56.dp

private sealed interface Saisie {
    data class Hauteur(val essence: String, val classe: Int) : Saisie
    data class Qualite(val essence: String, val classe: Int) : Saisie
    data class Avis(val essence: String, val classe: Int) : Saisie
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
    var menuReset by remember { mutableStateOf(false) }
    var confirmerReset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contexte?.nom ?: "Feuille de martelage") },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onRetour) {
                            Text("←", style = MaterialTheme.typography.titleLarge)
                        }
                        Box {
                            IconButton(onClick = { menuReset = true }) {
                                Text("⋮", style = MaterialTheme.typography.titleLarge)
                            }
                            DropdownMenu(expanded = menuReset, onDismissRequest = { menuReset = false }) {
                                DropdownMenuItem(
                                    text = { Text("Réinitialiser la fiche à zéro") },
                                    onClick = { menuReset = false; confirmerReset = true },
                                )
                            }
                        }
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
        val uniteLabel = if (ctx.mode == ModeMesure.DIAMETRE) "Ø" else "C"

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
        ) {
            Row {
                Entete(uniteLabel, LARGEUR_LABEL, HAUTEUR_ENTETE)
                ctx.essences.forEach { e ->
                    Entete(e.nom, LARGEUR_CELLULE, HAUTEUR_ENTETE, Color(e.couleurFondArgb), Color(e.couleurTexteArgb))
                }
            }
            classes.forEach { classe ->
                Row {
                    Entete("$classe", LARGEUR_LABEL, HAUTEUR_CELLULE)
                    ctx.essences.forEach { e ->
                        val total = totaux[CompteurCle(e.nom, classe)] ?: 0
                        CelluleCompteur(
                            total = total,
                            fond = Color(e.couleurFondArgb),
                            texte = Color(e.couleurTexteArgb),
                            onPlus = {
                                scope.launch { repository.ajouterTige(contexteId, e.nom, classe, quantite = ctx.increment) }
                            },
                            onMoins = {
                                if (total > 0) {
                                    val q = minOf(ctx.increment, total) // jamais en dessous de zéro
                                    scope.launch { repository.annulerTige(contexteId, e.nom, classe, quantite = q) }
                                }
                            },
                            onHauteur = { saisie = Saisie.Hauteur(e.nom, classe) },
                            onQualite = { saisie = Saisie.Qualite(e.nom, classe) },
                            onAvis = { saisie = Saisie.Avis(e.nom, classe) },
                        )
                    }
                }
            }
        }
    }

    if (confirmerReset) {
        AlertDialog(
            onDismissRequest = { confirmerReset = false },
            title = { Text("Réinitialiser la fiche ?") },
            text = { Text("Tous les compteurs reviennent à zéro. L'historique est conservé (annulations enregistrées).") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.reinitialiser(contexteId) }
                    confirmerReset = false
                }) { Text("Réinitialiser") }
            },
            dismissButton = { TextButton(onClick = { confirmerReset = false }) { Text("Annuler") } },
        )
    }

    when (val s = saisie) {
        is Saisie.Hauteur -> SaisieHauteurDialog(
            onAnnuler = { saisie = null },
            onValider = { texte ->
                scope.launch { repository.annoterDerniere(contexteId, s.essence, s.classe, hauteurTexte = texte, qualiteArbre = null) }
                saisie = null
            },
        )

        is Saisie.Qualite -> ChoixQualiteDialog(
            onAnnuler = { saisie = null },
            onChoisir = { qualite ->
                scope.launch { repository.annoterDerniere(contexteId, s.essence, s.classe, hauteurTexte = null, qualiteArbre = qualite) }
                saisie = null
            },
        )

        is Saisie.Avis -> AvisDialog(
            repository = repository,
            contexteId = contexteId,
            essence = s.essence,
            classe = s.classe,
            onFermer = { saisie = null },
        )

        null -> Unit
    }
}

@Composable
private fun Entete(
    texte: String,
    largeur: androidx.compose.ui.unit.Dp,
    hauteur: androidx.compose.ui.unit.Dp,
    fond: Color? = null,
    couleurTexte: Color? = null,
) {
    Box(
        modifier = Modifier.width(largeur).height(hauteur).padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (fond != null) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = fond), modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            texte,
                            color = couleurTexte ?: MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(2.dp),
                        )
                    }
                }
            }
        } else {
            Text(texte, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CelluleCompteur(
    total: Int,
    fond: Color,
    texte: Color,
    onPlus: () -> Unit,
    onMoins: () -> Unit,
    onHauteur: () -> Unit,
    onQualite: () -> Unit,
    onAvis: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.width(LARGEUR_CELLULE).height(HAUTEUR_CELLULE).padding(2.dp),
        colors = CardDefaults.cardColors(containerColor = fond),
    ) {
        Column(Modifier.fillMaxSize().padding(4.dp)) {
            // Haut : H, Q et le menu ⋮ (avis).
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onHauteur, contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
                    Text("H", color = texte)
                }
                TextButton(onClick = onQualite, contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
                    Text("Q", color = texte)
                }
                Box(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(28.dp)) {
                        Text("⋮", color = texte, style = MaterialTheme.typography.titleMedium)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Avis (si + / si −)") },
                            onClick = { menu = false; onAvis() },
                        )
                    }
                }
            }
            // Centre : la valeur.
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("$total", color = texte, style = MaterialTheme.typography.headlineMedium)
            }
            // Bas : − et + (zones les plus faciles à atteindre).
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onMoins,
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text("−", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onPlus,
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
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
                Box(Modifier.height(8.dp))
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
                            .padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(onClick = { onChoisir(qualite) }) { Text("Choisir « $qualite »") }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onAnnuler) { Text("Fermer") } },
    )
}

@Composable
private fun AvisDialog(
    repository: MartelageRepository,
    contexteId: String,
    essence: String,
    classe: Int,
    onFermer: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var avisPlus by remember { mutableStateOf("") }
    var avisMoins by remember { mutableStateOf("") }
    LaunchedEffect(essence, classe) {
        val config = repository.configCompteur(contexteId, essence, classe)
        avisPlus = config.avisSiPlus ?: ""
        avisMoins = config.avisSiMoins ?: ""
    }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Avis — $essence $classe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = avisPlus,
                    onValueChange = { avisPlus = it },
                    label = { Text("Avis si plus") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = avisMoins,
                    onValueChange = { avisMoins = it },
                    label = { Text("Avis si moins") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    repository.definirAvis(
                        contexteId, essence, classe,
                        avisPlus.trim().ifBlank { null },
                        avisMoins.trim().ifBlank { null },
                    )
                    onFermer()
                }
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } },
    )
}
