package io.github.pobsteta.marculus.gnss

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * Transport **série-sur-Bluetooth (SPP)** vers un récepteur GNSS appairé (Septentrio mosaic-X5,
 * Unicore UM980…) : ouvre un canal RFCOMM sur l'UUID SPP standard et expose le flux d'octets
 * entrant (NMEA). L'écriture sert au renvoi des corrections RTCM (G2).
 *
 * La permission `BLUETOOTH_CONNECT` (API 31+) doit être accordée avant d'appeler [lire] ;
 * l'appelant la vérifie (d'où `@SuppressLint("MissingPermission")`).
 */
class TransportBluetoothSpp(private val device: BluetoothDevice) : Transport {

    @Volatile private var sortie: OutputStream? = null

    @SuppressLint("MissingPermission")
    override fun lire(): Flow<ByteArray> = flow {
        val socket: BluetoothSocket = ouvrirSocket()
        try {
            sortie = socket.outputStream
            val entree = socket.inputStream
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

    /**
     * Ouvre le canal RFCOMM en essayant successivement : socket sécurisé sur l'UUID SPP, socket
     * non sécurisé, puis canal RFCOMM 1 par réflexion — contourne l'échec fréquent
     * « read failed, socket might closed or timeout, read ret: -1 » de `connect()`.
     */
    @SuppressLint("MissingPermission")
    private fun ouvrirSocket(): BluetoothSocket {
        val fabriques: List<() -> BluetoothSocket> = listOf(
            { device.createRfcommSocketToServiceRecord(UUID_SPP) },
            { device.createInsecureRfcommSocketToServiceRecord(UUID_SPP) },
            { device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType).invoke(device, 1) as BluetoothSocket },
        )
        var derniere: Exception? = null
        for (fabrique in fabriques) {
            val socket = runCatching { fabrique() }.getOrElse { derniere = it as? Exception; null } ?: continue
            try {
                socket.connect()
                return socket
            } catch (e: Exception) {
                derniere = e
                runCatching { socket.close() }
            }
        }
        throw derniere ?: IOException("Connexion Bluetooth impossible")
    }

    private companion object {
        /** UUID du profil port série (SPP) Bluetooth. */
        val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val TAILLE_TAMPON = 4096
    }
}
