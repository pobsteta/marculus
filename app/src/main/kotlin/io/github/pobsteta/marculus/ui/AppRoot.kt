package io.github.pobsteta.marculus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import fr.marculus.core.model.ModeMesure
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.ui.contextes.CreationContexteScreen
import io.github.pobsteta.marculus.ui.contextes.ListeContextesScreen
import io.github.pobsteta.marculus.ui.feuille.FeuilleMartelageScreen

/** Navigation minimale entre les trois écrans de la v1. */
sealed interface Route {
    data object Liste : Route
    data object Creation : Route
    data class Feuille(val contexteId: String) : Route
}

fun ModeMesure.libelle(): String = when (this) {
    ModeMesure.DIAMETRE -> "Diamètre"
    ModeMesure.CIRCONFERENCE -> "Circonférence"
}

@Composable
fun AppRoot(repository: MartelageRepository) {
    var route: Route by remember { mutableStateOf<Route>(Route.Liste) }

    when (val r = route) {
        Route.Liste -> ListeContextesScreen(
            repository = repository,
            onCreer = { route = Route.Creation },
            onOuvrir = { id -> route = Route.Feuille(id) },
        )

        Route.Creation -> CreationContexteScreen(
            repository = repository,
            onAnnuler = { route = Route.Liste },
            onCree = { id -> route = Route.Feuille(id) },
        )

        is Route.Feuille -> FeuilleMartelageScreen(
            repository = repository,
            contexteId = r.contexteId,
            onRetour = { route = Route.Liste },
        )
    }
}
