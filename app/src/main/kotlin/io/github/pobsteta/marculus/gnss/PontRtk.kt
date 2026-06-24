package io.github.pobsteta.marculus.gnss

import fr.marculus.core.AccumulateurSkyplot
import fr.marculus.core.NmeaDecoupeur
import fr.marculus.core.NmeaParser
import fr.marculus.core.TrameGsa
import fr.marculus.core.TrameGst
import fr.marculus.core.model.FixGnss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * Orchestrateur RTK reliant un [Transport] (récepteur) et, en topologie B, un [ClientNtrip].
 * Trois flux concurrents :
 *  - **montant** : NMEA du récepteur → [FixGnss] émis ;
 *  - **descendant** : corrections RTCM du caster → écrites vers le récepteur ;
 *  - **VRS** : renvoi périodique de la dernière trame GGA du récepteur au caster.
 *
 * Sans [clientNtrip] (topologie A : récepteur autonome), seul le flux montant tourne.
 */
class PontRtk(
    private val transport: Transport,
    private val clientNtrip: ClientNtrip? = null,
    private val intervalleGgaMs: Long = 10_000L,
) {
    fun fixs(): Flow<FixGnss?> = channelFlow {
        val decoupeur = NmeaDecoupeur()
        var derniereGst: TrameGst? = null
        var derniereGsa: TrameGsa? = null
        val skyplot = AccumulateurSkyplot()
        val derniereGgaBrute = AtomicReference<String?>(null)

        clientNtrip?.let { ntrip ->
            // Descendant : RTCM du caster → récepteur.
            launch(Dispatchers.IO) { ntrip.corrections().collect { transport.ecrire(it) } }
            // VRS : renvoi périodique de la GGA courante du récepteur au caster.
            launch {
                while (isActive) {
                    delay(intervalleGgaMs)
                    derniereGgaBrute.get()?.let { ntrip.envoyerGga(it) }
                }
            }
        }

        // Montant : NMEA du récepteur → FixGnss.
        transport.lire().collect { octets ->
            for (trame in decoupeur.pousser(octets.toString(Charsets.US_ASCII))) {
                NmeaParser.parseGst(trame)?.let { derniereGst = it }
                NmeaParser.parseGsa(trame)?.let { derniereGsa = it }
                NmeaParser.parseGsv(trame)?.let { skyplot.pousser(it) }
                NmeaParser.parseGga(trame)?.let { gga ->
                    derniereGgaBrute.set(trame)
                    send(NmeaParser.fixDepuis(gga, derniereGst, derniereGsa).copy(satellites = skyplot.satellites()))
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
