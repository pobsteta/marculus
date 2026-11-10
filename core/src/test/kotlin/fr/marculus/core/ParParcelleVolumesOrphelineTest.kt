package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige
import fr.nemeton.terrain.geo.Position
import kotlin.test.Test
import kotlin.test.assertEquals

/** Mission 019 (aller-retour 2) : les volumes de Par parcelle égalent ceux de Statut, annulation orpheline comprise. */
class ParParcelleVolumesOrphelineTest {
    private val ctx = Contexte(
        id = "c1", nom = "Affouage", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
        essences = listOf(EssenceColonne("Chêne", 0, 0), EssenceColonne("Hêtre", 0, 0)),
        tarif = TarifCubage.SCHAEFFER_RAPIDE, tarifNumero = 8,
    )

    @Test
    fun `annulation orpheline - volumes de Par parcelle egaux a VolumesMartelage totaux`() {
        val j = listOf(
            Tige("a", "c1", "Chêne", 30, ActionTige.PLUS, horodatage = 1, position = Position(45.0, 5.0)),
            Tige("d", "c1", "Chêne", 35, ActionTige.PLUS, horodatage = 2),
            Tige("x", "c1", "Hêtre", 40, ActionTige.ANNULATION, horodatage = 3),
        )
        val r = ParParcelle.repartir(j) { "A" }
        val sans = r.volumesSansPosition(ctx)
        val geo = r.parcelles.values.flatten().sumOf {
            VolumesMartelage.cubage(ctx, it).let { c -> c.volumeTotalM3 * it.quantite }
        }
        val geoTige = r.parcelles.values.flatten().sumOf { VolumesMartelage.cubage(ctx, it).volumeTigeM3 * it.quantite }
        val geoHoup = r.parcelles.values.flatten().sumOf { VolumesMartelage.cubage(ctx, it).volumeHouppierM3 * it.quantite }
        val attendu = VolumesMartelage.totaux(ctx, j)
        assertEquals(attendu.volumeTotalM3, geo + sans.volumeTotalM3, 1e-9)
        assertEquals(attendu.volumeTigeM3, geoTige + sans.volumeTigeM3, 1e-9)
        assertEquals(attendu.volumeHouppierM3, geoHoup + sans.volumeHouppierM3, 1e-9)
        // Le volume de l'annulation orpheline est bien retiré (sans lui, l'écart serait non nul).
        val sansOrpheline = VolumesMartelage.cubage(ctx, j[1]).volumeTotalM3
        assertEquals(true, sans.volumeTotalM3 < sansOrpheline - 1e-9)
    }

    @Test
    fun `ligne sans position - seulement avec tige sans position ou annulation orpheline`() {
        val avecPos = Tige("a", "c1", "Chêne", 30, ActionTige.PLUS, horodatage = 1, position = Position(45.0, 5.0))
        assertEquals(false, ParParcelle.repartir(listOf(avecPos)) { "A" }.aLigneSansPosition)
        val sans = Tige("d", "c1", "Chêne", 35, ActionTige.PLUS, horodatage = 2)
        assertEquals(true, ParParcelle.repartir(listOf(avecPos, sans)) { "A" }.aLigneSansPosition)
        val orpheline = Tige("x", "c1", "Hêtre", 40, ActionTige.ANNULATION, horodatage = 3)
        val r = ParParcelle.repartir(listOf(avecPos, orpheline)) { "A" }
        assertEquals(true, r.aLigneSansPosition)
        assertEquals(-1, r.sansPosition)
    }
}
