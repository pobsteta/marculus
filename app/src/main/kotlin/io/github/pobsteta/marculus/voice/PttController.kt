package io.github.pobsteta.marculus.voice

import android.media.ToneGenerator
import fr.marculus.core.voice.MicroPtt
import fr.marculus.core.voice.NoyauPtt

/**
 * Point de convergence unique des déclencheurs push-to-talk (bouton micro de la feuille, appui
 * long sur le volume bas, et plus tard télécommande). **Seul habilité** à ouvrir et fermer le
 * micro : les déclencheurs ne connaissent que [demarrer]/[arreter].
 *
 * La logique de garde vit dans [NoyauPtt] (`:core`, testée sans Android) ; cette enveloppe y
 * ajoute le bip d'ouverture, obligatoire — sans lui l'opérateur parle avant que le micro soit
 * ouvert et le début de « hetre » est tronqué, ce qui se solde par un rejet.
 */
class PttController(
    private val voix: VoskVoiceService,
    private val tonalites: ToneGenerator?,
) {
    private val noyau = NoyauPtt(object : MicroPtt {
        override fun bip() {
            tonalites?.startTone(ToneGenerator.TONE_PROP_BEEP2, DUREE_BIP_MS)
        }

        override fun demarrerEcoute() = voix.demarrerEcoute()

        override fun arreterEcoute() = voix.arreterEcoute()
    })

    /** Une session d'écoute est en cours (déclencheur tenu). */
    val actif: Boolean get() = noyau.actif

    /** Appui sur un déclencheur. Sans effet si une session est déjà ouverte. */
    fun demarrer() {
        if (voix.microDisponible) noyau.demarrer()
    }

    /** Relâchement d'un déclencheur : Vosk émet le résultat final à la fermeture du micro. */
    fun arreter() {
        noyau.arreter()
    }

    /** Anti-larsen : coupe le micro le temps d'une annonce, sans clore la session. */
    fun suspendrePourAnnonce() = noyau.suspendre()

    /** Rouvre le micro après l'annonce, si le déclencheur est toujours tenu. */
    fun reprendreApresAnnonce() = noyau.reprendre()

    companion object {
        private const val DUREE_BIP_MS = 120
    }
}
