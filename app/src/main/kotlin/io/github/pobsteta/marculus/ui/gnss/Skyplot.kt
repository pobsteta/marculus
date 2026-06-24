package io.github.pobsteta.marculus.ui.gnss

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.marculus.core.model.SatelliteGsv
import kotlin.math.cos
import kotlin.math.sin

/** Couleur d'un satellite selon son rapport signal/bruit (dB-Hz). */
private fun couleurSnr(snr: Int?): Color = when {
    snr == null || snr <= 0 -> Color(0xFF9E9E9E)
    snr < 25 -> Color(0xFFD32F2F)
    snr < 35 -> Color(0xFFF57C00)
    snr < 45 -> Color(0xFF9E9D24)
    else -> Color(0xFF2E7D32)
}

/**
 * Tracé polaire des satellites en vue (skyplot, façon SW Maps) : élévation du centre (90°) au
 * bord (0°), azimut horaire avec le Nord en haut. Couleur = qualité du signal.
 */
@Composable
fun Skyplot(satellites: List<SatelliteGsv>, modifier: Modifier = Modifier) {
    val grille = MaterialTheme.colorScheme.outline
    val texte = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.aspectRatio(1f)) {
        val rayon = size.minDimension / 2f * 0.82f
        val cx = size.width / 2f
        val cy = size.height / 2f
        listOf(1f, 2f / 3f, 1f / 3f).forEach { f ->
            drawCircle(grille, rayon * f, Offset(cx, cy), style = Stroke(width = 2f))
        }
        drawLine(grille, Offset(cx, cy - rayon), Offset(cx, cy + rayon), 2f)
        drawLine(grille, Offset(cx - rayon, cy), Offset(cx + rayon, cy), 2f)

        val nc = drawContext.canvas.nativeCanvas
        val pCard = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = rayon * 0.13f
            color = texte.toArgb()
            isFakeBoldText = true
        }
        nc.drawText("N", cx, cy - rayon - 4f + pCard.textSize, pCard)
        nc.drawText("S", cx, cy + rayon + pCard.textSize, pCard)
        nc.drawText("E", cx + rayon + pCard.textSize * 0.6f, cy + pCard.textSize / 3f, pCard)
        nc.drawText("O", cx - rayon - pCard.textSize * 0.6f, cy + pCard.textSize / 3f, pCard)

        val pSat = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = rayon * 0.10f
            color = texte.toArgb()
        }
        satellites.forEach { s ->
            val elev = s.elevation ?: return@forEach
            val az = s.azimut ?: return@forEach
            val r = rayon * (1f - elev.coerceIn(0, 90) / 90f)
            val rad = Math.toRadians(az.toDouble())
            val x = cx + (r * sin(rad)).toFloat()
            val y = cy - (r * cos(rad)).toFloat()
            drawCircle(couleurSnr(s.snr), rayon * 0.06f, Offset(x, y))
            nc.drawText(s.prn.toString(), x, y - rayon * 0.08f, pSat)
        }
    }
}

/** Barres de rapport signal/bruit par satellite (défilables horizontalement). */
@Composable
fun BarresSnr(satellites: List<SatelliteGsv>, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).heightIn(min = 64.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        satellites.sortedBy { it.prn }.forEach { s ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(8.dp)
                        .height((s.snr ?: 0).coerceIn(0, 55).dp)
                        .background(couleurSnr(s.snr)),
                )
                Text(s.prn.toString(), fontSize = 7.sp, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
