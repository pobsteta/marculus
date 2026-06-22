package io.github.pobsteta.marculus.ui.contextes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.marculus.core.Referentiels
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import io.github.pobsteta.marculus.R
import io.github.pobsteta.marculus.data.GpkgRepository
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.ui.libelle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreationContexteScreen(
    repository: MartelageRepository,
    gpkgRepository: GpkgRepository,
    contexteExistant: Contexte?,
    essencesReferentiel: List<String>,
    onAnnuler: () -> Unit,
    onEnregistre: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val edition = contexteExistant != null

    var nom by remember { mutableStateOf(contexteExistant?.nom ?: "") }
    var commentaire by remember { mutableStateOf(contexteExistant?.commentaire ?: "") }
    var mode by remember { mutableStateOf(contexteExistant?.mode ?: ModeMesure.DIAMETRE) }
    var min by remember { mutableStateOf(contexteExistant?.axe?.min?.toString() ?: "20") }
    var max by remember { mutableStateOf(contexteExistant?.axe?.max?.toString() ?: "90") }
    var pas by remember { mutableStateOf(contexteExistant?.axe?.pas?.toString() ?: "5") }
    var increment by remember { mutableStateOf(contexteExistant?.increment?.toString() ?: "1") }
    var tarif by remember { mutableStateOf(contexteExistant?.tarif ?: TarifCubage.AUCUN) }
    var tarifNumero by remember {
        mutableStateOf(contexteExistant?.tarifNumero?.takeIf { it > 0 }?.toString() ?: "")
    }

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
    fun fond(e: String) = couleurs[e]?.first ?: Referentiels.couleurFondDefaut(essencesDisponibles.indexOf(e))
    fun texte(e: String) = couleurs[e]?.second ?: Referentiels.COULEUR_TEXTE_DEFAUT

    var erreur by remember { mutableStateOf<String?>(null) }
    var picker by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var ajoutOuvert by remember { mutableStateOf(false) }
    var infoOuvert by remember { mutableStateOf(false) }
    var cheminGpkg by remember { mutableStateOf(contexteExistant?.cheminGpkg) }
    val gpkgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch { cheminGpkg = withContext(Dispatchers.IO) { gpkgRepository.importer(uri) } }
        }
    }

    // Messages d'erreur (résolus hors composition, dans le onClick).
    val errNom = stringResource(R.string.creation_erreur_nom_obligatoire)
    val errNombres = stringResource(R.string.creation_erreur_nombres_invalides)
    val errPas = stringResource(R.string.creation_erreur_pas_positif)
    val errMinMax = stringResource(R.string.creation_erreur_min_max)
    val errInc = stringResource(R.string.creation_erreur_increment)
    val errEssence = stringResource(R.string.creation_erreur_essence_requise)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (edition) R.string.creation_titre_modifier else R.string.creation_titre_nouveau))
                },
                navigationIcon = {
                    IconButton(onClick = onAnnuler) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.creation_action_annuler))
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
                label = { Text(stringResource(R.string.creation_label_nom)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = commentaire,
                onValueChange = { commentaire = it },
                label = { Text(stringResource(R.string.creation_label_commentaire)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.creation_section_mode_mesure), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeMesure.entries.forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.libelle()) })
                }
            }

            val paysage = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChampNombre(
                    stringResource(if (paysage) R.string.creation_champ_minimum_long else R.string.creation_champ_minimum_court),
                    min, { min = it }, Modifier.weight(1f),
                )
                ChampNombre(
                    stringResource(if (paysage) R.string.creation_champ_maximum_long else R.string.creation_champ_maximum_court),
                    max, { max = it }, Modifier.weight(1f),
                )
                ChampNombre(
                    stringResource(if (paysage) R.string.creation_champ_pas_long else R.string.creation_champ_pas_court),
                    pas, { pas = it }, Modifier.weight(1f),
                )
                ChampNombre(
                    stringResource(if (paysage) R.string.creation_champ_increment_long else R.string.creation_champ_increment_court),
                    increment, { increment = it }, Modifier.weight(1f),
                )
            }

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.creation_section_essences),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { infoOuvert = true }) {
                    Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.creation_cd_info_couleurs))
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
                Text(stringResource(R.string.creation_btn_ajouter_essence))
            }

            HorizontalDivider()
            Text(stringResource(R.string.creation_section_fond_carto), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.creation_fond_carto_description), style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { gpkgLauncher.launch(arrayOf("*/*")) }) {
                    Text(stringResource(if (cheminGpkg == null) R.string.creation_btn_choisir_gpkg else R.string.creation_btn_remplacer_gpkg))
                }
                Text(
                    cheminGpkg?.substringAfterLast('/') ?: stringResource(R.string.creation_gpkg_aucun_fichier),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                if (cheminGpkg != null) {
                    TextButton(onClick = { cheminGpkg = null }) { Text(stringResource(R.string.creation_btn_retirer_gpkg)) }
                }
            }

            HorizontalDivider()
            Text(stringResource(R.string.creation_tarif_section), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.creation_tarif_aide), style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = tarif == TarifCubage.AUCUN, onClick = { tarif = TarifCubage.AUCUN }, label = { Text(stringResource(R.string.creation_tarif_aucun)) })
                FilterChip(selected = tarif == TarifCubage.SCHAEFFER_RAPIDE, onClick = { tarif = TarifCubage.SCHAEFFER_RAPIDE }, label = { Text(stringResource(R.string.creation_tarif_rapide)) })
                FilterChip(selected = tarif == TarifCubage.SCHAEFFER_LENT, onClick = { tarif = TarifCubage.SCHAEFFER_LENT }, label = { Text(stringResource(R.string.creation_tarif_lent)) })
                FilterChip(selected = tarif == TarifCubage.EMERGE, onClick = { tarif = TarifCubage.EMERGE }, label = { Text(stringResource(R.string.creation_tarif_emerge)) })
            }
            if (tarif == TarifCubage.SCHAEFFER_RAPIDE || tarif == TarifCubage.SCHAEFFER_LENT) {
                OutlinedTextField(
                    value = tarifNumero,
                    onValueChange = { saisie -> tarifNumero = saisie.filter { it.isDigit() }.take(2) },
                    label = { Text(stringResource(R.string.creation_tarif_numero)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            erreur?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAnnuler, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.creation_action_annuler))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val mi = min.toIntOrNull()
                        val ma = max.toIntOrNull()
                        val pa = pas.toIntOrNull()
                        val inc = increment.toIntOrNull()
                        erreur = when {
                            nom.isBlank() -> errNom
                            mi == null || ma == null || pa == null -> errNombres
                            pa <= 0 -> errPas
                            mi > ma -> errMinMax
                            inc == null || inc <= 0 -> errInc
                            selection.isEmpty() -> errEssence
                            else -> {
                                val essences = essencesDisponibles
                                    .filter { it in selection }
                                    .map { EssenceColonne(it, fond(it), texte(it)) }
                                val commentaireFinal = commentaire.trim().ifBlank { null }
                                val numero = tarifNumero.toIntOrNull() ?: 0
                                scope.launch {
                                    if (edition) {
                                        repository.modifierContexte(
                                            contexteExistant!!.id, nom.trim(), mode,
                                            AxeClasses(mi, ma, pa), essences, commentaireFinal, inc, cheminGpkg, tarif, numero,
                                        )
                                        onEnregistre(contexteExistant.id)
                                    } else {
                                        val id = repository.creerContexte(
                                            nom.trim(), mode, AxeClasses(mi, ma, pa),
                                            essences, commentaireFinal, inc,
                                            cheminGpkg = cheminGpkg, tarif = tarif, tarifNumero = numero,
                                        )
                                        onEnregistre(id)
                                    }
                                }
                                null
                            }
                        }
                    },
                ) { Text(stringResource(if (edition) R.string.creation_btn_enregistrer else R.string.creation_btn_creer)) }
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

/** Description écologique d'une essence par défaut (BD Forêt V2), ou null. */
@Composable
private fun raisonCouleur(nom: String): String? = when (nom) {
    "Chêne" -> stringResource(R.string.creation_raison_chene)
    "Hêtre" -> stringResource(R.string.creation_raison_hetre)
    "Autres feuillus" -> stringResource(R.string.creation_raison_autres_feuillus)
    "Sapin" -> stringResource(R.string.creation_raison_sapin)
    "Épicéa" -> stringResource(R.string.creation_raison_epicea)
    "Autres résineux" -> stringResource(R.string.creation_raison_autres_resineux)
    else -> null
}

@Composable
private fun InfoCouleursDialog(onFermer: () -> Unit) {
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.creation_info_couleurs_titre)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.creation_info_couleurs_corps), style = MaterialTheme.typography.bodySmall)
                Referentiels.ESSENCES_DEFAUT.forEachIndexed { i, nom ->
                    val argb = Referentiels.couleurFondDefaut(i)
                    val raison = raisonCouleur(nom)
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
                            Text("$nom — ${"#%06X".format(0xFFFFFF and argb)}", style = MaterialTheme.typography.bodyMedium)
                            raison?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onFermer) { Text(stringResource(R.string.creation_btn_fermer)) } },
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
        title = { Text(stringResource(R.string.creation_selecteur_couleur_titre)) },
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
        dismissButton = { TextButton(onClick = onAnnuler) { Text(stringResource(R.string.creation_btn_fermer)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AjoutEssenceDialog(onAnnuler: () -> Unit, onAjouter: (String) -> Unit) {
    var nom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text(stringResource(R.string.creation_dialog_ajout_essence_titre)) },
        text = {
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it },
                label = { Text(stringResource(R.string.creation_dialog_ajout_essence_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onAjouter(nom.trim()) }) { Text(stringResource(R.string.creation_btn_ajouter)) } },
        dismissButton = { TextButton(onClick = onAnnuler) { Text(stringResource(R.string.creation_action_annuler)) } },
    )
}
