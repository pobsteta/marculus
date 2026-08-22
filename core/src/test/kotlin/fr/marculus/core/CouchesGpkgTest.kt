package fr.marculus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CouchesGpkgTest {

    @Test
    fun `le nom de table donne le role`() {
        assertEquals(TypeCouche.HOUPPIER, CouchesGpkg.type("houppier"))
        assertEquals(TypeCouche.HOUPPIER, CouchesGpkg.type("Houppiers"))
        assertEquals(TypeCouche.DESSERTE, CouchesGpkg.type("desserte"))
        assertEquals(TypeCouche.DESSERTE, CouchesGpkg.type(" Pistes "))
    }

    @Test
    fun `toute table inconnue reste une parcelle`() {
        // Comportement historique préservé : un GPKG de parcelles seules se lit comme avant.
        assertEquals(TypeCouche.PARCELLE, CouchesGpkg.type("parcelles"))
        assertEquals(TypeCouche.PARCELLE, CouchesGpkg.type("ufs_2026"))
        assertEquals(TypeCouche.PARCELLE, CouchesGpkg.type(""))
    }

    @Test
    fun `attribut trouve par alias, le nom canonique en premier`() {
        val attrs = mapOf("hauteur" to "18", "h_max" to "27.4")
        assertEquals("27.4", CouchesGpkg.attribut(attrs, CouchesGpkg.ALIAS_HAUTEUR))
        assertEquals("18", CouchesGpkg.attribut(mapOf("Hauteur" to "18"), CouchesGpkg.ALIAS_HAUTEUR))
    }

    @Test
    fun `attribut vide ou absent vaut null`() {
        assertNull(CouchesGpkg.attribut(mapOf("h_max" to "  "), CouchesGpkg.ALIAS_HAUTEUR))
        assertNull(CouchesGpkg.attribut(emptyMap(), CouchesGpkg.ALIAS_HAUTEUR))
    }
}
