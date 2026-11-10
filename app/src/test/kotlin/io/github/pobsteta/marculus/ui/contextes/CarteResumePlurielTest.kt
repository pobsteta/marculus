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
@Config(sdk = [35])
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

    /** La phrase complète comme la carte la compose : un retour à %5$d / %6$d ferait planter ici. */
    private fun phrase(langue: String, essences: Int, tiges: Int, verrouille: Boolean, martele: Boolean): String {
        val r = ressources(langue)
        return r.getString(
            R.string.liste_carte_resume, "Diam", 20, 60, 5,
            r.getQuantityString(R.plurals.liste_carte_essences, essences, essences),
            r.getQuantityString(R.plurals.liste_carte_tiges, tiges, tiges),
        ) + (if (verrouille) r.getString(R.string.liste_carte_resume_verrouille) else "") +
            (if (martele) r.getString(R.string.liste_carte_resume_martele) else "")
    }

    @Test fun `phrase complete francais`() {
        assertEquals("Diam · classes 20–60 (pas 5) · 1 essence · 1 tige", phrase("fr", 1, 1, false, false))
        assertEquals("Diam · classes 20–60 (pas 5) · 2 essences · 0 tige", phrase("fr", 2, 0, false, false))
        assertEquals("Diam · classes 20–60 (pas 5) · 2 essences · 2 tiges · 🔒", phrase("fr", 2, 2, true, false))
        assertEquals("Diam · classes 20–60 (pas 5) · 1 essence · 1 tige · ✓ martelé", phrase("fr", 1, 1, false, true))
        assertEquals("Diam · classes 20–60 (pas 5) · 1 essence · 2 tiges · 🔒 · ✓ martelé", phrase("fr", 1, 2, true, true))
    }

    @Test fun `phrase complete anglais`() {
        assertEquals("Diam · classes 20–60 (step 5) · 1 species · 1 stem", phrase("en", 1, 1, false, false))
        assertEquals("Diam · classes 20–60 (step 5) · 2 species · 2 stems · 🔒", phrase("en", 2, 2, true, false))
        assertEquals("Diam · classes 20–60 (step 5) · 1 species · 0 stems · ✓ marked", phrase("en", 1, 0, false, true))
        assertEquals("Diam · classes 20–60 (step 5) · 2 species · 1 stem · 🔒 · ✓ marked", phrase("en", 2, 1, true, true))
    }
}
