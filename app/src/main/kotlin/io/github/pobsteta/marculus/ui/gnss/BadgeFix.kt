package io.github.pobsteta.marculus.ui.gnss

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.marculus.core.model.FixGnss
import fr.marculus.core.model.QualiteFix

/**
 * Pastille de **qualité du fix GNSS** : couleur selon RTK fixe / flottant / DGPS / autonome,
 * libellé issu de [QualiteFix.libelle] et précision horizontale (cm ou m) si disponible.
 */
@Composable
fun BadgeFix(fix: FixGnss?, modifier: Modifier = Modifier) {
    val qualite = fix?.qualite ?: QualiteFix.INVALIDE
    val couleur = when (qualite) {
        QualiteFix.RTK_FIXE -> Color(0xFF2E7D32)
        QualiteFix.RTK_FLOAT -> Color(0xFF9E9D24)
        QualiteFix.DGPS -> Color(0xFF1565C0)
        QualiteFix.AUTONOME, QualiteFix.PPS -> Color(0xFF616161)
        else -> Color(0xFFB71C1C)
    }
    val precision = fix?.precisionHorizontaleM?.let { " · ${formaterPrecision(it)}" }.orEmpty()
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(couleur)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "${qualite.libelle}$precision",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Précision en cm sous le mètre, en m au-delà. */
private fun formaterPrecision(metres: Double): String =
    if (metres < 1.0) "${(metres * 100).toInt()} cm" else "%.1f m".format(metres)
