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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
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
import kotlin.math.roundToInt
import io.github.pobsteta.marculus.data.TarifsIfnRepository
import fr.marculus.core.TarifsIfn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import fr.marculus.core.model.TarifEssence
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import fr.marculus.core.Cubage
import fr.marculus.core.Referentiels
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import io.github.pobsteta.marculus.R
import io.github.pobsteta.marculus.ui.BandeauCompact
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
    tarifsIfnRepository: TarifsIfnRepository,
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
    var coefForme by remember { mutableStateOf((contexteExistant?.coefficientForme ?: 0.5).toString()) }
    var dateMartelage by remember { mutableStateOf(contexteExistant?.dateMartelage) }
    var affouage by remember { mutableStateOf(contexteExistant?.affouage ?: false) }
    var volumeMaxLot by remember {
        mutableStateOf(contexteExistant?.volumeMaxLotM3?.takeIf { it > 0.0 }?.let { formatDecimal(it, 3) } ?: "")
    }
    var steresParM3 by remember {
        mutableStateOf(formatDecimal(contexteExistant?.steresParM3 ?: Contexte.STERES_PAR_M3_DEFAUT))
    }
    fun coefSteres(): Double? = steresParM3.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 && it.isFinite() }
    // Estimation en stères du volume maximal : tenue à jour avec les m³, dans les deux sens.
    var volumeMaxLotSteres by remember {
        mutableStateOf(
            volumeMaxLot.toDoubleOrNull()?.let { m3 -> coefSteres()?.let { formatDecimal(m3 * it) } } ?: "",
        )
    }
    // Part de houppier (Schaeffer) saisie en %, par essence ; vide = 0 %.
    val partsHouppier = remember {
        mutableStateMapOf<String, String>().apply {
            contexteExistant?.partHouppier?.forEach { (e, p) -> put(e, formatPourcent(p * 100.0)) }
        }
    }
    var datePickerOuvert by remember { mutableStateOf(false) }

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
    // Essences cochées à l'ouverture (et ajoutées) : affichées en tête. Figé pendant la saisie,
    // pour qu'une ligne ne saute pas quand on la coche ou la décoche.
    val essencesEnTete = remember { mutableStateListOf<String>().apply { addAll(selection) } }
    // Tarif par essence (type + numéro saisi) : celui de l'essence, sinon le tarif par défaut du contexte.
    val tarifs = remember {
        mutableStateMapOf<String, TarifCubage>().apply {
            essencesDisponibles.forEach { e -> put(e, contexteExistant?.let { Cubage.tarifDe(it, e).tarif } ?: TarifCubage.AUCUN) }
        }
    }
    val numerosTarif = remember {
        mutableStateMapOf<String, String>().apply {
            essencesDisponibles.forEach { e ->
                contexteExistant?.let { Cubage.tarifDe(it, e).numero }?.takeIf { it > 0 }?.let { put(e, it.toString()) }
            }
        }
    }
    fun tarifDe(e: String) = tarifs[e] ?: TarifCubage.AUCUN

    // Tarifs IFN par sylvoécorégion : null tant que non lus, ou si les tables ne sont pas embarquées.
    val ifn by produceState<TarifsIfn?>(initialValue = null) { value = tarifsIfnRepository.referentiel() }
    var communeInsee by remember { mutableStateOf(contexteExistant?.communeInsee) }
    var ser by remember { mutableStateOf(contexteExistant?.ser) }
    // Dernier numéro proposé par essence : un champ encore égal à lui (ou vide) suit les propositions,
    // un champ retouché à la main n'est plus jamais réécrit (sauf « Reproposer »).
    val derniersProposes = remember { mutableStateMapOf<String, String>() }
    // Même règle pour la part de houppier (EMERGE sur les arbres IFN), proposée en Schaeffer seulement.
    val dernieresParts = remember { mutableStateMapOf<String, String>() }
    fun proposition(e: String) = ser?.let { s -> ifn?.proposer(s, e, tarifDe(e)) }
    fun appliquerPropositions(essences: Collection<String> = selection, forcer: Boolean = false) {
        fun suivre(champs: MutableMap<String, String>, derniers: MutableMap<String, String>, e: String, p: String?) {
            if (p == null) return
            val actuel = champs[e].orEmpty()
            if (forcer || actuel.isBlank() || actuel == derniers[e]) champs[e] = p
            derniers[e] = p
        }
        essences.filter { it in selection }.forEach { e ->
            suivre(numerosTarif, derniersProposes, e, proposition(e)?.numero?.toString())
            if (tarifDe(e).estSchaeffer) {
                val part = ser?.let { s -> ifn?.proposerPartHouppier(s, e) }?.let { formatPourcent(it.pourcent) }
                suivre(partsHouppier, dernieresParts, e, part)
            }
        }
    }
    fun choisirCommune(insee: String?) {
        communeInsee = insee
        val sers = insee?.let { ifn?.serDeCommune(it) }.orEmpty()
        ser = sers.firstOrNull { it.ser == ser }?.ser ?: sers.firstOrNull()?.ser
        appliquerPropositions()
    }
    fun fond(e: String) = couleurs[e]?.first ?: Referentiels.couleurFondDefaut(essencesDisponibles.indexOf(e))
    fun texte(e: String) = couleurs[e]?.second ?: Referentiels.COULEUR_TEXTE_DEFAUT

    var erreur by remember { mutableStateOf<String?>(null) }
    var picker by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var ajoutOuvert by remember { mutableStateOf(false) }
    var infoOuvert by remember { mutableStateOf(false) }
    var cheminGpkg by remember { mutableStateOf(contexteExistant?.cheminGpkg) }
    // Commune portée par les parcelles du GeoPackage (la plus fréquente), pour préremplir la recherche.
    // Seulement à la création ou quand on rattache un autre GeoPackage : un contexte existant n'est
    // jamais recalculé à l'ouverture.
    val communeGpkg by produceState<String?>(initialValue = null, cheminGpkg) {
        value = cheminGpkg?.takeIf { !edition || it != contexteExistant?.cheminGpkg }?.let { chemin ->
            withContext(Dispatchers.IO) {
                runCatching { gpkgRepository.parcellesDetail(chemin) }.getOrNull()
                    ?.mapNotNull { it.commune?.takeIf { c -> c.isNotBlank() } }
                    ?.groupingBy { it }?.eachCount()?.maxByOrNull { it.value }?.key
            }
        }
    }
    // Commune unique de ce nom dans le GeoPackage : retenue une fois, sans revenir après « Changer ».
    var communeGpkgAppliquee by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(communeGpkg, ifn) {
        val nom = communeGpkg ?: return@LaunchedEffect
        val referentiel = ifn ?: return@LaunchedEffect
        if (communeInsee == null && communeGpkgAppliquee != nom) {
            communeGpkgAppliquee = nom
            referentiel.communesNommees(nom).singleOrNull()?.let { choisirCommune(it.insee) }
        }
    }
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
    val errAffouageTarif = stringResource(R.string.creation_erreur_affouage_tarif)
    val errAffouageVolume = stringResource(R.string.creation_erreur_affouage_volume)
    val errHouppier = stringResource(R.string.creation_erreur_houppier)
    val errSteres = stringResource(R.string.creation_erreur_steres)

    Scaffold(
        topBar = {
            BandeauCompact(
                titre = stringResource(if (edition) R.string.creation_titre_modifier else R.string.creation_titre_nouveau),
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

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.creation_date_martelage), style = MaterialTheme.typography.titleSmall)
                    Text(
                        dateMartelage?.let { formatDateMartelage(it) } ?: stringResource(R.string.creation_date_aucune),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedButton(onClick = { datePickerOuvert = true }) { Text(stringResource(R.string.creation_date_choisir)) }
                if (dateMartelage != null) {
                    TextButton(onClick = { dateMartelage = null }) { Text(stringResource(R.string.creation_btn_retirer_gpkg)) }
                }
            }

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

            ifn?.let { referentiel ->
                HorizontalDivider()
                SectionCommune(
                    ifn = referentiel,
                    communeInsee = communeInsee,
                    ser = ser,
                    communeGpkg = communeGpkg,
                    onCommune = { choisirCommune(it) },
                    onSer = { code -> ser = code; appliquerPropositions() },
                    onReproposer = { appliquerPropositions(forcer = true) },
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
            Text(stringResource(R.string.creation_essences_aide), style = MaterialTheme.typography.bodySmall)
            val paysageEssences = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            val (enTete, autres) = essencesDisponibles.partition { it in essencesEnTete }
            (enTete + autres).forEach { essence ->
                if (essence == autres.firstOrNull() && enTete.isNotEmpty()) {
                    Text(
                        stringResource(R.string.creation_essences_autres),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                LigneEssence(
                    nom = essence,
                    coche = essence in selection,
                    fond = Color(fond(essence)),
                    texte = Color(texte(essence)),
                    tarif = tarifDe(essence),
                    numero = numerosTarif[essence] ?: "",
                    part = partsHouppier[essence] ?: "",
                    source = if (essence in selection && tarifDe(essence).estSchaeffer) {
                        sourceProposition(ifn, ser, essence, tarifDe(essence))
                    } else {
                        null
                    },
                    surUneLigne = paysageEssences,
                    onToggle = {
                        if (essence in selection) {
                            selection.remove(essence)
                        } else {
                            selection.add(essence)
                            appliquerPropositions(listOf(essence))
                        }
                    },
                    onPickFond = { picker = essence to true },
                    onPickTexte = { picker = essence to false },
                    onTarif = { tarifs[essence] = it; appliquerPropositions(listOf(essence)) },
                    onNumero = { saisie -> numerosTarif[essence] = saisie.filter { it.isDigit() }.take(2) },
                    onPart = { saisie -> partsHouppier[essence] = saisie.filter { it.isDigit() || it == '.' || it == ',' }.take(5) },
                )
            }
            val tarifsCoches = selection.map { tarifDe(it) }.toSet()
            if (tarifsCoches.any { it.estSchaeffer }) {
                Text(stringResource(R.string.creation_houppier_aide), style = MaterialTheme.typography.bodySmall)
            }
            if (TarifCubage.EMERGE in tarifsCoches) {
                Text(stringResource(R.string.creation_houppier_emerge), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = coefForme,
                    onValueChange = { saisie -> coefForme = saisie.filter { it.isDigit() || it == '.' || it == ',' } },
                    label = { Text(stringResource(R.string.creation_coef_forme)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.creation_coef_forme_aide), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { ajoutOuvert = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.creation_btn_ajouter_essence))
            }

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = affouage, onCheckedChange = { affouage = it })
                Text(stringResource(R.string.creation_affouage), style = MaterialTheme.typography.titleSmall)
            }
            if (affouage) {
                fun decimal(saisie: String) = saisie.filter { it.isDigit() || it == '.' || it == ',' }
                fun nombre(saisie: String) = saisie.replace(',', '.').toDoubleOrNull()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChampDecimal(
                        stringResource(R.string.creation_affouage_m3), volumeMaxLot,
                        { saisie ->
                            volumeMaxLot = decimal(saisie)
                            val m3 = nombre(volumeMaxLot)
                            val coef = coefSteres()
                            volumeMaxLotSteres = if (m3 != null && coef != null) formatDecimal(m3 * coef) else ""
                        },
                        Modifier.weight(1f),
                    )
                    ChampDecimal(
                        stringResource(R.string.creation_affouage_steres), volumeMaxLotSteres,
                        { saisie ->
                            volumeMaxLotSteres = decimal(saisie)
                            val st = nombre(volumeMaxLotSteres)
                            val coef = coefSteres()
                            if (coef != null) volumeMaxLot = st?.let { formatDecimal(it / coef, 3) } ?: ""
                        },
                        Modifier.weight(1f),
                    )
                    ChampDecimal(
                        stringResource(R.string.creation_affouage_coef_steres), steresParM3,
                        { saisie ->
                            steresParM3 = decimal(saisie)
                            val m3 = nombre(volumeMaxLot)
                            val coef = coefSteres()
                            if (m3 != null && coef != null) volumeMaxLotSteres = formatDecimal(m3 * coef)
                        },
                        Modifier.weight(0.8f),
                    )
                }
                Text(stringResource(R.string.creation_affouage_steres_aide), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.creation_affouage_aide), style = MaterialTheme.typography.bodySmall)
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
                        val vMaxLot = volumeMaxLot.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
                        // Essence → fraction (null si illisible). Seules les essences en Schaeffer s'en servent.
                        val partsSaisies = partsHouppier
                            .filterKeys { it in selection }
                            .filterValues { it.isNotBlank() }
                            .mapValues { (_, v) -> v.replace(',', '.').toDoubleOrNull()?.div(100.0) }
                        fun partValide(p: Double?) = p != null && p in 0.0..Cubage.PART_HOUPPIER_MAX
                        val partInvalide = partsSaisies.any { (e, p) -> tarifDe(e).estSchaeffer && !partValide(p) }
                        val essencesCubees = selection.filter { tarifDe(it) != TarifCubage.AUCUN }
                        erreur = when {
                            nom.isBlank() -> errNom
                            mi == null || ma == null || pa == null -> errNombres
                            pa <= 0 -> errPas
                            mi > ma -> errMinMax
                            inc == null || inc <= 0 -> errInc
                            selection.isEmpty() -> errEssence
                            affouage && essencesCubees.isEmpty() -> errAffouageTarif
                            affouage && vMaxLot == null -> errAffouageVolume
                            affouage && coefSteres() == null -> errSteres
                            partInvalide -> errHouppier
                            else -> {
                                val essences = essencesDisponibles
                                    .filter { it in selection }
                                    .map { EssenceColonne(it, fond(it), texte(it)) }
                                val commentaireFinal = commentaire.trim().ifBlank { null }
                                val tarifsFinaux = essences.associate { e ->
                                    e.nom to TarifEssence(tarifDe(e.nom), numerosTarif[e.nom]?.toIntOrNull() ?: 0).normalise()
                                }
                                // Tarif par défaut (lecteurs anciens, essences ajoutées par Nemeton) : la première essence cubée.
                                val defaut = essencesCubees.firstOrNull()?.let { tarifsFinaux[it] } ?: TarifEssence(TarifCubage.AUCUN)
                                val steres = coefSteres() ?: contexteExistant?.steresParM3 ?: Contexte.STERES_PAR_M3_DEFAUT
                                // Millésime des tables qui ont servi : gardé tel quel tant que la SER ne change pas.
                                val millesime = if (ser != null && ser == contexteExistant?.ser) {
                                    contexteExistant?.millesimeTarifs
                                } else {
                                    ser?.let { s -> essences.firstNotNullOfOrNull { ifn?.proposer(s, it.nom, tarifDe(it.nom)) }?.millesime }
                                }
                                val coef = coefForme.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.5
                                // Décoché, le volume saisi est conservé : recocher retrouve la borne.
                                val volumeLot = vMaxLot ?: contexteExistant?.volumeMaxLotM3 ?: 0.0
                                // Parts lisibles et > 0 ; celles d'une essence hors Schaeffer sont gardées pour un retour au Schaeffer.
                                val partsFinales = partsSaisies
                                    .filterValues { partValide(it) && it!! > 0.0 }
                                    .mapValues { it.value!! }
                                scope.launch {
                                    if (edition) {
                                        repository.modifierContexte(
                                            contexteExistant!!.id, nom.trim(), mode,
                                            AxeClasses(mi, ma, pa), essences, commentaireFinal, inc, cheminGpkg,
                                            defaut.tarif, defaut.numero, coef, dateMartelage,
                                            affouage, volumeLot, partsFinales, tarifsFinaux, steres,
                                            communeInsee, ser, millesime,
                                        )
                                        onEnregistre(contexteExistant.id)
                                    } else {
                                        val id = repository.creerContexte(
                                            nom.trim(), mode, AxeClasses(mi, ma, pa),
                                            essences, commentaireFinal, inc,
                                            cheminGpkg = cheminGpkg, tarif = defaut.tarif, tarifNumero = defaut.numero,
                                            coefficientForme = coef, dateMartelage = dateMartelage,
                                            affouage = affouage, volumeMaxLotM3 = volumeLot,
                                            partHouppier = partsFinales, tarifsEssence = tarifsFinaux,
                                            steresParM3 = steres,
                                            communeInsee = communeInsee, ser = ser, millesimeTarifs = millesime,
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
                    // La nouvelle essence reprend le tarif de la première essence cubée, à ajuster.
                    selection.firstOrNull { tarifDe(it) != TarifCubage.AUCUN }?.let { modele ->
                        tarifs[nouvelle] = tarifDe(modele)
                        numerosTarif[modele]?.let { numerosTarif[nouvelle] = it }
                    }
                    essencesDisponibles.add(nouvelle)
                    essencesEnTete.add(nouvelle)
                    selection.add(nouvelle)
                }
                ajoutOuvert = false
            },
        )
    }

    if (infoOuvert) {
        InfoCouleursDialog(onFermer = { infoOuvert = false })
    }

    if (datePickerOuvert) {
        val etat = rememberDatePickerState(initialSelectedDateMillis = dateMartelage)
        DatePickerDialog(
            onDismissRequest = { datePickerOuvert = false },
            confirmButton = {
                TextButton(onClick = { dateMartelage = etat.selectedDateMillis; datePickerOuvert = false }) {
                    Text(stringResource(R.string.creation_date_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerOuvert = false }) { Text(stringResource(R.string.creation_action_annuler)) }
            },
        ) { DatePicker(state = etat) }
    }
}

private fun formatDateMartelage(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChampDecimal(label: String, valeur: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

/**
 * Une essence : case, nom, réglages de cubage (tarif, numéro Schaeffer, part de houppier) et
 * couleurs. Réglages sur la même ligne en paysage ; en portrait, sous le nom, faute de largeur.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LigneEssence(
    nom: String,
    coche: Boolean,
    fond: Color,
    texte: Color,
    tarif: TarifCubage,
    numero: String,
    part: String,
    /** Origine du numéro proposé (texte secondaire), ou null. */
    source: String?,
    surUneLigne: Boolean,
    onToggle: () -> Unit,
    onPickFond: () -> Unit,
    onPickTexte: () -> Unit,
    onTarif: (TarifCubage) -> Unit,
    onNumero: (String) -> Unit,
    onPart: (String) -> Unit,
) {
    @Composable
    fun Reglages() {
        SelecteurTarif(nom, tarif, onTarif)
        if (tarif.estSchaeffer) {
            OutlinedTextField(
                value = numero,
                onValueChange = onNumero,
                label = { Text(stringResource(R.string.creation_tarif_numero_court), maxLines = 1) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(68.dp),
            )
        }
        // Part de houppier : utile aux seuls tarifs sans houppier (Schaeffer), grisée en EMERGE.
        if (tarif != TarifCubage.AUCUN) {
            OutlinedTextField(
                value = part,
                onValueChange = onPart,
                label = { Text(stringResource(R.string.creation_houppier_court), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("0") },
                suffix = { Text("%") },
                enabled = tarif != TarifCubage.EMERGE,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(100.dp),
            )
        }
    }
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = coche, onCheckedChange = { onToggle() })
            Text(nom, modifier = Modifier.weight(1f))
            if (coche && surUneLigne) Reglages()
            Echantillon(couleur = fond, onClick = onPickFond)
            EchantillonTexte(fond = fond, texte = texte, onClick = onPickTexte)
        }
        if (coche && !surUneLigne) {
            FlowRow(
                modifier = Modifier.padding(start = 40.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) { Reglages() }
        }
        source?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 40.dp, bottom = 4.dp),
            )
        }
    }
}

/** Menu déroulant du tarif d'une essence. */
@Composable
private fun SelecteurTarif(essence: String, tarif: TarifCubage, onTarif: (TarifCubage) -> Unit) {
    var ouvert by remember { mutableStateOf(false) }
    val description = stringResource(R.string.creation_cd_tarif_essence, essence)
    Box {
        OutlinedButton(
            onClick = { ouvert = true },
            contentPadding = PaddingValues(start = 12.dp, end = 4.dp),
            modifier = Modifier.semantics { contentDescription = description },
        ) {
            Text(tarif.libelleCourt(), maxLines = 1)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            TarifCubage.entries.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t.libelleLong()) },
                    onClick = { onTarif(t); ouvert = false },
                )
            }
        }
    }
}

@Composable
private fun TarifCubage.libelleCourt(): String = stringResource(
    when (this) {
        TarifCubage.AUCUN -> R.string.creation_tarif_court_aucun
        TarifCubage.SCHAEFFER_RAPIDE -> R.string.creation_tarif_court_rapide
        TarifCubage.SCHAEFFER_INTERMEDIAIRE -> R.string.creation_tarif_court_intermediaire
        TarifCubage.SCHAEFFER_LENT -> R.string.creation_tarif_court_lent
        TarifCubage.SCHAEFFER_TRES_LENT -> R.string.creation_tarif_court_tres_lent
        TarifCubage.EMERGE -> R.string.creation_tarif_court_emerge
    },
)

@Composable
private fun TarifCubage.libelleLong(): String = stringResource(
    when (this) {
        TarifCubage.AUCUN -> R.string.creation_tarif_aucun
        TarifCubage.SCHAEFFER_RAPIDE -> R.string.creation_tarif_rapide
        TarifCubage.SCHAEFFER_INTERMEDIAIRE -> R.string.creation_tarif_intermediaire
        TarifCubage.SCHAEFFER_LENT -> R.string.creation_tarif_lent
        TarifCubage.SCHAEFFER_TRES_LENT -> R.string.creation_tarif_tres_lent
        TarifCubage.EMERGE -> R.string.creation_tarif_emerge
    },
)

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

/** Pourcentage sans zéros inutiles : 30.0 → « 30 », 12.5 → « 12.5 ». */
private fun formatPourcent(v: Double): String = formatDecimal(v)

/** Nombre sans zéros inutiles, arrondi à [decimales] : 6.6667 → « 6.667 » (3), 10.0 → « 10 ». */
private fun formatDecimal(v: Double, decimales: Int = 2): String =
    "%.${decimales}f".format(java.util.Locale.ROOT, v).let { if ('.' in it) it.trimEnd('0').trimEnd('.') else it }

/**
 * Commune du martelage (recherche par nom) et sa sylvoécorégion : choix parmi les SER de la
 * commune, triées par part, la principale retenue par défaut.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionCommune(
    ifn: TarifsIfn,
    communeInsee: String?,
    ser: String?,
    communeGpkg: String?,
    onCommune: (String?) -> Unit,
    onSer: (String) -> Unit,
    onReproposer: () -> Unit,
) {
    Text(stringResource(R.string.creation_commune_titre), style = MaterialTheme.typography.titleSmall)
    Text(stringResource(R.string.creation_commune_aide), style = MaterialTheme.typography.bodySmall)
    val commune = communeInsee?.let { ifn.commune(it) }
    if (commune == null) {
        var saisie by remember(communeGpkg) { mutableStateOf(communeGpkg.orEmpty()) }
        OutlinedTextField(
            value = saisie,
            onValueChange = { saisie = it },
            label = { Text(stringResource(R.string.creation_commune_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ifn.rechercherCommunes(saisie, max = 6).forEach { c ->
            TextButton(onClick = { onCommune(c.insee) }, modifier = Modifier.fillMaxWidth()) {
                Text("${c.nom} (${c.departement})", modifier = Modifier.fillMaxWidth())
            }
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${commune.nom} (${commune.departement})", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = { onCommune(null) }) { Text(stringResource(R.string.creation_commune_changer)) }
    }
    val sers = ifn.serDeCommune(commune.insee)
    if (sers.size > 1) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sers.forEach { p ->
                FilterChip(
                    selected = p.ser == ser,
                    onClick = { onSer(p.ser) },
                    label = { Text(stringResource(R.string.creation_ser_part, p.ser, (p.part * 100).roundToInt())) },
                )
            }
        }
    }
    ser?.let { code ->
        Text(
            stringResource(R.string.creation_ser_libelle, code, ifn.ser(code)?.nom.orEmpty()),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = onReproposer) { Text(stringResource(R.string.creation_reproposer)) }
    }
}

/** « SER C20 — 1 240 arbres, CV 12 % », repli indiqué, et le type au plus faible CV s'il diffère. */
@Composable
private fun sourceProposition(ifn: TarifsIfn?, ser: String?, essence: String, tarif: TarifCubage): String? {
    if (ifn == null || ser == null) return null
    val p = ifn.proposer(ser, essence, tarif) ?: return stringResource(R.string.creation_source_aucune)
    val locale = LocalConfiguration.current.locales[0]
    val arbres = String.format(locale, "%,d", p.nArbres)
    val cv = String.format(locale, "%.0f", p.cv)
    val origine = when (p.niveau) {
        TarifsIfn.Niveau.SER -> stringResource(R.string.creation_source_ser, p.code, arbres, cv)
        TarifsIfn.Niveau.GRECO -> stringResource(R.string.creation_source_greco, p.code, arbres, cv)
        TarifsIfn.Niveau.FRANCE -> stringResource(R.string.creation_source_france, arbres, cv)
    }
    val conseille = ifn.typeConseille(ser, essence)?.takeIf { it != tarif }
        ?.let { stringResource(R.string.creation_type_conseille, it.libelleLong()) }
    val houppier = ifn.proposerPartHouppier(ser, essence)?.let { pp ->
        val niveau = when (pp.niveau) {
            TarifsIfn.Niveau.SER -> stringResource(R.string.creation_niveau_ser, pp.code)
            TarifsIfn.Niveau.GRECO -> stringResource(R.string.creation_niveau_greco, pp.code)
            TarifsIfn.Niveau.FRANCE -> stringResource(R.string.creation_niveau_france)
        }
        stringResource(R.string.creation_source_houppier, String.format(locale, "%.1f", pp.pourcent), niveau)
    }
    return listOfNotNull(origine, conseille, houppier).joinToString(" · ")
}

