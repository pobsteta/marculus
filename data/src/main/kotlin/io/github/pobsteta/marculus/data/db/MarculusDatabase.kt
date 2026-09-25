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

@Database(
    entities = [ContexteEntity::class, TigeEntity::class, CompteurConfigEntity::class],
    version = 13,
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
                .addMigrations(MIGRATION_12_13)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
