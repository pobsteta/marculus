package fr.marculus.core.nemeton

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Un cas nommé par ligne du jeu de contrôle (1 032 lignes) : le moteur doit redonner la valeur
 * (tolérance : PaquetDeTest.tolerance), sous le bon nom de sortie, avec le libellé d'essence de la ligne
 * et la version du paquet.
 */
@RunWith(Parameterized::class)
class JeuControleTest(
    @Suppress("unused") private val nom: String,
    private val ligne: PaquetDeTest.Ligne,
) {
    companion object {
        private val tarif: TarifNemeton by lazy { PaquetDeTest.charger() }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cas(): Collection<Array<Any>> = PaquetDeTest.jeu.map { arrayOf<Any>(it.nom, it) }
    }

    @Test
    fun `la sortie est recalculee`() {
        val s = assertNotNull(tarif.sorties(ligne.code, ligne.d, ligne.h), "code ${ligne.code} couvert")
        val obtenu = s.valeurs[ligne.sortie]
        assertNotNull(obtenu, "sortie ${ligne.sortie} absente de valeurs ${s.valeurs.keys}")
        val att = ligne.valeur
        val tol = PaquetDeTest.tolerance(att)
        assertTrue(kotlin.math.abs(obtenu - att) <= tol, "attendu $att, obtenu $obtenu, tolérance $tol")
        assertEquals(ligne.essence, s.essence)
        assertEquals(PaquetDeTest.VERSION, s.version)
    }
}
