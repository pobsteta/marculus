package io.github.pobsteta.marculus.ui.tige

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.marculus.core.model.ActionTige
import io.github.pobsteta.marculus.R

/**
 * Dialogue de saisie libre / correction d'une tige : essence (libre ou parmi le contexte),
 * classe/diamètre, quantité, hauteur et qualité libres — y compris hors matrice.
 * Réutilisé en création (feuille) et en édition (historique).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SaisieTigeDialog(
    edition: Boolean,
    essencesContexte: List<String>,
    qualites: List<String>,
    actionInitiale: ActionTige = ActionTige.PLUS,
    essenceInitiale: String = "",
    classeInitiale: String = "",
    quantiteInitiale: String = "1",
    hauteurInitiale: String = "",
    qualiteInitiale: String = "",
    onAnnuler: () -> Unit,
    onValider: (ActionTige, String, Int, Int, String?, String?) -> Unit,
) {
    var action by remember { mutableStateOf(actionInitiale) }
    var essence by remember { mutableStateOf(essenceInitiale) }
    var classe by remember { mutableStateOf(classeInitiale) }
    var quantite by remember { mutableStateOf(quantiteInitiale) }
    var hauteur by remember { mutableStateOf(hauteurInitiale) }
    var qualite by remember { mutableStateOf(qualiteInitiale) }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text(stringResource(if (edition) R.string.tige_modifier_titre else R.string.tige_saisir_titre)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = action == ActionTige.PLUS,
                        onClick = { action = ActionTige.PLUS },
                        label = { Text(stringResource(R.string.tige_action_plus)) },
                    )
                    FilterChip(
                        selected = action == ActionTige.ANNULATION,
                        onClick = { action = ActionTige.ANNULATION },
                        label = { Text(stringResource(R.string.tige_action_moins)) },
                    )
                }
                OutlinedTextField(
                    value = essence,
                    onValueChange = { essence = it },
                    label = { Text(stringResource(R.string.tige_essence)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (essencesContexte.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        essencesContexte.forEach { e ->
                            AssistChip(onClick = { essence = e }, label = { Text(e) })
                        }
                    }
                }
                OutlinedTextField(
                    value = classe,
                    onValueChange = { saisie -> classe = saisie.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.tige_classe)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = quantite,
                    onValueChange = { saisie -> quantite = saisie.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.tige_quantite)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (action == ActionTige.PLUS) {
                    OutlinedTextField(
                        value = hauteur,
                        onValueChange = { hauteur = it },
                        label = { Text(stringResource(R.string.tige_hauteur)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (qualites.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            qualites.forEach { q ->
                                FilterChip(
                                    selected = qualite == q,
                                    onClick = { qualite = if (qualite == q) "" else q },
                                    label = { Text(q) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val c = classe.toIntOrNull()
                val q = quantite.toIntOrNull() ?: 1
                if (essence.isNotBlank() && c != null && q > 0) {
                    onValider(
                        action,
                        essence.trim(),
                        c,
                        q,
                        hauteur.trim().ifBlank { null },
                        qualite.ifBlank { null },
                    )
                }
            }) { Text(stringResource(R.string.tige_valider)) }
        },
        dismissButton = { TextButton(onClick = onAnnuler) { Text(stringResource(R.string.tige_annuler)) } },
    )
}
