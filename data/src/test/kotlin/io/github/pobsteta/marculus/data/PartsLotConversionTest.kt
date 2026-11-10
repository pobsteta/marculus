package io.github.pobsteta.marculus.data

import fr.marculus.core.model.PartsLot
import kotlin.test.Test
import kotlin.test.assertEquals

/** Mission 026 : encodage en base des parties comptées au lot (`ContexteEntity.partsLot`). */
class PartsLotConversionTest {

    @Test
    fun `les deux cochees explicites survivent a l'aller-retour`() {
        val parts = mapOf("Chêne" to PartsLot(true, true), "Hêtre" to PartsLot(false, false))
        assertEquals(parts, decodePartsLot(encodePartsLot(parts)))
    }

    @Test
    fun `nom avec espace, accent et tiret`() {
        val parts = mapOf("Érable sycomore" to PartsLot(false, true), "Merisier-x" to PartsLot(true, false))
        assertEquals(parts, decodePartsLot(encodePartsLot(parts)))
    }

    @Test
    fun `un enregistrement mal forme est ignore, les autres sont lus`() {
        val bon = encodePartsLot(mapOf("Chêne" to PartsLot(false, true)))
        val s = listOf(bon, "Hêtre${US}2${US}1", "Frêne${US}1", "${US}1${US}1").joinToString(RS)
        assertEquals(mapOf("Chêne" to PartsLot(false, true)), decodePartsLot(s))
    }

    @Test
    fun `table vide - chaine vide`() {
        assertEquals("", encodePartsLot(emptyMap()))
        assertEquals(emptyMap(), decodePartsLot(""))
    }
}
