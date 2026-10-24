package fr.marculus.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

/** Mission 009 : le numéro de tarif n'a de sens que pour Schaeffer ; ailleurs il vaut 0. */
class TarifEssenceNormaliseTest {

    private val horsSchaeffer = listOf(TarifCubage.AUCUN, TarifCubage.EMERGE)
    private val schaeffer = listOf(
        TarifCubage.SCHAEFFER_RAPIDE, TarifCubage.SCHAEFFER_INTERMEDIAIRE,
        TarifCubage.SCHAEFFER_LENT, TarifCubage.SCHAEFFER_TRES_LENT,
    )

    @Test
    fun `hors Schaeffer le numero est remis a 0`() {
        for (t in horsSchaeffer) {
            assertEquals(TarifEssence(t, 0), TarifEssence(t, 9).normalise(), "tarif $t")
        }
    }

    @Test
    fun `un Schaeffer garde son numero`() {
        for (t in schaeffer) {
            assertEquals(TarifEssence(t, 9), TarifEssence(t, 9).normalise(), "tarif $t")
        }
    }

    @Test
    fun `les six tarifs avec un numero non nul`() {
        assertEquals(6, TarifCubage.entries.size)
        for (t in TarifCubage.entries) {
            val attendu = if (t.estSchaeffer) 13 else 0
            assertEquals(attendu, TarifEssence(t, 13).normalise().numero, "tarif $t")
            assertEquals(t, TarifEssence(t, 13).normalise().tarif, "le type ne change pas : $t")
        }
    }

    @Test
    fun `normalise est idempotent et laisse 0 inchange`() {
        for (t in TarifCubage.entries) {
            val une = TarifEssence(t, 7).normalise()
            assertEquals(une, une.normalise(), "tarif $t")
            assertEquals(TarifEssence(t, 0), TarifEssence(t, 0).normalise(), "tarif $t")
        }
    }
}
