package io.github.pobsteta.marculus.ui.feuille

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.Referentiels
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.Contexte
import io.github.pobsteta.marculus.data.MartelageRepository
import kotlinx.coroutines.launch

private val LARGEUR_CELLULE = 140.dp
private val HAUTEUR_CELLULE = 144.dp

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
    val configs by repository.configs(contexteId).collectAsStateWithLifecycle(emptyMap())
    var saisie by remember { mutableStateOf<Saisie?>(null) }
    var menuReset by remember { mutableStateOf(false) }
    var confirmerReset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contexte?.nom ?: "Feuille de martelage") },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuReset = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(expanded = menuReset, onDismissRequest = { menuReset = false }) {
                            DropdownMenuItem(
                                text = { Text("Réinitialiser la fiche à zéro") },
                                onClick = { menuReset = false; confirmerReset = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
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

        // Plus d'en-tête ni de colonne de classes : chaque cellule porte son libellé.
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
        ) {
            classes.forEach { classe ->
                Row {
                    ctx.essences.forEach { e ->
                        val cle = CompteurCle(e.nom, classe)
                        val total = totaux[cle] ?: 0
                        val cfg = configs[cle]
                        CelluleCompteur(
                            libelle = "${e.nom} $classe",
                            total = total,
                            fond = Color(e.couleurFondArgb),
                            texte = Color(e.couleurTexteArgb),
                            alerteMoins = cfg?.alerteMoins(total) ?: false,
                            alertePlus = cfg?.alertePlus(total) ?: false,
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
private fun CelluleCompteur(
    libelle: String,
    total: Int,
    fond: Color,
    texte: Color,
    alerteMoins: Boolean,
    alertePlus: Boolean,
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
            // Haut : H — libellé (essence + classe) — Q.
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onHauteur, modifier = Modifier.size(32.dp)) {
                    Text("H", color = texte, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    libelle,
                    color = texte,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                )
                IconButton(onClick = onQualite, modifier = Modifier.size(32.dp)) {
                    Text("Q", color = texte, style = MaterialTheme.typography.titleMedium)
                }
            }
            // Centre : la valeur encadrée des alertes (⚠− à gauche, ⚠+ à droite), menu ⋮ en coin.
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (alerteMoins) TriangleAlerte("−")
                    Text("$total", color = texte, style = MaterialTheme.typography.headlineMedium)
                    if (alertePlus) TriangleAlerte("+")
                }
                Box(Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Avis", tint = texte)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Avis (si + / si −)") }, onClick = { menu = false; onAvis() })
                    }
                }
            }
            // Bas : − et + (zones les plus faciles à atteindre).
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onMoins,
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text("−", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onPlus,
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

/** Petit triangle rouge avec un signe blanc (+ ou −) au centre, indicateur d'alerte. */
@Composable
private fun TriangleAlerte(signe: String) {
    Box(Modifier.size(22.dp), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.matchParentSize()) {
            val triangle = Path().apply {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(triangle, color = Color(0xFFD32F2F))
        }
        Text(
            signe,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 1.dp),
        )
    }
}

@Composable
private fun SaisieHauteurDialog(onAnnuler: () -> Unit, onValider: (String) -> Unit) {
    var hauteur by remember { mutableStateOf("") }
    var decoupe by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Hauteur de la dernière tige") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hauteur,
                    // Chiffres + un séparateur décimal (virgule ou point) : accepte 27, 27.5 ou 27,5.
                    onValueChange = { v -> hauteur = v.filter { it.isDigit() || it == '.' || it == ',' } },
                    label = { Text("Hauteur (m)") },
                    placeholder = { Text("ex. 27,5") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = decoupe,
                    onValueChange = { decoupe = it },
                    label = { Text("Découpe / qualités bois") },
                    placeholder = { Text("ex. 6AB4CD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "La découpe associe longueurs et qualités bois (ex. 6 m de AB, 4 m de CD).",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = hauteur.trim()
                val d = decoupe.trim()
                onValider(if (d.isNotBlank()) "$h-$d" else h)
            }) { Text("Valider") }
        },
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
                    TextButton(
                        onClick = { onChoisir(qualite) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(qualite, style = MaterialTheme.typography.bodyLarge) }
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
        avisPlus = config.avisSiPlus?.toString() ?: ""
        avisMoins = config.avisSiMoins?.toString() ?: ""
    }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Avis — $essence $classe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Seuils indicatifs : une alerte s'affiche tant que le total sort de l'intervalle. " +
                        "Purement informatif, sans blocage.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = avisMoins,
                    onValueChange = { v -> avisMoins = v.filter { it.isDigit() } },
                    label = { Text("Avis si − (minimum)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = avisPlus,
                    onValueChange = { v -> avisPlus = v.filter { it.isDigit() } },
                    label = { Text("Avis si + (maximum)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    repository.definirAvis(
                        contexteId, essence, classe,
                        avisPlus.toIntOrNull(), avisMoins.toIntOrNull(),
                    )
                    onFermer()
                }
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } },
    )
}
