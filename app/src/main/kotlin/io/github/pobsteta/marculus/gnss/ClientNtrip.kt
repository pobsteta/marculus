package io.github.pobsteta.marculus.gnss

import fr.marculus.core.EntreeSourcetable
import fr.marculus.core.Ntrip
import fr.marculus.core.StatutNtrip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
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

    /** Vrai quand la connexion au caster est ouverte (sortie disponible pour renvoyer la GGA). */
    val connecte: Boolean get() = sortie != null

    /**
     * Flux des corrections RTCM3 ; se termine si l'authentification échoue ou à la fermeture.
     * [onStatut] est appelé avec le statut renvoyé par le caster (OK, 401, sourcetable…), pour le
     * diagnostic dans l'UI.
     */
    fun corrections(onStatut: (StatutNtrip) -> Unit = {}): Flow<ByteArray> = flow {
        val socket = Socket(hote, port)
        try {
            val out = socket.getOutputStream()
            out.write(Ntrip.requete(mountpoint, hote, port, utilisateur, motDePasse).toByteArray(Charsets.US_ASCII))
            out.flush()
            sortie = out
            val entree = socket.getInputStream()
            // Ligne de statut (compatible v1 « ICY 200 OK » et v2 « HTTP/1.1 200 OK »).
            val statutLigne = lireLigne(entree)
            val statut = Ntrip.statutReponse(statutLigne)
            onStatut(statut)
            if (statut != StatutNtrip.OK) return@flow
            // NTRIP v2 (HTTP) : consommer les en-têtes jusqu'à la ligne vide.
            // NTRIP v1 (ICY) : pas d'en-têtes, le flux RTCM suit directement.
            if (!statutLigne.startsWith("ICY")) {
                var total = 0
                while (total < TAILLE_ENTETE_MAX) {
                    val ligne = lireLigne(entree)
                    total += ligne.length + 2
                    if (ligne.isEmpty()) break
                }
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

    /**
     * Renvoie une trame GGA au caster (sélection VRS/NEAR). Renvoie le nombre d'octets écrits
     * (0 si la connexion n'est pas ouverte ou en cas d'échec d'écriture).
     */
    suspend fun envoyerGga(phrase: String): Int {
        val donnees = (phrase + "\r\n").toByteArray(Charsets.US_ASCII)
        return runCatching { sortie?.let { it.write(donnees); it.flush(); donnees.size } ?: 0 }.getOrDefault(0)
    }

    /** Lit une ligne (jusqu'au LF) sans le CRLF final ; renvoie "" en fin de flux. */
    private fun lireLigne(entree: InputStream): String {
        val sb = StringBuilder()
        while (sb.length < 1024) {
            val b = entree.read()
            if (b < 0 || b == '\n'.code) break
            if (b != '\r'.code) sb.append(b.toChar())
        }
        return sb.toString()
    }

    companion object {
        private const val TAILLE_TAMPON = 4096
        private const val TAILLE_ENTETE_MAX = 4096

        /**
         * Interroge le caster (requête `GET /`) et renvoie ses points de montage (sourcetable).
         * Liste vide en cas d'échec (hôte injoignable, timeout…). Exécuté sur le dispatcher I/O.
         */
        suspend fun chargerMountpoints(hote: String, port: Int): List<EntreeSourcetable> =
            withContext(Dispatchers.IO) {
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(hote, port), 5000)
                    socket.soTimeout = 8000
                    socket.getOutputStream().apply {
                        write(Ntrip.requete("", hote, port, "", "").toByteArray(Charsets.US_ASCII))
                        flush()
                    }
                    val sortie = ByteArrayOutputStream()
                    val tampon = ByteArray(TAILLE_TAMPON)
                    val entree = socket.getInputStream()
                    runCatching {
                        while (true) {
                            val n = entree.read(tampon)
                            if (n < 0) break
                            sortie.write(tampon, 0, n)
                            if (sortie.size() > 256 * 1024) break
                            if (sortie.toString("US-ASCII").contains("ENDSOURCETABLE")) break
                        }
                    }
                    Ntrip.parseSourcetable(sortie.toString("US-ASCII"))
                } catch (_: Exception) {
                    emptyList()
                } finally {
                    runCatching { socket.close() }
                }
            }
    }
}
