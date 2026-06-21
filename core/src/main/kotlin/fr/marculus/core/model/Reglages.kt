package fr.marculus.core.model

/** Réglages de l'application (persistés via DataStore). */
data class Reglages(
    val antiVeille: Boolean = false,
    val pleinEcran: Boolean = false,
    val vibration: Boolean = false,
    val sonClic: Boolean = false,
    val themeSombre: Boolean = false,
    val capturePosition: Boolean = false,
)
