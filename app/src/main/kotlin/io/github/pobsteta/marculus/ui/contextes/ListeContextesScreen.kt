package io.github.pobsteta.marculus.ui.contextes

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.export.ExportCsv
import io.github.pobsteta.marculus.R
import fr.marculus.core.model.EtatKanban
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.data.ResumeContexte
import io.github.pobsteta.marculus.data.SauvegardeRepository
import io.github.pobsteta.marculus.ui.libelle
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Dépôt GitHub de Marculus (lien « À propos »). */
private const val URL_GITHUB = "https://github.com/pobsteta/marculus"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeContextesScreen(
    repository: MartelageRepository,
    sauvegardeRepository: SauvegardeRepository,
    operateur: String,
    vueKanban: Boolean = false,
    onCreer: () -> Unit,
    onOuvrir: (String) -> Unit,
    onModifier: (String) -> Unit,
    onParametres: () -> Unit,
    onReferentiels: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resumes by repository.resumes().collectAsStateWithLifecycle(emptyList())
    var aSupprimer by remember { mutableStateOf<ResumeContexte?>(null) }
    var aLire by remember { mutableStateOf<ResumeContexte?>(null) }
    var menuAppli by remember { mutableStateOf(false) }
    var aProposOuvert by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var recherche by remember { mutableStateOf("") }
    var modeKanban by remember { mutableStateOf(false) }
    // Glisser-déposer Kanban : carte tirée + position du doigt (coord. racine) + rectangles des colonnes.
    var dragResume by remember { mutableStateOf<ResumeContexte?>(null) }
    var dragPointer by remember { mutableStateOf(Offset.Zero) }
    val colonneRects = remember { mutableStateMapOf<Int, Rect>() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val id = pendingExport
        pendingExport = null
        if (uri != null && id != null) {
            scope.launch {
                val ctx = repository.contexte(id) ?: return@launch
                val journal = repository.journalInstantane(id)
                val csv = ExportCsv.contexteCsv(ctx, journal)
                context.contentResolver.openOutputStream(uri)?.use {
                    // BOM UTF-8 pour qu'Excel détecte l'encodage et affiche correctement les accents.
                    it.write("﻿$csv".toByteArray(Charsets.UTF_8))
                }
                repository.marquerExporte(id)
            }
        }
    }

    // Partage d'un contexte pour la synchro (fichier .marsync via le partage système).
    val titrePartage = stringResource(R.string.sync_partage_titre)
    fun partagerContexte(id: String, nom: String) {
        scope.launch {
            val json = sauvegardeRepository.exporterContexteJson(id)
            fun assainir(s: String) = s.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val nomCtx = assainir(nom).ifBlank { "contexte" }
            val op = assainir(operateur).take(16)
            val horodatage = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"))
            val fichier = File(context.cacheDir, "$nomCtx-$op-$horodatage.marsync")
            fichier.writeText(json)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichier)
            val envoi = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(envoi, titrePartage))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.liste_app_title)) },
                actions = {
                    if (vueKanban) {
                        IconButton(onClick = { modeKanban = !modeKanban }) {
                            Icon(
                                if (modeKanban) Icons.Filled.ViewList else Icons.Filled.ViewColumn,
                                contentDescription = stringResource(if (modeKanban) R.string.liste_vue_liste else R.string.liste_vue_kanban),
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { menuAppli = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.liste_menu_content_desc))
                        }
                        DropdownMenu(expanded = menuAppli, onDismissRequest = { menuAppli = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.liste_menu_referentiels)) },
                                onClick = { menuAppli = false; onReferentiels() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.liste_menu_parametres)) },
                                onClick = { menuAppli = false; onParametres() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.liste_menu_apropos)) },
                                onClick = { menuAppli = false; aProposOuvert = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreer) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.liste_fab_nouveau_contexte))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = recherche,
                onValueChange = { recherche = it },
                label = { Text(stringResource(R.string.liste_recherche)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            )
            val filtres = if (recherche.isBlank()) resumes else resumes.filter { correspond(it, recherche) }
            @Composable
            fun carte(resume: ResumeContexte) = CarteContexte(
                resume = resume,
                onOuvrir = { onOuvrir(resume.contexte.id) },
                onModifier = { onModifier(resume.contexte.id) },
                onSupprimer = { aSupprimer = resume },
                onLire = { aLire = resume },
                onDupliquer = { scope.launch { repository.dupliquerContexte(resume.contexte.id) } },
                onExporter = {
                    pendingExport = resume.contexte.id
                    exportLauncher.launch("${resume.contexte.nom}.csv")
                },
                onPartager = { partagerContexte(resume.contexte.id, resume.contexte.nom) },
            )
            when {
                filtres.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.liste_vide_message), textAlign = TextAlign.Center)
                    }
                }
                vueKanban && modeKanban -> {
                    val etats = EtatKanban.entries
                    Box(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            etats.forEachIndexed { index, etat ->
                                val cartes = filtres.filter { colonneEffective(it) == etat }
                                Column(
                                    Modifier.width(240.dp).fillMaxHeight()
                                        .onGloballyPositioned { colonneRects[index] = it.boundsInRoot() },
                                ) {
                                    Text(
                                        "${labelEtat(etat)} (${cartes.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp),
                                    )
                                    LazyColumn(
                                        contentPadding = PaddingValues(bottom = 80.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        items(cartes, key = { it.contexte.id }) { resume ->
                                            KanbanCarte(
                                                resume = resume,
                                                estTiree = dragResume?.contexte?.id == resume.contexte.id,
                                                onOuvrir = { onOuvrir(resume.contexte.id) },
                                                onDragStart = { p -> dragResume = resume; dragPointer = p },
                                                onDrag = { p -> dragPointer = p },
                                                onDragEnd = {
                                                    val cibleIdx = colonneRects.entries.firstOrNull { it.value.contains(dragPointer) }?.key
                                                    val nouvel = cibleIdx?.let { etats[it] }
                                                    // Respecte le plancher (tiges → Planifiée mini, export → Réalisée mini).
                                                    if (nouvel != null && nouvel != resume.contexte.statut &&
                                                        nouvel.ordinal >= plancherStatut(resume).ordinal
                                                    ) {
                                                        scope.launch { repository.modifierStatut(resume.contexte.id, nouvel) }
                                                    }
                                                    dragResume = null
                                                },
                                                onDragCancel = { dragResume = null },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        dragResume?.let { r ->
                            ElevatedCard(
                                Modifier
                                    .offset { IntOffset((dragPointer.x - 110.dp.toPx()).roundToInt(), (dragPointer.y - 24.dp.toPx()).roundToInt()) }
                                    .width(220.dp),
                            ) {
                                Text(
                                    r.contexte.nom,
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filtres, key = { it.contexte.id }) { resume -> carte(resume) }
                    }
                }
            }
        }
    }

    aSupprimer?.let { cible ->
        AlertDialog(
            onDismissRequest = { aSupprimer = null },
            title = { Text(stringResource(R.string.liste_dialog_supprimer_titre)) },
            text = { Text(stringResource(R.string.liste_dialog_supprimer_texte, cible.contexte.nom)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.supprimerContexte(cible.contexte.id) }
                    aSupprimer = null
                }) { Text(stringResource(R.string.liste_dialog_supprimer_confirmer)) }
            },
            dismissButton = { TextButton(onClick = { aSupprimer = null }) { Text(stringResource(R.string.liste_dialog_annuler)) } },
        )
    }

    if (aProposOuvert) {
        val versionNom = remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
        }
        AlertDialog(
            onDismissRequest = { aProposOuvert = false },
            title = { Text(stringResource(R.string.apropos_titre)) },
            text = {
                Column {
                    Text(stringResource(R.string.apropos_version, versionNom ?: ""))
                    Text(stringResource(R.string.apropos_desc))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_GITHUB))) }
                    aProposOuvert = false
                }) { Text(stringResource(R.string.apropos_github)) }
            },
            dismissButton = { TextButton(onClick = { aProposOuvert = false }) { Text(stringResource(R.string.liste_dialog_fermer)) } },
        )
    }

    aLire?.let { cible ->
        val c = cible.contexte
        AlertDialog(
            onDismissRequest = { aLire = null },
            title = { Text(c.nom) },
            text = {
                Column {
                    Text(stringResource(R.string.liste_detail_mesure, c.mode.libelle()))
                    Text(stringResource(R.string.liste_detail_classes, c.axe.min, c.axe.max, c.axe.pas))
                    Text(stringResource(R.string.liste_detail_increment, c.increment))
                    Text(stringResource(R.string.liste_detail_essences, c.essencesNoms.joinToString(", ")))
                    Text(stringResource(R.string.liste_detail_tiges, cible.nbEvenements))
                    Text(stringResource(R.string.liste_detail_exporte, stringResource(if (c.exporte) R.string.liste_oui else R.string.liste_non)))
                    c.commentaire?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.liste_detail_commentaire, it)) }
                    if (cible.verrouille) {
                        Text(
                            stringResource(R.string.liste_detail_verrouille),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { aLire = null }) { Text(stringResource(R.string.liste_dialog_fermer)) } },
        )
    }
}

private fun normRecherche(s: String): String =
    java.text.Normalizer.normalize(s.trim().lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

private fun formatDateListe(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))

/** Statut minimal imposé par l'avancement : tiges → Planifiée ; export → Réalisée. */
private fun plancherStatut(r: ResumeContexte): EtatKanban = when {
    r.contexte.exporte -> EtatKanban.REALISEE
    r.nbEvenements > 0 -> EtatKanban.PLANIFIEE
    else -> EtatKanban.PROPOSEE
}

/** Colonne réellement affichée : au moins le plancher, même si le statut stocké est inférieur. */
private fun colonneEffective(r: ResumeContexte): EtatKanban {
    val mini = plancherStatut(r)
    return if (r.contexte.statut.ordinal < mini.ordinal) mini else r.contexte.statut
}

/** Libellé localisé d'une colonne Kanban. */
@Composable
private fun labelEtat(etat: EtatKanban): String = stringResource(
    when (etat) {
        EtatKanban.PROPOSEE -> R.string.kanban_proposee
        EtatKanban.VALIDEE -> R.string.kanban_validee
        EtatKanban.PLANIFIEE -> R.string.kanban_planifiee
        EtatKanban.REALISEE -> R.string.kanban_realisee
        EtatKanban.ABANDONNEE -> R.string.kanban_abandonnee
    },
)

/** Carte compacte d'un contexte dans le Kanban, déplaçable par appui long + glissé. */
@Composable
private fun KanbanCarte(
    resume: ResumeContexte,
    estTiree: Boolean,
    onOuvrir: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    var coin by remember { mutableStateOf(Offset.Zero) }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coin = it.boundsInRoot().topLeft }
            .graphicsLayer { alpha = if (estTiree) 0.3f else 1f }
            .pointerInput(resume.contexte.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> onDragStart(coin + offset) },
                    onDrag = { change, _ -> change.consume(); onDrag(coin + change.position) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() },
                )
            }
            .clickable { onOuvrir() },
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                resume.contexte.nom,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            resume.contexte.dateMartelage?.let { d ->
                Text("📅 ${formatDateListe(d)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                stringResource(R.string.liste_detail_tiges, resume.nbEvenements),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun correspond(r: ResumeContexte, q: String): Boolean {
    val n = normRecherche(q)
    val c = r.contexte
    val champs = listOfNotNull(c.nom, c.commentaire, c.dateMartelage?.let { formatDateListe(it) })
    return champs.any { normRecherche(it).contains(n) }
}

@Composable
private fun CarteContexte(
    resume: ResumeContexte,
    onOuvrir: () -> Unit,
    onModifier: () -> Unit,
    onSupprimer: () -> Unit,
    onLire: () -> Unit,
    onDupliquer: () -> Unit,
    onExporter: () -> Unit,
    onPartager: () -> Unit,
) {
    val contexte = resume.contexte
    var menuOuvert by remember { mutableStateOf(false) }
    ElevatedCard(onClick = onOuvrir, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(contexte.nom, style = MaterialTheme.typography.titleLarge)
                contexte.commentaire?.takeIf { it.isNotBlank() }?.let { commentaire ->
                    Text(
                        commentaire,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    stringResource(R.string.liste_carte_resume,
                        contexte.mode.libelle(),
                        contexte.axe.min,
                        contexte.axe.max,
                        contexte.axe.pas,
                        contexte.essences.size,
                        resume.nbEvenements,
                    ) + if (resume.verrouille) stringResource(R.string.liste_carte_resume_verrouille) else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
                contexte.dateMartelage?.let { d ->
                    Text(
                        "📅 ${formatDateListe(d)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOuvert = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.liste_carte_options_content_desc))
                }
                DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.liste_carte_menu_lire)) }, onClick = { menuOuvert = false; onLire() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.liste_carte_menu_dupliquer)) }, onClick = { menuOuvert = false; onDupliquer() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.liste_carte_menu_exporter_csv)) }, onClick = { menuOuvert = false; onExporter() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.liste_carte_menu_partager)) }, onClick = { menuOuvert = false; onPartager() })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.liste_carte_menu_modifier)) },
                        enabled = !resume.verrouille,
                        onClick = { menuOuvert = false; onModifier() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.liste_carte_menu_supprimer)) },
                        enabled = !resume.verrouille,
                        onClick = { menuOuvert = false; onSupprimer() },
                    )
                }
            }
        }
    }
}
