package io.github.pobsteta.marculus.ui.feuille

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.pobsteta.marculus.R
import fr.marculus.core.voice.FrenchNumbers
import fr.marculus.core.voice.OptionsHauteur
import fr.marculus.core.voice.ReferentielParle
import fr.marculus.core.voice.SpokenQualite
import fr.marculus.core.voice.VoiceCommands
import fr.marculus.core.voice.VoiceEvent
import io.github.pobsteta.marculus.voice.ModeleVosk
import io.github.pobsteta.marculus.voice.PttController
import io.github.pobsteta.marculus.voice.VocabulaireVosk
import io.github.pobsteta.marculus.voice.VoskVoiceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dictée vocale vue par la feuille de martelage : un état observable et quatre gestes
 * (démarrer, arrêter, annonce terminée, simuler). L'écran ne voit ni Vosk, ni le micro,
 * ni la grammaire — seul [PttController] ouvre et ferme l'écoute.
 */
@Stable
class EtatDictee internal constructor(
    private val service: VoskVoiceService,
    private val ptt: PttController,
) {
    /** Modèle chargé et grammaire construite : les énoncés du contexte sont interprétables. */
    var pret: Boolean by mutableStateOf(false)
        internal set

    /** Micro utilisable : sans lui, la grammaire existe mais aucun déclencheur n'ouvre l'écoute. */
    var microPret: Boolean by mutableStateOf(false)
        internal set

    /** Micro ouvert (déclencheur tenu). */
    var enEcoute: Boolean by mutableStateOf(false)
        internal set

    /** Le modèle vocal n'est pas encore téléchargé. */
    var modeleAbsent: Boolean by mutableStateOf(false)
        internal set

    /** Formes à dicter pour les essences du contexte : libellé → énoncé attendu. */
    var formesEssences: List<Pair<String, String>> by mutableStateOf(emptyList())
        internal set

    /** Formes à dicter pour les qualités du référentiel : code → énoncé attendu. */
    var formesQualites: List<Pair<String, String>> by mutableStateOf(emptyList())
        internal set

    /** Lettres de qualité bois dictables dans la découpe : lettre → mot radio. */
    var formesBois: List<Pair<Char, String>> by mutableStateOf(emptyList())
        internal set

    /**
     * Énoncé d'exemple construit sur le contexte réellement ouvert. Un exemple figé dans les
     * textes finit par mentir : les formes dépendent des essences et du référentiel de qualités.
     */
    var exemple: String by mutableStateOf("")
        internal set

    /** Appui sur un déclencheur PTT (bouton micro de l'écran, appui long sur le volume bas). */
    fun demarrer() {
        if (!microPret) return
        ptt.demarrer()
        enEcoute = ptt.actif
    }

    /** Relâchement du déclencheur : Vosk livre son résultat final à la fermeture du micro. */
    fun arreter() {
        ptt.arreter()
        enEcoute = ptt.actif
    }

    /** Anti-larsen : coupe le micro avant une annonce, sans clore la session PTT. */
    internal fun suspendrePourAnnonce() = ptt.suspendrePourAnnonce()

    /** Fin de l'annonce TTS : le micro se rouvre si le déclencheur est toujours tenu. */
    fun annonceTerminee() = ptt.reprendreApresAnnonce()

    /** Le mode rafale repart de zéro (changement de parcelle rattachée). */
    fun reinitialiserRafale() {
        service.essenceCourante = null
    }

    /** Exemple d'énoncé de hauteur pour ce contexte (vide si la découpe n'est pas dictable). */
    var exempleHauteur: String by mutableStateOf("")
        internal set

    /** Dictée simulée : injecte un énoncé sans micro (démo et recette sur émulateur). */
    fun simuler(texte: String) = service.injecterTexte(texte)
}

/** Hauteur maximale dictable, en mètres : au-delà, ce n'est plus un arbre de nos forêts. */
private const val HAUTEUR_MAX_M = 50

/** Valeurs de l'énoncé d'exemple affiché dans l'aide. */
private const val EXEMPLE_HAUTEUR_M = 27
private const val EXEMPLE_BILLON_M = 6

/** Aiguillage des événements Vosk vers la composition courante (les rappels changent, pas le service). */
private class AiguillageVocal {
    var traiter: (VoiceEvent) -> Unit = {}
}

/**
 * Prépare la dictée vocale pour le contexte affiché. Le recognizer est reconstruit à chaque
 * changement d'essences, d'axe de classes ou de qualités.
 *
 * @param actif au moins un déclencheur est activé dans les réglages
 * @param onTige insertion d'une tige dictée par le mécanisme existant (UUID, GNSS, annonce TTS) ;
 *   la hauteur est non nulle quand elle a été dictée dans le même énoncé
 * @param onQualite qualité dictée seule → annote la dernière tige, comme le bouton Q
 * @param onAnnule commande « annule » → annulation par événement du journal append-only
 * @param onRepete commande « repete » → ré-annonce de la dernière tige
 * @param onRejet énoncé non compris → vibration double + « non compris », jamais d'insertion devinée
 */
@Composable
fun rememberDicteeVocale(
    actif: Boolean,
    essences: List<String>,
    classes: List<Int>,
    qualites: List<String>,
    qualitesBois: List<String>,
    tonalites: ToneGenerator?,
    onTige: (essence: String, classe: Int, qualite: String?, hauteur: String?) -> Unit,
    onHauteur: (texte: String) -> Unit,
    onQualite: (code: String) -> Unit,
    onAnnule: () -> Unit,
    onRepete: () -> Unit,
    onRejet: () -> Unit,
): EtatDictee {
    val context = LocalContext.current
    val aiguillage = remember { AiguillageVocal() }
    val service = remember { VoskVoiceService(context) { aiguillage.traiter(it) } }
    val ptt = remember { PttController(service, tonalites) }
    val dictee = remember { EtatDictee(service, ptt) }

    // Code métier renvoyé par le parseur → libellé exact de la colonne du contexte.
    var codesVersNoms by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Réarmé à chaque composition : les rappels de l'écran (journal, TTS) sont recréés à chaque fois.
    SideEffect {
        aiguillage.traiter = { evenement ->
            // Anti-larsen : le micro est coupé AVANT toute annonce ; il rouvrira sur `onDone`.
            dictee.suspendrePourAnnonce()
            when (evenement) {
                is VoiceEvent.Tige -> {
                    val nom = codesVersNoms[evenement.codeOnf]
                    if (nom == null) {
                        onRejet()
                    } else {
                        onTige(nom, evenement.classe, evenement.qualite, evenement.hauteurTexte)
                    }
                }

                is VoiceEvent.Hauteur -> onHauteur(evenement.texte)

                is VoiceEvent.Qualite -> onQualite(evenement.code)

                is VoiceEvent.Commande -> when (evenement.nom) {
                    VoiceCommands.ANNULE -> onAnnule()
                    VoiceCommands.REPETE -> onRepete()
                    else -> dictee.annonceTerminee()
                }

                is VoiceEvent.Rejet -> onRejet()
            }
        }
    }

    // Permission micro : même mécanique que la permission de localisation du GNSS interne.
    var micAutorise by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val demandeMicro = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        micAutorise = it
    }
    LaunchedEffect(actif, micAutorise) {
        if (actif && !micAutorise) demandeMicro.launch(Manifest.permission.RECORD_AUDIO)
    }

    // (Re)construction de la grammaire fermée : essences de la matrice, axe des classes, qualités.
    LaunchedEffect(actif, micAutorise, essences, classes, qualites, qualitesBois) {
        dictee.pret = false
        dictee.microPret = false
        dictee.modeleAbsent = false
        if (!actif || !micAutorise) return@LaunchedEffect
        if (!ModeleVosk.estInstalle(context)) {
            dictee.modeleAbsent = true
            return@LaunchedEffect
        }
        // Le modèle ignore silencieusement les mots hors de son lexique : on lui demande ce
        // qu'il connaît avant de promettre quoi que ce soit à l'opérateur.
        val connus = withContext(Dispatchers.IO) {
            VocabulaireVosk.motsConnus(
                ModeleVosk.dossier(context),
                ReferentielParle.motsCandidats(essences, qualites),
            )
        }
        val motConnu: (String) -> Boolean = { it in connus }
        val parlees = ReferentielParle.essences(essences, motConnu)
        val qualitesParlees: List<SpokenQualite> = ReferentielParle.qualites(qualites, motConnu)
        val lettresBois = ReferentielParle.lettresBois(qualitesBois)
        val optionsHauteur = if (lettresBois.isEmpty()) {
            null
        } else {
            OptionsHauteur(
                maxMetres = HAUTEUR_MAX_M,
                lettresBois = lettresBois,
                motRadio = { ReferentielParle.motRadio(it, motConnu) },
            )
        }
        codesVersNoms = parlees.associate { it.codeOnf to it.nom }
        dictee.formesEssences = parlees.map { it.nom to it.spoken.joinToString(" ") }
        dictee.formesQualites = qualitesParlees.map { it.code to it.spoken }
        dictee.formesBois = lettresBois.map { it to ReferentielParle.motRadio(it, motConnu) }
        dictee.exempleHauteur = optionsHauteur?.let { options ->
            val lettre = options.motRadio(lettresBois.first())
            "${VoiceCommands.HAUTEUR} " + FrenchNumbers.toTokens(EXEMPLE_HAUTEUR_M).joinToString(" ") +
                " " + FrenchNumbers.toTokens(EXEMPLE_BILLON_M).joinToString(" ") + " " + lettre
        } ?: ""
        dictee.exemple = listOfNotNull(
            parlees.firstOrNull()?.spoken?.joinToString(" "),
            classes.getOrNull(classes.size / 2)?.let { FrenchNumbers.toTokens(it).joinToString(" ") },
            qualitesParlees.firstOrNull()?.spoken,
        ).joinToString(" ")
        val configure = service.chargerModele().mapCatching {
            service.configurerPourContexte(
                parlees.map { it.versGrammaire() },
                classes,
                qualitesParlees,
                optionsHauteur,
            ).getOrThrow()
        }
        dictee.pret = configure.isSuccess && service.grammairePrete
        dictee.microPret = dictee.pret && service.microDisponible
    }

    DisposableEffect(service) {
        onDispose { service.liberer() }
    }
    return dictee
}

/**
 * Actions de la feuille de martelage offertes aux déclencheurs vocaux. Elles sont posées par
 * l'écran une fois le contexte chargé : la dictée, elle, existe dès la première composition.
 */
class ActionsVocales {
    var tige: (essence: String, classe: Int, qualite: String?, hauteur: String?) -> Unit =
        { _, _, _, _ -> }
    var hauteur: (texte: String) -> Unit = {}
    var qualite: (code: String) -> Unit = {}
    var annule: () -> Unit = {}
    var repete: () -> Unit = {}
    var rejet: () -> Unit = {}
}

/**
 * Bouton micro maintenu appuyé : l'écoute dure exactement le temps de l'appui — relâcher, c'est
 * demander à Vosk son résultat final.
 *
 * Le push-to-talk est piloté par les **interactions du bouton** et non par un détecteur de gestes
 * posé par-dessus : `FloatingActionButton` installe son propre `clickable` à l'intérieur du
 * modifier qu'on lui passe, consomme l'appui, et un `detectTapGestures` extérieur ne verrait
 * jamais l'événement. `PressInteraction` donne en prime le cas « doigt glissé hors du bouton »
 * (Cancel), qui doit refermer le micro comme un relâchement.
 *
 * **Taille** : le FAB Material par défaut (56 dp) est une cible de doigt nu. Le déclencheur de
 * dictée se cherche **en gant**, sans regarder l'écran, dans le coin de l'écran : il fait donc
 * [TAILLE_MICRO] de côté, soit **quatre fois la surface** d'un FAB standard, avec une icône
 * agrandie dans la même proportion.
 */
@Composable
fun BoutonMicroPtt(
    pret: Boolean,
    enEcoute: Boolean,
    onAppui: () -> Unit,
    onRelache: () -> Unit,
) {
    val couleur = when {
        !pret -> MaterialTheme.colorScheme.surfaceVariant
        enEcoute -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val interactions = remember { MutableInteractionSource() }
    val appui by rememberUpdatedState(onAppui)
    val relache by rememberUpdatedState(onRelache)
    LaunchedEffect(interactions) {
        interactions.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> appui()
                is PressInteraction.Release, is PressInteraction.Cancel -> relache()
                else -> Unit
            }
        }
    }
    FloatingActionButton(
        onClick = {}, // l'action utile est l'appui maintenu, pas le clic
        containerColor = couleur,
        interactionSource = interactions,
        modifier = Modifier.size(TAILLE_MICRO),
    ) {
        Icon(
            imageVector = if (pret) Icons.Filled.Mic else Icons.Filled.MicOff,
            contentDescription = stringResource(R.string.voix_micro_description),
            modifier = Modifier.size(ICONE_MICRO),
        )
    }
}

/** Côté du bouton micro : 4 × la surface du FAB Material (56 dp), pour une cible gantée. */
internal val TAILLE_MICRO = 112.dp

/** Icône du micro, agrandie dans la même proportion que le bouton (24 dp × 2). */
private val ICONE_MICRO = 48.dp

/** Aide-mémoire : ce qu'il faut dire pour ce contexte, essence par essence. */
@Composable
fun DialogueFormesParlees(
    dictee: EtatDictee,
    pttEcran: Boolean,
    pttVolume: Boolean,
    onFermer: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.voix_formes_titre)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when {
                    !pttEcran && !pttVolume -> Text(stringResource(R.string.voix_aucun_declencheur))
                    dictee.modeleAbsent -> Text(stringResource(R.string.voix_modele_absent))
                    !dictee.pret -> Text(stringResource(R.string.voix_indisponible))
                    !dictee.microPret -> Text(stringResource(R.string.voix_micro_indisponible))
                    else -> Text(stringResource(R.string.voix_formes_aide, dictee.exemple))
                }
                if (dictee.formesEssences.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(stringResource(R.string.voix_formes_essences), style = MaterialTheme.typography.titleSmall)
                    dictee.formesEssences.forEach { (libelle, parle) ->
                        Text("$libelle → « $parle »", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (dictee.formesQualites.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(stringResource(R.string.voix_formes_qualites), style = MaterialTheme.typography.titleSmall)
                    dictee.formesQualites.forEach { (code, parle) ->
                        Text("$code → « $parle »", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        stringResource(R.string.voix_qualite_seule_note),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (dictee.formesBois.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(stringResource(R.string.voix_formes_hauteur), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.voix_formes_hauteur_aide, dictee.exempleHauteur),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        dictee.formesBois.joinToString("  ·  ") { (lettre, parle) -> "$lettre → « $parle »" },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text(stringResource(R.string.voix_formes_commandes), style = MaterialTheme.typography.bodySmall)
                if (dictee.pret) SimulationDictee(dictee)
            }
        },
        confirmButton = { TextButton(onClick = onFermer) { Text(stringResource(R.string.liste_dialog_fermer)) } },
    )
}

/**
 * Dictée simulée, réservée aux builds de débogage : injecte un énoncé Vosk sans micro. C'est le
 * chemin de la démo sur émulateur, où la capture audio n'est pas exploitable.
 */
@Composable
private fun SimulationDictee(dictee: EtatDictee) {
    val context = LocalContext.current
    val debogage = remember {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    if (!debogage) return
    var texte by remember { mutableStateOf("") }
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
    OutlinedTextField(
        value = texte,
        onValueChange = { texte = it },
        label = { Text(stringResource(R.string.voix_simuler)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = { dictee.simuler(texte) }) { Text(stringResource(R.string.voix_simuler_action)) }
}
