package fr.marculus.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GrilleWebMercatorTest {
    private val e = GrilleWebMercator.DEMI_ETENDUE_M

    private fun matrice(zoom: Int, cote: Long = 1L shl zoom, tuile: Long = 256) =
        MatriceTuiles(zoom, cote, cote, tuile, tuile)

    /** Pyramide de l'export Nemeton : zooms 13 à 19, GoogleMapsCompatible. */
    private val pyramide = (13..19).map { matrice(it) }

    @Test
    fun `ortho GoogleMapsCompatible reconnue`() {
        assertTrue(GrilleWebMercator.estStandard("EPSG", 3857, -e, -e, e, e, pyramide))
        // Bornes telles que GDAL les écrit, et organisation en minuscules.
        assertTrue(
            GrilleWebMercator.estStandard(
                "epsg", 3857, -20037508.342789244, -20037508.342789244, 20037508.342789244, 20037508.342789244, pyramide,
            ),
        )
    }

    @Test
    fun `autre projection a reprojeter`() {
        assertFalse(GrilleWebMercator.estStandard("EPSG", 2154, -e, -e, e, e, pyramide))
    }

    @Test
    fun `bornes restreintes a l emprise du chantier`() {
        assertFalse(GrilleWebMercator.estStandard("EPSG", 3857, 600000.0, 5800000.0, 610000.0, 5810000.0, pyramide))
    }

    @Test
    fun `tuiles de 512 ou matrice hors grille`() {
        assertFalse(GrilleWebMercator.estStandard("EPSG", 3857, -e, -e, e, e, listOf(matrice(15, tuile = 512))))
        assertFalse(GrilleWebMercator.estStandard("EPSG", 3857, -e, -e, e, e, listOf(matrice(15, cote = 1000))))
    }

    @Test
    fun `aucune tuile rien a servir`() {
        assertFalse(GrilleWebMercator.estStandard("EPSG", 3857, -e, -e, e, e, emptyList()))
    }
}
