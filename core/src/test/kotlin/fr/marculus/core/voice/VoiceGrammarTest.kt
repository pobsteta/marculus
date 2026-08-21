package fr.marculus.core.voice

import fr.marculus.core.model.AxeClasses
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FrenchNumbersTest {

    @Test
    fun `unites et dizaines simples`() {
        assertEquals(listOf("cinq"), FrenchNumbers.toTokens(5))
        assertEquals(listOf("quarante", "cinq"), FrenchNumbers.toTokens(45))
        assertEquals(listOf("vingt"), FrenchNumbers.toTokens(20))
        assertEquals(listOf("vingt", "et", "un"), FrenchNumbers.toTokens(21))
    }

    @Test
    fun `septante et huitante a la francaise`() {
        assertEquals(listOf("soixante", "dix"), FrenchNumbers.toTokens(70))
        assertEquals(listOf("soixante", "et", "onze"), FrenchNumbers.toTokens(71))
        assertEquals(listOf("soixante", "quinze"), FrenchNumbers.toTokens(75))
        assertEquals(listOf("quatre", "vingt"), FrenchNumbers.toTokens(80))
        assertEquals(listOf("quatre", "vingt", "quinze"), FrenchNumbers.toTokens(95))
        assertEquals(listOf("quatre", "vingt", "dix"), FrenchNumbers.toTokens(90))
    }

    @Test
    fun `centaines`() {
        assertEquals(listOf("cent"), FrenchNumbers.toTokens(100))
        assertEquals(listOf("cent", "vingt", "cinq"), FrenchNumbers.toTokens(125))
        assertEquals(listOf("deux", "cent"), FrenchNumbers.toTokens(200))
        assertEquals(listOf("deux", "cent", "quarante"), FrenchNumbers.toTokens(240))
    }

    @Test
    fun `hors bornes refuse`() {
        assertFailsWith<IllegalArgumentException> { FrenchNumbers.toTokens(0) }
        assertFailsWith<IllegalArgumentException> { FrenchNumbers.toTokens(1000) }
    }
}

class GrammarBuilderTest {

    private val essences = listOf(
        SpokenEssence("HET", listOf("hetre")),
        SpokenEssence("CHS", listOf("chene", "sessile")),
        SpokenEssence("CHP", listOf("chene", "pedoncule")),
    )
    private val qualites = listOf(SpokenQualite("A", "alpha"), SpokenQualite("AB", "alpha bravo"))
    private val classes = listOf(20, 45, 95)

    @Test
    fun `la grammaire contient unk et aucun doublon`() {
        val json = GrammarBuilder.buildJson(essences, classes, qualites)
        val phrases = json.removeSurrounding("[", "]").split(",").map { it.trim().removeSurrounding("\"") }
        assertTrue("[unk]" in phrases, "le jeton [unk] est obligatoire : il capte le hors-grammaire")
        assertEquals(phrases.size, phrases.distinct().size, "grammaire avec doublons : $phrases")
    }

    @Test
    fun `les formes multi-mots sont jointes par des espaces`() {
        val phrases = GrammarBuilder.buildJson(essences, classes, qualites)
        assertTrue("\"chene sessile\"" in phrases)
        assertTrue("\"quatre vingt quinze\"" in phrases)
        assertTrue("\"alpha bravo\"" in phrases)
    }

    @Test
    fun `les commandes figurent dans la grammaire`() {
        val phrases = GrammarBuilder.buildJson(essences, classes, qualites)
        assertTrue("\"${VoiceCommands.ANNULE}\"" in phrases)
        assertTrue("\"${VoiceCommands.REPETE}\"" in phrases)
    }

    @Test
    fun `aller-retour sur toutes les classes de l axe`() {
        val classesAxe = AxeClasses(min = 15, max = 195, pas = 5).classes()
        val parser = UtteranceParser(GrammarBuilder.buildLexicon(essences, classesAxe, qualites))
        classesAxe.forEach { classe ->
            val dictee = "hetre " + FrenchNumbers.toTokens(classe).joinToString(" ")
            assertEquals(
                VoiceEvent.Tige("HET", classe, null),
                parser.parse(dictee),
                "aller-retour raté pour la classe $classe (« $dictee »)",
            )
        }
    }

    @Test
    fun `la longueur de cle maximale couvre les formes multi-mots`() {
        val lexique = GrammarBuilder.buildLexicon(essences, classes, qualites)
        assertEquals(3, lexique.maxKeyLen) // "quatre vingt quinze"
    }
}
