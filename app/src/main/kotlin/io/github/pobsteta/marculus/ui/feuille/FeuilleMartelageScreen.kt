package io.github.pobsteta.marculus.ui.feuille

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import io.github.pobsteta.marculus.R
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import java.text.DecimalFormatSymbols
import fr.marculus.core.Cubage
import fr.marculus.core.HauteurParser
import fr.marculus.core.Houppier
import fr.marculus.core.model.CompteurCle
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.Reglages
import io.github.pobsteta.marculus.data.GpkgRepository
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.data.ParcelleGpkg
import io.github.pobsteta.marculus.ui.BandeauCompact
import io.github.pobsteta.marculus.ui.ToucheVolume
import io.github.pobsteta.marculus.ui.gnss.BadgeGnss
import io.github.pobsteta.marculus.ui.gnss.DialogueEtatGnss
import io.github.pobsteta.marculus.ui.gnss.DialogueEtatGnssTelephone
import io.github.pobsteta.marculus.ui.tige.SaisieTigeDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val LARGEUR_CELLULE = 140.dp
private val HAUTEUR_CELLULE = 144.dp

/** Marge de fin de grille laissant défiler la dernière ligne au-dessus du bouton micro. */
private val MARGE_BOUTON_MICRO = TAILLE_MICRO + 16.dp

private sealed interface Saisie {
    /** `initial` = texte de hauteur déjà porté par la tige (estimé MNH, dicté ou saisi). */
    data class Hauteur(val uuid: String, val initial: String = "") : Saisie
    data class Qualite(val uuid: String) : Saisie
    data class Avis(val essence: String, val classe: Int) : Saisie
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeuilleMartelageScreen(
    repository: MartelageRepository,
    contexteId: String,
    reglages: Reglages,
    qualitesArbre: List<String>,
    qualitesBois: List<String>,
    gpkgRepository: GpkgRepository,
    onRetour: () -> Unit,
    onStatut: () -> Unit,
    onCarte: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val contexte by produceState<Contexte?>(initialValue = null, contexteId) {
        value = repository.contexte(contexteId)
    }
    // Parcelles du contexte : pour figer le rattachement spatial dans la tige au moment du martelage.
    val parcelles by produceState(initialValue = emptyList<ParcelleGpkg>(), contexte) {
        value = contexte?.cheminGpkg?.let { withContext(Dispatchers.IO) { gpkgRepository.parcellesDetail(it) } } ?: emptyList()
    }
    // Houppiers (MNH) : lus seulement si le réglage est actif — un GPKG sans couche `houppier`
    // rend une liste vide, et l'estimation ne se déclenche jamais.
    val estimerMnh = reglages.estimerHauteurMnh
    val houppiers by produceState(initialValue = emptyList<Houppier>(), contexte, estimerMnh) {
        value = if (!estimerMnh) {
            emptyList()
        } else {
            contexte?.cheminGpkg?.let { withContext(Dispatchers.IO) { gpkgRepository.houppiers(it) } } ?: emptyList()
        }
    }
    val session = sessionMartelage(
        repository = repository,
        contexteId = contexteId,
        contexte = contexte,
        reglages = reglages,
        qualitesArbre = qualitesArbre,
        qualitesBois = qualitesBois,
        parcelles = parcelles,
        houppiers = houppiers,
    )
    val dictee = session.dictee
    val totaux = session.totaux
    val configs = session.configs
    val fixTige = session.fixTige
    val derniereSaisie = session.derniereSaisie
    var saisie by remember { mutableStateOf<Saisie?>(null) }
    var menuReset by remember { mutableStateOf(false) }
    var confirmerReset by remember { mutableStateOf(false) }
    var saisieLibre by remember { mutableStateOf(false) }
    var etatGnssOuvert by remember { mutableStateOf(false) }
    var formesParleesOuvertes by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BandeauCompact(
                titre = contexte?.nom ?: stringResource(R.string.feuille_titre),
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.feuille_retour))
                    }
                },
                actions = {
                    // Badge tri-état : 📡 externe / 📱 interne / ⚠ sans position (capture coupée).
                    BadgeGnss(
                        capture = reglages.capturePosition,
                        rtkActif = session.rtkActif,
                        fix = fixTige,
                        ponctuel = reglages.gnssPonctuel,
                        gnssCoupe = session.gnssCoupe,
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = { etatGnssOuvert = true },
                    )
                    Box {
                        IconButton(onClick = { menuReset = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.feuille_menu))
                        }
                        DropdownMenu(expanded = menuReset, onDismissRequest = { menuReset = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.tige_saisir_titre)) },
                                onClick = { menuReset = false; saisieLibre = true },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.voix_formes_titre)) },
                                onClick = { menuReset = false; formesParleesOuvertes = true },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.feuille_menu_statut)) },
                                onClick = { menuReset = false; onStatut() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.feuille_menu_carte)) },
                                onClick = { menuReset = false; onCarte() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.feuille_menu_reinitialiser)) },
                                onClick = { menuReset = false; confirmerReset = true },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (reglages.pttEcran && contexte != null) {
                BoutonMicroPtt(
                    pret = dictee.microPret,
                    enEcoute = dictee.enEcoute,
                    onAppui = { dictee.demarrer() },
                    onRelache = { dictee.arreter() },
                )
            }
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

        val ajouter = session.ajouter
        val retirer = session.retirer

        // Comptage par boutons de volume : agit sur la cellule active (dernier +).
        DisposableEffect(reglages.boutonsVolume, derniereSaisie, totaux) {
            ToucheVolume.onVolume = if (reglages.boutonsVolume) {
                { haut ->
                    val cible = derniereSaisie
                    if (cible != null) {
                        if (haut) ajouter(cible.essence, cible.classe) else retirer(cible.essence, cible.classe)
                        true
                    } else {
                        false
                    }
                }
            } else {
                null
            }
            onDispose { ToucheVolume.onVolume = null }
        }

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
                        val estDerniere = derniereSaisie?.let { it.essence == e.nom && it.classe == classe } ?: false
                        CelluleCompteur(
                            libelle = if (reglages.afficherCodeEssence) {
                                "${Cubage.codeEssence(e.nom) ?: e.nom.uppercase().take(3)} $classe"
                            } else {
                                "${e.nom} $classe"
                            },
                            libelleGrand = reglages.afficherCodeEssence,
                            total = total,
                            fond = Color(e.couleurFondArgb),
                            texte = Color(e.couleurTexteArgb),
                            alerteMoins = cfg?.alerteMoins(total) ?: false,
                            alertePlus = cfg?.alertePlus(total) ?: false,
                            hqActif = estDerniere,
                            onPlus = { ajouter(e.nom, classe) },
                            onMoins = { retirer(e.nom, classe) },
                            onHauteur = {
                                // On rouvre sur la valeur courante : sinon, ajouter une découpe à
                                // une hauteur estimée (MNH) ou déjà dictée obligerait à la retaper.
                                derniereSaisie?.let { d ->
                                    scope.launch {
                                        saisie = Saisie.Hauteur(d.uuid, repository.tige(d.uuid)?.hauteurTexte.orEmpty())
                                    }
                                }
                            },
                            onQualite = { derniereSaisie?.let { saisie = Saisie.Qualite(it.uuid) } },
                            onAvis = { saisie = Saisie.Avis(e.nom, classe) },
                        )
                    }
                }
            }
            // Le bouton micro flotte au-dessus du coin bas-droit : sans cette marge, la dernière
            // ligne de la grille resterait dessous, hors d'atteinte en fin de défilement.
            if (reglages.pttEcran) Spacer(Modifier.height(MARGE_BOUTON_MICRO))
        }
    }

    if (confirmerReset) {
        AlertDialog(
            onDismissRequest = { confirmerReset = false },
            title = { Text(stringResource(R.string.feuille_reset_titre)) },
            text = { Text(stringResource(R.string.feuille_reset_texte)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.reinitialiser(contexteId) }
                    confirmerReset = false
                }) { Text(stringResource(R.string.feuille_reset_confirmer)) }
            },
            dismissButton = { TextButton(onClick = { confirmerReset = false }) { Text(stringResource(R.string.feuille_annuler)) } },
        )
    }

    when (val s = saisie) {
        is Saisie.Hauteur -> SaisieHauteurDialog(
            initial = s.initial,
            qualitesBois = qualitesBois,
            onAnnuler = { saisie = null },
            onValider = { texte ->
                scope.launch { repository.annoterHauteur(s.uuid, texte) }
                saisie = null
            },
        )

        is Saisie.Qualite -> ChoixQualiteDialog(
            qualites = qualitesArbre,
            onAnnuler = { saisie = null },
            onChoisir = { qualite ->
                scope.launch { repository.annoterQualite(s.uuid, qualite) }
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

    if (saisieLibre) {
        val ctxLibre = contexte
        SaisieTigeDialog(
            edition = false,
            essencesContexte = ctxLibre?.essences?.map { it.nom } ?: emptyList(),
            qualites = qualitesArbre,
            quantiteInitiale = (ctxLibre?.increment ?: 1).toString(),
            onAnnuler = { saisieLibre = false },
            onValider = { action, essence, classe, quantite, hauteur, qualite ->
                session.saisirLibre(action, essence, classe, quantite, hauteur, qualite)
                saisieLibre = false
            },
        )
    }


    if (etatGnssOuvert) {
        // GNSS du téléphone : un panneau qui dit pourquoi il n'y a pas (encore) de position.
        if (session.rtkActif || !reglages.capturePosition) {
            DialogueEtatGnss(fixTige) { etatGnssOuvert = false }
        } else {
            DialogueEtatGnssTelephone(reglages.gnssPonctuel) { etatGnssOuvert = false }
        }
    }

    if (formesParleesOuvertes) {
        DialogueFormesParlees(
            dictee = dictee,
            pttEcran = reglages.pttEcran,
            pttVolume = reglages.pttVolumeLong,
            onFermer = { formesParleesOuvertes = false },
        )
    }
}

@Composable
private fun CelluleCompteur(
    libelle: String,
    libelleGrand: Boolean,
    total: Int,
    fond: Color,
    texte: Color,
    alerteMoins: Boolean,
    alertePlus: Boolean,
    hqActif: Boolean,
    onPlus: () -> Unit,
    onMoins: () -> Unit,
    onHauteur: () -> Unit,
    onQualite: () -> Unit,
    onAvis: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val couleurHQ = texte.copy(alpha = if (hqActif) 1f else 0.38f)
    Card(
        modifier = Modifier.width(LARGEUR_CELLULE).height(HAUTEUR_CELLULE).padding(2.dp),
        colors = CardDefaults.cardColors(containerColor = fond),
    ) {
        Column(Modifier.fillMaxSize().padding(4.dp)) {
            // Haut : H — libellé (essence + classe) — Q.
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onHauteur, enabled = hqActif, modifier = Modifier.size(32.dp)) {
                    Text("H", color = couleurHQ, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    libelle,
                    color = texte,
                    style = if (libelleGrand) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelMedium,
                    fontWeight = if (libelleGrand) FontWeight.Bold else null,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    // En mode code : auto-dimensionnement pour remplir la cellule sans jamais tronquer.
                    autoSize = if (libelleGrand) TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 24.sp) else null,
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                )
                IconButton(onClick = onQualite, enabled = hqActif, modifier = Modifier.size(32.dp)) {
                    Text("Q", color = couleurHQ, style = MaterialTheme.typography.titleMedium)
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
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.feuille_avis), tint = texte)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.feuille_avis_menu)) }, onClick = { menu = false; onAvis() })
                        HorizontalDivider()
                        // Retirer une tige : action de correction = l'exception, donc reléguée au
                        // menu (grisée quand inapplicable). Évite les décréments accidentels (gants).
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.feuille_retirer_tige)) },
                            enabled = hqActif,
                            onClick = { menu = false; onMoins() },
                        )
                    }
                }
            }
            // Bas : « + » en pleine largeur (action dominante, cible large = sûre avec des gants).
            // Le « − » (correction, exception) est dans le menu ⋮ au-dessus.
            Button(
                onClick = onPlus,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaisieHauteurDialog(
    initial: String = "",
    qualitesBois: List<String>,
    onAnnuler: () -> Unit,
    onValider: (String) -> Unit,
) {
    // Séparateur décimal de la langue de l'application (fr → « , », en → « . »).
    val separateur = DecimalFormatSymbols.getInstance(LocalConfiguration.current.locales[0]).decimalSeparator
    // « 27-6AB4CD » se rouvre en deux champs : la hauteur d'un côté, la découpe de l'autre.
    val sep = initial.indexOf('-')
    var hauteur by remember(initial) {
        mutableStateOf((if (sep >= 0) initial.substring(0, sep) else initial).trim().replace('.', separateur))
    }
    var decoupe by remember(initial) {
        mutableStateOf(TextFieldValue(if (sep >= 0) initial.substring(sep + 1).trim() else ""))
    }
    // Validation non bloquante : codes qualité saisis absents du référentiel.
    val connues = remember(qualitesBois) { qualitesBois.map { it.uppercase() }.toSet() }
    val inconnues = remember(decoupe.text, connues) {
        HauteurParser.parse("0-${decoupe.text}").segments.map { it.qualiteBois }.filter { it !in connues }.distinct()
    }
    // Insère un code à la position du curseur et place le curseur juste après.
    fun insererCode(code: String) {
        val texte = decoupe.text
        val debut = decoupe.selection.min
        val fin = decoupe.selection.max
        val nouveau = texte.substring(0, debut) + code + texte.substring(fin)
        decoupe = TextFieldValue(nouveau, selection = TextRange(debut + code.length))
    }
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text(stringResource(R.string.feuille_hauteur_titre)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = hauteur,
                    // Chiffres + un seul séparateur, normalisé sur celui de la locale.
                    onValueChange = { v ->
                        hauteur = buildString {
                            var sepVue = false
                            for (c in v) when {
                                c.isDigit() -> append(c)
                                (c == '.' || c == ',') && !sepVue -> { append(separateur); sepVue = true }
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.feuille_hauteur_label)) },
                    placeholder = { Text(stringResource(R.string.feuille_hauteur_placeholder, separateur)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = decoupe,
                    onValueChange = { decoupe = it },
                    label = { Text(stringResource(R.string.feuille_decoupe_label)) },
                    placeholder = { Text(stringResource(R.string.feuille_decoupe_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (qualitesBois.isNotEmpty()) {
                    Text(stringResource(R.string.feuille_qualites_bois_titre), style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        qualitesBois.forEach { code ->
                            AssistChip(onClick = { insererCode(code) }, label = { Text(code) })
                        }
                    }
                }
                if (inconnues.isNotEmpty()) {
                    Text(
                        stringResource(R.string.feuille_codes_inconnus, inconnues.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    stringResource(R.string.feuille_decoupe_aide),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = hauteur.trim()
                val d = decoupe.text.trim()
                onValider(if (d.isNotBlank()) "$h-$d" else h)
            }) { Text(stringResource(R.string.feuille_valider)) }
        },
        dismissButton = { TextButton(onClick = onAnnuler) { Text(stringResource(R.string.feuille_annuler)) } },
    )
}

@Composable
private fun ChoixQualiteDialog(qualites: List<String>, onAnnuler: () -> Unit, onChoisir: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text(stringResource(R.string.feuille_qualite_titre)) },
        text = {
            Column {
                qualites.forEach { qualite ->
                    TextButton(
                        onClick = { onChoisir(qualite) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(qualite, style = MaterialTheme.typography.bodyLarge) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onAnnuler) { Text(stringResource(R.string.feuille_fermer)) } },
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
        title = { Text(stringResource(R.string.feuille_avis_titre, essence, classe)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.feuille_avis_aide),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = avisMoins,
                    onValueChange = { v -> avisMoins = v.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.feuille_avis_moins_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = avisPlus,
                    onValueChange = { v -> avisPlus = v.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.feuille_avis_plus_label)) },
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
            }) { Text(stringResource(R.string.feuille_enregistrer)) }
        },
        dismissButton = { TextButton(onClick = onFermer) { Text(stringResource(R.string.feuille_annuler)) } },
    )
}
