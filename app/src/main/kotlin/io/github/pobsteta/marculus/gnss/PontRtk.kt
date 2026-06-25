package io.github.pobsteta.marculus.gnss

import fr.marculus.core.AccumulateurSkyplot
import fr.marculus.core.NmeaDecoupeur
import fr.marculus.core.NmeaParser
import fr.marculus.core.TrameGsa
import fr.marculus.core.TrameGst
import fr.marculus.core.TrameRmc
import fr.marculus.core.model.FixGnss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/** Événements remontés par le pont, pour le suivi et le diagnostic du lien. */
sealed interface EvenementRtk {
    /** Des octets bruts ont été reçus du récepteur (preuve que le lien BT/TCP est actif). */
    data class Octets(val n: Int) : EvenementRtk

    /** Une trame NMEA complète a été reçue (telle quelle). */
    data class Trame(val ligne: String) : EvenementRtk

    /** Un fix complet a été décodé (présence d'une GGA). */
    data class Fix(val fix: FixGnss) : EvenementRtk

    /** Des octets RTCM (corrections) ont été renvoyés vers le récepteur (sens téléphone → GNSS). */
    data class Rtcm(val n: Int) : EvenementRtk
}

/**
 * Orchestrateur RTK reliant un [Transport] (récepteur) et, en topologie B, un [ClientNtrip].
 * Émet un flux d'[EvenementRtk] : octets reçus (lien actif), trames NMEA brutes (communication),
 * et fix décodés. Trois flux concurrents : montant (NMEA→événements), descendant (RTCM→récepteur),
 * VRS (renvoi périodique de la GGA au caster).
 */
class PontRtk(
    private val transport: Transport,
    private val clientNtrip: ClientNtrip? = null,
    private val intervalleGgaMs: Long = 10_000L,
) {
    fun evenements(): Flow<EvenementRtk> = channelFlow {
        val decoupeur = NmeaDecoupeur()
        var derniereGst: TrameGst? = null
        var derniereGsa: TrameGsa? = null
        var derniereRmc: TrameRmc? = null
        val skyplot = AccumulateurSkyplot()
        val derniereGgaBrute = AtomicReference<String?>(null)

        clientNtrip?.let { ntrip ->
            launch(Dispatchers.IO) {
                ntrip.corrections().collect { rtcm ->
                    transport.ecrire(rtcm)
                    send(EvenementRtk.Rtcm(rtcm.size))
                }
            }
            launch {
                while (isActive) {
                    delay(intervalleGgaMs)
                    derniereGgaBrute.get()?.let { ntrip.envoyerGga(it) }
                }
            }
        }

        transport.lire().collect { octets ->
            send(EvenementRtk.Octets(octets.size))
            for (trame in decoupeur.pousser(octets.toString(Charsets.US_ASCII))) {
                send(EvenementRtk.Trame(trame))
                NmeaParser.parseGst(trame)?.let { derniereGst = it }
                NmeaParser.parseGsa(trame)?.let { derniereGsa = it }
                NmeaParser.parseRmc(trame)?.let { derniereRmc = it }
                NmeaParser.parseGsv(trame)?.let { skyplot.pousser(it) }
                NmeaParser.parseGga(trame)?.let { gga ->
                    derniereGgaBrute.set(trame)
                    send(EvenementRtk.Fix(NmeaParser.fixDepuis(gga, derniereGst, derniereGsa, derniereRmc).copy(satellites = skyplot.satellites())))
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
