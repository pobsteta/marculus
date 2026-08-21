package fr.marculus.core.voice

import fr.marculus.core.Referentiels
import fr.marculus.core.model.AxeClasses
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Chaîne complète telle qu'elle tourne dans l'application : libellés réels du contexte et du
 * référentiel → formes parlées → grammaire → parseur. C'est ici que se voient les décalages
 * entre ce qui est promis à l'opérateur et ce que la grammaire accepte réellement.
 */
class ChaineDicteeTest {

    private val essences = listOf("Chêne", "Hêtre", "Sapin")
    private val classes = AxeClasses(20, 90, 5).classes()

    private fun parseurAvec(qualites: List<String>): UtteranceParser {
        val parlees = ReferentielParle.essences(essences).map { it.versGrammaire() }
        val qualitesParlees = ReferentielParle.qualites(qualites)
        return UtteranceParser(GrammarBuilder.buildLexicon(parlees, classes, qualitesParlees))
    }

    @Test
    fun `qualite arbre du referentiel par defaut dictee en toutes lettres`() {
        val parseur = parseurAvec(Referentiels.QUALITE_ARBRE_DEFAUT)
        assertEquals(
            VoiceEvent.Tige("HET", 45, "Chablis"),
            parseur.parse("hetre quarante cinq chablis"),
            "la qualité doit être le libellé du référentiel, pas un code",
        )
        assertEquals(VoiceEvent.Tige("HET", 45, "Sec"), parseur.parse("hetre quarante cinq sec"))
    }

    @Test
    fun `l alphabet radio n est PAS accepte avec un referentiel en toutes lettres`() {
        // Piège documenté : « bravo » n'existe dans la grammaire que si le référentiel de
        // qualités contient des codes en lettres. Sinon c'est un mot hors lexique → rejet.
        val parseur = parseurAvec(Referentiels.QUALITE_ARBRE_DEFAUT)
        assertEquals(
            VoiceEvent.Rejet("hetre quarante cinq bravo", VoiceEvent.Raison.AMBIGU),
            parseur.parse("hetre quarante cinq bravo"),
        )
    }

    @Test
    fun `referentiel en codes lettres rend l alphabet radio dictable`() {
        val parseur = parseurAvec(Referentiels.QUALITE_BOIS_DEFAUT) // A, B, C, D, AB, BC, CD
        assertEquals(VoiceEvent.Tige("HET", 45, "B"), parseur.parse("hetre quarante cinq bravo"))
        assertEquals(VoiceEvent.Tige("HET", 45, "AB"), parseur.parse("hetre quarante cinq alpha bravo"))
    }

    @Test
    fun `une qualite dictee seule annote la derniere tige`() {
        val parseur = parseurAvec(Referentiels.QUALITE_ARBRE_DEFAUT)
        assertEquals(VoiceEvent.Qualite("Chablis"), parseur.parse("chablis"))
        // Même en rafale : l'essence courante ne suffit pas à faire une tige sans classe.
        assertEquals(VoiceEvent.Qualite("Sec"), parseur.parse("sec", essenceCourante = "HET"))
    }

    @Test
    fun `une essence sans classe reste incomplete, meme suivie d une qualite`() {
        // On n'invente pas la classe manquante : « hetre chablis » n'est pas une tige.
        val parseur = parseurAvec(Referentiels.QUALITE_ARBRE_DEFAUT)
        assertEquals(
            VoiceEvent.Rejet("hetre chablis", VoiceEvent.Raison.INCOMPLET),
            parseur.parse("hetre chablis"),
        )
    }

    @Test
    fun `la qualite reste attachee a la tige quand la classe est dictee`() {
        val parseur = parseurAvec(Referentiels.QUALITE_ARBRE_DEFAUT)
        assertEquals(
            VoiceEvent.Tige("HET", 45, "Chablis"),
            parseur.parse("hetre quarante cinq chablis"),
        )
    }
}
