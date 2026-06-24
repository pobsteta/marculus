package fr.marculus.core

import fr.marculus.core.model.Position
import java.util.Base64
import java.util.Locale
import kotlin.math.abs

/** Une entrée `STR` de la sourcetable NTRIP : un point de montage proposé par le caster. */
data class EntreeSourcetable(val mountpoint: String, val identifiant: String, val format: String)

/** Statut de la réponse d'un caster à une requête NTRIP. */
enum class StatutNtrip { OK, SOURCETABLE, NON_AUTORISE, INCONNU }

/**
 * Protocole NTRIP côté **client**, pur et testable : fabrication de la requête, génération de la
 * trame GGA renvoyée au caster (réseaux **VRS** : sélection de la base la plus proche), lecture de
 * la sourcetable et du statut de réponse. L'E/S réseau elle-même est dans `:app` (`ClientNtrip`).
 */
object Ntrip {

    /** Requête NTRIP/2.0 « GET mountpoint » avec authentification Basic, terminée par une ligne vide. */
    fun requete(
        mountpoint: String,
        hote: String,
        port: Int,
        utilisateur: String,
        motDePasse: String,
        agent: String = "Marculus",
    ): String {
        val mp = if (mountpoint.startsWith("/")) mountpoint else "/$mountpoint"
        val auth = Base64.getEncoder().encodeToString("$utilisateur:$motDePasse".toByteArray(Charsets.UTF_8))
        return buildString {
            append("GET $mp HTTP/1.1\r\n")
            append("Host: $hote:$port\r\n")
            append("Ntrip-Version: Ntrip/2.0\r\n")
            append("User-Agent: NTRIP $agent\r\n")
            append("Authorization: Basic $auth\r\n")
            append("\r\n")
        }
    }

    /**
     * Trame `$GPGGA` annonçant la position du rover au caster (sélection VRS de la base la plus
     * proche). [heureUtc] au format `hhmmss.ss`, fourni par l'appelant → fonction déterministe.
     */
    fun gga(
        position: Position,
        heureUtc: String = "000000.00",
        qualite: Int = 1,
        nbSatellites: Int = 10,
        hdop: Double = 1.0,
        altitudeM: Double = 0.0,
    ): String {
        val (lat, ns) = degVersNmea(position.latitude, estLatitude = true)
        val (lon, ew) = degVersNmea(position.longitude, estLatitude = false)
        val corps = String.format(
            Locale.ROOT,
            "GPGGA,%s,%s,%s,%s,%s,%d,%02d,%.1f,%.1f,M,0.0,M,,",
            heureUtc, lat, ns, lon, ew, qualite, nbSatellites, hdop, altitudeM,
        )
        return "\$$corps*${NmeaParser.sommeControle(corps)}"
    }

    /** Points de montage (lignes `STR;`) extraits d'une sourcetable. */
    fun parseSourcetable(texte: String): List<EntreeSourcetable> =
        texte.lineSequence().filter { it.startsWith("STR;") }.mapNotNull { ligne ->
            val f = ligne.split(';')
            if (f.size < 4) null else EntreeSourcetable(f[1], f[2], f[3])
        }.toList()

    /** Statut déduit de la première ligne de la réponse du caster (ICY/HTTP 200, 401, sourcetable). */
    fun statutReponse(premiereLigne: String): StatutNtrip {
        val l = premiereLigne.trim()
        return when {
            l.contains("401") -> StatutNtrip.NON_AUTORISE
            l.startsWith("SOURCETABLE") -> StatutNtrip.SOURCETABLE
            l.contains("200") -> StatutNtrip.OK
            else -> StatutNtrip.INCONNU
        }
    }

    /** Degrés décimaux → `ddmm.mmmm` (latitude) / `dddmm.mmmm` (longitude) + hémisphère. */
    private fun degVersNmea(degres: Double, estLatitude: Boolean): Pair<String, String> {
        val hemisphere = when {
            estLatitude -> if (degres >= 0) "N" else "S"
            else -> if (degres >= 0) "E" else "W"
        }
        val a = abs(degres)
        val d = a.toInt()
        val minutes = (a - d) * 60.0
        val largeur = if (estLatitude) 2 else 3
        return String.format(Locale.ROOT, "%0${largeur}d%07.4f", d, minutes) to hemisphere
    }
}
