package io.github.pobsteta.marculus.ui.gnss

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.marculus.core.model.FixGnss
import fr.marculus.core.model.QualiteFix

/**
 * Pastille de **qualité du fix GNSS** : couleur selon RTK fixe / flottant / DGPS / autonome,
 * libellé issu de [QualiteFix.libelle] et précision horizontale (cm ou m) si disponible. Utilisée
 * dans le test RTK des Paramètres (source toujours externe).
 */
@Composable
fun BadgeFix(fix: FixGnss?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val qualite = fix?.qualite ?: QualiteFix.INVALIDE
    val precision = fix?.precisionHorizontaleM?.let { " · ${formaterPrecision(it)}" }.orEmpty()
    Pastille("${qualite.libelle}$precision", couleurQualite(qualite), null, modifier, onClick)
}

/**
 * Badge GNSS **tri-état** du bandeau de martelage : distingue récepteur externe (icône antenne),
 * GNSS interne du téléphone (icône smartphone) et **absence de position** (capture coupée) — ce
 * dernier en rouge explicite, pour ne jamais marteler « à l'aveugle » sans s'en rendre compte.
 * La couleur suit la qualité du fix.
 *
 * @param capture maître-interrupteur « enregistrer la position GNSS ».
 * @param rtkActif vrai si la source est le récepteur externe (sinon GNSS interne).
 * @param fix fix retenu pour la tige (null = pas encore de position).
 */
@Composable
fun BadgeGnss(
    capture: Boolean,
    rtkActif: Boolean,
    fix: FixGnss?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    if (!capture) {
        Pastille("Sans position", Color(0xFFB71C1C), Icons.Filled.LocationOff, modifier, onClick)
        return
    }
    val icone = if (rtkActif) Icons.Filled.SatelliteAlt else Icons.Filled.Smartphone
    if (fix == null) {
        // Capture active mais pas encore de fix (récepteur en connexion / GNSS en recherche).
        Pastille("Recherche…", Color(0xFF616161), icone, modifier, onClick)
        return
    }
    val precision = fix.precisionHorizontaleM?.let { " · ${formaterPrecision(it)}" }.orEmpty()
    Pastille("${fix.qualite.libelle}$precision", couleurQualite(fix.qualite), icone, modifier, onClick)
}

/** Couleur de la pastille selon la qualité du fix (vert RTK fixe → rouge invalide). */
private fun couleurQualite(q: QualiteFix): Color = when (q) {
    QualiteFix.RTK_FIXE -> Color(0xFF2E7D32)
    QualiteFix.RTK_FLOAT -> Color(0xFF9E9D24)
    QualiteFix.DGPS -> Color(0xFF1565C0)
    QualiteFix.AUTONOME, QualiteFix.PPS -> Color(0xFF616161)
    else -> Color(0xFFB71C1C)
}

/** Pastille arrondie (fond coloré, icône + texte blancs), cliquable si [onClick] est fourni. */
@Composable
private fun Pastille(
    texte: String,
    couleur: Color,
    icone: ImageVector?,
    modifier: Modifier,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .background(couleur)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Text(
            text = texte,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Précision en cm sous le mètre, en m au-delà. */
private fun formaterPrecision(metres: Double): String =
    if (metres < 1.0) "${(metres * 100).toInt()} cm" else "%.1f m".format(metres)
