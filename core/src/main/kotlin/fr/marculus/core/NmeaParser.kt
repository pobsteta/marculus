package fr.marculus.core

import fr.marculus.core.model.FixGnss
import fr.marculus.core.model.Position
import fr.marculus.core.model.QualiteFix
import fr.marculus.core.model.SatelliteGsv
import kotlin.math.sqrt

/** Trame GGA décodée : position + qualité du fix + métadonnées de qualité. */
data class TrameGga(
    val position: Position,
    val qualite: QualiteFix,
    val nbSatellites: Int,
    val hdop: Double?,
    val altitudeM: Double?,
    val ageCorrectionsS: Double?,
    val stationRef: String? = null,
)

/** Trame GSA décodée : dilutions de précision (position, horizontale, verticale). */
data class TrameGsa(val pdop: Double?, val hdop: Double?, val vdop: Double?)

/**
 * Trame RMC décodée : cap et vitesse **sur le fond** (déplacement). [capDeg] absent à l'arrêt
 * (champ vide), [vitesseMs] convertie depuis les nœuds. Sert à orienter le cône de direction.
 */
data class TrameRmc(val capDeg: Double?, val vitesseMs: Double?)

/** Une trame GSV (un message d'une séquence) : satellites en vue d'un système. */
data class TrameGsv(
    val systeme: String,
    val nbMessages: Int,
    val numMessage: Int,
    val satellitesEnVue: Int,
    val satellites: List<SatelliteGsv>,
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

    /** Un nœud (mille marin par heure) en mètres par seconde. */
    private const val NOEUD_EN_MS = 0.514444

    /** Somme de contrôle NMEA (XOR des octets du corps, hors `$` et `*hh`) en hexa deux chiffres. */
    fun sommeControle(corps: String): String {
        var c = 0
        for (ch in corps) c = c xor ch.code
        return "%02X".format(c)
    }

    /** Vrai si la somme de contrôle `*hh` (XOR des octets entre `$` et `*`) est correcte. */
    fun checksumValide(phrase: String): Boolean {
        val debut = phrase.indexOf('$')
        val etoile = phrase.lastIndexOf('*')
        if (debut < 0 || etoile < debut + 1 || etoile + 3 > phrase.length) return false
        val attendu = phrase.substring(etoile + 1, etoile + 3)
        return sommeControle(phrase.substring(debut + 1, etoile)).equals(attendu, ignoreCase = true)
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
            stationRef = f.getOrNull(14)?.takeIf { it.isNotBlank() },
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

    /** Décode une trame GSA ; null si ce n'est pas une GSA valide. PDOP/HDOP/VDOP en fin de trame. */
    fun parseGsa(phrase: String): TrameGsa? {
        val f = champs(phrase) ?: return null
        if (f.isEmpty() || !f[0].endsWith("GSA")) return null
        return TrameGsa(
            pdop = f.getOrNull(15)?.toDoubleOrNull(),
            hdop = f.getOrNull(16)?.toDoubleOrNull(),
            vdop = f.getOrNull(17)?.toDoubleOrNull(),
        )
    }

    /**
     * Décode une trame RMC ; null si ce n'est pas une RMC valide. Champs : 7 = vitesse fond (nœuds),
     * 8 = cap fond (° vrais). Le cap n'est défini que si le récepteur se déplace.
     */
    fun parseRmc(phrase: String): TrameRmc? {
        val f = champs(phrase) ?: return null
        if (f.isEmpty() || !f[0].endsWith("RMC")) return null
        val vitesseNoeuds = f.getOrNull(7)?.toDoubleOrNull()
        return TrameRmc(
            capDeg = f.getOrNull(8)?.toDoubleOrNull(),
            vitesseMs = vitesseNoeuds?.let { it * NOEUD_EN_MS },
        )
    }

    /** Décode une trame GSV (satellites en vue) ; null si ce n'est pas une GSV valide. */
    fun parseGsv(phrase: String): TrameGsv? {
        val f = champs(phrase) ?: return null
        if (f.isEmpty() || !f[0].endsWith("GSV")) return null
        val systeme = f[0].dropLast(3)
        val nbMessages = f.getOrNull(1)?.toIntOrNull() ?: return null
        val numMessage = f.getOrNull(2)?.toIntOrNull() ?: return null
        val enVue = f.getOrNull(3)?.toIntOrNull() ?: 0
        val sats = mutableListOf<SatelliteGsv>()
        var i = 4
        while (i < f.size) { // groupes de 4 (PRN, élévation, azimut, SNR) ; SNR final parfois absent
            val prn = f.getOrNull(i)?.toIntOrNull() ?: break
            sats += SatelliteGsv(
                prn = prn,
                elevation = f.getOrNull(i + 1)?.toIntOrNull(),
                azimut = f.getOrNull(i + 2)?.toIntOrNull(),
                snr = f.getOrNull(i + 3)?.toIntOrNull(),
                systeme = systeme,
            )
            i += 4
        }
        return TrameGsv(systeme, nbMessages, numMessage, enVue, sats)
    }

    /** Assemble un fix complet depuis une GGA et (optionnellement) les dernières GST, GSA et RMC. */
    fun fixDepuis(
        gga: TrameGga,
        gst: TrameGst? = null,
        gsa: TrameGsa? = null,
        rmc: TrameRmc? = null,
    ): FixGnss = FixGnss(
        position = gga.position,
        qualite = gga.qualite,
        nbSatellites = gga.nbSatellites,
        hdop = gga.hdop ?: gsa?.hdop,
        altitudeM = gga.altitudeM,
        ageCorrectionsS = gga.ageCorrectionsS,
        precisionHorizontaleM = gst?.precisionHorizontaleM,
        pdop = gsa?.pdop,
        vdop = gsa?.vdop,
        stationRef = gga.stationRef,
        capDeg = rmc?.capDeg,
        vitesseMs = rmc?.vitesseMs,
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
