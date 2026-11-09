package io.github.pobsteta.marculus.data

import java.io.ByteArrayOutputStream
import java.io.InputStream
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reconnaissance d'un fichier `.marsync` reçu (« Ouvrir avec » / « Partager »). Le contenu décide,
 * pas le nom : un `content://` n'a pas toujours de nom lisible, et un `.marsync` illisible est refusé.
 * Accepte un objet JSON portant `contextes` ou `tiges` sous forme de tableau (une sauvegarde complète
 * convient, comme pour le bouton des Paramètres).
 */
object FichierMarsync {
    @Suppress("UNUSED_PARAMETER")
    fun estMarsync(nom: String?, contenu: String): Boolean {
        if (contenu.isBlank()) return false
        val racine = runCatching { JSONObject(contenu) }.getOrNull() ?: return false
        return racine.opt("contextes") is JSONArray || racine.opt("tiges") is JSONArray
    }

    /**
     * Plafond de taille d'un fichier reçu : 50 Mo. Un `.marsync` de contexte pèse quelques Mo au plus ;
     * Marculus s'offre pour tout `application/octet-stream` (ZIP, APK...), qu'il ne doit pas lire en entier.
     */
    const val TAILLE_MAX_OCTETS: Long = 50L * 1024 * 1024

    /** Vrai si la taille annoncée est acceptable ; une taille inconnue (null ou négative) passe, la lecture étant bornée. */
    fun taillePermise(taille: Long?): Boolean = taille == null || taille < 0 || taille <= TAILLE_MAX_OCTETS

    /**
     * Lit le flux en UTF-8 sans jamais dépasser [max] + 1 octet ; null si le flux dépasse [max] octets.
     */
    fun lireBorne(flux: InputStream, max: Long = TAILLE_MAX_OCTETS): String? {
        val sortie = ByteArrayOutputStream()
        val tampon = ByteArray(8192)
        var total = 0L
        while (true) {
            val reste = max + 1 - total
            if (reste <= 0) return null
            val n = flux.read(tampon, 0, minOf(tampon.size.toLong(), reste).toInt())
            if (n < 0) break
            total += n
            if (total > max) return null
            sortie.write(tampon, 0, n)
        }
        return sortie.toString(Charsets.UTF_8.name())
    }
}
