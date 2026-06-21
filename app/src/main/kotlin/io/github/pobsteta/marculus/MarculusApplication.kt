package io.github.pobsteta.marculus

import android.app.Application
import io.github.pobsteta.marculus.data.MarculusData

/** Conteneur d'injection minimal : instances uniques pour toute l'app. */
class MarculusApplication : Application() {
    private val data by lazy { MarculusData.creer(this) }
    val repository get() = data.repository
    val reglages get() = data.reglages
    val referentiels get() = data.referentiels
    val sauvegarde get() = data.sauvegarde
}
