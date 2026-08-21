package io.github.pobsteta.marculus.voice

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext

/**
 * Cycle de vie du modèle acoustique Vosk `vosk-model-small-fr-0.22` (Apache 2.0, ~42 Mo).
 *
 * **Choix : téléchargement au premier usage plutôt qu'embarquement dans les assets.** L'APK
 * release pèse ~11 Mo ; embarquer le modèle le porterait à ~53 Mo, soit cinq fois plus lourd à
 * publier, à télécharger et à installer — pour une fonction optionnelle, désactivée par défaut.
 * Le modèle est donc tiré une seule fois depuis les Paramètres (Wi-Fi conseillé, avant la sortie
 * en forêt) et vit ensuite dans le stockage interne : la reconnaissance reste 100 % hors ligne.
 */
object ModeleVosk {

    const val URL_MODELE = "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip"
    const val TAILLE_MO = 42

    private const val DOSSIER = "vosk-model-fr"
    private const val TEMOIN = ".installe"

    /** Dossier d'installation du modèle (stockage interne privé de l'application). */
    fun dossier(context: Context): File = File(context.filesDir, DOSSIER)

    /** Le modèle est présent et complet (le témoin n'est écrit qu'en fin de décompression). */
    fun estInstalle(context: Context): Boolean = File(dossier(context), TEMOIN).exists()

    /** Supprime le modèle installé (libère ~42 Mo). */
    fun supprimer(context: Context) {
        dossier(context).deleteRecursively()
    }

    /**
     * Télécharge et décompresse le modèle. [progression] reçoit un ratio 0..1 (ou -1 tant que la
     * taille annoncée est inconnue). Renvoie un [Result] : l'appelant affiche l'erreur telle quelle.
     * Annulable — un téléchargement interrompu laisse le dossier incomplet, donc sans témoin.
     */
    suspend fun telecharger(context: Context, progression: (Float) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            val cible = dossier(context)
            val archive = File(context.cacheDir, "vosk-model-fr.zip")
            runCatching {
                cible.deleteRecursively()
                archive.delete()
                val connexion = (URL(URL_MODELE).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                }
                try {
                    val total = connexion.contentLengthLong
                    connexion.inputStream.use { entree ->
                        archive.outputStream().use { sortie ->
                            val tampon = ByteArray(64 * 1024)
                            var cumul = 0L
                            while (true) {
                                coroutineContext.ensureActive()
                                val lu = entree.read(tampon)
                                if (lu < 0) break
                                sortie.write(tampon, 0, lu)
                                cumul += lu
                                // 95 % de la barre pour le réseau, le reste pour la décompression.
                                progression(if (total > 0) (cumul.toFloat() / total) * 0.95f else -1f)
                            }
                        }
                    }
                } finally {
                    connexion.disconnect()
                }
                decompresser(archive, cible)
                File(cible, TEMOIN).writeText(URL_MODELE)
                progression(1f)
            }.onFailure {
                cible.deleteRecursively()
            }.also {
                archive.delete()
            }
        }

    /**
     * Décompresse l'archive en supprimant son dossier racine (`vosk-model-small-fr-0.22/`), pour
     * que [dossier] pointe directement sur le modèle. Les entrées sortant du dossier cible
     * (« zip slip ») sont refusées.
     */
    private fun decompresser(archive: File, cible: File) {
        cible.mkdirs()
        val racine = cible.canonicalFile
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                val entree = zip.nextEntry ?: break
                val relatif = entree.name.substringAfter('/', missingDelimiterValue = "")
                if (relatif.isEmpty()) {
                    zip.closeEntry()
                    continue
                }
                val destination = File(cible, relatif)
                require(destination.canonicalPath.startsWith(racine.path + File.separator)) {
                    "Entrée d'archive hors du dossier cible : ${entree.name}"
                }
                if (entree.isDirectory) {
                    destination.mkdirs()
                } else {
                    destination.parentFile?.mkdirs()
                    destination.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
            }
        }
    }
}
