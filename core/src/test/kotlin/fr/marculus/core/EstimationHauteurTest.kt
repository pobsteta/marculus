package fr.marculus.core

import fr.marculus.core.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EstimationHauteurTest {

    /** Carré de côté `cote` centré sur (lat, lon) — un houppier schématique. */
    private fun carre(lat: Double, lon: Double, cote: Double, h: Double): Houppier {
        val d = cote / 2
        return Houppier(
            hauteurM = h,
            anneaux = listOf(
                listOf(
                    Position(lat - d, lon - d),
                    Position(lat + d, lon - d),
                    Position(lat + d, lon + d),
                    Position(lat - d, lon + d),
                    Position(lat - d, lon - d),
                ),
            ),
        )
    }

    @Test
    fun `hauteur du houppier contenant la position`() {
        val houppiers = listOf(carre(48.0, 6.0, 0.001, 27.4))
        assertEquals(27, EstimationHauteur.pour(houppiers, Position(48.0, 6.0)))
    }

    @Test
    fun `hors de tout houppier, aucune estimation`() {
        // Cas normal : trouée, bord de peuplement, ou tige dominée absente du MNH.
        val houppiers = listOf(carre(48.0, 6.0, 0.001, 27.4))
        assertNull(EstimationHauteur.pour(houppiers, Position(48.5, 6.5)))
    }

    @Test
    fun `sans houppier du tout, aucune estimation`() {
        assertNull(EstimationHauteur.pour(emptyList(), Position(48.0, 6.0)))
    }

    @Test
    fun `houppiers superposes, le plus haut domine`() {
        // La segmentation par enveloppe convexe produit des houppiers qui se recouvrent :
        // au point commun, c'est l'apex le plus haut qui est au-dessus de l'opérateur.
        val houppiers = listOf(
            carre(48.0, 6.0, 0.002, 22.0),
            carre(48.0, 6.0, 0.001, 31.0),
        )
        assertEquals(31, EstimationHauteur.pour(houppiers, Position(48.0, 6.0)))
    }

    @Test
    fun `hauteur aberrante ignoree`() {
        // Un h_max à 0 (houppier vide) ou en centimètres (2740) n'est pas une hauteur d'arbre :
        // mieux vaut aucune estimation qu'une estimation absurde écrite dans le journal.
        assertNull(EstimationHauteur.pour(listOf(carre(48.0, 6.0, 0.001, 0.0)), Position(48.0, 6.0)))
        assertNull(EstimationHauteur.pour(listOf(carre(48.0, 6.0, 0.001, 2740.0)), Position(48.0, 6.0)))
    }

    @Test
    fun `le texte produit est celui de la saisie manuelle`() {
        val houppiers = listOf(carre(48.0, 6.0, 0.001, 26.6))
        val texte = EstimationHauteur.texte(houppiers, Position(48.0, 6.0))
        assertEquals("27", texte)
        // Et il se relit par le parseur de la saisie manuelle, sans rien de nouveau au schéma.
        assertEquals(27.0, HauteurParser.parse(texte!!).hauteurTotale)
    }
}
