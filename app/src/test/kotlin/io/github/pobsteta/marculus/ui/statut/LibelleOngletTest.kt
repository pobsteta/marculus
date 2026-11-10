package io.github.pobsteta.marculus.ui.statut

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibelleOngletTest {
    @Test
    fun `Historiqu puis e, un mot coupe au milieu est detecte`() {
        assertTrue(coupeUnMot("Historique", listOf(8, 10)))
    }

    @Test
    fun `Par puis parcelle, une coupe entre deux mots est acceptee`() {
        assertFalse(coupeUnMot("Par parcelle", listOf(4, 12)))
    }

    @Test
    fun `une seule ligne ne coupe rien`() {
        assertFalse(coupeUnMot("Historique", listOf(10)))
        assertFalse(coupeUnMot("", emptyList()))
    }
}
