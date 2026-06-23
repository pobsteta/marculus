package fr.marculus.core.model

/** Réglages de l'application (persistés via DataStore). */
data class Reglages(
    val antiVeille: Boolean = false,
    val pleinEcran: Boolean = false,
    val vibration: Boolean = false,
    val sonClic: Boolean = false,
    val themeSombre: Boolean = false,
    val capturePosition: Boolean = false,
    /** Lit le nouveau total à voix haute à chaque comptage (synthèse vocale). */
    val annonceNombre: Boolean = false,
    /** Lit l'étiquette « essence classe » à voix haute à chaque comptage. */
    val annonceEtiquette: Boolean = false,
    /** Compter avec les boutons de volume (volume + = +, volume − = −). */
    val boutonsVolume: Boolean = false,
    /** Rouvrir le dernier contexte utilisé au lancement de l'application. */
    val rouvrirDernier: Boolean = false,
    /** Identifiant du dernier contexte ouvert (pour « rouvrir le dernier »). */
    val dernierContexteId: String? = null,
    /** Nom de la voix de synthèse choisie (Voice.name), ou null = voix par défaut. */
    val voixTts: String? = null,
    /** Nom de l'opérateur (attaché à chaque tige, pour la synchro multi-opérateurs). */
    val operateur: String? = null,
    /** Acquisition GNSS ponctuelle (au clic) plutôt qu'en écoute continue (économie de batterie). */
    val gnssPonctuel: Boolean = false,
    /** Afficher le code essence ONF (3 car.) en gros au lieu du nom complet dans les cellules. */
    val afficherCodeEssence: Boolean = false,
    /** Proposer la vue Kanban (À faire / En cours / Terminé) dans la liste des contextes. */
    val vueKanban: Boolean = false,
    /** Annoncer vocalement quand un avis − est défini et la limite inférieure non atteinte. */
    val annonceAvisMoins: Boolean = false,
    /** Annoncer vocalement quand un avis + est défini et la limite supérieure dépassée. */
    val annonceAvisPlus: Boolean = false,
)
