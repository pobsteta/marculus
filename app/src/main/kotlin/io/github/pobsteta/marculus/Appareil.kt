package io.github.pobsteta.marculus

import android.content.Context
import java.util.UUID

/**
 * Identifiant stable de l'appareil, généré une seule fois et conservé. Sert d'identité
 * d'opérateur par défaut (unicité garantie) quand aucun nom n'est saisi.
 */
object Appareil {
    private const val PREFS = "appareil"
    private const val CLE = "id"

    fun id(ctx: Context): String {
        val prefs = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(CLE, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(CLE, it).apply()
        }
    }
}
