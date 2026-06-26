package fr.marculus.core.model

/** Lien physique vers le récepteur GNSS externe. */
enum class TransportRtk { BLUETOOTH, TCP }

/**
 * Configuration du récepteur GNSS externe RTK et du caster NTRIP. [pontNtrip] = false signifie que
 * le récepteur (ou son compagnon) fait lui-même les corrections (topologie A) ; true = l'application
 * tire le RTCM du caster et le renvoie au récepteur (topologie B). Caster pré-rempli Centipede.
 */
data class ConfigRtk(
    val actif: Boolean = false,
    val transport: TransportRtk = TransportRtk.BLUETOOTH,
    /** Adresse de l'appareil Bluetooth appairé (MAC), ou null. */
    val appareilBt: String? = null,
    /** Nom lisible de l'appareil Bluetooth choisi (affichage). */
    val appareilBtNom: String? = null,
    val hoteTcp: String = "192.168.4.1",
    val portTcp: Int = 2947,
    val pontNtrip: Boolean = false,
    val casterHote: String = "crtk.net",
    val casterPort: Int = 2101,
    val mountpoint: String = "NEAR",
    val utilisateur: String = "centipede",
    val motDePasse: String = "centipede",
)

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
    /**
     * Estimer la hauteur de la tige depuis la couche « houppier » du GPKG (MNH) : à la saisie, on
     * cherche le houppier contenant la position GNSS et on pré‑remplit H = `h_max`. Sans effet tant
     * que le GPKG importé ne contient pas cette couche. Voir docs/specs/couche-houppier-mnh.md.
     */
    val estimerHauteurMnh: Boolean = false,
    /** Afficher le code essence ONF (3 car.) en gros au lieu du nom complet dans les cellules. */
    val afficherCodeEssence: Boolean = false,
    /** Proposer la vue Kanban (À faire / En cours / Terminé) dans la liste des contextes. */
    val vueKanban: Boolean = false,
    /** Annoncer vocalement quand un avis − est défini et la limite inférieure non atteinte. */
    val annonceAvisMoins: Boolean = false,
    /** Annoncer vocalement quand un avis + est défini et la limite supérieure dépassée. */
    val annonceAvisPlus: Boolean = false,
    /** Récepteur GNSS externe RTK + caster NTRIP. */
    val rtk: ConfigRtk = ConfigRtk(),
)
