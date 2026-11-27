package fr.marculus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Chaque ligne de `docs/specs/2026-11-27-rattachement-essences.md` (soumise à Pascal) : le nom saisi reçoit
 * les coefficients de la clé proposée, pas ceux d'une autre. La liste ci-dessous est celle du fichier.
 */
class RattachementEssencesTest {

    private val lignes: List<Pair<String, String>> = listOf(
        "Chêne" to "chene sessile",
        "Sapin" to "sapin pectine",
        "Épicéa" to "epicea commun",
        "Mélèze" to "meleze d europe",
        "Érable" to "erable sycomore",
        "Pin" to "pin generique",
        "Pin noir" to "pin noir d autriche",
        "Pin laricio" to "pin laricio de corse",
        "Laricio" to "pin laricio de corse",
        "Alep" to "pin d alep",
        "Frêne" to "frene commun",
        "Tilleul" to "tilleul a petites feuilles",
        "Bouleau" to "bouleau verruqueux",
        "Peuplier" to "peuplier autre cultive",
        "Peuplier tremble" to "tremble",
        "Aulne" to "aulne glutineux",
        "Robinier" to "robinier faux acacia",
        "Noyer" to "noyer commun",
        "Orme" to "orme champetre",
        "Cèdre" to "cedre de l atlas",
        "Saule" to "saule generique",
        "Platane" to "platane a feuilles d erable",
        "Sorbier" to "sorbier des oiseleurs",
        "Alisier" to "alisier torminal",
        "Noisetier" to "noisetier coudrier",
        "Sureau" to "sureau noir",
        "Cyprès" to "cypres de provence",
        "Thuya" to "thuya geant",
        "Genévrier" to "genevrier commun",
        "Pommier" to "pommier sauvage",
        "Poirier" to "poirier commun",
        "Marronnier" to "marronnier d inde",
        "Tulipier" to "tulipier de virginie",
        "Prunier" to "prunier domestique",
        "Mûrier" to "murier blanc",
        "Olivier" to "olivier d europe",
        "Fruitier" to "fruitiers divers",
        "Fruitiers" to "fruitiers divers",
        "Charme commun" to "charme",
    )

    @Test
    fun `chaque nom saisi recoit les volumes de la cle proposee`() {
        for ((saisi, cle) in lignes) {
            val obtenu = assertNotNull(Cubage.volumesEmerge(saisi, 100.0, 20.0), "« $saisi » : coefficients attendus")
            val attendu = assertNotNull(Cubage.volumesEmerge(cle, 100.0, 20.0), "clé « $cle » absente de la table EMERGE")
            assertEquals(attendu, obtenu, "« $saisi » → $cle")
            assertTrue(Cubage.estCouverteEmerge(saisi))
        }
    }

    @Test
    fun `chaque ligne du test est dans la table de rattachement et inversement`() {
        val noms = lignes.map { it.first }.toSet()
        assertEquals(RATTACHEMENT_NOMS_NORMALISES, noms.map { Cubage.normaliser(it) }.toSet() + setOf("quercus petraea", "quercus robur"))
        for ((n, cle) in Cubage.RATTACHEMENT_ESSENCES) assertNotNull(Cubage.volumesEmerge(cle, 100.0, 20.0), "$n → $cle")
    }

    @Test
    fun `les noms sans rattachement restent sans essence`() {
        for (e in listOf("Arbre", "Autre", "Inconnu", "Hêtres", "Chênes", "Pins")) assertNull(Cubage.volumesEmerge(e, 100.0, 20.0), e)
    }

    @Test
    fun `les codes ONF suivent le meme rattachement quand la cle en a un`() {
        assertEquals("ERS", Cubage.codeEssence("Érable"))
        assertEquals("MEE", Cubage.codeEssence("Mélèze"))
        assertEquals("PEX", Cubage.codeEssence("Pin"))
        assertEquals("AUG", Cubage.codeEssence("Aulne"))
    }

    private companion object {
        val RATTACHEMENT_NOMS_NORMALISES: Set<String> get() = Cubage.RATTACHEMENT_ESSENCES.keys
    }
}
