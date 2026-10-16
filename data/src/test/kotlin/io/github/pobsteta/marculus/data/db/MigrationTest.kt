package io.github.pobsteta.marculus.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Migrations Room à partir des schémas exportés (`data/schemas/`), sous Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MarculusDatabase::class.java)

    @Test
    fun `v16 vers v17 - le contexte garde tout, commune SER et millesime vides`() {
        helper.createDatabase(BASE, 16).use { db ->
            db.execSQL(
                "INSERT INTO contexte (id, nom, mode, classeMin, classeMax, classePas, essences, commentaire, " +
                    "increment, exporte, dateCreation, operateur, cheminGpkg, tarif, tarifNumero, coefficientForme, " +
                    "dateMartelage, statut, modifie, dejaExporte, affouage, volumeMaxLotM3, partHouppierAffouage, " +
                    "tarifsEssence, steresParM3) VALUES ('c1', 'P12', 'DIAMETRE', 20, 90, 5, '', NULL, 1, 0, 0, NULL, " +
                    "NULL, 'SCHAEFFER_LENT', 9, 0.5, NULL, 'PROPOSEE', 7, 0, 1, 12.5, '', '', 1.4)",
            )
        }
        helper.runMigrationsAndValidate(BASE, 17, true, MIGRATION_16_17).use { db ->
            db.query("SELECT nom, tarif, tarifNumero, volumeMaxLotM3, steresParM3, communeInsee, ser, millesimeTarifs FROM contexte").use { c ->
                c.moveToFirst()
                assertEquals("P12", c.getString(0))
                assertEquals("SCHAEFFER_LENT", c.getString(1))
                assertEquals(9, c.getInt(2))
                assertEquals(12.5, c.getDouble(3))
                assertEquals(1.4, c.getDouble(4))
                assertNull(c.getString(5))
                assertNull(c.getString(6))
                assertNull(c.getString(7))
            }
        }
    }

    @Test
    fun `la base s ouvre en v17 avec toutes les migrations declarees`() {
        helper.createDatabase(BASE, 16).close()
        val db = androidx.room.Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(), MarculusDatabase::class.java, BASE,
        ).addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
            .allowMainThreadQueries().build()
        db.openHelper.writableDatabase.version.let { assertEquals(17, it) }
        db.close()
    }

    private companion object {
        const val BASE = "migration-test.db"
    }
}
