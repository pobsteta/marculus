package io.github.pobsteta.marculus.gnss

import fr.marculus.core.NmeaDecoupeur
import fr.marculus.core.NmeaParser
import fr.marculus.core.TrameGst
import fr.marculus.core.model.FixGnss
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Source de position alimentée par un récepteur **externe** via un [Transport] : reconstitue les
 * trames NMEA ([NmeaDecoupeur]) puis les décode ([NmeaParser]). Chaque GGA est enrichie de la
 * **dernière GST** connue (précision horizontale).
 */
class SourcePositionExterne(private val transport: Transport) : SourcePosition {
    override fun fixs(): Flow<FixGnss?> = flow {
        val decoupeur = NmeaDecoupeur()
        var derniereGst: TrameGst? = null
        transport.lire().collect { octets ->
            for (trame in decoupeur.pousser(octets.toString(Charsets.US_ASCII))) {
                NmeaParser.parseGst(trame)?.let { derniereGst = it }
                NmeaParser.parseGga(trame)?.let { gga -> emit(NmeaParser.fixDepuis(gga, derniereGst)) }
            }
        }
    }
}
