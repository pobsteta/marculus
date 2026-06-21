package io.github.pobsteta.marculus

import android.app.Application
import io.github.pobsteta.marculus.data.MarculusData
import org.osmdroid.config.Configuration

/** Conteneur d'injection minimal : instances uniques pour toute l'app. */
class MarculusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // osmdroid : user-agent requis avant toute MapView.
        Configuration.getInstance().userAgentValue = packageName
    }

    private val data by lazy { MarculusData.creer(this) }
    val repository get() = data.repository
    val reglages get() = data.reglages
    val referentiels get() = data.referentiels
    val sauvegarde get() = data.sauvegarde
}
