package io.github.pobsteta.marculus.data

import android.content.Context
import io.github.pobsteta.marculus.data.db.MarculusDatabase

/** Point d'entrée du module données : construit le repository sans exposer Room à l'app. */
object MarculusData {
    fun creerRepository(context: Context): MartelageRepository {
        val db = MarculusDatabase.creer(context)
        return MartelageRepository(db.contexteDao(), db.tigeDao(), db.compteurConfigDao())
    }

    fun creerReglages(context: Context): ReglagesRepository = ReglagesRepository(context)
}
