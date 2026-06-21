package io.github.pobsteta.marculus

import android.app.Application
import io.github.pobsteta.marculus.data.MarculusData
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.data.ReferentielsRepository
import io.github.pobsteta.marculus.data.ReglagesRepository

/** Conteneur d'injection minimal : instances uniques pour toute l'app. */
class MarculusApplication : Application() {
    val repository: MartelageRepository by lazy { MarculusData.creerRepository(this) }
    val reglages: ReglagesRepository by lazy { MarculusData.creerReglages(this) }
    val referentiels: ReferentielsRepository by lazy { MarculusData.creerReferentiels(this) }
}
