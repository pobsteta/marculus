package io.github.pobsteta.marculus

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import fr.marculus.core.CheminDocument

/**
 * À appeler après avoir écrit un fichier choisi par le sélecteur (SAF). Le sélecteur crée le
 * document **vide** avant qu'on y écrive, et le serveur MTP garde cette taille : branché en USB,
 * le PC voit et copie un fichier de 0 octet. Réindexer le fichier rafraîchit ce que voit le MTP.
 * Sans effet, sans erreur, sur un document qui n'est pas un fichier local (Drive…).
 *
 * **Réindexé plusieurs fois** : une réindexation immédiate arrive avant que le PC ait fini de
 * traiter la création du fichier vide, et il garde alors 0 octet (constaté sur le téléphone de
 * terrain) ; quelques secondes plus tard, elle est prise en compte.
 */
object FichierEcrit {
    /** Délais des réindexations, après l'écriture (ms). */
    private val DELAIS_MS = longArrayOf(0L, 3_000L, 15_000L)

    fun signaler(context: Context, uri: Uri) {
        val chemin = runCatching { chemin(context, uri) }.getOrNull() ?: return
        val app = context.applicationContext
        val principal = Handler(Looper.getMainLooper())
        DELAIS_MS.forEach { delai ->
            principal.postDelayed({
                MediaScannerConnection.scanFile(app, arrayOf(chemin), null) { p, _ ->
                    Log.d("Marculus.Fichier", "Réindexé pour le MTP (+$delai ms) : $p")
                }
            }, delai)
        }
    }

    private fun chemin(context: Context, uri: Uri): String? {
        if (!DocumentsContract.isDocumentUri(context, uri)) return null
        val autorite = uri.authority
        val id = DocumentsContract.getDocumentId(uri)
        @Suppress("DEPRECATION")
        val racine = Environment.getExternalStorageDirectory().absolutePath
        CheminDocument.depuisIdentifiant(autorite, id, racine)?.let { return it }
        // Téléchargements (`msf:123`) : l'identifiant est celui de MediaStore, qui connaît le chemin.
        val idMedia = CheminDocument.idMediaStore(autorite, id) ?: return null
        @Suppress("DEPRECATION")
        val colonne = MediaStore.MediaColumns.DATA
        return context.contentResolver.query(
            MediaStore.Files.getContentUri("external", idMedia), arrayOf(colonne), null, null, null,
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }
}
