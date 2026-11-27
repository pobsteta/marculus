package fr.marculus.core.nemeton

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Pour chaque point (code, D, H) du jeu de contrôle, l'ensemble des clés de `valeurs` est exactement
 * l'ensemble des sorties du jeu : une sortie absente du jeu (bois fort tige des feuillus, bois fort total
 * des résineux, biomasse et carbone des autres feuillus) est absente de la map, jamais 0.
 */
@RunWith(Parameterized::class)
class SortiesAbsentesTest(
    @Suppress("unused") private val nom: String,
    private val code: String,
    private val d: Double,
    private val h: Double,
    private val attendues: Set<String>,
) {
    companion object {
        private val tarif: TarifNemeton by lazy { PaquetDeTest.charger() }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun points(): Collection<Array<Any>> =
            PaquetDeTest.jeu.groupBy { Triple(it.code, it.d, it.h) }.map { (k, lignes) ->
                arrayOf<Any>("${lignes.first().essence} code=${k.first} D=${k.second} H=${k.third}", k.first, k.second, k.third, lignes.map { it.sortie }.toSet())
            }
    }

    @Test
    fun `les cles de valeurs sont exactement les sorties du jeu`() {
        val s = assertNotNull(tarif.sorties(code, d, h))
        assertEquals(attendues, s.valeurs.keys)
    }
}
