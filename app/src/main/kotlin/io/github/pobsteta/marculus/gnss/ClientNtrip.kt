package io.github.pobsteta.marculus.gnss

import fr.marculus.core.Ntrip
import fr.marculus.core.StatutNtrip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.OutputStream
import java.net.Socket

/**
 * Client NTRIP (côté **E/S**) : ouvre une connexion TCP au caster (Centipede), envoie la requête
 * `GET mountpoint` (auth Basic, cf. [Ntrip.requete]), vérifie le statut, puis expose le flux des
 * octets **RTCM3** reçus. La trame GGA du rover (réseaux VRS) est renvoyée via [envoyerGga].
 * Le framing du protocole est dans `:core` ([Ntrip]).
 */
class ClientNtrip(
    private val hote: String,
    private val port: Int,
    private val mountpoint: String,
    private val utilisateur: String,
    private val motDePasse: String,
) {
    @Volatile private var sortie: OutputStream? = null

    /** Flux des corrections RTCM3 ; se termine si l'authentification échoue ou à la fermeture. */
    fun corrections(): Flow<ByteArray> = flow {
        val socket = Socket(hote, port)
        try {
            val out = socket.getOutputStream()
            out.write(Ntrip.requete(mountpoint, hote, port, utilisateur, motDePasse).toByteArray(Charsets.US_ASCII))
            out.flush()
            sortie = out
            val entree = socket.getInputStream()
            // En-tête de réponse NTRIP/2.0 : lire jusqu'à la ligne vide, contrôler le statut.
            val enTete = StringBuilder()
            while (!enTete.endsWith("\r\n\r\n") && enTete.length < TAILLE_ENTETE_MAX) {
                val b = entree.read()
                if (b < 0) break
                enTete.append(b.toChar())
            }
            if (Ntrip.statutReponse(enTete.lineSequence().firstOrNull().orEmpty()) != StatutNtrip.OK) {
                return@flow
            }
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

    /** Renvoie une trame GGA au caster (sélection VRS). Sans effet si la connexion n'est pas ouverte. */
    suspend fun envoyerGga(phrase: String) {
        runCatching { sortie?.apply { write((phrase + "\r\n").toByteArray(Charsets.US_ASCII)); flush() } }
    }

    private companion object {
        const val TAILLE_TAMPON = 4096
        const val TAILLE_ENTETE_MAX = 4096
    }
}
