package io.github.pobsteta.marculus.data

import android.content.Context
import io.github.pobsteta.marculus.data.db.MarculusDatabase

/** Point d'entrée du module données : construit les dépôts sans exposer Room à l'app. */
object MarculusData {
    data class Conteneur(
        val repository: MartelageRepository,
        val reglages: ReglagesRepository,
        val referentiels: ReferentielsRepository,
        val sauvegarde: SauvegardeRepository,
        val gpkg: GpkgRepository,
    )

    fun creer(context: Context): Conteneur {
        val db = MarculusDatabase.creer(context)
        val referentiels = ReferentielsRepository(context)
        return Conteneur(
            repository = MartelageRepository(db.contexteDao(), db.tigeDao(), db.compteurConfigDao()),
            reglages = ReglagesRepository(context),
            referentiels = referentiels,
            sauvegarde = SauvegardeRepository(db.contexteDao(), db.tigeDao(), db.compteurConfigDao(), referentiels),
            gpkg = GpkgRepository(context),
        )
    }
}
