package io.github.pobsteta.marculus.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v13 : `dejaExporte`, amorcé avec l'état exporté courant (le passé récent est connu, pas l'ancien). */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE contexte ADD COLUMN dejaExporte INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE contexte SET dejaExporte = exporte")
    }
}

/** v14 : contexte d'affouage (case + volume maximal d'un lot) et lot figé sur la tige. */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE contexte ADD COLUMN affouage INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE contexte ADD COLUMN volumeMaxLotM3 REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tige ADD COLUMN lot INTEGER")
    }
}

/** v15 : part de houppier par essence (tarifs sans houppier) ; colonne nommée à sa création, pour l'affouage. */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE contexte ADD COLUMN partHouppierAffouage TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [ContexteEntity::class, TigeEntity::class, CompteurConfigEntity::class],
    version = 15,
    exportSchema = false,
)
abstract class MarculusDatabase : RoomDatabase() {
    abstract fun contexteDao(): ContexteDao
    abstract fun tigeDao(): TigeDao
    abstract fun compteurConfigDao(): CompteurConfigDao
    abstract fun mergeDao(): MergeDao

    companion object {
        fun creer(context: Context): MarculusDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MarculusDatabase::class.java,
                "marculus.db",
            )
                // Les données de terrain sont réelles depuis la v13 : chaque changement de schéma
                // porte sa migration. Le repli destructif ne reste que pour les versions de dev.
                .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
