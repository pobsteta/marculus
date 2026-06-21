package io.github.pobsteta.marculus

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.model.Reglages
import io.github.pobsteta.marculus.ui.AppRoot
import io.github.pobsteta.marculus.ui.theme.MarculusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as MarculusApplication).repository
        val reglagesRepo = (application as MarculusApplication).reglages
        val referentielsRepo = (application as MarculusApplication).referentiels
        val sauvegardeRepo = (application as MarculusApplication).sauvegarde
        setContent {
            val reglages by reglagesRepo.reglages.collectAsStateWithLifecycle(Reglages())

            // Anti-veille : garder l'écran allumé.
            LaunchedEffect(reglages.antiVeille) {
                if (reglages.antiVeille) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            // Plein écran : masquer les barres système (réapparaissent au glissement).
            LaunchedEffect(reglages.pleinEcran) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                if (reglages.pleinEcran) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            MarculusTheme(darkTheme = reglages.themeSombre || isSystemInDarkTheme()) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(repository, reglagesRepo, reglages, referentielsRepo, sauvegardeRepo)
                }
            }
        }
    }
}
