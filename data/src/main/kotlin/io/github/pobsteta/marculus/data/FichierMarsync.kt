package io.github.pobsteta.marculus.data

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
}
