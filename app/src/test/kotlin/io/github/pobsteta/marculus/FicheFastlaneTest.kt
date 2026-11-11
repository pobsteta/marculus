package io.github.pobsteta.marculus

import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mission 035 : la fiche de Marculus pour IzzyOnDroid (métadonnées Fastlane).
 * Conditions : https://izzyondroid.org/docs/general/Fastlane/ (description courte de 80 caractères au plus).
 */
class FicheFastlaneTest {

    private val langues = listOf("fr-FR", "en-US")

    /** Le test tourne dans `app/` ; le dépôt est un cran au-dessus. */
    private fun dossier(langue: String): File {
        val racine = generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "fastlane/metadata/android").isDirectory }
        return File(racine, "fastlane/metadata/android/$langue")
    }

    @Test
    fun textesPresentsDansLesDeuxLangues() {
        for (l in langues) for (nom in listOf("title.txt", "short_description.txt", "full_description.txt")) {
            val f = File(dossier(l), nom)
            assertTrue("$l/$nom absent ou vide", f.isFile && f.readText().isNotBlank())
        }
    }

    @Test
    fun titreEstMarculus() {
        for (l in langues) assertEquals("Marculus", File(dossier(l), "title.txt").readText().trim())
    }

    @Test
    fun descriptionCourteTientEn80Caracteres() {
        for (l in langues) {
            val court = File(dossier(l), "short_description.txt").readText().trim()
            assertTrue("$l : ${court.codePointCount(0, court.length)} caractères", court.codePointCount(0, court.length) <= 80)
        }
    }

    /** IzzyOnDroid refuse une capture dont le grand côté dépasse deux fois le petit (« Image sizes »). */
    @Test
    fun chaqueCaptureRespecteLeRapportDeuxPourUn() {
        for (l in langues) {
            val captures = File(dossier(l), "images/phoneScreenshots").listFiles { f -> f.extension in listOf("png", "jpg") }.orEmpty()
            for (f in captures) {
                val img = ImageIO.read(f)
                val grand = maxOf(img.width, img.height).toDouble()
                val petit = minOf(img.width, img.height).toDouble()
                assertTrue("$l/${f.name} : ${img.width}x${img.height}, rapport ${grand / petit} > 2", grand / petit <= 2.0)
            }
        }
    }

    @Test
    fun descriptionLongueTientEn4000Caracteres() {
        for (l in langues) assertTrue(File(dossier(l), "full_description.txt").readText().length <= 4000)
    }

    @Test
    fun jamaisGpsMaisGnss() {
        for (l in langues) for (nom in listOf("title.txt", "short_description.txt", "full_description.txt")) {
            val t = File(dossier(l), nom).readText()
            assertTrue("$l/$nom dit GPS", !Regex("\\bGPS\\b").containsMatchIn(t))
        }
    }

    @Test
    fun iconeFait512Par512() {
        for (l in langues) {
            val img = ImageIO.read(File(dossier(l), "images/icon.png"))
            assertEquals("$l largeur", 512, img.width)
            assertEquals("$l hauteur", 512, img.height)
        }
    }

    @Test
    fun auMoinsDeuxCapturesParLangue() {
        for (l in langues) {
            val n = File(dossier(l), "images/phoneScreenshots").listFiles { f -> f.extension in listOf("png", "jpg") }.orEmpty().size
            assertTrue("$l : $n capture(s)", n >= 2)
        }
    }
}
