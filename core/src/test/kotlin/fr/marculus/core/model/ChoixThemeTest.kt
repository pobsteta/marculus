package fr.marculus.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChoixThemeTest {

    // --- estSombre : 3 choix x 2 états du système ---

    @Test
    fun `systeme suit le telephone en sombre`() {
        assertTrue(ChoixTheme.SYSTEME.estSombre(systemeSombre = true))
    }

    @Test
    fun `systeme suit le telephone en clair`() {
        assertFalse(ChoixTheme.SYSTEME.estSombre(systemeSombre = false))
    }

    @Test
    fun `clair reste clair sur un telephone en sombre`() {
        assertFalse(ChoixTheme.CLAIR.estSombre(systemeSombre = true))
    }

    @Test
    fun `clair reste clair sur un telephone en clair`() {
        assertFalse(ChoixTheme.CLAIR.estSombre(systemeSombre = false))
    }

    @Test
    fun `sombre reste sombre sur un telephone en sombre`() {
        assertTrue(ChoixTheme.SOMBRE.estSombre(systemeSombre = true))
    }

    @Test
    fun `sombre reste sombre sur un telephone en clair`() {
        assertTrue(ChoixTheme.SOMBRE.estSombre(systemeSombre = false))
    }

    // --- choixThemeDepuis ---

    @Test
    fun `chaque nom valide est relu tel quel`() {
        for (c in ChoixTheme.entries) {
            assertEquals(c, choixThemeDepuis(c.name, null))
        }
    }

    @Test
    fun `ancienne case cochee devient sombre`() {
        assertEquals(ChoixTheme.SOMBRE, choixThemeDepuis(null, true))
    }

    @Test
    fun `ancienne case decochee devient systeme`() {
        assertEquals(ChoixTheme.SYSTEME, choixThemeDepuis(null, false))
    }

    @Test
    fun `aucun reglage donne systeme`() {
        assertEquals(ChoixTheme.SYSTEME, choixThemeDepuis(null, null))
    }

    @Test
    fun `chaine inconnue se comporte comme l'absence (case cochee)`() {
        assertEquals(ChoixTheme.SOMBRE, choixThemeDepuis("VIOLET", true))
    }

    @Test
    fun `chaine inconnue se comporte comme l'absence (case decochee ou absente)`() {
        assertEquals(ChoixTheme.SYSTEME, choixThemeDepuis("VIOLET", false))
        assertEquals(ChoixTheme.SYSTEME, choixThemeDepuis("VIOLET", null))
        assertEquals(ChoixTheme.SYSTEME, choixThemeDepuis("", null))
    }

    @Test
    fun `le nom enregistre prime sur l'ancienne case`() {
        assertEquals(ChoixTheme.CLAIR, choixThemeDepuis("CLAIR", true))
        assertEquals(ChoixTheme.SOMBRE, choixThemeDepuis("SOMBRE", false))
        assertEquals(ChoixTheme.SYSTEME, choixThemeDepuis("SYSTEME", true))
    }

    // --- Reglages ---

    @Test
    fun `le theme par defaut des reglages est systeme`() {
        assertEquals(ChoixTheme.SYSTEME, Reglages().theme)
    }
}
