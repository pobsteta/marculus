package io.github.pobsteta.marculus.ui.statut

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Taille de départ (sp) du libellé d'un onglet : celle du style des onglets Material 3. */
internal const val TAILLE_ONGLET_SP = 14f

/** Plancher (sp) : en dessous, le libellé n'est plus lisible, on laisse alors le système couper. */
internal const val TAILLE_ONGLET_MIN_SP = 9f

/**
 * Vrai si un retour à la ligne tombe au milieu d'un mot.
 * [finsDeLigne] : offsets de fin de chaque ligne, espace final compris, la dernière ligne comprise.
 * Une coupe entre deux mots laisse l'espace en fin de ligne ; sinon le mot est coupé.
 */
internal fun coupeUnMot(texte: String, finsDeLigne: List<Int>): Boolean =
    finsDeLigne.dropLast(1).any { fin -> fin in 1..texte.length && !texte[fin - 1].isWhitespace() }

/**
 * Libellé d'onglet : passe à la ligne entre les mots, et si un mot ne tient quand même pas
 * dans la largeur de l'onglet, réduit la taille du texte jusqu'à ce qu'aucun mot ne soit coupé ni masqué (au-delà de 2 lignes).
 */
@Composable
internal fun LibelleOnglet(texte: String) {
    var taille by remember(texte) { mutableFloatStateOf(TAILLE_ONGLET_SP) }
    // Marge de 4 dp au lieu des 16 dp du `Tab` à libellé simple : un onglet sur quatre fait 90 dp à 360 dp.
    Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Text(
            text = texte,
            fontSize = taille.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            onTextLayout = { r ->
                val fins = (0 until r.lineCount).map { r.getLineEnd(it) }
                if ((coupeUnMot(texte, fins) || r.didOverflowWidth || r.didOverflowHeight) && taille > TAILLE_ONGLET_MIN_SP) {
                    taille = maxOf(TAILLE_ONGLET_MIN_SP, taille - 1f)
                }
            },
        )
    }
}
