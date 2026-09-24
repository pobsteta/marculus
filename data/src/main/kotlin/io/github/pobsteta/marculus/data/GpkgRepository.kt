package io.github.pobsteta.marculus.data

import android.content.Context
import android.net.Uri
import android.util.Log
import fr.marculus.core.CouchesGpkg
import fr.marculus.core.Geodesie
import fr.marculus.core.GrilleWebMercator
import fr.marculus.core.MatriceTuiles
import fr.marculus.core.Houppier
import fr.marculus.core.TypeCouche
import fr.marculus.core.model.Position
import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.geopackage.tiles.reproject.TileReprojection
import mil.nga.geopackage.tiles.reproject.TileReprojectionOptimize
import mil.nga.geopackage.tiles.retriever.GeoPackageTileRetriever
import mil.nga.geopackage.tiles.user.TileDao
import mil.nga.proj.ProjectionConstants
import mil.nga.proj.ProjectionFactory
import mil.nga.proj.ProjectionTransform
import mil.nga.sf.Geometry
import mil.nga.sf.GeometryCollection
import mil.nga.sf.LineString
import mil.nga.sf.MultiLineString
import mil.nga.sf.MultiPolygon
import mil.nga.sf.Polygon
import org.locationtech.proj4j.ProjCoordinate
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
    fun parcelles(chemin: String): List<List<Position>> {
        val fichier = File(chemin)
        if (!fichier.exists()) {
            Log.w("Marculus.Gpkg", "Fichier absent: $chemin")
            return emptyList()
        }
        val anneaux = mutableListOf<List<Position>>()
        try {
            val manager = GeoPackageFactory.getManager(context)
            val gpkg = manager.openExternal(fichier) ?: run {
                Log.w("Marculus.Gpkg", "openExternal a renvoyé null pour $chemin")
                return emptyList()
            }
            try {
                Log.d("Marculus.Gpkg", "tables features=${gpkg.featureTables}, tiles=${gpkg.tileTables}")
                val wgs84 = ProjectionFactory.getProjection(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM.toLong())
                for (table in gpkg.featureTables) {
                    if (CouchesGpkg.type(table) != TypeCouche.PARCELLE) continue
                    val dao = gpkg.getFeatureDao(table)
                    val transform = dao.projection.getTransformation(wgs84)
                    val rs = dao.queryForAll()
                    try {
                        while (rs.moveToNext()) {
                            val geom = rs.row.geometry?.geometry ?: continue
                            collecter(geom, transform, anneaux)
                        }
                    } finally {
                        rs.close()
                    }
                }
            } finally {
                gpkg.close()
            }
        } catch (e: Exception) {
            Log.e("Marculus.Gpkg", "Erreur lecture GPKG", e)
        }
        Log.d("Marculus.Gpkg", "anneaux lus = ${anneaux.size}")
        return anneaux
    }

    /** Parcelles détaillées (attributs + anneaux WGS84), pour le rattachement spatial et la carte. */
    fun parcellesDetail(chemin: String): List<ParcelleGpkg> {
        val fichier = File(chemin)
        if (!fichier.exists()) return emptyList()
        val out = mutableListOf<ParcelleGpkg>()
        try {
            val manager = GeoPackageFactory.getManager(context)
            val gpkg = manager.openExternal(fichier) ?: return emptyList()
            try {
                val wgs84 = ProjectionFactory.getProjection(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM.toLong())
                for (table in gpkg.featureTables) {
                    // Houppiers et dessertes ne sont pas des parcelles : sans ce filtre, un
                    // houppier deviendrait candidat au rattachement spatial de la tige.
                    if (CouchesGpkg.type(table) != TypeCouche.PARCELLE) continue
                    val dao = gpkg.getFeatureDao(table)
                    val transform = dao.projection.getTransformation(wgs84)
                    val geomCol = dao.geometryColumnName
                    val colonnes = dao.columnNames.filter { it != geomCol }
                    val rs = dao.queryForAll()
                    try {
                        while (rs.moveToNext()) {
                            val row = rs.row
                            val geom = row.geometry?.geometry ?: continue
                            val anneaux = mutableListOf<List<Position>>()
                            collecter(geom, transform, anneaux)
                            if (anneaux.isEmpty()) continue
                            val attrs = colonnes.mapNotNull { c ->
                                val v = runCatching { row.getValue(c) }.getOrNull()
                                if (v != null) c to v.toString() else null
                            }.toMap()
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
                            val label = if (parties.isEmpty()) "Parcelle ${row.id}" else parties.joinToString(" · ")
                            out.add(
                                ParcelleGpkg(
                                    row.id, label, prop, proprietaires, foret, parcelleNom, commune, surfaceHa, attrs, anneaux,
                                ),
                            )
                        }
                    } finally {
                        rs.close()
                    }
                }
            } finally {
                gpkg.close()
            }
        } catch (e: Exception) {
            Log.e("Marculus.Gpkg", "parcellesDetail", e)
        }
        return out
    }

    /**
     * Houppiers du GPKG (couche `houppier`) : contour en WGS84 + hauteur d'apex en mètres.
     * Une entité sans attribut de hauteur exploitable est ignorée — elle ne pourrait rien estimer.
     */
    fun houppiers(chemin: String): List<Houppier> =
        entites(chemin, TypeCouche.HOUPPIER) { _, attrs, geom, transform ->
            val h = CouchesGpkg.attribut(attrs, CouchesGpkg.ALIAS_HAUTEUR)
                ?.replace(',', '.')?.toDoubleOrNull() ?: return@entites null
            val anneaux = mutableListOf<List<Position>>()
            collecter(geom, transform, anneaux)
            if (anneaux.isEmpty()) null else Houppier(hauteurM = h, anneaux = anneaux)
        }

    /** Dessertes du GPKG (couche `desserte`) : routes, pistes et chemins, en lignes WGS84. */
    fun dessertes(chemin: String): List<DesserteGpkg> =
        entites(chemin, TypeCouche.DESSERTE) { id, attrs, geom, transform ->
            val lignes = mutableListOf<List<Position>>()
            collecterLignes(geom, transform, lignes)
            if (lignes.isEmpty()) {
                null
            } else {
                val nom = CouchesGpkg.attribut(attrs, CouchesGpkg.ALIAS_NOM)
                val type = CouchesGpkg.attribut(attrs, CouchesGpkg.ALIAS_TYPE)
                DesserteGpkg(id = id, label = nom ?: type, type = type, lignes = lignes)
            }
        }

    /**
     * Parcourt les entités des tables jouant le rôle `type` et applique `extraire` à chacune.
     * Le GPKG est ouvert, lu et refermé ici : les erreurs de lecture (fichier absent, table
     * illisible) rendent une liste vide plutôt qu'une exception — la carte doit survivre à un
     * GPKG imparfait.
     */
    private fun <T> entites(
        chemin: String,
        type: TypeCouche,
        extraire: (Long, Map<String, String>, Geometry, ProjectionTransform) -> T?,
    ): List<T> {
        val fichier = File(chemin)
        if (!fichier.exists()) return emptyList()
        val out = mutableListOf<T>()
        try {
            val manager = GeoPackageFactory.getManager(context)
            val gpkg = manager.openExternal(fichier) ?: return emptyList()
            try {
                val wgs84 = ProjectionFactory.getProjection(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM.toLong())
                for (table in gpkg.featureTables) {
                    if (CouchesGpkg.type(table) != type) continue
                    val dao = gpkg.getFeatureDao(table)
                    val transform = dao.projection.getTransformation(wgs84)
                    val colonnes = dao.columnNames.filter { it != dao.geometryColumnName }
                    val rs = dao.queryForAll()
                    try {
                        while (rs.moveToNext()) {
                            val row = rs.row
                            val geom = row.geometry?.geometry ?: continue
                            val attrs = colonnes.mapNotNull { c ->
                                val v = runCatching { row.getValue(c) }.getOrNull()
                                if (v != null) c to v.toString() else null
                            }.toMap()
                            extraire(row.id, attrs, geom, transform)?.let { out.add(it) }
                        }
                    } finally {
                        rs.close()
                    }
                }
            } finally {
                gpkg.close()
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

    /** Polylignes d'une géométrie linéaire (une desserte peut être un MULTILINESTRING). */
    private fun collecterLignes(g: Geometry, transform: ProjectionTransform, sortie: MutableList<List<Position>>) {
        when (g) {
            is MultiLineString -> g.lineStrings.forEach { collecterLignes(it, transform, sortie) }
            is LineString -> sortie.add(
                g.points.map { p ->
                    val w = transform.transform(ProjCoordinate(p.x, p.y))
                    Position(latitude = w.y, longitude = w.x)
                },
            )
            // Une desserte cartographiée en surface (route large) : on en garde les contours.
            is MultiPolygon, is Polygon -> collecter(g, transform, sortie)
            is GeometryCollection<*> -> g.geometries.forEach { collecterLignes(it as Geometry, transform, sortie) }
            else -> Unit
        }
    }

    private fun collecter(g: Geometry, transform: ProjectionTransform, sortie: MutableList<List<Position>>) {
        when (g) {
            is MultiPolygon -> g.polygons.forEach { collecter(it, transform, sortie) }
            is Polygon -> g.rings.forEach { anneau ->
                sortie.add(
                    anneau.points.map { p ->
                        val w = transform.transform(ProjCoordinate(p.x, p.y))
                        Position(latitude = w.y, longitude = w.x)
                    },
                )
            }
            is GeometryCollection<*> -> g.geometries.forEach { collecter(it as Geometry, transform, sortie) }
            else -> Unit
        }
    }
}
