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
     * Ce que contient un `.marsync`, lu dans le contenu (clés `contextes[].nom|operateur` et `tiges[]`,
     * voir `SauvegardeRepository.exporterContexteJson`). [nbTiges] compte les éléments de `tiges`,
     * annulations comprises (une annulation est une ligne du journal). Fonction pure de [contenu] ;
     * null si le contenu n'est pas un `.marsync` reconnu.
     */
    data class Resume(
        val nbContextes: Int,
        /** Noms des contextes, sans les noms absents ou vides, dans l'ordre du fichier. */
        val noms: List<String>,
        val nbTiges: Int,
        /** Opérateurs distincts (contextes puis tiges), sans doublon ni vide. */
        val operateurs: List<String>,
    )

    fun resume(contenu: String): Resume? {
        val racine = runCatching { JSONObject(contenu) }.getOrNull() ?: return null
        val contextes = racine.opt("contextes") as? JSONArray
        val tiges = racine.opt("tiges") as? JSONArray
        if (contextes == null && tiges == null) return null
        fun texte(o: Any?, cle: String): String? =
            (o as? JSONObject)?.optString(cle, "")?.trim()?.takeIf { it.isNotEmpty() }
        val ctx = contextes?.let { a -> List(a.length()) { a.opt(it) } }.orEmpty()
        val tg = tiges?.let { a -> List(a.length()) { a.opt(it) } }.orEmpty()
        return Resume(
            nbContextes = ctx.size,
            noms = ctx.mapNotNull { texte(it, "nom") },
            nbTiges = tg.size,
            operateurs = (ctx.mapNotNull { texte(it, "operateur") } + tg.mapNotNull { texte(it, "operateur") }).distinct(),
        )
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
