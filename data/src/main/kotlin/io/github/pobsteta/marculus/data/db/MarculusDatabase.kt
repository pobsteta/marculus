package io.github.pobsteta.marculus.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ContexteEntity::class, TigeEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MarculusDatabase : RoomDatabase() {
    abstract fun contexteDao(): ContexteDao
    abstract fun tigeDao(): TigeDao

    companion object {
        fun creer(context: Context): MarculusDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MarculusDatabase::class.java,
                "marculus.db",
            ).build()
    }
}
