package io.github.pobsteta.marculus.data

import android.content.Context
import android.net.Uri
import android.util.Log
import fr.marculus.core.model.Position
import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.geopackage.tiles.reproject.TileReprojection
import mil.nga.geopackage.tiles.reproject.TileReprojectionOptimize
import mil.nga.geopackage.tiles.retriever.GeoPackageTileRetriever
import mil.nga.proj.ProjectionConstants
import mil.nga.proj.ProjectionFactory
import mil.nga.proj.ProjectionTransform
import mil.nga.sf.Geometry
import mil.nga.sf.GeometryCollection
import mil.nga.sf.MultiPolygon
import mil.nga.sf.Polygon
import org.locationtech.proj4j.ProjCoordinate
import java.io.File

/** Fournisseur de tuiles ortho (reprojetées en Web Mercator), servi à osmdroid. À fermer après usage. */
class OrthoSource(
    private val gpkg: GeoPackage,
    private val retriever: GeoPackageTileRetriever,
    val zoomMin: Int,
    val zoomMax: Int,
) {
    @Synchronized
    fun tuile(zoom: Int, x: Int, y: Int): ByteArray? =
        try {
            retriever.getTile(x, y, zoom)?.data
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

/** Une parcelle lue du GPKG : identifiant, étiquette dérivée, attributs bruts et anneaux (WGS84). */
data class ParcelleGpkg(
    val id: Long,
    val label: String,
    val attributs: Map<String, String>,
    val anneaux: List<List<Position>>,
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
                            out.add(ParcelleGpkg(row.id, etiquette(row.id, attrs), attrs, anneaux))
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

    /** Étiquette générique : propriétaire · forêt · parcelle si détectés, sinon « Parcelle <id> ». */
    private fun etiquette(id: Long, attrs: Map<String, String>): String {
        fun trouver(vararg cles: String): String? =
            attrs.entries.firstOrNull { e ->
                cles.any { it.equals(e.key, ignoreCase = true) } && e.value.isNotBlank()
            }?.value
        val prop = trouver("proprietaire", "propriétaire", "owner", "prop")
        val foret = trouver("foret", "forêt", "forest")
        val parc = trouver("parcelle", "numero", "num", "n_parcelle", "id_parcelle", "idu", "section")
        val parties = listOfNotNull(prop, foret, parc?.let { "Parc. $it" })
        return if (parties.isEmpty()) "Parcelle $id" else parties.joinToString(" · ")
    }

    /** Ouvre l'ortho du GPKG, la reprojette en Web Mercator si besoin, et renvoie un fournisseur de tuiles. */
    fun ouvrirOrtho(chemin: String): OrthoSource? {
        val fichier = File(chemin)
        if (!fichier.exists()) return null
        return try {
            val manager = GeoPackageFactory.getManager(context)
            val gpkg = manager.openExternal(fichier) ?: return null
            val tables = gpkg.tileTables
            if (tables.isEmpty()) {
                gpkg.close()
                return null
            }
            val source = tables.first()
            val cible = source + "_wm"
            if (!gpkg.tileTables.contains(cible)) {
                Log.d("Marculus.Gpkg", "Reprojection ortho $source -> $cible (grille Web Mercator standard)…")
                TileReprojection.reproject(gpkg, source, cible, TileReprojectionOptimize.webMercator())
            }
            val tableFinale = if (gpkg.tileTables.contains(cible)) cible else source
            val dao = gpkg.getTileDao(tableFinale)
            Log.d("Marculus.Gpkg", "Ortho prête: $tableFinale zoom ${dao.minZoom}..${dao.maxZoom}")
            OrthoSource(gpkg, GeoPackageTileRetriever(dao), dao.minZoom.toInt(), dao.maxZoom.toInt())
        } catch (e: Exception) {
            Log.e("Marculus.Gpkg", "ouvrirOrtho", e)
            null
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
