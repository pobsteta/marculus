package io.github.pobsteta.marculus.data

import io.github.pobsteta.marculus.data.db.ContexteEntity
import io.github.pobsteta.marculus.data.db.TigeEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ReglagesTerrainTest {

    private fun contexte(
        affouage: Boolean = false,
        volumeMax: Double = 0.0,
        tarif: String = "AUCUN",
        numero: Int = 0,
        coef: Double = 0.5,
        gpkg: String? = null,
        nom: String = "P12",
        modifie: Long = 1,
    ) = ContexteEntity(
        id = "c1", nom = nom, mode = "DIAMETRE", classeMin = 20, classeMax = 90, classePas = 5,
        essences = "", commentaire = null, increment = 1, exporte = false, dateCreation = 0,
        operateur = null, cheminGpkg = gpkg, tarif = tarif, tarifNumero = numero, coefficientForme = coef,
        modifie = modifie, affouage = affouage, volumeMaxLotM3 = volumeMax,
    )

    private val local = contexte(true, 12.5, "SCHAEFFER_RAPIDE", 8, 0.45, "/data/p12.gpkg")

    @Test
    fun `contexte reemis par Nemeton sans reglages - le terrain est garde, le reste mis a jour`() {
        val entrant = contexte(nom = "P12 bis", modifie = 2)
        val r = ReglagesTerrain.completer(entrant, local, emptySet())
        assertEquals(contexte(true, 12.5, "SCHAEFFER_RAPIDE", 8, 0.45, "/data/p12.gpkg", "P12 bis", 2), r)
    }

    @Test
    fun `une cle presente l'emporte, meme pour revenir au defaut`() {
        val entrant = contexte(affouage = false, volumeMax = 0.0, modifie = 2)
        val r = ReglagesTerrain.completer(entrant, local, setOf("affouage", "volumeMaxLotM3"))
        assertEquals(false, r.affouage)
        assertEquals(0.0, r.volumeMaxLotM3)
        assertEquals("SCHAEFFER_RAPIDE", r.tarif) // absent : gardé
    }

    @Test
    fun `contexte nouveau - les defauts s'appliquent`() {
        val entrant = contexte(modifie = 2)
        assertSame(entrant, ReglagesTerrain.completer(entrant, null, emptySet()))
    }

    private fun tige(lot: Int?, modifie: Long = 1) = TigeEntity(
        uuid = "t1", contexteId = "c1", essence = "Chêne", classe = 40, action = "PLUS", horodatage = 1,
        quantite = 1, hauteurTexte = null, qualiteArbre = null, latitude = null, longitude = null,
        operateur = null, modifie = modifie, lot = lot,
    )

    @Test
    fun `tige repartagee sans lot - le lot local est garde`() {
        assertEquals(3, ReglagesTerrain.completer(tige(null, 2), tige(3), emptySet()).lot)
        assertEquals(null, ReglagesTerrain.completer(tige(null, 2), tige(3), setOf("lot")).lot)
        assertEquals(null, ReglagesTerrain.completer(tige(null, 2), null, emptySet()).lot)
    }
}
