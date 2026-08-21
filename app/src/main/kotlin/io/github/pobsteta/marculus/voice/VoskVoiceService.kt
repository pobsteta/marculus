package io.github.pobsteta.marculus.voice

import android.content.Context
import android.util.Log
import fr.marculus.core.voice.GrammarBuilder
import fr.marculus.core.voice.SpokenEssence
import fr.marculus.core.voice.SpokenQualite
import fr.marculus.core.voice.UtteranceParser
import fr.marculus.core.voice.VoiceEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

/**
 * Enveloppe Android de Vosk (module `:app`) : cycle de vie modèle → recognizer → micro.
 *
 * Points d'architecture :
 *  - le modèle est **téléchargé** au premier usage (cf. [ModeleVosk]), jamais embarqué ;
 *  - le recognizer est **reconstruit à chaque changement de contexte de martelage** : la grammaire
 *    fermée dépend des essences de la matrice, de l'axe des classes et des qualités du référentiel ;
 *  - l'ouverture/fermeture du micro n'est **jamais** appelée directement : elle passe par
 *    [PttController], seul point de convergence des déclencheurs push-to-talk ;
 *  - anti-larsen : le micro est coupé avant toute annonce TTS et rouvert après `onDone`.
 *
 * Les événements sont remontés sur le thread principal (Vosk poste ses callbacks sur le Looper
 * principal), ce qui autorise l'appelant à toucher l'état Compose sans passer par un dispatcher.
 */
class VoskVoiceService(
    context: Context,
    private val onEvent: (VoiceEvent) -> Unit,
) {
    private val appContext = context.applicationContext

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null
    private var parser: UtteranceParser? = null

    /** Essence courante du mode rafale (« hetre quarante » puis « quarante cinq », « cinquante »…). */
    var essenceCourante: String? = null

    /** La grammaire du contexte est en place : les énoncés peuvent être interprétés. */
    val grammairePrete: Boolean get() = parser != null

    /**
     * Le micro est utilisable. Faux quand la capture audio n'a pas pu s'ouvrir (émulateur sans
     * micro, permission révoquée en cours de route) : la grammaire reste alors exploitable pour
     * une dictée simulée, mais aucun déclencheur PTT ne doit ouvrir d'écoute.
     */
    val microDisponible: Boolean get() = speechService != null

    /** Charge le modèle téléchargé. Opération lourde (~1 s) : appelée hors du thread principal. */
    suspend fun chargerModele(): Result<Unit> = withContext(Dispatchers.IO) {
        if (model != null) return@withContext Result.success(Unit)
        runCatching {
            require(ModeleVosk.estInstalle(appContext)) { "Modèle vocal absent" }
            model = Model(ModeleVosk.dossier(appContext).absolutePath)
        }
    }

    /**
     * (Re)construit la grammaire fermée et le recognizer pour un contexte de martelage.
     * À appeler à l'ouverture de la feuille et à chaque changement de matrice/axe/référentiel.
     */
    suspend fun configurerPourContexte(
        essences: List<SpokenEssence>,
        classes: List<Int>,
        qualites: List<SpokenQualite>,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val m = model ?: error("Modèle Vosk non chargé")
            libererRecognizer()
            val grammaire = GrammarBuilder.buildJson(essences, classes, qualites)
            parser = UtteranceParser(GrammarBuilder.buildLexicon(essences, classes, qualites))
            essenceCourante = null
            val r = Recognizer(m, FREQUENCE_HZ, grammaire)
            recognizer = r
            // Échec d'ouverture de la capture : on garde la grammaire, on perd seulement le micro.
            speechService = runCatching { SpeechService(r, FREQUENCE_HZ) }
                .onFailure { Log.w(TAG, "Capture audio indisponible", it) }
                .getOrNull()
        }.onFailure { Log.w(TAG, "Configuration du recognizer impossible", it) }
    }

    /** Ouvre le micro. Réservé à [PttController]. */
    fun demarrerEcoute() {
        val service = speechService ?: return
        runCatching {
            recognizer?.reset() // repart d'un état propre à chaque appui PTT
            service.startListening(ecouteur)
        }.onFailure { Log.w(TAG, "Ouverture du micro impossible", it) }
    }

    /** Coupe le micro — relâchement du PTT, ou avant une annonce TTS. Réservé à [PttController]. */
    fun arreterEcoute() {
        runCatching { speechService?.stop() }
            .onFailure { Log.w(TAG, "Fermeture du micro impossible", it) }
    }

    /**
     * Injecte un énoncé comme s'il venait du décodeur : sert la démo et la recette sur émulateur,
     * où il n'y a pas de micro exploitable. Passe par le même parseur que la dictée réelle.
     */
    fun injecterTexte(texte: String) = traiter(texte)

    /** Libère le micro et le recognizer, en gardant le modèle chargé. */
    fun libererRecognizer() {
        runCatching {
            speechService?.stop()
            speechService?.shutdown()
        }
        speechService = null
        runCatching { recognizer?.close() }
        recognizer = null
        parser = null
    }

    /** Libère tout, modèle compris (sortie de la feuille de martelage). */
    fun liberer() {
        libererRecognizer()
        runCatching { model?.close() }
        model = null
    }

    private fun traiter(texte: String) {
        if (texte.isBlank()) return
        val evenement = parser?.parse(texte, essenceCourante) ?: return
        if (evenement is VoiceEvent.Tige) essenceCourante = evenement.codeOnf
        onEvent(evenement)
    }

    private val ecouteur = object : RecognitionListener {
        override fun onResult(hypothesis: String) {
            traiter(runCatching { JSONObject(hypothesis).optString("text") }.getOrDefault(""))
        }

        override fun onFinalResult(hypothesis: String) = onResult(hypothesis)

        override fun onPartialResult(hypothesis: String) = Unit

        override fun onError(exception: Exception) {
            Log.w(TAG, "Erreur de reconnaissance", exception)
        }

        override fun onTimeout() = Unit
    }

    companion object {
        private const val TAG = "VoskVoiceService"

        /** Fréquence d'échantillonnage attendue par les modèles Vosk. */
        const val FREQUENCE_HZ = 16000.0f
    }
}
