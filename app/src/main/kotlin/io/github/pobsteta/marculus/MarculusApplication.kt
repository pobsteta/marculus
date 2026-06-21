package io.github.pobsteta.marculus

import android.app.Application
import android.content.Context
import io.github.pobsteta.marculus.data.MarculusData
import org.osmdroid.config.Configuration
import java.io.File

/** Conteneur d'injection minimal : instances uniques pour toute l'app. */
class MarculusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // osmdroid : user-agent requis + cache dans le dossier privé de l'app
        // (le cache externe par défaut est inaccessible sur Android 10+ → tuiles invisibles).
        val config = Configuration.getInstance()
        config.load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        config.userAgentValue = "Marculus/1.0 (+$packageName)"
        val base = File(cacheDir, "osmdroid").apply { mkdirs() }
        config.osmdroidBasePath = base
        config.osmdroidTileCache = File(base, "tiles").apply { mkdirs() }
    }

    private val data by lazy { MarculusData.creer(this) }
    val repository get() = data.repository
    val reglages get() = data.reglages
    val referentiels get() = data.referentiels
    val sauvegarde get() = data.sauvegarde
}
