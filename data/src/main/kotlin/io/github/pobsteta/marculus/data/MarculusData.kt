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
        val lot: LotRepository,
    )

    fun creer(context: Context): Conteneur {
        val db = MarculusDatabase.creer(context)
        val referentiels = ReferentielsRepository(context)
        val martelage = MartelageRepository(db.contexteDao(), db.tigeDao(), db.compteurConfigDao())
        val sauvegarde =
            SauvegardeRepository(db.contexteDao(), db.tigeDao(), db.compteurConfigDao(), db.mergeDao(), referentiels)
        return Conteneur(
            repository = martelage,
            reglages = ReglagesRepository(context),
            referentiels = referentiels,
            sauvegarde = sauvegarde,
            gpkg = GpkgRepository(context),
            lot = LotRepository(context, sauvegarde, martelage),
        )
    }
}
