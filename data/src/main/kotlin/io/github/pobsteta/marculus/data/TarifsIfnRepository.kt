package io.github.pobsteta.marculus.data

import android.content.Context
import fr.marculus.core.TarifsIfn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * Référentiel IFN des numéros de tarif Schaeffer par sylvoécorégion, embarqué dans les assets
 * (`tarifs-ifn/`, tables produites par nemeton). Lu une seule fois, au premier besoin. Null si
 * les tables ne sont pas embarquées : l'écran n'affiche alors ni commune ni proposition.
 */
class TarifsIfnRepository(private val context: Context) {
    private val verrou = Mutex()
    private var lu = false
    private var referentiel: TarifsIfn? = null

    suspend fun referentiel(): TarifsIfn? = verrou.withLock {
        if (!lu) {
            referentiel = withContext(Dispatchers.IO) { lire() }
            lu = true
        }
        referentiel
    }

    private fun lire(): TarifsIfn? = try {
        fun texte(nom: String) = context.assets.open("$DOSSIER/$nom").bufferedReader().use { it.readText() }
        TarifsIfn.charger(texte("tarifs_ser.csv"), texte("communes_ser.csv"), texte("ser.csv"))
    } catch (_: FileNotFoundException) {
        null
    }

    private companion object {
        const val DOSSIER = "tarifs-ifn"
    }
}
