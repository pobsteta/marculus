package io.github.pobsteta.marculus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import fr.nemeton.terrain.style.Schema
import fr.nemeton.terrain.style.StyleTerrain

/**
 * Style commun des applications de terrain : les jetons de `terrain-style` (v0.7.1, ADR 0006 de
 * terrain), recopiés 1:1, rôle par rôle, dans le `ColorScheme`. Aucune couleur n'est définie ici ;
 * le vert de Marculus (VertVif en `primary`, VertMarculus en `primaryContainer`) vient de
 * [StyleTerrain]. `ThemeTest` vérifie l'égalité des 48 rôles.
 */
private fun c(argb: Long) = Color(argb)

internal fun Schema.versClair(): ColorScheme = lightColorScheme(
    primary = c(primary), onPrimary = c(onPrimary),
    primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
    inversePrimary = c(inversePrimary),
    secondary = c(secondary), onSecondary = c(onSecondary),
    secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
    tertiary = c(tertiary), onTertiary = c(onTertiary),
    tertiaryContainer = c(tertiaryContainer), onTertiaryContainer = c(onTertiaryContainer),
    background = c(background), onBackground = c(onBackground),
    surface = c(surface), onSurface = c(onSurface),
    surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant),
    surfaceTint = c(surfaceTint),
    inverseSurface = c(inverseSurface), inverseOnSurface = c(inverseOnSurface),
    error = c(error), onError = c(onError),
    errorContainer = c(errorContainer), onErrorContainer = c(onErrorContainer),
    outline = c(outline), outlineVariant = c(outlineVariant), scrim = c(scrim),
    surfaceBright = c(surfaceBright), surfaceDim = c(surfaceDim),
    surfaceContainer = c(surfaceContainer), surfaceContainerHigh = c(surfaceContainerHigh),
    surfaceContainerHighest = c(surfaceContainerHighest),
    surfaceContainerLow = c(surfaceContainerLow), surfaceContainerLowest = c(surfaceContainerLowest),
    primaryFixed = c(primaryFixed), primaryFixedDim = c(primaryFixedDim),
    onPrimaryFixed = c(onPrimaryFixed), onPrimaryFixedVariant = c(onPrimaryFixedVariant),
    secondaryFixed = c(secondaryFixed), secondaryFixedDim = c(secondaryFixedDim),
    onSecondaryFixed = c(onSecondaryFixed), onSecondaryFixedVariant = c(onSecondaryFixedVariant),
    tertiaryFixed = c(tertiaryFixed), tertiaryFixedDim = c(tertiaryFixedDim),
    onTertiaryFixed = c(onTertiaryFixed), onTertiaryFixedVariant = c(onTertiaryFixedVariant),
)

internal fun Schema.versSombre(): ColorScheme = darkColorScheme(
    primary = c(primary), onPrimary = c(onPrimary),
    primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
    inversePrimary = c(inversePrimary),
    secondary = c(secondary), onSecondary = c(onSecondary),
    secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
    tertiary = c(tertiary), onTertiary = c(onTertiary),
    tertiaryContainer = c(tertiaryContainer), onTertiaryContainer = c(onTertiaryContainer),
    background = c(background), onBackground = c(onBackground),
    surface = c(surface), onSurface = c(onSurface),
    surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant),
    surfaceTint = c(surfaceTint),
    inverseSurface = c(inverseSurface), inverseOnSurface = c(inverseOnSurface),
    error = c(error), onError = c(onError),
    errorContainer = c(errorContainer), onErrorContainer = c(onErrorContainer),
    outline = c(outline), outlineVariant = c(outlineVariant), scrim = c(scrim),
    surfaceBright = c(surfaceBright), surfaceDim = c(surfaceDim),
    surfaceContainer = c(surfaceContainer), surfaceContainerHigh = c(surfaceContainerHigh),
    surfaceContainerHighest = c(surfaceContainerHighest),
    surfaceContainerLow = c(surfaceContainerLow), surfaceContainerLowest = c(surfaceContainerLowest),
    primaryFixed = c(primaryFixed), primaryFixedDim = c(primaryFixedDim),
    onPrimaryFixed = c(onPrimaryFixed), onPrimaryFixedVariant = c(onPrimaryFixedVariant),
    secondaryFixed = c(secondaryFixed), secondaryFixedDim = c(secondaryFixedDim),
    onSecondaryFixed = c(onSecondaryFixed), onSecondaryFixedVariant = c(onSecondaryFixedVariant),
    tertiaryFixed = c(tertiaryFixed), tertiaryFixedDim = c(tertiaryFixedDim),
    onTertiaryFixed = c(onTertiaryFixed), onTertiaryFixedVariant = c(onTertiaryFixedVariant),
)

internal val SchemaClair: ColorScheme = StyleTerrain.clair.versClair()
internal val SchemaSombre: ColorScheme = StyleTerrain.sombre.versSombre()

@Composable
fun MarculusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) SchemaSombre else SchemaClair,
        content = content,
    )
}
