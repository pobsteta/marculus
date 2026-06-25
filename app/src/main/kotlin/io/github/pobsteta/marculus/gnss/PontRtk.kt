package io.github.pobsteta.marculus.gnss

import fr.marculus.core.AccumulateurSkyplot
import fr.marculus.core.NmeaDecoupeur
import fr.marculus.core.NmeaParser
import fr.marculus.core.Ntrip
import fr.marculus.core.StatutNtrip
import fr.marculus.core.TrameGsa
import fr.marculus.core.TrameGst
import fr.marculus.core.TrameRmc
import fr.marculus.core.model.FixGnss
import fr.marculus.core.model.Position
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

    /** État de la connexion au caster NTRIP ([statut] renvoyé, ou [message] d'erreur réseau). */
    data class Ntrip(val statut: StatutNtrip?, val message: String? = null) : EvenementRtk
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
    /** Position approchée du rover pour amorcer un mountpoint VRS/NEAR quand le récepteur n'a pas
     *  encore émis de GGA (typiquement la dernière position connue du GNSS interne du téléphone). */
    private val positionRover: () -> Position? = { null },
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
                // Une erreur NTRIP (caster injoignable, 401…) ne doit pas tuer le lien récepteur :
                // on l'isole et on la remonte comme événement de diagnostic.
                runCatching {
                    ntrip.corrections { statut -> trySend(EvenementRtk.Ntrip(statut)) }.collect { rtcm ->
                        transport.ecrire(rtcm)
                        send(EvenementRtk.Rtcm(rtcm.size))
                    }
                }.onFailure { e -> trySend(EvenementRtk.Ntrip(null, e.message ?: "erreur NTRIP")) }
            }
            // Renvoi de la GGA au caster (sélection VRS/NEAR). On envoie DÈS que la connexion est
            // ouverte (sans attendre 10 s) : la GGA du récepteur si disponible, sinon une GGA
            // synthétisée depuis la position du rover, pour amorcer un mountpoint NEAR même avant
            // que le récepteur ait un fix. Cadence rapide jusqu'au premier envoi, puis périodique.
            launch {
                var amorce = false
                while (isActive) {
                    if (ntrip.connecte) {
                        val gga = derniereGgaBrute.get() ?: positionRover()?.let { Ntrip.gga(it) }
                        if (gga != null) {
                            ntrip.envoyerGga(gga)
                            amorce = true
                        }
                    }
                    delay(if (amorce) intervalleGgaMs else 500L)
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
