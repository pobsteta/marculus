package io.github.pobsteta.marculus.data

import android.content.Context
import android.net.Uri
import android.util.Log
import fr.marculus.core.LotMartelage
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Réception d'un **lot de chantiers** produit par Nemeton : une archive ZIP plate contenant un
 * `.marsync` (tous les contextes) et un GeoPackage par contexte.
 *
 * Ce dépôt est délibérément **séparé** de la restauration de sauvegarde. Les deux lisent un ZIP,
 * mais la restauration *efface* contextes, tiges et configs avant d'insérer, là où un lot
 * **fusionne** : le confondre coûterait une journée de martelage. Aucun chemin de ce fichier ne
 * mène à `importerJson()`.
 */
class LotRepository(
    private val context: Context,
    private val sauvegarde: SauvegardeRepository,
    private val martelage: MartelageRepository,
) {
    /** Ce qu'a produit l'import, ou pourquoi il n'a rien produit. */
    sealed interface Resultat {
        /**
         * [contextes] créés ou mis à jour par fusion, [rattaches] GeoPackages copiés et reliés,
         * [sansGpkg] contextes dont le GeoPackage annoncé manquait à l'archive.
         */
        data class Ok(val contextes: Int, val rattaches: Int, val sansGpkg: List<String>) : Resultat

        /** Archive lisible, mais aucun `.marsync` : ce n'est pas un lot. */
        data object SansMarsync : Resultat

        /** Plusieurs `.marsync` : on ne devine pas lequel fait foi. */
        data object MarsyncMultiple : Resultat

        /** Entrée au nom anormal (chemin, `..`) : l'archive entière est refusée. */
        data class EntreeRefusee(val nom: String) : Resultat

        /** Archive illisible, ou JSON inexploitable. */
        data object Illisible : Resultat
    }

    /**
     * Importe le lot désigné par [uri]. L'archive est extraite dans le cache, puis les
     * GeoPackages retenus sont **déplacés** dans le stockage privé sous un nom déterministe —
     * ré-importer le même lot écrase au lieu d'accumuler.
     */
    suspend fun importer(uri: Uri): Resultat {
        val tempo = File(context.cacheDir, "lot-${System.currentTimeMillis()}")
        try {
            if (!tempo.mkdirs()) return Resultat.Illisible
            val extrait = extraire(uri, tempo) ?: return Resultat.Illisible
            if (extrait is Extraction.Refus) return Resultat.EntreeRefusee(extrait.nom)
            val ok = extrait as Extraction.Ok
            if (ok.marsyncs.isEmpty()) return Resultat.SansMarsync
            if (ok.marsyncs.size > 1) return Resultat.MarsyncMultiple

            val json = ok.marsyncs.single()
            val contextes = runCatching { sauvegarde.contextesDuLot(json) }.getOrNull()
                ?: return Resultat.Illisible

            // Le lot n'émet pas `cheminGpkg` (la machine émettrice ignore nos chemins privés) :
            // si Nemeton ré-émet un contexte plus récent, la fusion le remplacerait donc par un
            // contexte sans carte. On note ce qui est rattaché avant, pour le rendre après à ceux
            // que le lot ne rattache pas lui-même — un rattachement fait à la main survit.
            val avant = contextes.mapNotNull { c ->
                martelage.contexte(c.id)?.cheminGpkg?.let { c.id to it }
            }.toMap()

            runCatching { sauvegarde.fusionnerJson(json) }.getOrElse { return Resultat.Illisible }

            val appariement = LotMartelage.apparier(contextes, ok.gpkgs.keys)
            var rattaches = 0
            appariement.rattachements.forEach { r ->
                val source = ok.gpkgs[r.gpkgNom] ?: return@forEach
                val cible = File(context.filesDir, LotMartelage.nomLocal(r.gpkgNom))
                if (deplacer(source, cible)) {
                    martelage.enregistrerCheminGpkg(r.contexteId, cible.absolutePath)
                    rattaches++
                }
            }
            val reattaches = appariement.rattachements.map { it.contexteId }.toSet()
            avant.forEach { (id, chemin) ->
                if (id !in reattaches && martelage.contexte(id)?.cheminGpkg == null) {
                    martelage.enregistrerCheminGpkg(id, chemin)
                }
            }
            return Resultat.Ok(
                contextes = contextes.size,
                rattaches = rattaches,
                sansGpkg = appariement.contextesSansGpkg,
            )
        } catch (e: Exception) {
            Log.e("Marculus.Lot", "importer", e)
            return Resultat.Illisible
        } finally {
            tempo.deleteRecursively()
        }
    }

    private sealed interface Extraction {
        /** [marsyncs] contenus JSON lus en mémoire, [gpkgs] nom d'origine → fichier extrait. */
        data class Ok(val marsyncs: List<String>, val gpkgs: Map<String, File>) : Extraction
        data class Refus(val nom: String) : Extraction
    }

    /**
     * Parcourt l'archive une seule fois : les `.marsync` (quelques dizaines de Ko) sont gardés en
     * mémoire, les GeoPackages (plusieurs Mo chacun) écrits sur disque au fil du flux.
     *
     * Toute entrée dont le nom n'est pas un nom de fichier nu **arrête l'import** : on ne
     * réécrit pas un chemin en silence. Les entrées d'un autre type sont simplement ignorées —
     * une archive peut porter un `README` sans cesser d'être un lot.
     */
    private fun extraire(uri: Uri, destination: File): Extraction? =
        context.contentResolver.openInputStream(uri)?.use { entree ->
            ZipInputStream(entree.buffered()).use { zis ->
                val marsyncs = mutableListOf<String>()
                val gpkgs = mutableMapOf<String, File>()
                var e = zis.nextEntry
                while (e != null) {
                    val nom = e.name
                    if (!e.isDirectory) {
                        if (!LotMartelage.entreeAcceptee(nom)) return@use Extraction.Refus(nom)
                        when {
                            LotMartelage.estMarsync(nom) -> marsyncs.add(zis.readBytes().decodeToString())
                            LotMartelage.estGpkg(nom) -> {
                                val fichier = File(destination, LotMartelage.nomLocal(nom))
                                fichier.outputStream().use { sortie -> zis.copyTo(sortie) }
                                gpkgs[nom] = fichier
                            }
                        }
                    }
                    e = zis.nextEntry
                }
                Extraction.Ok(marsyncs, gpkgs)
            }
        }

    /** Déplacement dans le stockage privé ; repli sur une copie si le renommage échoue. */
    private fun deplacer(source: File, cible: File): Boolean {
        if (cible.exists() && !cible.delete()) return false
        if (source.renameTo(cible)) return true
        return runCatching { source.copyTo(cible, overwrite = true) }.isSuccess
    }
}
