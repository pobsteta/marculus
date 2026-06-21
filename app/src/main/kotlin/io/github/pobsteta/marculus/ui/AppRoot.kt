package io.github.pobsteta.marculus.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.ModeMesure
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.ui.contextes.CreationContexteScreen
import io.github.pobsteta.marculus.ui.contextes.ListeContextesScreen
import io.github.pobsteta.marculus.ui.feuille.FeuilleMartelageScreen

/** Navigation minimale entre les écrans de la v1. */
sealed interface Route {
    data object Liste : Route
    data object Creation : Route
    data class Edition(val contexteId: String) : Route
    data class Feuille(val contexteId: String) : Route
}

private val RouteSaver = listSaver<Route, String>(
    save = { route ->
        when (route) {
            Route.Liste -> listOf("liste")
            Route.Creation -> listOf("creation")
            is Route.Edition -> listOf("edition", route.contexteId)
            is Route.Feuille -> listOf("feuille", route.contexteId)
        }
    },
    restore = { l ->
        when (l[0]) {
            "creation" -> Route.Creation
            "edition" -> Route.Edition(l[1])
            "feuille" -> Route.Feuille(l[1])
            else -> Route.Liste
        }
    },
)

fun ModeMesure.libelle(): String = when (this) {
    ModeMesure.DIAMETRE -> "Diamètre"
    ModeMesure.CIRCONFERENCE -> "Circonférence"
}

@Composable
fun AppRoot(repository: MartelageRepository) {
    // rememberSaveable : la navigation survit aux rotations / recréations d'activité.
    var route: Route by rememberSaveable(stateSaver = RouteSaver) { mutableStateOf<Route>(Route.Liste) }

    when (val r = route) {
        Route.Liste -> ListeContextesScreen(
            repository = repository,
            onCreer = { route = Route.Creation },
            onOuvrir = { id -> route = Route.Feuille(id) },
            onModifier = { id -> route = Route.Edition(id) },
        )

        Route.Creation -> CreationContexteScreen(
            repository = repository,
            contexteExistant = null,
            onAnnuler = { route = Route.Liste },
            onEnregistre = { id -> route = Route.Feuille(id) },
        )

        is Route.Edition -> {
            val contexte by produceState<Contexte?>(initialValue = null, r.contexteId) {
                value = repository.contexte(r.contexteId)
            }
            val ctx = contexte
            if (ctx == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                CreationContexteScreen(
                    repository = repository,
                    contexteExistant = ctx,
                    onAnnuler = { route = Route.Liste },
                    onEnregistre = { route = Route.Liste },
                )
            }
        }

        is Route.Feuille -> FeuilleMartelageScreen(
            repository = repository,
            contexteId = r.contexteId,
            onRetour = { route = Route.Liste },
        )
    }
}
