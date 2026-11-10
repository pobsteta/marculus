package io.github.pobsteta.marculus.ui.contextes

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import io.github.pobsteta.marculus.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.assertEquals

/** Mission 033 : « 1 tige », « 1 essence » au singulier sur la carte de la liste (le français : 0 et 1). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CarteResumePlurielTest {
    private fun ressources(langue: String) = ApplicationProvider.getApplicationContext<Context>().let {
        val conf = Configuration(it.resources.configuration).apply { setLocale(Locale.forLanguageTag(langue)) }
        it.createConfigurationContext(conf).resources
    }

    private fun tiges(langue: String, n: Int) = ressources(langue).getQuantityString(R.plurals.liste_carte_tiges, n, n)
    private fun essences(langue: String, n: Int) = ressources(langue).getQuantityString(R.plurals.liste_carte_essences, n, n)

    @Test fun `francais tiges`() {
        assertEquals("0 tige", tiges("fr", 0))
        assertEquals("1 tige", tiges("fr", 1))
        assertEquals("2 tiges", tiges("fr", 2))
    }

    @Test fun `francais essences`() {
        assertEquals("0 essence", essences("fr", 0))
        assertEquals("1 essence", essences("fr", 1))
        assertEquals("2 essences", essences("fr", 2))
    }

    @Test fun `anglais stems et species`() {
        assertEquals("0 stems", tiges("en", 0))
        assertEquals("1 stem", tiges("en", 1))
        assertEquals("2 stems", tiges("en", 2))
        assertEquals("1 species", essences("en", 1))
        assertEquals("2 species", essences("en", 2))
    }
}
