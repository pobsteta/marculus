package fr.marculus.core

import fr.marculus.core.model.FixGnss
import fr.marculus.core.model.Position
import fr.marculus.core.model.QualiteFix
import kotlin.math.sqrt

/** Trame GGA décodée : position + qualité du fix + métadonnées de qualité. */
data class TrameGga(
    val position: Position,
    val qualite: QualiteFix,
    val nbSatellites: Int,
    val hdop: Double?,
    val altitudeM: Double?,
    val ageCorrectionsS: Double?,
)

/** Trame GST décodée : écarts-types de position (m) → précision horizontale estimée. */
data class TrameGst(
    val ecartTypeLatM: Double,
    val ecartTypeLonM: Double,
) {
    /** Précision horizontale (m) = norme des écarts-types latitude / longitude. */
    val precisionHorizontaleM: Double
        get() = sqrt(ecartTypeLatM * ecartTypeLatM + ecartTypeLonM * ecartTypeLonM)
}

/**
 * Analyse des trames NMEA 0183 d'un récepteur GNSS externe (Septentrio mosaic-X5, Unicore
 * UM980…). On n'exploite que **GGA** (position + qualité de fix) et **GST** (précision) ; le
 * talker (GP/GN/GL…) est ignoré. Tout est pur — testable en JVM, sans E/S ni dépendance Android.
 */
object NmeaParser {

    /** Vrai si la somme de contrôle `*hh` (XOR des octets entre `$` et `*`) est correcte. */
    fun checksumValide(phrase: String): Boolean {
        val debut = phrase.indexOf('$')
        val etoile = phrase.lastIndexOf('*')
        if (debut < 0 || etoile < debut + 1 || etoile + 3 > phrase.length) return false
        var calc = 0
        for (i in debut + 1 until etoile) calc = calc xor phrase[i].code
        val attendu = phrase.substring(etoile + 1, etoile + 3).toIntOrNull(16) ?: return false
        return calc == attendu
    }

    /** Champs d'une trame valide (sans `$` ni `*hh`), ou null si la somme de contrôle est mauvaise. */
    private fun champs(phrase: String): List<String>? {
        if (!checksumValide(phrase)) return null
        val debut = phrase.indexOf('$')
        val etoile = phrase.lastIndexOf('*')
        return phrase.substring(debut + 1, etoile).split(',')
    }

    /** Décode une trame GGA ; null si ce n'est pas une GGA valide munie d'une position. */
    fun parseGga(phrase: String): TrameGga? {
        val f = champs(phrase) ?: return null
        if (f.isEmpty() || !f[0].endsWith("GGA")) return null
        val lat = coordonnee(f.getOrNull(2), f.getOrNull(3)) ?: return null
        val lon = coordonnee(f.getOrNull(4), f.getOrNull(5)) ?: return null
        return TrameGga(
            position = Position(lat, lon),
            qualite = QualiteFix.depuisCode(f.getOrNull(6)?.toIntOrNull() ?: 0),
            nbSatellites = f.getOrNull(7)?.toIntOrNull() ?: 0,
            hdop = f.getOrNull(8)?.toDoubleOrNull(),
            altitudeM = f.getOrNull(9)?.toDoubleOrNull(),
            ageCorrectionsS = f.getOrNull(13)?.toDoubleOrNull(),
        )
    }

    /** Décode une trame GST ; null si ce n'est pas une GST valide. */
    fun parseGst(phrase: String): TrameGst? {
        val f = champs(phrase) ?: return null
        if (f.isEmpty() || !f[0].endsWith("GST")) return null
        val lat = f.getOrNull(6)?.toDoubleOrNull() ?: return null
        val lon = f.getOrNull(7)?.toDoubleOrNull() ?: return null
        return TrameGst(lat, lon)
    }

    /** Assemble un fix complet depuis une GGA et (optionnellement) la dernière GST connue. */
    fun fixDepuis(gga: TrameGga, gst: TrameGst? = null): FixGnss = FixGnss(
        position = gga.position,
        qualite = gga.qualite,
        nbSatellites = gga.nbSatellites,
        hdop = gga.hdop,
        altitudeM = gga.altitudeM,
        ageCorrectionsS = gga.ageCorrectionsS,
        precisionHorizontaleM = gst?.precisionHorizontaleM,
    )

    /** Convertit une coordonnée NMEA `ddmm.mmmm` + hémisphère en degrés décimaux signés. */
    private fun coordonnee(valeur: String?, hemisphere: String?): Double? {
        val v = valeur?.toDoubleOrNull() ?: return null
        if (hemisphere.isNullOrBlank()) return null
        val deg = (v / 100).toInt()
        val min = v - deg * 100
        val dec = deg + min / 60.0
        return when (hemisphere.uppercase()) {
            "N", "E" -> dec
            "S", "W" -> -dec
            else -> null
        }
    }
}
