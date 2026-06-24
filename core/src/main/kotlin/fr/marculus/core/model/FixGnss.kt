package fr.marculus.core.model

/**
 * Qualité du fix GNSS, d'après le champ « indicateur de qualité » de la trame NMEA GGA.
 * RTK_FIXE = ambiguïtés résolues (précision centimétrique) ; RTK_FLOAT = solution flottante
 * (décimétrique) ; en deçà, la correction RTK n'est pas (ou plus) acquise.
 */
enum class QualiteFix(val codeNmea: Int, val libelle: String) {
    INVALIDE(0, "Pas de fix"),
    AUTONOME(1, "Autonome"),
    DGPS(2, "DGPS"),
    PPS(3, "PPS"),
    RTK_FIXE(4, "RTK fixe"),
    RTK_FLOAT(5, "RTK flottant"),
    ESTIME(6, "Estimé"),
    MANUEL(7, "Manuel"),
    SIMULATION(8, "Simulation"),
    ;

    /** Vrai pour une solution corrigée RTK (fixe ou flottante). */
    val estRtk: Boolean get() = this == RTK_FIXE || this == RTK_FLOAT

    companion object {
        /** Qualité correspondant au code NMEA ; INVALIDE pour tout code inconnu. */
        fun depuisCode(code: Int): QualiteFix = entries.firstOrNull { it.codeNmea == code } ?: INVALIDE
    }
}

/**
 * Fix GNSS instantané dérivé des trames NMEA (GGA + GST). Représente une position issue d'une
 * source « externe » (récepteur RTK) ou interne ; l'UI s'appuie dessus sans connaître l'origine.
 * Les champs nullables sont absents quand la trame ne les fournit pas.
 */
data class FixGnss(
    val position: Position,
    val qualite: QualiteFix,
    val nbSatellites: Int,
    val hdop: Double?,
    val altitudeM: Double?,
    val ageCorrectionsS: Double?,
    val precisionHorizontaleM: Double?,
    val pdop: Double? = null,
    val vdop: Double? = null,
    /** Identifiant de la station de référence des corrections (champ 14 de la GGA). */
    val stationRef: String? = null,
)
