package fr.marculus.core.voice

import fr.marculus.core.HauteurParser
import fr.marculus.core.SegmentDecoupe
import fr.marculus.core.Referentiels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Dictée de la hauteur et de la découpe : « hauteur vingt sept six alpha bravo ». */
class HauteurDicteeTest {

    private val options = OptionsHauteur(
        maxMetres = 60,
        lettresBois = ReferentielParle.lettresBois(Referentiels.QUALITE_BOIS_DEFAUT),
    )
    private val parseur = UtteranceParser(
        GrammarBuilder.buildLexicon(
            essences = listOf(SpokenEssence("HET", listOf("hetre"))),
            classes = listOf(40, 45, 50),
            qualites = ReferentielParle.qualites(Referentiels.QUALITE_ARBRE_DEFAUT),
            hauteur = options,
        ),
    )

    @Test
    fun `les lettres de decoupe viennent du referentiel qualite bois`() {
        // A, B, C, D, AB, BC, CD → quatre lettres distinctes.
        assertEquals(listOf('A', 'B', 'C', 'D'), options.lettresBois)
    }

    @Test
    fun `hauteur seule`() {
        assertEquals(VoiceEvent.Hauteur("27"), parseur.parse("hauteur vingt sept"))
        assertEquals(VoiceEvent.Hauteur("8"), parseur.parse("hauteur huit"))
    }

    @Test
    fun `hauteur avec decoupe, au format de la saisie manuelle`() {
        val evenement = parseur.parse("hauteur vingt sept six alpha bravo quatre charlie delta")
        assertEquals(VoiceEvent.Hauteur("27-6AB4CD"), evenement)

        // Le texte produit doit se relire avec l'analyseur existant, sans adaptation.
        val relu = HauteurParser.parse((evenement as VoiceEvent.Hauteur).texte)
        assertEquals(27.0, relu.hauteurTotale)
        assertEquals(listOf(SegmentDecoupe(6.0, "AB"), SegmentDecoupe(4.0, "CD")), relu.segments)
    }

    @Test
    fun `decoupe a une seule lettre`() {
        assertEquals(VoiceEvent.Hauteur("30-10A"), parseur.parse("hauteur trente dix alpha"))
    }

    @Test
    fun `hauteur sans nombre est incomplete`() {
        assertEquals(
            VoiceEvent.Rejet("hauteur", VoiceEvent.Raison.INCOMPLET),
            parseur.parse("hauteur"),
        )
    }

    @Test
    fun `longueur de billon sans qualite est incomplete`() {
        assertEquals(
            VoiceEvent.Rejet("hauteur vingt sept six", VoiceEvent.Raison.INCOMPLET),
            parseur.parse("hauteur vingt sept six"),
        )
    }

    @Test
    fun `mot etranger dans l enonce de hauteur`() {
        assertEquals(
            VoiceEvent.Rejet("hauteur vingt sept bidule", VoiceEvent.Raison.AMBIGU),
            parseur.parse("hauteur vingt sept bidule"),
        )
    }

    @Test
    fun `la dictee de tige n est pas perturbee par les nombres de hauteur`() {
        assertEquals(VoiceEvent.Tige("HET", 45, null), parseur.parse("hetre quarante cinq"))
        // 42 est un nombre admis en hauteur mais pas une classe de l'axe : rejet, pas d'invention.
        assertEquals(
            VoiceEvent.Rejet("hetre quarante deux", VoiceEvent.Raison.AMBIGU),
            parseur.parse("hetre quarante deux"),
        )
    }

    @Test
    fun `la grammaire contient le mot-cle, les nombres et les lettres radio`() {
        val json = GrammarBuilder.buildJson(
            essences = listOf(SpokenEssence("HET", listOf("hetre"))),
            classes = listOf(45),
            qualites = emptyList(),
            hauteur = options,
        )
        assertTrue("\"hauteur\"" in json)
        assertTrue("\"vingt sept\"" in json)
        assertTrue("\"alpha\"" in json && "\"delta\"" in json)
    }

    @Test
    fun `sans options de hauteur, le mot-cle n est pas dictable`() {
        val sansHauteur = UtteranceParser(
            GrammarBuilder.buildLexicon(
                essences = listOf(SpokenEssence("HET", listOf("hetre"))),
                classes = listOf(45),
                qualites = emptyList(),
            ),
        )
        assertEquals(
            VoiceEvent.Rejet("hauteur vingt sept", VoiceEvent.Raison.AMBIGU),
            sansHauteur.parse("hauteur vingt sept"),
        )
    }
}

/** Le référentiel parlé ne doit jamais promettre un mot que le modèle ne sait pas décoder. */
class VocabulaireModeleTest {

    // Vocabulaire de test : le modèle small-fr réel ignore « volis », « foxtrot », « juliett »,
    // « uniform » et « xray » — constaté en lisant sa table de symboles.
    private val inconnus = setOf("volis", "foxtrot", "juliett", "uniform", "xray")
    private val motConnu: (String) -> Boolean = { it !in inconnus }

    @Test
    fun `un libelle hors vocabulaire est epele en radio`() {
        val qualites = ReferentielParle.qualites(Referentiels.QUALITE_ARBRE_DEFAUT, motConnu)
            .associate { it.code to it.spoken }
        assertEquals("sec", qualites["Sec"])
        assertEquals("chablis", qualites["Chablis"])
        assertEquals("malade", qualites["Malade"])
        // « Volis » est le seul introuvable : il devient l'épellation de son initiale.
        assertEquals("victor", qualites["Volis"])
    }

    @Test
    fun `l epellation s allonge juste assez pour rester unique`() {
        // « Volis » et « Vent » divergent dès la 2e lettre : deux mots suffisent.
        val qualites = ReferentielParle.qualites(listOf("Volis", "Vent"), motConnu = { it !in setOf("volis", "vent") })
            .associate { it.code to it.spoken }
        assertEquals("victor oscar", qualites["Volis"])
        assertEquals("victor echo", qualites["Vent"])
    }

    @Test
    fun `deux libelles indictables aux memes initiales, le second est ecarte`() {
        // Limite assumée : l'épellation est plafonnée à trois lettres. « Volis » et « Volée »
        // produisent la même forme ; plutôt que d'ouvrir une ambiguïté, la seconde est retirée
        // de la grammaire (elle reste saisissable au bouton Q).
        val qualites = ReferentielParle.qualites(
            listOf("Volis", "Volée"),
            motConnu = { it !in setOf("volis", "volee") },
        )
        assertEquals(listOf("Volis"), qualites.map { it.code })
        assertEquals("victor oscar lima", qualites.single().spoken)
    }

    @Test
    fun `les mots radio absents du modele ont un repli`() {
        assertEquals("foxtrot", ReferentielParle.motRadio('F'))
        assertEquals("fox", ReferentielParle.motRadio('F', motConnu))
        assertEquals("juliette", ReferentielParle.motRadio('J', motConnu))
        assertEquals("uniforme", ReferentielParle.motRadio('U', motConnu))
        assertEquals("xavier", ReferentielParle.motRadio('X', motConnu))
        assertEquals("alpha", ReferentielParle.motRadio('A', motConnu))
    }

    @Test
    fun `la desambiguisation chene frene reste dictable`() {
        val formes = ReferentielParle.essences(listOf("Chêne", "Frêne"), motConnu)
            .associate { it.nom to it.spoken }
        // « frene fox » et non « frene foxtrot », que le modèle ne saurait pas décoder.
        assertTrue(formes.getValue("Frêne").all(motConnu), "forme indictable : ${formes["Frêne"]}")
        assertTrue(formes.getValue("Chêne").all(motConnu))
    }

    @Test
    fun `une essence au libelle inconnu bascule sur l epellation du code`() {
        val ref = ReferentielParle.essences(listOf("Hêtre", "Volis"), motConnu).associateBy { it.nom }
        assertEquals(listOf("hetre"), ref.getValue("Hêtre").spoken)
        assertTrue(ref.getValue("Volis").spoken.all(motConnu))
        assertTrue(ref.getValue("Volis").spoken.size >= 2, "le code doit être épelé")
    }
}
