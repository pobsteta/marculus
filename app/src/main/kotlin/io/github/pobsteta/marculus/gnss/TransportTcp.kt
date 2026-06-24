package io.github.pobsteta.marculus.gnss

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Transport **TCP/IP** vers un récepteur GNSS exposant son flux NMEA sur un socket : carte à
 * interface IP (Septentrio mosaic-X5) ou compagnon WiFi (ESP32) servant le NMEA. Même contrat que
 * [TransportBluetoothSpp] : lecture en flux d'octets, écriture pour le renvoi des corrections RTCM.
 *
 * Pratique aussi **hors matériel** : pointer vers un PC rejouant un log NMEA (`nc -l <port> < trame.nmea`)
 * permet de valider toute la chaîne (parsing → fix → UI) sans récepteur ni Bluetooth.
 */
class TransportTcp(
    private val hote: String,
    private val port: Int,
    private val delaiConnexionMs: Int = 5000,
) : Transport {

    @Volatile private var sortie: OutputStream? = null

    override fun lire(): Flow<ByteArray> = flow {
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(hote, port), delaiConnexionMs)
            sortie = socket.getOutputStream()
            val entree = socket.getInputStream()
            val tampon = ByteArray(TAILLE_TAMPON)
            while (true) {
                val n = entree.read(tampon)
                if (n < 0) break
                if (n > 0) emit(tampon.copyOf(n))
            }
        } finally {
            sortie = null
            runCatching { socket.close() }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun ecrire(donnees: ByteArray) {
        runCatching { sortie?.apply { write(donnees); flush() } }
    }

    private companion object {
        const val TAILLE_TAMPON = 4096
    }
}
