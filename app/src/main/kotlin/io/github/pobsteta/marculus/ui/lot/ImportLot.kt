package io.github.pobsteta.marculus.ui.lot

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.pobsteta.marculus.R
import io.github.pobsteta.marculus.data.LotRepository
import kotlinx.coroutines.launch

/**
 * Import d'un lot de chantiers (.zip Nemeton), offert par deux écrans : la **liste des contextes**,
 * où l'on reçoit un lot, et les **Paramètres**, à côté de la synchro. Un seul code pour les deux :
 * le geste doit être identique, et surtout aucun des deux ne doit pouvoir dériver vers la
 * restauration destructive.
 */
@Stable
class EtatImportLot internal constructor() {
    /** Import en cours : le bouton se désarme (une archive de 12 Mo prend quelques secondes). */
    var enCours: Boolean by mutableStateOf(false)
        internal set

    /** Compte rendu à afficher, une fois l'import terminé. */
    internal var message: String? by mutableStateOf(null)

    internal var declencher: () -> Unit = {}

    /** Ouvre le sélecteur de fichier. */
    fun lancer() = declencher()
}

/** Prépare l'import : sélecteur de fichier, appel au dépôt, compte rendu. */
@Composable
fun rememberImportLot(lotRepository: LotRepository): EtatImportLot {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val etat = remember { EtatImportLot() }

    val msgSansMarsync = stringResource(R.string.lot_msg_sans_marsync)
    val msgMarsyncMultiple = stringResource(R.string.lot_msg_marsync_multiple)
    val msgEntreeRefusee = stringResource(R.string.lot_msg_entree_refusee)
    val msgIllisible = stringResource(R.string.param_msg_illisible)

    val lanceur = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            etat.enCours = true
            scope.launch {
                etat.message = when (val r = lotRepository.importer(uri)) {
                    is LotRepository.Resultat.Ok -> resume(context, r)
                    LotRepository.Resultat.SansMarsync -> msgSansMarsync
                    LotRepository.Resultat.MarsyncMultiple -> msgMarsyncMultiple
                    is LotRepository.Resultat.EntreeRefusee -> String.format(msgEntreeRefusee, r.nom)
                    LotRepository.Resultat.Illisible -> msgIllisible
                }
                etat.enCours = false
            }
        }
    }
    etat.declencher = {
        // « */* » en dernier : certains gestionnaires n'annoncent pas de type pour un .zip.
        lanceur.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
    }
    return etat
}

/** Compte rendu de l'import, à poser dans l'écran qui offre le bouton. */
@Composable
fun DialogueResultatLot(etat: EtatImportLot) {
    val texte = etat.message ?: return
    AlertDialog(
        onDismissRequest = { etat.message = null },
        confirmButton = {
            TextButton(onClick = { etat.message = null }) { Text(stringResource(R.string.param_ok)) }
        },
        text = { Text(texte) },
    )
}

/**
 * Résumé lisible d'un import. Les contextes restés sans carte sont **nommés** : un contexte muet
 * dont on ignore pourquoi il n'a pas de carte est pire qu'un message.
 */
private fun resume(context: Context, r: LotRepository.Resultat.Ok): String {
    val base = context.getString(R.string.lot_msg_ok, r.contextes, r.rattaches)
    if (r.sansGpkg.isEmpty()) return base
    return base + "\n\n" + context.getString(
        R.string.lot_msg_sans_gpkg,
        r.sansGpkg.size,
        r.sansGpkg.joinToString(", "),
    )
}
