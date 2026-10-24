package io.github.pobsteta.marculus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import fr.marculus.core.model.ChoixTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Reprise de l'ancien réglage « Thème sombre » (clé `theme_sombre`, mission 008) :
 * lue si `theme` est absent, `theme` prime, et `theme_sombre` n'est jamais réécrite.
 *
 * Le DataStore est un singleton de processus (délégué de fichier) : on l'atteint par réflexion
 * pour le vider et y semer les anciennes clés avant chaque cas.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReglagesThemeMigrationTest {

    private val cleAncienne = booleanPreferencesKey("theme_sombre")
    private val cleTheme = stringPreferencesKey("theme")

    private lateinit var context: Context
    private lateinit var ds: DataStore<Preferences>
    private lateinit var repo: ReglagesRepository

    @Before
    fun preparer() {
        context = ApplicationProvider.getApplicationContext()
        val getter = Class.forName("io.github.pobsteta.marculus.data.ReglagesRepositoryKt")
            .getDeclaredMethod("getDataStore", Context::class.java)
        getter.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        ds = getter.invoke(null, context) as DataStore<Preferences>
        runBlocking { ds.edit { it.clear() } }
        repo = ReglagesRepository(context)
    }

    private fun semer(ancienne: Boolean? = null, theme: String? = null) = runBlocking {
        ds.edit { p ->
            ancienne?.let { p[cleAncienne] = it }
            theme?.let { p[cleTheme] = it }
        }
    }

    private fun themeLu() = runBlocking { repo.reglages.first().theme }

    @Test
    fun `theme_sombre coche seul donne SOMBRE`() {
        semer(ancienne = true)
        assertEquals(ChoixTheme.SOMBRE, themeLu())
    }

    @Test
    fun `theme_sombre decoche seul donne SYSTEME`() {
        semer(ancienne = false)
        assertEquals(ChoixTheme.SYSTEME, themeLu())
    }

    @Test
    fun `aucune cle donne SYSTEME`() {
        assertEquals(ChoixTheme.SYSTEME, themeLu())
    }

    @Test
    fun `la cle theme prime sur theme_sombre`() {
        semer(ancienne = true, theme = ChoixTheme.CLAIR.name)
        assertEquals(ChoixTheme.CLAIR, themeLu())
    }

    @Test
    fun `theme inconnu retombe sur theme_sombre`() {
        semer(ancienne = true, theme = "VIOLET")
        assertEquals(ChoixTheme.SOMBRE, themeLu())
    }

    @Test
    fun `enregistrer ne reecrit pas theme_sombre coche`() = runBlocking {
        semer(ancienne = true)
        repo.enregistrer(repo.reglages.first().copy(theme = ChoixTheme.CLAIR))
        val p = ds.data.first()
        assertEquals(true, p[cleAncienne])
        assertEquals("CLAIR", p[cleTheme])
        assertEquals(ChoixTheme.CLAIR, repo.reglages.first().theme)
    }

    @Test
    fun `enregistrer ne reecrit pas theme_sombre decoche`() = runBlocking {
        semer(ancienne = false)
        repo.enregistrer(repo.reglages.first().copy(theme = ChoixTheme.SOMBRE))
        assertEquals(false, ds.data.first()[cleAncienne])
    }

    @Test
    fun `enregistrer n'ajoute pas theme_sombre quand elle etait absente`() = runBlocking {
        repo.enregistrer(repo.reglages.first().copy(theme = ChoixTheme.SOMBRE))
        val p = ds.data.first()
        assertNull(p[cleAncienne])
        assertFalse(p.contains(cleAncienne))
        assertTrue(p.contains(cleTheme))
    }
}
