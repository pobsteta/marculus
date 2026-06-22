package io.github.pobsteta.marculus.data

import android.content.Context
import android.net.Uri
import android.util.Log
import fr.marculus.core.model.Position
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.proj.ProjectionConstants
import mil.nga.proj.ProjectionFactory
import mil.nga.proj.ProjectionTransform
import mil.nga.sf.Geometry
import mil.nga.sf.GeometryCollection
import mil.nga.sf.MultiPolygon
import mil.nga.sf.Polygon
import org.locationtech.proj4j.ProjCoordinate
import java.io.File

/** Lecture d'un GeoPackage (parcelles vectorielles, reprojetées en WGS84 pour la carte). */
class GpkgRepository(private val context: Context) {

    /** Copie le GPKG choisi dans le stockage privé et renvoie son chemin. */
    fun importer(uri: Uri): String? {
        val dest = File(context.filesDir, "parcelles.gpkg")
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
