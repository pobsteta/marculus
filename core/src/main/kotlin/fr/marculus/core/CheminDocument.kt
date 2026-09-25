package fr.marculus.core

/**
 * Chemin de fichier d'un document SAF, quand son identifiant le donne. Sert à faire réindexer un
 * fichier qu'on vient d'écrire : sans cela, le MTP (câble USB vers un PC) l'annonce à 0 octet —
 * il garde la taille du document vide que le sélecteur a créé avant qu'on y écrive.
 */
object CheminDocument {
    const val STOCKAGE_EXTERNE = "com.android.externalstorage.documents"
    const val TELECHARGEMENTS = "com.android.providers.downloads.documents"

    /**
     * @param racinePrimaire racine du stockage interne partagé (`/storage/emulated/0` en général)
     * @return le chemin absolu, ou `null` si l'identifiant ne le porte pas (ex. `msf:123`, qu'il
     *   faut résoudre par MediaStore)
     */
    fun depuisIdentifiant(autorite: String?, idDocument: String?, racinePrimaire: String): String? {
        if (autorite == null || idDocument == null) return null
        return when (autorite) {
            STOCKAGE_EXTERNE -> {
                val volume = idDocument.substringBefore(':', "")
                val relatif = idDocument.substringAfter(':', "")
                when {
                    volume.isEmpty() || relatif.isEmpty() -> null
                    volume.equals("primary", ignoreCase = true) -> "${racinePrimaire.trimEnd('/')}/$relatif"
                    else -> "/storage/$volume/$relatif"
                }
            }
            TELECHARGEMENTS -> idDocument.takeIf { it.startsWith("raw:/") }?.removePrefix("raw:")
            else -> null
        }
    }

    /** Identifiant MediaStore d'un document des Téléchargements (`msf:123` → 123), ou `null`. */
    fun idMediaStore(autorite: String?, idDocument: String?): Long? =
        if (autorite == TELECHARGEMENTS && idDocument?.startsWith("msf:") == true) {
            idDocument.removePrefix("msf:").toLongOrNull()
        } else {
            null
        }
}
