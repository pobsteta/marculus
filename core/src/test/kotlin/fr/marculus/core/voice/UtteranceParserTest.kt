package fr.marculus.core.voice

import kotlin.test.Test
import kotlin.test.assertEquals

class UtteranceParserTest {

    private val essences = listOf(
        SpokenEssence("HET", listOf("hetre")),
        SpokenEssence("FRE", listOf("frene", "commun")),
        SpokenEssence("CHS", listOf("chene", "sessile")),
    )
    private val qualites = listOf(
        SpokenQualite("A", "alpha"),
        SpokenQualite("B", "bravo"),
        SpokenQualite("AB", "alpha bravo"),
    )
    private val classes = listOf(20, 25, 30, 35, 40, 45, 50)
    private val parser = UtteranceParser(GrammarBuilder.buildLexicon(essences, classes, qualites))

    @Test
    fun `triplet complet essence classe qualite`() {
        assertEquals(VoiceEvent.Tige("HET", 45, "B"), parser.parse("hetre quarante cinq bravo"))
    }

    @Test
    fun `essence et classe sans qualite`() {
        assertEquals(VoiceEvent.Tige("HET", 45, null), parser.parse("hetre quarante cinq"))
    }

    @Test
    fun `mode rafale la classe seule reprend l essence courante`() {
        assertEquals(VoiceEvent.Tige("HET", 45, null), parser.parse("quarante cinq", essenceCourante = "HET"))
        assertEquals(VoiceEvent.Tige("HET", 50, "A"), parser.parse("cinquante alpha", essenceCourante = "HET"))
    }

    @Test
    fun `l essence dictee prime sur l essence courante`() {
        assertEquals(VoiceEvent.Tige("CHS", 30, null), parser.parse("chene sessile trente", essenceCourante = "HET"))
    }

    @Test
    fun `qualite epelee en radio sur deux lettres`() {
        assertEquals(VoiceEvent.Tige("HET", 40, "AB"), parser.parse("hetre quarante alpha bravo"))
    }

    @Test
    fun `commandes reconnues`() {
        assertEquals(VoiceEvent.Commande(VoiceCommands.ANNULE), parser.parse("annule"))
        assertEquals(VoiceEvent.Commande(VoiceCommands.REPETE), parser.parse("repete"))
    }

    @Test
    fun `tout unk invalide l enonce entier`() {
        assertEquals(
            VoiceEvent.Rejet("[unk] quarante cinq", VoiceEvent.Raison.UNK),
            parser.parse("[unk] quarante cinq"),
        )
        // Même avec une tige complète et valide devant : aucune récupération partielle.
        assertEquals(
            VoiceEvent.Rejet("hetre quarante cinq [unk]", VoiceEvent.Raison.UNK),
            parser.parse("hetre quarante cinq [unk]"),
        )
    }

    @Test
    fun `enonce incomplet`() {
        assertEquals(VoiceEvent.Rejet("hetre", VoiceEvent.Raison.INCOMPLET), parser.parse("hetre"))
        // Classe seule sans essence courante : rien à rattacher.
        assertEquals(VoiceEvent.Rejet("quarante", VoiceEvent.Raison.INCOMPLET), parser.parse("quarante"))
        assertEquals(VoiceEvent.Rejet("   ", VoiceEvent.Raison.INCOMPLET), parser.parse("   "))
    }

    @Test
    fun `deux essences dans le meme enonce sont ambigues`() {
        assertEquals(
            VoiceEvent.Rejet("hetre frene commun quarante", VoiceEvent.Raison.AMBIGU),
            parser.parse("hetre frene commun quarante"),
        )
    }

    @Test
    fun `deux classes ou deux qualites sont ambigues`() {
        assertEquals(
            VoiceEvent.Rejet("hetre quarante cinquante", VoiceEvent.Raison.AMBIGU),
            parser.parse("hetre quarante cinquante"),
        )
        assertEquals(
            VoiceEvent.Rejet("hetre quarante alpha bravo alpha", VoiceEvent.Raison.AMBIGU),
            parser.parse("hetre quarante alpha bravo alpha"),
        )
    }

    @Test
    fun `un mot hors lexique sans unk est ambigu`() {
        assertEquals(
            VoiceEvent.Rejet("hetre bidule quarante", VoiceEvent.Raison.AMBIGU),
            parser.parse("hetre bidule quarante"),
        )
    }

    @Test
    fun `appariement au plus long la forme longue gagne`() {
        // "alpha bravo" doit s'apparier en un seul coup, pas comme "alpha" puis "bravo"
        // (qui donnerait deux qualités, donc un Rejet AMBIGU).
        assertEquals(VoiceEvent.Tige("HET", 20, "AB"), parser.parse("hetre vingt alpha bravo"))
    }

    @Test
    fun `casse et espaces multiples tolerees`() {
        assertEquals(VoiceEvent.Tige("HET", 45, null), parser.parse("  Hetre   quarante  cinq "))
    }
}
