package io.github.pobsteta.marculus

import android.app.Application
import io.github.pobsteta.marculus.data.MarculusData
import io.github.pobsteta.marculus.data.MartelageRepository

/** Conteneur d'injection minimal : une instance unique du repository pour toute l'app. */
class MarculusApplication : Application() {
    val repository: MartelageRepository by lazy { MarculusData.creerRepository(this) }
}
