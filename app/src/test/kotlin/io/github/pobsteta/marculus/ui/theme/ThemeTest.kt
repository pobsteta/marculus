package io.github.pobsteta.marculus.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import fr.nemeton.terrain.style.Schema
import fr.nemeton.terrain.style.StyleTerrain
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Mission 018 (brief surculus 2026-10-23, issue #2) : le `ColorScheme` de Marculus est égal, rôle par rôle,
 * aux jetons de `terrain-style` (v0.7.1), clair et sombre ; Marculus garde son vert.
 */
class ThemeTest {
    private fun roles(s: Schema, c: ColorScheme): List<Triple<String, Color, Color>> = listOf(
        Triple("primary", Color(s.primary), c.primary),
        Triple("onPrimary", Color(s.onPrimary), c.onPrimary),
        Triple("primaryContainer", Color(s.primaryContainer), c.primaryContainer),
        Triple("onPrimaryContainer", Color(s.onPrimaryContainer), c.onPrimaryContainer),
        Triple("inversePrimary", Color(s.inversePrimary), c.inversePrimary),
        Triple("secondary", Color(s.secondary), c.secondary),
        Triple("onSecondary", Color(s.onSecondary), c.onSecondary),
        Triple("secondaryContainer", Color(s.secondaryContainer), c.secondaryContainer),
        Triple("onSecondaryContainer", Color(s.onSecondaryContainer), c.onSecondaryContainer),
        Triple("tertiary", Color(s.tertiary), c.tertiary),
        Triple("onTertiary", Color(s.onTertiary), c.onTertiary),
        Triple("tertiaryContainer", Color(s.tertiaryContainer), c.tertiaryContainer),
        Triple("onTertiaryContainer", Color(s.onTertiaryContainer), c.onTertiaryContainer),
        Triple("background", Color(s.background), c.background),
        Triple("onBackground", Color(s.onBackground), c.onBackground),
        Triple("surface", Color(s.surface), c.surface),
        Triple("onSurface", Color(s.onSurface), c.onSurface),
        Triple("surfaceVariant", Color(s.surfaceVariant), c.surfaceVariant),
        Triple("onSurfaceVariant", Color(s.onSurfaceVariant), c.onSurfaceVariant),
        Triple("surfaceTint", Color(s.surfaceTint), c.surfaceTint),
        Triple("inverseSurface", Color(s.inverseSurface), c.inverseSurface),
        Triple("inverseOnSurface", Color(s.inverseOnSurface), c.inverseOnSurface),
        Triple("error", Color(s.error), c.error),
        Triple("onError", Color(s.onError), c.onError),
        Triple("errorContainer", Color(s.errorContainer), c.errorContainer),
        Triple("onErrorContainer", Color(s.onErrorContainer), c.onErrorContainer),
        Triple("outline", Color(s.outline), c.outline),
        Triple("outlineVariant", Color(s.outlineVariant), c.outlineVariant),
        Triple("scrim", Color(s.scrim), c.scrim),
        Triple("surfaceBright", Color(s.surfaceBright), c.surfaceBright),
        Triple("surfaceDim", Color(s.surfaceDim), c.surfaceDim),
        Triple("surfaceContainer", Color(s.surfaceContainer), c.surfaceContainer),
        Triple("surfaceContainerHigh", Color(s.surfaceContainerHigh), c.surfaceContainerHigh),
        Triple("surfaceContainerHighest", Color(s.surfaceContainerHighest), c.surfaceContainerHighest),
        Triple("surfaceContainerLow", Color(s.surfaceContainerLow), c.surfaceContainerLow),
        Triple("surfaceContainerLowest", Color(s.surfaceContainerLowest), c.surfaceContainerLowest),
        Triple("primaryFixed", Color(s.primaryFixed), c.primaryFixed),
        Triple("primaryFixedDim", Color(s.primaryFixedDim), c.primaryFixedDim),
        Triple("onPrimaryFixed", Color(s.onPrimaryFixed), c.onPrimaryFixed),
        Triple("onPrimaryFixedVariant", Color(s.onPrimaryFixedVariant), c.onPrimaryFixedVariant),
        Triple("secondaryFixed", Color(s.secondaryFixed), c.secondaryFixed),
        Triple("secondaryFixedDim", Color(s.secondaryFixedDim), c.secondaryFixedDim),
        Triple("onSecondaryFixed", Color(s.onSecondaryFixed), c.onSecondaryFixed),
        Triple("onSecondaryFixedVariant", Color(s.onSecondaryFixedVariant), c.onSecondaryFixedVariant),
        Triple("tertiaryFixed", Color(s.tertiaryFixed), c.tertiaryFixed),
        Triple("tertiaryFixedDim", Color(s.tertiaryFixedDim), c.tertiaryFixedDim),
        Triple("onTertiaryFixed", Color(s.onTertiaryFixed), c.onTertiaryFixed),
        Triple("onTertiaryFixedVariant", Color(s.onTertiaryFixedVariant), c.onTertiaryFixedVariant),
    )

    private fun verifie(s: Schema, c: ColorScheme) {
        val r = roles(s, c)
        assertEquals(48, r.size)
        r.forEach { (nom, attendu, obtenu) -> assertEquals("rôle $nom", attendu, obtenu) }
    }

    @Test
    fun `clair - les 48 roles egalent les jetons de terrain-style`() = verifie(StyleTerrain.clair, SchemaClair)

    @Test
    fun `sombre - les 48 roles egalent les jetons de terrain-style`() = verifie(StyleTerrain.sombre, SchemaSombre)

    @Test
    fun `Marculus garde son vert`() {
        assertEquals(Color(0xFF3E7A40), SchemaClair.primary)
        assertEquals(Color(0xFF374742), SchemaClair.primaryContainer)
        assertEquals(Color(0xFF3E7A40), SchemaSombre.primary)
    }
}
