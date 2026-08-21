package fr.marculus.core.voice

/**
 * Le micro vu par le noyau push-to-talk : trois gestes, aucune dépendance Android.
 * L'implémentation réelle (bip système + Vosk) vit dans `:app`.
 */
interface MicroPtt {
    /** Bip d'ouverture — obligatoire : sans lui, l'opérateur parle avant l'ouverture du micro. */
    fun bip()
    fun demarrerEcoute()
    fun arreterEcoute()
}

/**
 * Machine à états du push-to-talk, partagée par tous les déclencheurs (bouton écran, appui long
 * sur le volume, télécommande à venir). Le garde [actif] neutralise les doubles démarrages : deux
 * sources tenues en même temps n'ouvrent qu'une seule session d'écoute, et la première relâchée
 * la referme.
 *
 * Pure JVM pour rester testable sans micro ni modèle ; `PttController` (`:app`) en est l'enveloppe
 * Android et reste le seul point autorisé à appeler start/stopListening.
 */
class NoyauPtt(private val micro: MicroPtt) {

    var actif: Boolean = false
        private set

    /** Ouvre le micro si aucune session n'est en cours. Renvoie true si l'écoute vient de démarrer. */
    fun demarrer(): Boolean {
        if (actif) return false
        actif = true
        micro.bip()
        micro.demarrerEcoute()
        return true
    }

    /** Ferme le micro si une session est en cours. Renvoie true si l'écoute vient de s'arrêter. */
    fun arreter(): Boolean {
        if (!actif) return false
        actif = false
        micro.arreterEcoute()
        return true
    }

    /**
     * Coupe le micro sans clore la session (anti-larsen pendant une annonce TTS) : l'écoute
     * reprend par [reprendre] quand la synthèse est terminée, si le déclencheur est toujours tenu.
     */
    fun suspendre() {
        if (actif) micro.arreterEcoute()
    }

    /** Rouvre le micro après une annonce, uniquement si le déclencheur est toujours tenu. */
    fun reprendre() {
        if (actif) micro.demarrerEcoute()
    }
}
