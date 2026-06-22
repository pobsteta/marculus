package io.github.pobsteta.marculus.ui.statut

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.AttributionSpatiale
import fr.marculus.core.Cubage
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.CategorieBois
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.SeuilsCategories
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.Tige
import io.github.pobsteta.marculus.R
import io.github.pobsteta.marculus.data.GpkgRepository
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.data.ParcelleGpkg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatutHistoriqueScreen(
    repository: MartelageRepository,
    gpkgRepository: GpkgRepository,
    contexteId: String,
    seuils: SeuilsCategories,
    onRetour: () -> Unit,
) {
    val contexte by produceState<Contexte?>(initialValue = null, contexteId) {
        value = repository.contexte(contexteId)
    }
    val totaux by repository.totaux(contexteId).collectAsStateWithLifecycle(emptyMap())
    val journal by repository.journal(contexteId).collectAsStateWithLifecycle(emptyList())
    val parcelles by produceState(initialValue = emptyList<ParcelleGpkg>(), contexte) {
        value = contexte?.cheminGpkg?.let { withContext(Dispatchers.IO) { gpkgRepository.parcellesDetail(it) } } ?: emptyList()
    }
    var onglet by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val ctx = contexte
        if (uri != null && ctx != null) {
            val csv = csvFoncier(ctx, journal, parcelles, locale)
            context.contentResolver.openOutputStream(uri)?.use {
                it.write("﻿".toByteArray(Charsets.UTF_8))
                it.write(csv.toByteArray(Charsets.UTF_8))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contexte?.nom ?: stringResource(R.string.statut_titre_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.statut_retour_cd))
                    }
                },
                actions = {
                    TextButton(onClick = { exportLauncher.launch("martelage-foncier.csv") }) {
                        Text(stringResource(R.string.statut_export_csv), color = MaterialTheme.colorScheme.onPrimary)
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
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = onglet) {
                Tab(selected = onglet == 0, onClick = { onglet = 0 }, text = { Text(stringResource(R.string.statut_onglet_statut)) })
                Tab(selected = onglet == 1, onClick = { onglet = 1 }, text = { Text(stringResource(R.string.statut_onglet_par_parcelle)) })
                Tab(selected = onglet == 2, onClick = { onglet = 2 }, text = { Text(stringResource(R.string.statut_onglet_historique)) })
            }
            when (onglet) {
                0 -> OngletStatut(ctx, totaux, seuils)
                1 -> OngletParcelles(ctx, journal, parcelles)
                else -> OngletHistorique(ctx, journal)
            }
        }
    }
}

@Composable
private fun OngletStatut(contexte: Contexte, totaux: Map<CompteurCle, Int>, seuils: SeuilsCategories) {
    val couleurs = contexte.essences.associate { it.nom to it.couleurFondArgb }
    // Total par essence (pour le donut), dans l'ordre des colonnes.
    val parEssence = contexte.essencesNoms.map { nom ->
        nom to contexte.axe.classes().sumOf { c -> totaux[CompteurCle(nom, c)] ?: 0 }
    }
    val total = parEssence.sumOf { it.second }
    val classes = contexte.axe.classes()
    // Couleur d'une classe : teinte par catégorie (PB/BM/GB/TGB), dégradé clair→foncé dans la catégorie.
    val couleurClasse: (Int) -> Color = { classe ->
        val cat = seuils.categorie(classe, contexte.mode)
        val membres = classes.filter { seuils.categorie(it, contexte.mode) == cat }
        val f = if (membres.size > 1) membres.indexOf(classe).coerceAtLeast(0).toFloat() / (membres.size - 1) else 0f
        gradientCategorie(cat, f)
    }
    val maxEssence = parEssence.maxOfOrNull { it.second } ?: 0
    val locale = LocalConfiguration.current.locales[0]
    val volumeTotal = if (contexte.tarif != TarifCubage.AUCUN) {
        totaux.entries.sumOf { (cle, n) -> Cubage.volumeUnitaire(contexte, cle.classe) * n }
    } else {
        0.0
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (contexte.tarif != TarifCubage.AUCUN && total > 0) {
            item {
                Text(
                    stringResource(R.string.statut_volume_total, String.format(locale, "%.2f", volumeTotal)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        item {
            if (total == 0) {
                Text(stringResource(R.string.statut_aucune_tige), style = MaterialTheme.typography.bodyMedium)
            } else {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(contentAlignment = Alignment.Center) {
                        Donut(parEssence.filter { it.second > 0 }, couleurs, total)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$total", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.statut_tiges), style = MaterialTheme.typography.labelMedium)
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
        if (total > 0) {
            item { HorizontalDivider() }
            item { Text(stringResource(R.string.statut_detail_par_classe), style = MaterialTheme.typography.titleSmall) }
            item { LegendeClasses(classes, couleurClasse) }
            items(contexte.essencesNoms) { essence ->
                BarreEmpilee(
                    essence = essence,
                    classesAvecTotal = classes.map { c -> c to (totaux[CompteurCle(essence, c)] ?: 0) },
                    couleurParClasse = couleurClasse,
                    maxEssence = maxEssence,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OngletParcelles(contexte: Contexte, journal: List<Tige>, parcelles: List<ParcelleGpkg>) {
    val strHorsParcelle = stringResource(R.string.statut_hors_parcelle)
    val strSansProprietaire = stringResource(R.string.statut_sans_proprietaire)
    val strParcelle = stringResource(R.string.statut_parcelle_prefix)
    val couleurs = contexte.essences.associate { it.nom to it.couleurFondArgb }
    val plus = journal.filter { it.action == ActionTige.PLUS }
    val geo = plus.filter { it.position != null }
    val sansPosition = plus.filter { it.position == null }.sumOf { it.quantite }

    fun parcelleDe(t: Tige): ParcelleGpkg? =
        parcelles.firstOrNull { AttributionSpatiale.contient(it.anneaux, t.position!!) }

    fun essences(tiges: List<Tige>): List<Pair<String, Int>> =
        contexte.essencesNoms.mapNotNull { nom ->
            val n = tiges.filter { it.essence == nom }.sumOf { it.quantite }
            if (n > 0) nom to n else null
        }

    // Rattachement point-dans-polygone, puis regroupement Propriétaire → Forêt → Parcelle.
    val locale = LocalConfiguration.current.locales[0]
    val rattachees = geo.map { it to parcelleDe(it) }
    fun total(l: List<Pair<Tige, ParcelleGpkg?>>) = l.sumOf { it.first.quantite }
    val parProp = rattachees
        .groupBy { (_, p) -> p?.proprietaire ?: if (p == null) strHorsParcelle else strSansProprietaire }
        .toList().sortedByDescending { (_, l) -> total(l) }
    var filtre by remember { mutableStateOf<String?>(null) }
    val affiches = parProp.filter { filtre == null || it.first == filtre }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (parcelles.isEmpty()) {
            item { Text(stringResource(R.string.statut_aucun_gpkg), style = MaterialTheme.typography.bodyMedium) }
        }
        if (parProp.size > 1) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = filtre == null,
                        onClick = { filtre = null },
                        label = { Text(stringResource(R.string.statut_filtre_tous)) },
                    )
                    parProp.forEach { (prop, _) ->
                        FilterChip(
                            selected = filtre == prop,
                            onClick = { filtre = if (filtre == prop) null else prop },
                            label = { Text(prop) },
                        )
                    }
                }
            }
        }
        if (parProp.isEmpty()) {
            item { Text(stringResource(R.string.statut_aucune_tige_geo), style = MaterialTheme.typography.bodyMedium) }
        }
        items(affiches) { (prop, tigesProp) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LigneNiveau(prop, total(tigesProp), 0, MaterialTheme.typography.titleMedium)
                if (prop == strHorsParcelle) {
                    essences(tigesProp.map { it.first }).forEach { (nom, n) -> LigneEssenceParc(nom, n, couleurs, 1) }
                } else {
                    tigesProp.groupBy { (_, p) -> p?.foret ?: "—" }
                        .toList().sortedByDescending { (_, l) -> total(l) }
                        .forEach { (foret, tigesForet) ->
                            LigneNiveau(foret, total(tigesForet), 1, MaterialTheme.typography.titleSmall)
                            tigesForet.groupBy { (_, p) -> p }
                                .toList().sortedByDescending { (_, l) -> total(l) }
                                .forEach { (pcl, tigesParc) ->
                                    val libelle = pcl?.parcelleNom?.let { "$strParcelle $it" }
                                        ?: pcl?.let { "$strParcelle ${it.id}" } ?: "—"
                                    val tot = total(tigesParc)
                                    LigneNiveau(libelle, tot, 2, MaterialTheme.typography.bodyMedium)
                                    val ha = pcl?.surfaceHa ?: 0.0
                                    if (ha > 0.0) {
                                        Text(
                                            stringResource(
                                                R.string.statut_surface_densite,
                                                String.format(locale, "%.2f", ha),
                                                String.format(locale, "%.1f", tot / ha),
                                            ),
                                            modifier = Modifier.padding(start = 36.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                    if (contexte.tarif != TarifCubage.AUCUN) {
                                        val volParc = tigesParc.sumOf {
                                            Cubage.volumeUnitaire(contexte, it.first.classe) * it.first.quantite
                                        }
                                        Text(
                                            stringResource(
                                                R.string.statut_volume_parcelle,
                                                String.format(locale, "%.2f", volParc),
                                                if (ha > 0.0) String.format(locale, "%.2f", volParc / ha) else "—",
                                            ),
                                            modifier = Modifier.padding(start = 36.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                    essences(tigesParc.map { it.first }).forEach { (nom, n) -> LigneEssenceParc(nom, n, couleurs, 3) }
                                }
                        }
                }
                HorizontalDivider(Modifier.padding(top = 4.dp))
            }
        }
        if (sansPosition > 0) {
            item {
                Text(
                    stringResource(R.string.statut_tiges_sans_position_gnss, sansPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

/** Construit le CSV « par foncier » (propriétaire/forêt/commune/parcelle × essence × classe). */
private fun csvFoncier(
    contexte: Contexte,
    journal: List<Tige>,
    parcelles: List<ParcelleGpkg>,
    locale: java.util.Locale,
): String {
    val plus = journal.filter { it.action == ActionTige.PLUS && it.position != null }
    fun parcelleDe(t: Tige) = parcelles.firstOrNull { p -> t.position?.let { AttributionSpatiale.contient(p.anneaux, it) } == true }
    fun champ(s: String) = s.replace(';', ' ').replace('\n', ' ')
    val sb = StringBuilder()
    val tarif = contexte.tarif != TarifCubage.AUCUN
    sb.append("Proprietaire;Foret;Commune;Parcelle;Surface_ha;Essence;Classe;Nombre;Tiges_ha;Volume_m3;Volume_ha\n")
    plus.groupBy { parcelleDe(it) }.forEach { (pcl, tiges) ->
        val ha = pcl?.surfaceHa ?: 0.0
        val totalParcelle = tiges.sumOf { it.quantite }
        val densite = if (ha > 0.0) String.format(locale, "%.1f", totalParcelle / ha) else ""
        val surface = if (ha > 0.0) String.format(locale, "%.4f", ha) else ""
        val volumeParcelle = if (tarif) tiges.sumOf { Cubage.volumeUnitaire(contexte, it.classe) * it.quantite } else 0.0
        val volHaStr = if (tarif && ha > 0.0) String.format(locale, "%.2f", volumeParcelle / ha) else ""
        for (essence in contexte.essencesNoms) {
            for (classe in contexte.axe.classes()) {
                val n = tiges.filter { it.essence == essence && it.classe == classe }.sumOf { it.quantite }
                if (n > 0) {
                    val volRow = if (tarif) String.format(locale, "%.3f", Cubage.volumeUnitaire(contexte, classe) * n) else ""
                    sb.append(champ(pcl?.proprietaire ?: "")).append(';')
                        .append(champ(pcl?.foret ?: "")).append(';')
                        .append(champ(pcl?.commune ?: "")).append(';')
                        .append(champ(pcl?.parcelleNom ?: pcl?.id?.toString() ?: "")).append(';')
                        .append(surface).append(';')
                        .append(champ(essence)).append(';')
                        .append(classe).append(';')
                        .append(n).append(';')
                        .append(densite).append(';')
                        .append(volRow).append(';')
                        .append(volHaStr).append('\n')
                }
            }
        }
    }
    return sb.toString()
}

/** Ligne d'un niveau de la hiérarchie (propriétaire/forêt/parcelle) avec son total, indentée. */
@Composable
private fun LigneNiveau(libelle: String, total: Int, niveau: Int, style: TextStyle) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = (niveau * 12).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(libelle, modifier = Modifier.weight(1f), style = style, fontWeight = if (niveau == 0) FontWeight.Bold else null)
        Text("$total", style = style, fontWeight = FontWeight.Bold)
    }
}

/** Détail par essence (pastille couleur) sous une parcelle, indenté. */
@Composable
private fun LigneEssenceParc(nom: String, n: Int, couleurs: Map<String, Int>, niveau: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = (niveau * 12).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(12.dp).background(Color(couleurs[nom] ?: 0xFF888888.toInt())))
        Text(nom, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text("$n", style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LegendeClasses(classes: List<Int>, couleur: (Int) -> Color) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        classes.forEach { c ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(12.dp).background(couleur(c)))
                Text("$c", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun gradientCategorie(cat: CategorieBois, f: Float): Color {
    // Dégradé clair (petite classe) → foncé (grande classe) par catégorie.
    val (clair, fonce) = when (cat) {
        CategorieBois.PB -> Color(0xFF90CAF9) to Color(0xFF0D47A1)  // bleu
        CategorieBois.BM -> Color(0xFFFFE082) to Color(0xFFF57F17)  // ambre
        CategorieBois.GB -> Color(0xFFFFB74D) to Color(0xFFE65100)  // orange
        CategorieBois.TGB -> Color(0xFFEF9A9A) to Color(0xFFB71C1C) // rouge
    }
    return lerp(clair, fonce, f.coerceIn(0f, 1f))
}

@Composable
private fun BarreEmpilee(
    essence: String,
    classesAvecTotal: List<Pair<Int, Int>>,
    couleurParClasse: (Int) -> Color,
    maxEssence: Int,
) {
    val essenceTotal = classesAvecTotal.sumOf { it.second }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(essence, modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodySmall)
        Box(Modifier.weight(1f).height(24.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxSize()) {
                classesAvecTotal.forEach { (classe, t) ->
                    if (t > 0) {
                        Box(Modifier.weight(t.toFloat()).fillMaxHeight().background(couleurParClasse(classe)))
                    }
                }
                val reste = maxEssence - essenceTotal
                if (reste > 0) Spacer(Modifier.weight(reste.toFloat()))
            }
        }
        Text("$essenceTotal", modifier = Modifier.width(36.dp), fontWeight = FontWeight.Bold)
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
private fun OngletHistorique(contexte: Contexte, journal: List<Tige>) {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", locale) }
    val evenements = journal.sortedByDescending { it.horodatage }

    if (evenements.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.statut_aucun_evenement), style = MaterialTheme.typography.bodyMedium)
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
                            tige.parcelle?.takeIf { it.isNotBlank() }?.let { add("⌖ $it") }
                            if (contexte.tarif != TarifCubage.AUCUN) {
                                val v = Cubage.volumeUnitaire(contexte, tige.classe) * tige.quantite
                                if (v > 0.0) add(String.format(locale, "%.3f m³", v))
                            }
                            tige.position?.let {
                                add("GNSS " + "%.5f, %.5f".format(java.util.Locale.US, it.latitude, it.longitude))
                            }
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
