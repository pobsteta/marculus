package io.github.pobsteta.marculus

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Choix de langue de l'application : « system » (langue de l'appareil), « fr » ou « en ».
 * Stocké en SharedPreferences (lecture synchrone nécessaire dans attachBaseContext) et
 * appliqué en forçant la locale du contexte de base de l'Activity.
 */
object Langue {
    private const val PREFS = "langue"
    private const val CLE = "code"

    fun code(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLE, "system") ?: "system"

    fun definir(ctx: Context, code: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CLE, code).apply()

    /** Contexte avec la locale forcée selon le choix (ou tel quel si « system »). */
    fun appliquer(base: Context): Context {
        val code = code(base)
        if (code == "system") return base
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
