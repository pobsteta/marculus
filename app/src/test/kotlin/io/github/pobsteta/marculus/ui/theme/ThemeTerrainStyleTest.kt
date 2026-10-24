package io.github.pobsteta.marculus.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import fr.nemeton.terrain.style.Schema
import fr.nemeton.terrain.style.StyleTerrain
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Mission 005 (issue #2, critère d'acceptation 2) : les `ColorScheme` clair et sombre de Marculus
 * sont égaux, rôle par rôle, aux jetons `StyleTerrain.clair` et `StyleTerrain.sombre` de
 * `terrain-style` v0.6.0. Un échec nomme le rôle fautif. `reussi`, `alerte` et `interditsEnTexte`
 * ne sont pas des rôles de `ColorScheme` : hors test.
 *
 * Contrat (mission 006) : `internal val SchemaClair` et `SchemaSombre` dans `Theme.kt`.
 */
class ThemeTerrainStyleTest {

    private class Paire(val jeton: (Schema) -> Long, val couleur: (ColorScheme) -> Color)

    // Les 48 rôles de ColorScheme (Material 3 1.4.0), dans l'ordre de `Schema`.
    private val roles: List<Pair<String, Paire>> = listOf(
        "primary" to Paire({ it.primary }, { it.primary }),
        "onPrimary" to Paire({ it.onPrimary }, { it.onPrimary }),
        "primaryContainer" to Paire({ it.primaryContainer }, { it.primaryContainer }),
        "onPrimaryContainer" to Paire({ it.onPrimaryContainer }, { it.onPrimaryContainer }),
        "inversePrimary" to Paire({ it.inversePrimary }, { it.inversePrimary }),
        "secondary" to Paire({ it.secondary }, { it.secondary }),
        "onSecondary" to Paire({ it.onSecondary }, { it.onSecondary }),
        "secondaryContainer" to Paire({ it.secondaryContainer }, { it.secondaryContainer }),
        "onSecondaryContainer" to Paire({ it.onSecondaryContainer }, { it.onSecondaryContainer }),
        "tertiary" to Paire({ it.tertiary }, { it.tertiary }),
        "onTertiary" to Paire({ it.onTertiary }, { it.onTertiary }),
        "tertiaryContainer" to Paire({ it.tertiaryContainer }, { it.tertiaryContainer }),
        "onTertiaryContainer" to Paire({ it.onTertiaryContainer }, { it.onTertiaryContainer }),
        "background" to Paire({ it.background }, { it.background }),
        "onBackground" to Paire({ it.onBackground }, { it.onBackground }),
        "surface" to Paire({ it.surface }, { it.surface }),
        "onSurface" to Paire({ it.onSurface }, { it.onSurface }),
        "surfaceVariant" to Paire({ it.surfaceVariant }, { it.surfaceVariant }),
        "onSurfaceVariant" to Paire({ it.onSurfaceVariant }, { it.onSurfaceVariant }),
        "surfaceTint" to Paire({ it.surfaceTint }, { it.surfaceTint }),
        "inverseSurface" to Paire({ it.inverseSurface }, { it.inverseSurface }),
        "inverseOnSurface" to Paire({ it.inverseOnSurface }, { it.inverseOnSurface }),
        "error" to Paire({ it.error }, { it.error }),
        "onError" to Paire({ it.onError }, { it.onError }),
        "errorContainer" to Paire({ it.errorContainer }, { it.errorContainer }),
        "onErrorContainer" to Paire({ it.onErrorContainer }, { it.onErrorContainer }),
        "outline" to Paire({ it.outline }, { it.outline }),
        "outlineVariant" to Paire({ it.outlineVariant }, { it.outlineVariant }),
        "scrim" to Paire({ it.scrim }, { it.scrim }),
        "surfaceBright" to Paire({ it.surfaceBright }, { it.surfaceBright }),
        "surfaceDim" to Paire({ it.surfaceDim }, { it.surfaceDim }),
        "surfaceContainer" to Paire({ it.surfaceContainer }, { it.surfaceContainer }),
        "surfaceContainerHigh" to Paire({ it.surfaceContainerHigh }, { it.surfaceContainerHigh }),
        "surfaceContainerHighest" to Paire({ it.surfaceContainerHighest }, { it.surfaceContainerHighest }),
        "surfaceContainerLow" to Paire({ it.surfaceContainerLow }, { it.surfaceContainerLow }),
        "surfaceContainerLowest" to Paire({ it.surfaceContainerLowest }, { it.surfaceContainerLowest }),
        "primaryFixed" to Paire({ it.primaryFixed }, { it.primaryFixed }),
        "primaryFixedDim" to Paire({ it.primaryFixedDim }, { it.primaryFixedDim }),
        "onPrimaryFixed" to Paire({ it.onPrimaryFixed }, { it.onPrimaryFixed }),
        "onPrimaryFixedVariant" to Paire({ it.onPrimaryFixedVariant }, { it.onPrimaryFixedVariant }),
        "secondaryFixed" to Paire({ it.secondaryFixed }, { it.secondaryFixed }),
        "secondaryFixedDim" to Paire({ it.secondaryFixedDim }, { it.secondaryFixedDim }),
        "onSecondaryFixed" to Paire({ it.onSecondaryFixed }, { it.onSecondaryFixed }),
        "onSecondaryFixedVariant" to Paire({ it.onSecondaryFixedVariant }, { it.onSecondaryFixedVariant }),
        "tertiaryFixed" to Paire({ it.tertiaryFixed }, { it.tertiaryFixed }),
        "tertiaryFixedDim" to Paire({ it.tertiaryFixedDim }, { it.tertiaryFixedDim }),
        "onTertiaryFixed" to Paire({ it.onTertiaryFixed }, { it.onTertiaryFixed }),
        "onTertiaryFixedVariant" to Paire({ it.onTertiaryFixedVariant }, { it.onTertiaryFixedVariant }),
    )

    @Test
    fun la_table_couvre_les_48_roles_du_schema() {
        assertEquals(48, roles.size, "la table des rôles du test")
        assertEquals(roles.size, roles.map { it.first }.toSet().size, "rôles en double dans la table")
        val champsArgb = Schema::class.java.declaredFields.count { it.type == java.lang.Long.TYPE }
        assertEquals(champsArgb, roles.size, "Schema a gagné ou perdu un rôle : compléter la table")
    }

    @Test
    fun schema_clair_egal_aux_jetons_terrain_style() = comparer("clair", SchemaClair, StyleTerrain.clair)

    @Test
    fun schema_sombre_egal_aux_jetons_terrain_style() = comparer("sombre", SchemaSombre, StyleTerrain.sombre)

    private fun comparer(nom: String, obtenu: ColorScheme, jetons: Schema) {
        val ecarts = roles.mapNotNull { (role, p) ->
            val attendu = Color(p.jeton(jetons))
            val reel = p.couleur(obtenu)
            if (reel == attendu) null else "$role : $reel au lieu de $attendu"
        }
        assertEquals(emptyList<String>(), ecarts, "schéma $nom : rôles différents des jetons terrain-style")
    }
}
