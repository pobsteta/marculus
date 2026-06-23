package io.github.pobsteta.marculus.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.pobsteta.marculus.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import fr.marculus.core.Referentiels
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.Reglages
import fr.marculus.core.model.SeuilsCategories
import io.github.pobsteta.marculus.Appareil
import io.github.pobsteta.marculus.data.GpkgRepository
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.data.ReferentielsRepository
import io.github.pobsteta.marculus.data.ReglagesRepository
import io.github.pobsteta.marculus.data.SauvegardeRepository
import io.github.pobsteta.marculus.ui.carte.CarteScreen
import io.github.pobsteta.marculus.ui.contextes.CreationContexteScreen
import io.github.pobsteta.marculus.ui.contextes.ListeContextesScreen
import io.github.pobsteta.marculus.ui.feuille.FeuilleMartelageScreen
import io.github.pobsteta.marculus.ui.parametres.ParametresScreen
import io.github.pobsteta.marculus.ui.referentiels.ReferentielsScreen
import io.github.pobsteta.marculus.ui.statut.StatutHistoriqueScreen

/** Navigation minimale entre les écrans de la v1. */
sealed interface Route {
    data object Liste : Route
    data object Creation : Route
    data object Parametres : Route
    data object Referentiels : Route
    data class Edition(val contexteId: String) : Route
    data class Feuille(val contexteId: String) : Route
    data class Statut(val contexteId: String) : Route
    data class Carte(val contexteId: String) : Route
}

private val RouteSaver = listSaver<Route, String>(
    save = { route ->
        when (route) {
            Route.Liste -> listOf("liste")
            Route.Creation -> listOf("creation")
            Route.Parametres -> listOf("parametres")
            Route.Referentiels -> listOf("referentiels")
            is Route.Edition -> listOf("edition", route.contexteId)
            is Route.Feuille -> listOf("feuille", route.contexteId)
            is Route.Statut -> listOf("statut", route.contexteId)
            is Route.Carte -> listOf("carte", route.contexteId)
        }
    },
    restore = { l ->
        when (l[0]) {
            "creation" -> Route.Creation
            "parametres" -> Route.Parametres
            "referentiels" -> Route.Referentiels
            "edition" -> Route.Edition(l[1])
            "feuille" -> Route.Feuille(l[1])
            "statut" -> Route.Statut(l[1])
            "carte" -> Route.Carte(l[1])
            else -> Route.Liste
        }
    },
)

@Composable
fun ModeMesure.libelle(): String = when (this) {
    ModeMesure.DIAMETRE -> stringResource(R.string.app_mode_diametre)
    ModeMesure.CIRCONFERENCE -> stringResource(R.string.app_mode_circonference)
}

@Composable
fun AppRoot(
    repository: MartelageRepository,
    reglagesRepository: ReglagesRepository,
    reglages: Reglages,
    referentielsRepository: ReferentielsRepository,
    sauvegardeRepository: SauvegardeRepository,
    gpkgRepository: GpkgRepository,
) {
    // rememberSaveable : la navigation survit aux rotations / recréations d'activité.
    var route: Route by rememberSaveable(stateSaver = RouteSaver) { mutableStateOf<Route>(Route.Liste) }
    val scope = rememberCoroutineScope()

    // Ouvre une feuille et mémorise le contexte (pour « rouvrir le dernier »).
    fun ouvrir(id: String) {
        scope.launch { reglagesRepository.enregistrerDernierContexte(id) }
        route = Route.Feuille(id)
    }

    // Au tout premier affichage : rouvrir le dernier contexte si l'option est active.
    var routageInitial by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!routageInitial) {
            routageInitial = true
            val r = reglagesRepository.reglages.first()
            val id = r.dernierContexteId
            if (r.rouvrirDernier && id != null && route == Route.Liste && repository.contexte(id) != null) {
                route = Route.Feuille(id)
            }
        }
    }

    val essences by referentielsRepository.essences.collectAsStateWithLifecycle(Referentiels.ESSENCES_DEFAUT)
    val qualitesArbre by referentielsRepository.qualitesArbre.collectAsStateWithLifecycle(Referentiels.QUALITE_ARBRE_DEFAUT)
    val qualitesBois by referentielsRepository.qualitesBois.collectAsStateWithLifecycle(Referentiels.QUALITE_BOIS_DEFAUT)
    val seuils by referentielsRepository.seuils.collectAsStateWithLifecycle(SeuilsCategories.DEFAUT)

    val context = LocalContext.current
    var dernierRetour by remember { mutableStateOf(0L) }
    BackHandler {
        when (val r = route) {
            Route.Liste -> {
                val maintenant = System.currentTimeMillis()
                if (maintenant - dernierRetour < 2000) {
                    (context as? Activity)?.finish()
                } else {
                    dernierRetour = maintenant
                    Toast.makeText(
                        context,
                        context.getString(R.string.app_double_retour),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }

            Route.Creation, Route.Parametres, Route.Referentiels -> route = Route.Liste
            is Route.Edition -> route = Route.Liste
            is Route.Feuille -> route = Route.Liste
            is Route.Statut -> route = Route.Feuille(r.contexteId)
            is Route.Carte -> route = Route.Feuille(r.contexteId)
        }
    }

    when (val r = route) {
        Route.Liste -> ListeContextesScreen(
            repository = repository,
            sauvegardeRepository = sauvegardeRepository,
            operateur = reglages.operateur?.takeIf { it.isNotBlank() } ?: Appareil.id(context),
            vueKanban = reglages.vueKanban,
            onCreer = { route = Route.Creation },
            onOuvrir = { id -> ouvrir(id) },
            onModifier = { id -> route = Route.Edition(id) },
            onParametres = { route = Route.Parametres },
            onReferentiels = { route = Route.Referentiels },
        )

        Route.Creation -> CreationContexteScreen(
            repository = repository,
            gpkgRepository = gpkgRepository,
            contexteExistant = null,
            essencesReferentiel = essences,
            onAnnuler = { route = Route.Liste },
            onEnregistre = { id -> ouvrir(id) },
        )

        Route.Parametres -> ParametresScreen(
            reglagesRepository = reglagesRepository,
            sauvegardeRepository = sauvegardeRepository,
            onRetour = { route = Route.Liste },
        )

        Route.Referentiels -> ReferentielsScreen(
            referentielsRepository = referentielsRepository,
            onRetour = { route = Route.Liste },
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
                    gpkgRepository = gpkgRepository,
                    contexteExistant = ctx,
                    essencesReferentiel = essences,
                    onAnnuler = { route = Route.Liste },
                    onEnregistre = { route = Route.Liste },
                )
            }
        }

        is Route.Feuille -> FeuilleMartelageScreen(
            repository = repository,
            contexteId = r.contexteId,
            reglages = reglages,
            qualitesArbre = qualitesArbre,
            qualitesBois = qualitesBois,
            gpkgRepository = gpkgRepository,
            onRetour = { route = Route.Liste },
            onStatut = { route = Route.Statut(r.contexteId) },
            onCarte = { route = Route.Carte(r.contexteId) },
        )

        is Route.Statut -> StatutHistoriqueScreen(
            repository = repository,
            gpkgRepository = gpkgRepository,
            contexteId = r.contexteId,
            seuils = seuils,
            qualitesArbre = qualitesArbre,
            onRetour = { route = Route.Feuille(r.contexteId) },
        )

        is Route.Carte -> CarteScreen(
            repository = repository,
            contexteId = r.contexteId,
            gpkgRepository = gpkgRepository,
            onRetour = { route = Route.Feuille(r.contexteId) },
        )
    }
}
