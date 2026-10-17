package io.github.pobsteta.marculus.data

import android.content.Context
import android.net.Uri
import android.util.Log
import fr.marculus.core.CouchesGpkg
import fr.nemeton.terrain.geo.Geodesie
import fr.marculus.core.GrilleWebMercator
import fr.marculus.core.MatriceTuiles
import fr.marculus.core.Houppier
import fr.marculus.core.TypeCouche
import fr.nemeton.terrain.geo.Position
import fr.nemeton.terrain.gpkg.Entite
import fr.nemeton.terrain.gpkg.Systeme
import fr.nemeton.terrain.gpkg.GeoPackage as GeoPackageVecteur
import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.geopackage.tiles.reproject.TileReprojection
import mil.nga.geopackage.tiles.reproject.TileReprojectionOptimize
import mil.nga.geopackage.tiles.retriever.GeoPackageTileRetriever
import mil.nga.geopackage.tiles.user.TileDao
import java.io.File

/** Suffixe de la table ortho reprojetée en Web Mercator, créée à la première ouverture. */
private const val SUFFIXE_WM = "_wm"

/**
 * Fournisseur de tuiles ortho en grille Web Mercator (XYZ), servi à osmdroid. À fermer après usage.
 *
 * @param lire tuile (zoom, x, y) : lue telle quelle quand le GPKG est déjà sur la grille, sinon
 *   extraite de la table reprojetée.
 */
class OrthoSource(
    private val gpkg: GeoPackage,
    private val lire: (zoom: Int, x: Int, y: Int) -> ByteArray?,
    val zoomMin: Int,
    val zoomMax: Int,
) {
    @Synchronized
    fun tuile(zoom: Int, x: Int, y: Int): ByteArray? =
        try {
            lire(zoom, x, y)
        } catch (e: Exception) {
            null
        }

    @Synchronized
    fun fermer() {
        try {
            gpkg.close()
        } catch (_: Exception) {
        }
    }
}

/** Une parcelle lue du GPKG : identifiant, hiérarchie foncière détectée, étiquette, attributs et anneaux (WGS84). */
data class ParcelleGpkg(
    val id: Long,
    val label: String,
    val proprietaire: String?,
    /** Co-propriétaires en indivision (propriétaire scindé sur ; / &), sinon liste à un élément. */
    val proprietaires: List<String>,
    val foret: String?,
    val parcelleNom: String?,
    val commune: String?,
    /** Surface calculée depuis la géométrie (hectares). */
    val surfaceHa: Double,
    val attributs: Map<String, String>,
    val anneaux: List<List<Position>>,
)

/** Un tronçon de desserte lu du GPKG : routes, pistes et chemins, en lignes (WGS84). */
data class DesserteGpkg(
    val id: Long,
    /** Libellé à afficher (nom, à défaut nature, à défaut rien). */
    val label: String?,
    /** Nature du tronçon (route empierrée, piste, cloisonnement…), si l'attribut existe. */
    val type: String?,
    /** Une polyligne par tronçon ; un MULTILINESTRING en donne plusieurs. */
    val lignes: List<List<Position>>,
)

/** Lecture d'un GeoPackage (parcelles vectorielles, reprojetées en WGS84 pour la carte). */
class GpkgRepository(private val context: Context) {

    /** Copie le GPKG choisi dans le stockage privé (nom horodaté unique) et renvoie son chemin. */
    fun importer(uri: Uri): String? {
        // Nom unique : chaque contexte garde son propre GPKG (pas de suppression globale).
        val dest = File(context.filesDir, "parcelles-${System.currentTimeMillis()}.gpkg")
        val ok = context.contentResolver.openInputStream(uri)?.use { entree ->
            dest.outputStream().use { sortie -> entree.copyTo(sortie); true }
        } ?: false
        return if (ok) dest.absolutePath else null
    }

    /** Anneaux (contours) de toutes les parcelles, en WGS84 : chaque anneau = liste de positions. */
    fun parcelles(chemin: String): List<List<Position>> =
        entites(chemin, TypeCouche.PARCELLE) { e, systeme -> e.anneauxWgs84(systeme) }.flatten()

    /** Parcelles détaillées (attributs + anneaux WGS84), pour le rattachement spatial et la carte. */
    fun parcellesDetail(chemin: String): List<ParcelleGpkg> =
        // Houppiers et dessertes ne sont pas des parcelles : sans ce filtre, un houppier
        // deviendrait candidat au rattachement spatial de la tige.
        entites(chemin, TypeCouche.PARCELLE) { e, systeme ->
            val anneaux = e.anneauxWgs84(systeme)
            if (anneaux.isEmpty()) return@entites null
            val attrs = e.attributs
            val prop = trouver(attrs, "proprietaire", "propriétaire", "owner", "prop")
            val foret = trouver(attrs, "foret", "forêt", "forest")
            val commune = trouver(attrs, "commune", "nom_com", "ville")
            val section = trouver(attrs, "section", "sect")
            val numero = trouver(attrs, "numero", "num", "n_parcelle", "parcelle", "id_parcelle", "idu")
            val parcelleNom = if (section != null && numero != null) "$section $numero" else numero
            val proprietaires = prop?.split(';', '/', '&')
                ?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            val surfaceHa = Geodesie.aireHa(anneaux)
            val parties = listOfNotNull(prop, foret, commune, parcelleNom?.let { "Parc. $it" })
            val label = if (parties.isEmpty()) "Parcelle ${e.id}" else parties.joinToString(" · ")
            ParcelleGpkg(e.id, label, prop, proprietaires, foret, parcelleNom, commune, surfaceHa, attrs, anneaux)
        }

    /**
     * Houppiers du GPKG (couche `houppier`) : contour en WGS84 + hauteur d'apex en mètres.
     * Une entité sans attribut de hauteur exploitable est ignorée — elle ne pourrait rien estimer.
     */
    fun houppiers(chemin: String): List<Houppier> =
        entites(chemin, TypeCouche.HOUPPIER) { e, systeme ->
            val h = CouchesGpkg.attribut(e.attributs, CouchesGpkg.ALIAS_HAUTEUR)
                ?.replace(',', '.')?.toDoubleOrNull() ?: return@entites null
            val anneaux = e.anneauxWgs84(systeme)
            if (anneaux.isEmpty()) null else Houppier(hauteurM = h, anneaux = anneaux)
        }

    /** Dessertes du GPKG (couche `desserte`) : routes, pistes et chemins, en lignes WGS84. */
    fun dessertes(chemin: String): List<DesserteGpkg> =
        entites(chemin, TypeCouche.DESSERTE) { e, systeme ->
            // Une desserte cartographiée en surface (route large) : on en garde les contours.
            val lignes = e.lignesWgs84(systeme)
            if (lignes.isEmpty()) {
                null
            } else {
                val nom = CouchesGpkg.attribut(e.attributs, CouchesGpkg.ALIAS_NOM)
                val type = CouchesGpkg.attribut(e.attributs, CouchesGpkg.ALIAS_TYPE)
                DesserteGpkg(id = e.id, label = nom ?: type, type = type, lignes = lignes)
            }
        }

    /**
     * Parcourt les entités des tables jouant le rôle `type` (lues par terrain-gpkg, en SQL simple)
     * et applique `extraire` à chacune. Le GPKG est ouvert en lecture seule, lu et refermé ici :
     * un fichier absent ou illisible rend une liste vide, une couche illisible ou dans un système
     * non géré (ni Lambert-93, ni WGS 84, ni Web Mercator) est sautée — la carte doit survivre à un
     * GPKG imparfait.
     */
    private fun <T> entites(chemin: String, type: TypeCouche, extraire: (Entite, Systeme) -> T?): List<T> {
        if (!File(chemin).exists()) {
            Log.w("Marculus.Gpkg", "Fichier absent: $chemin")
            return emptyList()
        }
        val out = mutableListOf<T>()
        try {
            SqliteAndroid.lecture(chemin).use { base ->
                val gpkg = GeoPackageVecteur(base)
                for (couche in gpkg.couches()) {
                    if (CouchesGpkg.type(couche.table) != type) continue
                    try {
                        val systeme = couche.systeme
                        gpkg.entites(couche).forEach { e -> if (systeme != null) extraire(e, systeme)?.let { out.add(it) } }
                    } catch (e: Exception) {
                        Log.e("Marculus.Gpkg", "Couche ${couche.table} ignorée", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Marculus.Gpkg", "entites($type)", e)
        }
        Log.d("Marculus.Gpkg", "entites($type) = ${out.size}")
        return out
    }

    /** Première valeur non vide parmi les colonnes dont le nom correspond (insensible à la casse). */
    private fun trouver(attrs: Map<String, String>, vararg cles: String): String? =
        attrs.entries.firstOrNull { e ->
            cles.any { it.equals(e.key, ignoreCase = true) } && e.value.isNotBlank()
        }?.value

    /** Zooms de la table qui portent au moins une tuile (GDAL déclare aussi les zooms vides). */
    private fun zoomsPresents(dao: TileDao): List<Long> =
        dao.zoomLevels.filter { dao.count(it) > 0 }.sorted()

    /**
     * Ouvre l'ortho du GPKG et renvoie un fournisseur de tuiles XYZ.
     *
     * - Déjà sur la grille Web Mercator standard (export Nemeton, GDAL GoogleMapsCompatible) :
     *   servie **telle quelle**, sans reprojection.
     * - Sinon : reprojetée une fois dans `<table>_wm`, **zoom par zoom présent** — une matrice
     *   déclarée mais vide faisait planter la reprojection de toute la pyramide.
     */
    fun ouvrirOrtho(chemin: String): OrthoSource? {
        val fichier = File(chemin)
        if (!fichier.exists()) return null
        return try {
            val manager = GeoPackageFactory.getManager(context)
            val gpkg = manager.openExternal(fichier) ?: return null
            val tables = gpkg.tileTables
            // La table reprojetée d'une ouverture précédente n'est pas une source.
            val source = tables.firstOrNull { !it.endsWith(SUFFIXE_WM) } ?: tables.firstOrNull()
            if (source == null) {
                gpkg.close()
                return null
            }
            val daoSource = gpkg.getTileDao(source)
            val zoomsSource = zoomsPresents(daoSource)
            if (estGrilleWebMercator(daoSource, zoomsSource)) {
                Log.d("Marculus.Gpkg", "Ortho $source déjà en grille Web Mercator : servie telle quelle, zoom $zoomsSource")
                return OrthoSource(
                    gpkg,
                    { z, x, y -> daoSource.queryForTile(x.toLong(), y.toLong(), z.toLong())?.tileData },
                    zoomsSource.first().toInt(),
                    zoomsSource.last().toInt(),
                )
            }
            val cible = source + SUFFIXE_WM
            if (!source.endsWith(SUFFIXE_WM) && !gpkg.tileTables.contains(cible) && zoomsSource.isNotEmpty()) {
                Log.d("Marculus.Gpkg", "Reprojection ortho $source -> $cible, zooms $zoomsSource…")
                TileReprojection.create(gpkg, source, cible, TileReprojectionOptimize.webMercator())
                    .reproject(zoomsSource)
            }
            val tableFinale = if (gpkg.tileTables.contains(cible)) cible else source
            val dao = gpkg.getTileDao(tableFinale)
            val zooms = zoomsPresents(dao)
            if (zooms.isEmpty()) {
                gpkg.close()
                return null
            }
            Log.d("Marculus.Gpkg", "Ortho prête: $tableFinale zoom $zooms")
            val retriever = GeoPackageTileRetriever(dao)
            OrthoSource(gpkg, { z, x, y -> retriever.getTile(x, y, z)?.data }, zooms.first().toInt(), zooms.last().toInt())
        } catch (e: Exception) {
            Log.e("Marculus.Gpkg", "ouvrirOrtho", e)
            null
        }
    }

    /** Table déjà tuilée sur la grille XYZ d'osmdroid : EPSG:3857, monde entier, 256 px, 2^z. */
    private fun estGrilleWebMercator(dao: TileDao, zooms: List<Long>): Boolean {
        val jeu = dao.tileMatrixSet
        val srs = jeu.srs
        val matrices = zooms.mapNotNull { z ->
            dao.getTileMatrix(z)?.let {
                MatriceTuiles(z.toInt(), it.matrixWidth, it.matrixHeight, it.tileWidth, it.tileHeight)
            }
        }
        return matrices.size == zooms.size && GrilleWebMercator.estStandard(
            srs?.organization, srs?.organizationCoordsysId ?: -1,
            jeu.minX, jeu.minY, jeu.maxX, jeu.maxY, matrices,
        )
    }
}
