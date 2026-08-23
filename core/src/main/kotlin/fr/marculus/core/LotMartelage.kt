package fr.marculus.core

/**
 * Lot de chantiers de martelage : une archive ZIP **plate** contenant un fichier `.marsync`
 * (tous les contextes) et un GeoPackage par contexte, apparié par le champ `gpkgNom`.
 *
 * Ce fichier ne contient que les décisions ; la lecture de l'archive et les copies de fichiers
 * vivent dans `:data`. Un lot légitime est produit par Nemeton, mais rien ne garantit que
 * l'archive ouverte en soit un : c'est ici qu'on décide de ce qu'on refuse.
 */
object LotMartelage {

    const val EXTENSION_MARSYNC = ".marsync"
    const val EXTENSION_GPKG = ".gpkg"

    /**
     * Une entrée d'archive est acceptable si son nom est un **nom de fichier nu**. Le lot est
     * plat par construction : tout séparateur de chemin, tout `..`, tout nom vide est déjà une
     * anomalie. On **refuse** plutôt qu'on assainit — un chemin réécrit en silence est la
     * définition même de la faille « zip slip », et ce qui reste ne serait de toute façon pas
     * le lot attendu.
     */
    fun entreeAcceptee(nom: String): Boolean =
        nom.isNotBlank() &&
            !nom.contains('/') &&
            !nom.contains('\\') &&
            !nom.contains("..") &&
            nom != "." &&
            !nom.startsWith("~")

    fun estMarsync(nom: String): Boolean = nom.endsWith(EXTENSION_MARSYNC, ignoreCase = true)

    fun estGpkg(nom: String): Boolean = nom.endsWith(EXTENSION_GPKG, ignoreCase = true)

    /**
     * Nom de fichier sous lequel le GeoPackage d'un lot est rangé dans le stockage privé.
     * **Déterministe** : ré-importer le même lot écrase le fichier au lieu d'accumuler des
     * copies. Les caractères hors ASCII imprimable sont neutralisés — `gpkgNom` est annoncé
     * ASCII, mais l'archive n'est pas nécessairement celle qu'on croit.
     */
    fun nomLocal(gpkgNom: String): String =
        "lot-" + gpkgNom.map { if (it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in "._-") it else '_' }
            .joinToString("")

    /** Ce qu'un contexte du lot attend : son identité, et le GeoPackage qui devrait l'accompagner. */
    data class ContexteDuLot(val id: String, val nom: String, val gpkgNom: String?)

    /** Un GeoPackage présent dans l'archive, à rattacher à un contexte. */
    data class Rattachement(val contexteId: String, val gpkgNom: String)

    /**
     * Résultat de l'appariement : ce qui est rattachable, et les contextes qui resteront **sans
     * carte**. Un lot amputé reste utile — douze chantiers valent mieux que zéro — mais l'écart
     * doit être dit : un contexte muet dont on ignore pourquoi il n'a pas de carte est pire
     * qu'un message.
     */
    data class Appariement(
        val rattachements: List<Rattachement>,
        val contextesSansGpkg: List<String>,
    )

    /** Apparie les contextes du `.marsync` aux GeoPackages réellement présents dans l'archive. */
    fun apparier(contextes: List<ContexteDuLot>, gpkgPresents: Set<String>): Appariement {
        val rattachements = mutableListOf<Rattachement>()
        val sansGpkg = mutableListOf<String>()
        contextes.forEach { c ->
            val nom = c.gpkgNom
            when {
                // Un contexte sans `gpkgNom` n'attend pas de carte : ce n'est pas un manque.
                nom == null -> Unit
                nom in gpkgPresents -> rattachements.add(Rattachement(c.id, nom))
                else -> sansGpkg.add(c.nom)
            }
        }
        return Appariement(rattachements, sansGpkg)
    }
}
