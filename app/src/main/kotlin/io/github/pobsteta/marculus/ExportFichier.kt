package io.github.pobsteta.marculus

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

/**
 * Enregistre un export directement dans `Téléchargements/Marculus/`, sans sélecteur.
 *
 * Le sélecteur (SAF) crée le fichier **vide** avant qu'on y écrive : le PC branché en USB (MTP)
 * l'apprend à 0 octet et le client MTP de GNOME ignore la mise à jour qui suit — le fichier
 * restait à 0 octet jusqu'au débranchement du câble. Ici le fichier est créé **en attente**
 * (`IS_PENDING`) : le MTP ne l'annonce qu'une fois écrit, avec sa taille finale.
 *
 * Android 8–9 (pas d'`IS_PENDING`) : dossier propre à l'application, visible en MTP sous
 * `Android/data/…/files/Download/Marculus/`, sans permission à demander.
 */
object ExportFichier {
    private const val DOSSIER = "Marculus"

    /**
     * @param nom nom souhaité (nettoyé des caractères interdits ; suffixé « (1) »… s'il existe déjà)
     * @param ecrire écrit le contenu dans le flux fourni
     * @return l'emplacement lisible du fichier écrit (« Download/Marculus/x.csv »), ou `null` en cas d'échec
     */
    suspend fun enregistrer(context: Context, nom: String, mime: String, ecrire: (OutputStream) -> Unit): String? =
        withContext(Dispatchers.IO) {
            val propre = nom.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    viaMediaStore(context, propre, mime, ecrire)
                } else {
                    dansDossierApplication(context, propre, ecrire)
                }
            }.onFailure { Log.e("Marculus.Export", "Échec de l'export $propre", it) }.getOrNull()
        }

    /** Message bref : où le fichier a été écrit, ou l'échec. */
    fun annoncer(context: Context, emplacement: String?) {
        val texte = emplacement?.let { context.getString(R.string.export_ok, it) }
            ?: context.getString(R.string.export_echec)
        android.widget.Toast.makeText(context, texte, android.widget.Toast.LENGTH_LONG).show()
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun viaMediaStore(context: Context, nom: String, mime: String, ecrire: (OutputStream) -> Unit): String {
        val resolver = context.contentResolver
        val dossier = "${Environment.DIRECTORY_DOWNLOADS}/$DOSSIER"
        val valeurs = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nom)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, dossier)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valeurs)
            ?: error("MediaStore a refusé la création de $nom")
        try {
            resolver.openOutputStream(uri)?.use(ecrire) ?: error("Flux indisponible pour $nom")
            // Fin de l'attente : c'est maintenant que le MTP annonce le fichier, taille finale.
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        // Le nom réellement retenu : MediaStore suffixe « (1) » si le fichier existe déjà.
        val retenu = resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: nom
        return "$dossier/$retenu"
    }

    private fun dansDossierApplication(context: Context, nom: String, ecrire: (OutputStream) -> Unit): String {
        val dossier = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), DOSSIER).apply { mkdirs() }
        val base = nom.substringBeforeLast('.')
        val ext = nom.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        var fichier = File(dossier, nom)
        var n = 1
        while (fichier.exists()) fichier = File(dossier, "$base ($n)$ext").also { n++ }
        fichier.outputStream().use(ecrire)
        return fichier.absolutePath
    }
}
