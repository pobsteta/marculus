package io.github.pobsteta.marculus.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import fr.marculus.core.model.PartsLot
import io.github.pobsteta.marculus.data.versDomaine
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Mission 026, règle 2 : un contexte existant (base v17) passe en v18 avec « les deux cochées »
 * pour toutes ses essences. Noms proposés : `MIGRATION_17_18` (migration), colonne `partsLot` de
 * `contexte` (texte, comme `partHouppierAffouage`), `Contexte.partsLot: Map<String, PartsLot>`.
 * Une essence absente de `partsLot` vaut `PartsLot()` (tige et houppier cochés).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationPartsLotTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MarculusDatabase::class.java)

    private fun contexteV17(db: androidx.sqlite.db.SupportSQLiteDatabase) = db.execSQL(
        "INSERT INTO contexte (id, nom, mode, classeMin, classeMax, classePas, essences, commentaire, " +
            "increment, exporte, dateCreation, operateur, cheminGpkg, tarif, tarifNumero, coefficientForme, " +
            "dateMartelage, statut, modifie, dejaExporte, affouage, volumeMaxLotM3, partHouppierAffouage, " +
            "tarifsEssence, steresParM3) VALUES ('c1', 'Affouage', 'DIAMETRE', 20, 90, 5, '', NULL, 1, 0, 0, NULL, " +
            "NULL, 'SCHAEFFER_LENT', 9, 0.5, NULL, 'PROPOSEE', 7, 0, 1, 14.0, '', '', 1.5)",
    )

    @Test
    fun `v17 vers v18 - le contexte garde ses reglages, la colonne partsLot existe`() {
        helper.createDatabase(BASE, 17).use { contexteV17(it) }
        helper.runMigrationsAndValidate(BASE, 18, true, MIGRATION_17_18).use { db ->
            db.query("SELECT nom, tarif, tarifNumero, affouage, volumeMaxLotM3, partsLot FROM contexte").use { c ->
                c.moveToFirst()
                assertEquals("Affouage", c.getString(0))
                assertEquals("SCHAEFFER_LENT", c.getString(1))
                assertEquals(9, c.getInt(2))
                assertEquals(1, c.getInt(3))
                assertEquals(14.0, c.getDouble(4))
            }
        }
    }

    @Test
    fun `contexte existant apres migration - toute essence a les deux cases cochees`() = runBlocking {
        helper.createDatabase(BASE, 17).use { contexteV17(it) }
        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), MarculusDatabase::class.java, BASE)
            .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18)
            .allowMainThreadQueries().build()
        try {
            assertEquals(18, db.openHelper.writableDatabase.version)
            val c = db.contexteDao().parId("c1")!!.versDomaine()
            assertEquals(14.0, c.volumeMaxLotM3)
            for (essence in listOf("Chêne", "Hêtre", "CHE")) {
                assertEquals(PartsLot(tige = true, houppier = true), c.partsLot[essence] ?: PartsLot(), essence)
            }
            // aucune essence n'est décochée par la migration
            assertEquals(true, c.partsLot.values.all { it.tige && it.houppier })
        } finally {
            db.close()
        }
    }

    private companion object {
        const val BASE = "migration-parts-lot-test.db"
    }
}
