package io.github.pobsteta.marculus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VertMarculus = Color(0xFF374742)
private val VertVif = Color(0xFF3E7A40)
private val Creme = Color(0xFFF5EBD8)

private val LightColors = lightColorScheme(
    primary = VertVif,
    onPrimary = Color.White,
    primaryContainer = VertMarculus,
    onPrimaryContainer = Creme,
    secondary = VertMarculus,
)

private val DarkColors = darkColorScheme(
    primary = VertVif,
    onPrimary = Color.White,
    primaryContainer = VertMarculus,
    onPrimaryContainer = Creme,
    secondary = Creme,
)

@Composable
fun MarculusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
