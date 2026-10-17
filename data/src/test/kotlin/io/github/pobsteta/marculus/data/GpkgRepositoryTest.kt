package io.github.pobsteta.marculus.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Lecture vecteur du GPKG d'essai de terrain-gpkg (fabriqué par GDAL : parcelles, desserte,
 * houppier, points en WGS 84 / Web Mercator / 3D / UTM). Le résumé attendu est celui que donnait
 * l'ancienne lecture par NGA geopackage-android (proj4j), sommets arrondis à 1e-7 °.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GpkgRepositoryTest {
    private val chemin = Files.createTempFile("essai", ".gpkg").toFile().also { f ->
        javaClass.getResourceAsStream("/gpkg/essai.gpkg")!!.use { e -> f.outputStream().use { e.copyTo(it) } }
    }.absolutePath
    private val repo = GpkgRepository(ApplicationProvider.getApplicationContext())

    private fun resume(): String = buildString {
        fun pos(l: List<fr.nemeton.terrain.geo.Position>) =
            l.joinToString(" ") { String.format(Locale.ROOT, "%.7f,%.7f", it.longitude, it.latitude) }
        repo.parcellesDetail(chemin).forEach { p ->
            appendLine("parcelle ${p.id} ${p.label} | ${p.proprietaires} | ${p.parcelleNom} | ${String.format(Locale.ROOT, "%.4f", p.surfaceHa)} ha | ${p.attributs.toSortedMap()}")
            p.anneaux.forEach { appendLine("  " + pos(it)) }
        }
        appendLine("anneaux ${repo.parcelles(chemin).size}")
        repo.houppiers(chemin).forEach { h -> appendLine("houppier ${h.hauteurM} ${h.anneaux.size} " + pos(h.anneaux.first())) }
        repo.dessertes(chemin).forEach { d -> appendLine("desserte ${d.id} ${d.label} ${d.type} ${d.lignes.size} " + d.lignes.joinToString(" / ") { pos(it) }) }
    }

    @Test
    fun `parcelles, houppiers et dessertes`() {
        val r = resume()
        System.getenv("MARCULUS_GPKG_RESUME")?.let { File(it).writeText(r) }
        assertTrue(r.contains("parcelle 1 Commune de Dijon · Personnelle · Parc. 1"), r)
        assertEquals(2, Regex("^parcelle ", RegexOption.MULTILINE).findAll(r).count(), r)
        assertTrue(r.contains("houppier 24.5 2 "), r)
        assertTrue(r.contains("desserte 1 Route forestière du Chêne route empierrée 1 "), r)
        assertTrue(r.contains("desserte 2 piste piste 2 "), r)
    }

    @Test
    fun `fichier absent - listes vides`() {
        assertTrue(repo.parcellesDetail("/nulle/part.gpkg").isEmpty())
        assertTrue(repo.dessertes("/nulle/part.gpkg").isEmpty())
    }
}
