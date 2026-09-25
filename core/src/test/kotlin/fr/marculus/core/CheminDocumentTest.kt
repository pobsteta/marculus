package fr.marculus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CheminDocumentTest {
    private val racine = "/storage/emulated/0"

    @Test
    fun `stockage interne`() {
        assertEquals(
            "/storage/emulated/0/Download/Marculus/parcelle 1122.csv",
            CheminDocument.depuisIdentifiant(CheminDocument.STOCKAGE_EXTERNE, "primary:Download/Marculus/parcelle 1122.csv", racine),
        )
    }

    @Test
    fun `carte SD`() {
        assertEquals(
            "/storage/1A2B-3C4D/Martelage/x.csv",
            CheminDocument.depuisIdentifiant(CheminDocument.STOCKAGE_EXTERNE, "1A2B-3C4D:Martelage/x.csv", racine),
        )
    }

    @Test
    fun `telechargements`() {
        assertEquals(
            "/storage/emulated/0/Download/x.csv",
            CheminDocument.depuisIdentifiant(CheminDocument.TELECHARGEMENTS, "raw:/storage/emulated/0/Download/x.csv", racine),
        )
        assertNull(CheminDocument.depuisIdentifiant(CheminDocument.TELECHARGEMENTS, "msf:123", racine))
        assertEquals(123L, CheminDocument.idMediaStore(CheminDocument.TELECHARGEMENTS, "msf:123"))
        assertNull(CheminDocument.idMediaStore(CheminDocument.TELECHARGEMENTS, "raw:/x"))
    }

    @Test
    fun `autre fournisseur ou identifiant incomplet`() {
        assertNull(CheminDocument.depuisIdentifiant("com.google.android.apps.docs.storage", "doc=123", racine))
        assertNull(CheminDocument.depuisIdentifiant(CheminDocument.STOCKAGE_EXTERNE, "primary:", racine))
        assertNull(CheminDocument.depuisIdentifiant(null, null, racine))
    }
}
