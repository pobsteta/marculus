package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.AxeClasses
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige
import fr.nemeton.terrain.geo.Position
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Mission 019 : le CSV foncier compte les tiges nettes. Contrat imposé au dev, dans `:core`
 * (fichier `CsvFoncier.kt`, package `fr.marculus.core`), repris de `csvFoncier` de
 * `StatutHistoriqueScreen.kt` avec les parcelles abstraites :
 *
 *     data class ParcelleFonciere(
 *         val id: Long, val proprietaire: String?, val foret: String?, val commune: String?,
 *         val parcelleNom: String?, val surfaceHa: Double,
 *     )
 *     object CsvFoncier {
 *         fun construire(
 *             contexte: Contexte, journal: List<Tige>,
 *             parcelleDe: (Tige) -> ParcelleFonciere?, locale: Locale,
 *         ): String
 *     }
 *
 * Colonnes inchangées : Proprietaire;Foret;Commune;Parcelle;Surface_ha;Essence;Classe;Nombre;
 * Tiges_ha;G_m2;G_ha;Volume_m3;Volume_ha;Houppier_m3;Total_m3. Comme aujourd'hui, seules les tiges
 * avec position figurent au CSV (les tiges sans position n'y sont pas).
 */
class CsvFoncierTest {
    private val ctx = Contexte(
        id = "c1", nom = "Affouage", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
        essences = listOf(EssenceColonne("Chêne", 0, 0), EssenceColonne("Hêtre", 0, 0)),
        tarif = TarifCubage.SCHAEFFER_RAPIDE, tarifNumero = 8,
    )
    private val pa = ParcelleFonciere(1, "Commune X", "Forêt X", "X", "A", 2.0)
    private val pb = ParcelleFonciere(2, "Commune X", "Forêt X", "X", "B", 4.0)
    private val parcelleDe: (Tige) -> ParcelleFonciere? = {
        when (it.position?.latitude) {
            45.0 -> pa
            46.0 -> pb
            else -> null
        }
    }

    private fun plus(uuid: String, t: Long, lat: Double?, essence: String = "Chêne", classe: Int = 30, q: Int = 1) =
        Tige(
            uuid, "c1", essence, classe, ActionTige.PLUS, horodatage = t, quantite = q,
            position = lat?.let { Position(it, 5.0) },
        )

    private fun moins(uuid: String, t: Long, essence: String = "Chêne", classe: Int = 30) =
        Tige(uuid, "c1", essence, classe, ActionTige.ANNULATION, horodatage = t)

    private fun csv(j: List<Tige>) = CsvFoncier.construire(ctx, j, parcelleDe, Locale.ROOT)

    /** (Parcelle, Essence, Classe, Nombre) de chaque ligne de données. */
    private fun lignes(csv: String) = csv.trim().lines().drop(1).filter { it.isNotBlank() }.map {
        val c = it.split(';')
        listOf(c[3], c[5], c[6], c[7])
    }

    @Test
    fun `l en-tete ne change pas`() {
        assertEquals(
            "Proprietaire;Foret;Commune;Parcelle;Surface_ha;Essence;Classe;Nombre;Tiges_ha;G_m2;G_ha;Volume_m3;Volume_ha;Houppier_m3;Total_m3",
            csv(emptyList()).lines().first(),
        )
    }

    @Test
    fun `la colonne Nombre suit le net - la derniere tige de la case est retiree`() {
        val j = listOf(plus("a", 1, 45.0), plus("b", 2, 46.0), moins("x", 3))
        assertEquals(listOf(listOf("A", "Chêne", "30", "1")), lignes(csv(j)))
    }

    @Test
    fun `paquet de 3 annule de 1 donne Nombre 2`() {
        val j = listOf(plus("a", 1, 45.0, q = 3), moins("x", 2))
        assertEquals(listOf(listOf("A", "Chêne", "30", "2")), lignes(csv(j)))
    }

    @Test
    fun `une annulation de chene 30 ne retire rien au hetre 30`() {
        val j = listOf(plus("h", 1, 45.0, "Hêtre"), plus("c", 2, 46.0), moins("x", 3))
        assertEquals(listOf(listOf("A", "Hêtre", "30", "1")), lignes(csv(j)))
    }

    @Test
    fun `tout annule - plus aucune ligne`() {
        assertEquals(emptyList(), lignes(csv(listOf(plus("a", 1, 45.0), moins("x", 2)))))
    }

    @Test
    fun `la somme de Nombre egale le net de Statut pour les tiges positionnees`() {
        // Chêne 30 : pile a(3) b c ; x retire c, y retire b ; Hêtre 40 : d retiré par z.
        val j = listOf(
            plus("a", 1, 45.0, q = 3), plus("b", 2, 46.0), plus("c", 3, 10.0), plus("d", 4, 45.0, "Hêtre", 40),
            moins("x", 5), moins("y", 6), moins("z", 7, "Hêtre", 40),
        )
        val somme = lignes(csv(j)).sumOf { it[3].toInt() }
        assertEquals(TotauxMartelage(j).totalContexte(), somme)
        assertEquals(3, somme)
    }
}
