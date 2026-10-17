package io.github.pobsteta.marculus.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import fr.nemeton.terrain.gpkg.BaseSqlite
import java.io.Closeable

/**
 * [BaseSqlite] de terrain-gpkg sur le SQLite d'Android. Les paramètres des requêtes de lecture
 * passent en texte (`rawQuery`), ceux des écritures gardent leur type (`execSQL`).
 */
class SqliteAndroid private constructor(private val db: SQLiteDatabase) : BaseSqlite, Closeable {

    override fun executer(sql: String, args: List<Any?>) {
        if (args.isEmpty()) db.execSQL(sql) else db.execSQL(sql, args.toTypedArray())
    }

    override fun requete(sql: String, args: List<Any?>): List<Map<String, Any?>> =
        db.rawQuery(sql, args.map { it?.toString() }.toTypedArray()).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        (0 until c.columnCount).associate { i ->
                            c.getColumnName(i) to when (c.getType(i)) {
                                Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                                Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                                Cursor.FIELD_TYPE_STRING -> c.getString(i)
                                Cursor.FIELD_TYPE_BLOB -> c.getBlob(i)
                                else -> null
                            }
                        },
                    )
                }
            }
        }

    override fun <T> transactionImmediate(bloc: () -> T): T {
        db.beginTransactionNonExclusive() // BEGIN IMMEDIATE
        try {
            return bloc().also { db.setTransactionSuccessful() }
        } finally {
            db.endTransaction()
        }
    }

    override fun close() = db.close()

    companion object {
        fun lecture(chemin: String) = SqliteAndroid(SQLiteDatabase.openDatabase(chemin, null, SQLiteDatabase.OPEN_READONLY))

        fun ecriture(chemin: String) = SqliteAndroid(SQLiteDatabase.openDatabase(chemin, null, SQLiteDatabase.OPEN_READWRITE))
    }
}
