package io.github.pobsteta.marculus.ui.contextes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.content.res.Configuration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.marculus.core.Referentiels
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.ui.libelle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationContexteScreen(
    repository: MartelageRepository,
    contexteExistant: Contexte?,
    essencesReferentiel: List<String>,
    onAnnuler: () -> Unit,
    onEnregistre: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val edition = contexteExistant != null

    var nom by remember { mutableStateOf(contexteExistant?.nom ?: "") }
    var commentaire by remember { mutableStateOf(contexteExistant?.commentaire ?: "") }
    var mode by remember { mutableStateOf(contexteExistant?.mode ?: ModeMesure.DIAMETRE) }
    var min by remember { mutableStateOf(contexteExistant?.axe?.min?.toString() ?: "20") }
    var max by remember { mutableStateOf(contexteExistant?.axe?.max?.toString() ?: "90") }
    var pas by remember { mutableStateOf(contexteExistant?.axe?.pas?.toString() ?: "5") }
    var increment by remember { mutableStateOf(contexteExistant?.increment?.toString() ?: "1") }

    val essencesDisponibles = remember {
        mutableStateListOf<String>().apply {
            val base = essencesReferentiel.toMutableList()
            contexteExistant?.essencesNoms?.forEach { if (it !in base) base.add(it) }
            addAll(base)
        }
    }
    val selection = remember {
        mutableStateListOf<String>().apply {
            addAll(contexteExistant?.essencesNoms ?: essencesReferentiel)
        }
    }
    val couleurs = remember {
        mutableStateMapOf<String, Pair<Int, Int>>().apply {
            contexteExistant?.essences?.forEach { put(it.nom, it.couleurFondArgb to it.couleurTexteArgb) }
        }
    }
    // Fond distinct par défaut selon la position de l'essence ; texte blanc.
    fun fond(e: String) = couleurs[e]?.first ?: Referentiels.couleurFondDefaut(essencesDisponibles.indexOf(e))
    fun texte(e: String) = couleurs[e]?.second ?: Referentiels.COULEUR_TEXTE_DEFAUT

    var erreur by remember { mutableStateOf<String?>(null) }
    var picker by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // (essence, true=fond)
    var ajoutOuvert by remember { mutableStateOf(false) }
    var infoOuvert by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (edition) "Modifier le contexte" else "Nouveau contexte") },
                navigationIcon = {
                    IconButton(onClick = onAnnuler) {
                        Icon(Icons.Filled.Close, contentDescription = "Annuler")
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
            OutlinedTextField(
                value = commentaire,
                onValueChange = { commentaire = it },
                label = { Text("Commentaire") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Mode de mesure", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeMesure.entries.forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.libelle()) })
                }
            }

            val paysage = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChampNombre(if (paysage) "Minimum" else "Min", min, { min = it }, Modifier.weight(1f))
                ChampNombre(if (paysage) "Maximum" else "Max", max, { max = it }, Modifier.weight(1f))
                ChampNombre(if (paysage) "Par pas de" else "Pas", pas, { pas = it }, Modifier.weight(1f))
                ChampNombre(if (paysage) "Incrément" else "Inc", increment, { increment = it }, Modifier.weight(1f))
            }

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Essences (colonnes) et couleurs",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { infoOuvert = true }) {
                    Icon(Icons.Filled.Info, contentDescription = "Informations sur les couleurs")
                }
            }
            essencesDisponibles.forEach { essence ->
                LigneEssence(
                    nom = essence,
                    coche = essence in selection,
                    fond = Color(fond(essence)),
                    texte = Color(texte(essence)),
                    onToggle = { if (essence in selection) selection.remove(essence) else selection.add(essence) },
                    onPickFond = { picker = essence to true },
                    onPickTexte = { picker = essence to false },
                )
            }
            OutlinedButton(onClick = { ajoutOuvert = true }, modifier = Modifier.fillMaxWidth()) {
                Text("+ Ajouter une essence")
            }

            erreur?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAnnuler, modifier = Modifier.weight(1f)) { Text("Annuler") }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val mi = min.toIntOrNull()
                        val ma = max.toIntOrNull()
                        val pa = pas.toIntOrNull()
                        val inc = increment.toIntOrNull()
                        erreur = when {
                            nom.isBlank() -> "Le nom est obligatoire"
                            mi == null || ma == null || pa == null -> "Min, max et pas doivent être des nombres"
                            pa <= 0 -> "Le pas doit être positif"
                            mi > ma -> "Min doit être inférieur ou égal à max"
                            inc == null || inc <= 0 -> "L'incrément doit être un entier positif"
                            selection.isEmpty() -> "Sélectionnez au moins une essence"
                            else -> {
                                val essences = essencesDisponibles
                                    .filter { it in selection }
                                    .map { EssenceColonne(it, fond(it), texte(it)) }
                                val commentaireFinal = commentaire.trim().ifBlank { null }
                                scope.launch {
                                    if (edition) {
                                        repository.modifierContexte(
                                            contexteExistant!!.id, nom.trim(), mode,
                                            AxeClasses(mi, ma, pa), essences, commentaireFinal, inc,
                                        )
                                        onEnregistre(contexteExistant.id)
                                    } else {
                                        val id = repository.creerContexte(
                                            nom.trim(), mode, AxeClasses(mi, ma, pa),
                                            essences, commentaireFinal, inc,
                                        )
                                        onEnregistre(id)
                                    }
                                }
                                null
                            }
                        }
                    },
                ) { Text(if (edition) "Enregistrer" else "Créer") }
            }
        }
    }

    picker?.let { (essence, estFond) ->
        SelecteurCouleurDialog(
            onAnnuler = { picker = null },
            onChoisir = { couleur ->
                couleurs[essence] = if (estFond) couleur to texte(essence) else fond(essence) to couleur
                picker = null
            },
        )
    }

    if (ajoutOuvert) {
        AjoutEssenceDialog(
            onAnnuler = { ajoutOuvert = false },
            onAjouter = { nouvelle ->
                if (nouvelle.isNotBlank() && nouvelle !in essencesDisponibles) {
                    essencesDisponibles.add(nouvelle)
                    selection.add(nouvelle)
                }
                ajoutOuvert = false
            },
        )
    }

    if (infoOuvert) {
        InfoCouleursDialog(onFermer = { infoOuvert = false })
    }
}

private val RAISONS_COULEURS = mapOf(
    "Chêne" to "Chênes décidus, large amplitude tempérée.",
    "Hêtre" to "Mésophile, atlantique-montagnard, frais.",
    "Autres feuillus" to "Catégorie ouverte des feuillus (bleu-gris neutre).",
    "Sapin" to "Sapin/épicéa montagnards humides, sciaphiles.",
    "Épicéa" to "Résineux ; teinte distincte du sapin (déviation assumée).",
    "Autres résineux" to "Catégorie ouverte des conifères (gris-brun).",
)

@Composable
private fun InfoCouleursDialog(onFermer: () -> Unit) {
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Couleurs des essences") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Couleurs dérivées du référentiel BD Forêt® V2. Teinte = famille botanique ; " +
                        "nuance = gradient écologique (du mésophile humide vers le thermo-xérique).",
                    style = MaterialTheme.typography.bodySmall,
                )
                Referentiels.ESSENCES_DEFAUT.forEachIndexed { i, nom ->
                    val argb = Referentiels.couleurFondDefaut(i)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier.size(28.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .background(Color(argb)),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "$nom — ${"#%06X".format(0xFFFFFF and argb)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            RAISONS_COULEURS[nom]?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onFermer) { Text("Fermer") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChampNombre(label: String, valeur: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = valeur,
        onValueChange = { saisie -> onChange(saisie.filter { it.isDigit() }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun LigneEssence(
    nom: String,
    coche: Boolean,
    fond: Color,
    texte: Color,
    onToggle: () -> Unit,
    onPickFond: () -> Unit,
    onPickTexte: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = coche, onCheckedChange = { onToggle() })
        Text(nom, modifier = Modifier.weight(1f))
        // Aperçu : fond seul, puis fond + lettre dans la couleur du texte.
        Echantillon(couleur = fond, onClick = onPickFond)
        EchantillonTexte(fond = fond, texte = texte, onClick = onPickTexte)
    }
}

@Composable
private fun Echantillon(couleur: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .background(couleur)
            .clickable { onClick() },
    )
}

@Composable
private fun EchantillonTexte(fond: Color, texte: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .background(fond)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text("A", color = texte, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SelecteurCouleurDialog(onAnnuler: () -> Unit, onChoisir: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Choisir une couleur") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Referentiels.PALETTE.chunked(5).forEach { ligne ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ligne.forEach { argb ->
                            Echantillon(couleur = Color(argb), onClick = { onChoisir(argb) })
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onAnnuler) { Text("Fermer") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AjoutEssenceDialog(onAnnuler: () -> Unit, onAjouter: (String) -> Unit) {
    var nom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Ajouter une essence") },
        text = {
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it },
                label = { Text("Nom de l'essence") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onAjouter(nom.trim()) }) { Text("Ajouter") } },
        dismissButton = { TextButton(onClick = onAnnuler) { Text("Annuler") } },
    )
}
