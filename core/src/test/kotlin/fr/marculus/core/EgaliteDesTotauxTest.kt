package fr.marculus.core

import fr.marculus.core.export.ExportBilanLots
import fr.marculus.core.export.ExportCsv
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
import kotlin.test.assertTrue

/**
 * Mission 025 : pour un même journal, tous les endroits de `:core` qui comptent ou exportent des
 * tiges donnent le même nombre net et les mêmes volumes (tige, houppier, total). Un seul jeu de
 * journaux ([CAS]), passé à chaque endroit ; le nombre net attendu de chaque journal est calculé
 * **à la main** dans [Cas] (pas par le code testé). Les endroits de `:data` (liste des contextes,
 * boîte de fusion) sont dans `EgaliteDesTotauxDataTest` (même jeu, recopié, mêmes nombres).
 *
 * Exceptions décidées (et seules) :
 *  1. l'annulation orpheline (sans tige à retirer) n'apparaît pas au CSV foncier : décision de
 *     Pascal du 2026-11-09, `pilotage/journal.md` l. 166 (« n'apparaît pas au CSV foncier ») ;
 *  2. le lot courant d'affouage ne redescend pas quand une tige de ce lot est annulée
 *     (`AffouageLots.etat`, mission 022 ; doc de `AffouageLots.etat`).
 */
class EgaliteDesTotauxTest {

    // ---------- contextes ----------
    private fun ctx(tarif: TarifCubage, numero: Int, affouage: Boolean = false) = Contexte(
        id = "c1", nom = "Essai", mode = ModeMesure.DIAMETRE, axe = AxeClasses(20, 80, 5),
        essences = listOf(EssenceColonne("Chêne", 0, 0), EssenceColonne("Hêtre", 0, 0)),
        tarif = tarif, tarifNumero = numero,
        partHouppier = if (tarif == TarifCubage.EMERGE) emptyMap() else mapOf("Chêne" to 0.4, "Hêtre" to 0.25),
        affouage = affouage, volumeMaxLotM3 = if (affouage) 2.0 else 0.0,
    )

    private val schaeffer = ctx(TarifCubage.SCHAEFFER_RAPIDE, 8)
    private val affouage = ctx(TarifCubage.SCHAEFFER_LENT, 6, affouage = true)
    private val emerge = ctx(TarifCubage.EMERGE, 0)
    private val contextes = listOf(schaeffer, affouage, emerge)

    // ---------- journaux ----------
    private fun plus(
        u: String, t: Long, e: String = "Chêne", c: Int = 30, q: Int = 1,
        lat: Double? = null, lot: Int? = null, h: String? = "24",
    ) = Tige(
        u, "c1", e, c, ActionTige.PLUS, horodatage = t, quantite = q, hauteurTexte = h,
        position = lat?.let { Position(it, 5.0) }, lot = lot,
    )

    private fun moins(u: String, t: Long, e: String = "Chêne", c: Int = 30, q: Int = 1) =
        Tige(u, "c1", e, c, ActionTige.ANNULATION, horodatage = t, quantite = q)

    /**
     * Un journal et ce qu'il doit donner, calculé à la main (voir les commentaires).
     * [net] : nombre net de Statut (PLUS moins ANNULATION, une orpheline compte en négatif).
     * [parcelles] : tiges nettes positionnées par parcelle (A : lat 45, B : lat 46, null : hors parcelle).
     * [sansPosition] : net « sans position » de Par parcelle (orphelines en négatif).
     * [lots] : tiges PLUS nettes par lot (null : sans lot) — Par lot ne porte que des PLUS.
     * [orphelines] : quantité totale des annulations sans tige à retirer.
     */
    class Cas(
        val nom: String, val journal: List<Tige>, val net: Int,
        val parcelles: Map<String?, Int>, val sansPosition: Int, val lots: Map<Int?, Int>,
        val orphelines: Int = 0,
    ) {
        /** Tiges PLUS nettes avec position (celles du CSV foncier). */
        val positionnees: Int get() = parcelles.values.sum()
        override fun toString() = nom
    }

    private val CAS: List<Cas> = listOf(
        Cas("vide", emptyList(), 0, emptyMap(), 0, emptyMap()),
        // a(t1) puis annulation x(t2) : tout annulé.
        Cas("tout annulé", listOf(plus("a", 1, lat = 45.0, lot = 1), moins("x", 2)), 0, emptyMap(), 0, emptyMap()),
        // Chronologie : a(1) c(2, sans position) b(3, q2) x(4) ; x retire 1 de b → 1+1+1 = 3.
        Cas(
            "désordre",
            listOf(
                plus("b", 3, q = 2, lat = 46.0, lot = 2), moins("x", 4),
                plus("c", 2, lot = 1), plus("a", 1, lat = 45.0, lot = 1),
            ),
            3, mapOf("A" to 1, "B" to 1), 1, mapOf(1 to 2, 2 to 1),
        ),
        // Hêtre 40 : pile a(5) b(3) ; x(4) retire b(3) et 1 de a ; y(2) retire 2 de a → reste a(2).
        Cas(
            "paquets partiellement annulés",
            listOf(
                plus("a", 1, "Hêtre", 40, 5, 45.0, 1), plus("b", 2, "Hêtre", 40, 3, 46.0, 1),
                moins("x", 3, "Hêtre", 40, 4), moins("y", 4, "Hêtre", 40, 2),
            ),
            2, mapOf("A" to 2), 0, mapOf(1 to 2),
        ),
        // Chêne 30 : a(2) puis x(5) retire 2, reste 3 orphelin ; Hêtre 40 : y(1) orphelin ; z(3) intact.
        // Net : 3 - (3 + 1) = -1 ; sans position : -4 ; PLUS net : z(3) seul.
        Cas(
            "annulations orphelines",
            listOf(
                plus("a", 1, q = 2, lat = 45.0, lot = 1), moins("x", 2, q = 5), moins("y", 3, "Hêtre", 40),
                plus("z", 4, "Chêne", 45, 3, 46.0, 1),
            ),
            -1, mapOf("B" to 3), -4, mapOf(1 to 3), orphelines = 4,
        ),
        // Chêne 30 : a(2) w retire 1 → a(1) ; Hêtre 30 : b(3) e(1) m(2) retire e puis 1 de b → b(2) ;
        // Chêne 50 : c(1) ; Hêtre 50 : d(4) n(2) → d(2) ; e (lot 3) annulée : le lot 3 disparaît du bilan.
        // Total 1+2+1+2 = 6 ; a, c en A ; b en B ; d hors parcelle (lat 10).
        Cas(
            "plusieurs cases et lots",
            listOf(
                plus("a", 1, "Chêne", 30, 2, 45.0, 1), plus("b", 2, "Hêtre", 30, 3, 46.0, 1),
                plus("c", 3, "Chêne", 50, 1, 45.0, 2), plus("d", 4, "Hêtre", 50, 4, 10.0, 2),
                moins("w", 5, "Chêne", 30), moins("n", 6, "Hêtre", 50, 2),
                plus("e", 7, "Hêtre", 30, 1, null, 3), moins("m", 8, "Hêtre", 30, 2),
            ),
            6, mapOf("A" to 2, "B" to 2, null to 2), 0, mapOf(1 to 3, 2 to 3),
        ),
        // Chêne 30 : a(2, sans position) x(1) → a(1) ; Hêtre 40 : b(1, sans position, sans lot), c(3, hors parcelle).
        Cas(
            "tiges sans position",
            listOf(
                plus("a", 1, q = 2, lot = 1), plus("b", 2, "Hêtre", 40, 1, null, null),
                plus("c", 3, "Hêtre", 40, 3, 10.0, 2), moins("x", 4),
            ),
            5, mapOf(null to 3), 2, mapOf(1 to 1, null to 1, 2 to 3),
        ),
        // Même horodatage : l'ordre est celui des uuid : a, m, z → m retire a (lat 46), reste z (lat 45).
        Cas(
            "égalité d'horodatage",
            listOf(plus("z", 1, lat = 45.0, lot = 1), moins("m", 1), plus("a", 1, lat = 46.0, lot = 2)),
            1, mapOf("A" to 1), 0, mapOf(1 to 1),
        ),
    )

    private val parcelleDe: (Tige) -> String? = { t ->
        when (t.position?.latitude) { 45.0 -> "A"; 46.0 -> "B"; else -> null }
    }
    private val fonciere = mapOf(
        "A" to ParcelleFonciere(1, "Commune X", "Forêt X", "X", "A", 2.0),
        "B" to ParcelleFonciere(2, "Commune X", "Forêt X", "X", "B", 4.0),
    )
    private val parcelleFonciereDe: (Tige) -> ParcelleFonciere? = { fonciere[parcelleDe(it)] }

    /** Volumes (tige, houppier, total) d'un ensemble de tiges, signés (annulation orpheline < 0). */
    private data class Vol(val tige: Double, val houppier: Double, val total: Double) {
        operator fun plus(o: Vol) = Vol(tige + o.tige, houppier + o.houppier, total + o.total)
        operator fun minus(o: Vol) = Vol(tige - o.tige, houppier - o.houppier, total - o.total)
    }

    private fun vol(c: Contexte, tiges: List<Tige>): Vol {
        var v = Vol(0.0, 0.0, 0.0)
        tiges.forEach {
            val cu = VolumesMartelage.cubage(c, it)
            val n = it.quantite
            v += Vol(cu.volumeTigeM3 * n, cu.volumeHouppierM3 * n, cu.volumeTotalM3 * n)
        }
        return v
    }

    private fun statut(c: Contexte, j: List<Tige>) =
        VolumesMartelage.totaux(c, j).let { Vol(it.volumeTigeM3, it.volumeHouppierM3, it.volumeTotalM3) }

    private fun egal(attendu: Vol, obtenu: Vol, eps: Double, msg: String) {
        assertEquals(attendu.tige, obtenu.tige, eps, "$msg : volume tige")
        assertEquals(attendu.houppier, obtenu.houppier, eps, "$msg : volume houppier")
        assertEquals(attendu.total, obtenu.total, eps, "$msg : volume total")
    }

    private val eps = 1e-9

    private fun chaque(corps: (Cas, Contexte) -> Unit) = CAS.forEach { cas -> contextes.forEach { c -> corps(cas, c) } }

    // ---------- Statut ----------
    @Test
    fun `Statut - le nombre net est celui calcule a la main`() = chaque { cas, _ ->
        assertEquals(cas.net, TotauxMartelage(cas.journal).totalContexte(), "${cas.nom} : totalContexte")
        assertEquals(cas.net, TotauxMartelage(cas.journal).totaux().values.sum(), "${cas.nom} : somme des cases")
        assertEquals(
            cas.net,
            JournalNet.tiges(cas.journal).sumOf { if (it.action == ActionTige.PLUS) it.quantite else -it.quantite },
            "${cas.nom} : JournalNet",
        )
    }

    // ---------- Par parcelle ----------
    @Test
    fun `Par parcelle - nombres par parcelle, sans position et total`() = CAS.forEach { cas ->
        val r = ParParcelle.repartir(cas.journal, parcelleDe)
        assertEquals(cas.parcelles.getOrDefault("A", 0), r.nombre("A"), "${cas.nom} : A")
        assertEquals(cas.parcelles.getOrDefault("B", 0), r.nombre("B"), "${cas.nom} : B")
        assertEquals(cas.parcelles.getOrDefault(null, 0), r.nombre(null), "${cas.nom} : hors parcelle")
        assertEquals(cas.sansPosition, r.sansPosition, "${cas.nom} : sans position")
        assertEquals(cas.net, r.total, "${cas.nom} : total = Statut")
    }

    @Test
    fun `Par parcelle - parcelles plus sans position egalent les volumes de Statut`() = chaque { cas, c ->
        val r = ParParcelle.repartir(cas.journal, parcelleDe)
        val parcelles = vol(c, r.parcelles.values.flatten())
        val sans = r.volumesSansPosition(c).let { Vol(it.volumeTigeM3, it.volumeHouppierM3, it.volumeTotalM3) }
        egal(statut(c, cas.journal), parcelles + sans, eps, "${cas.nom} ${c.tarif}")
    }

    // ---------- CSV foncier ----------
    private data class Foncier(val nombre: Int, val vol: Vol, val lignes: Int)

    private fun foncier(c: Contexte, j: List<Tige>): Foncier {
        val csv = CsvFoncier.construire(c, j, parcelleFonciereDe, Locale.ROOT)
        val lignes = csv.trim().lines().drop(1).filter { it.isNotBlank() }.map { it.split(';') }
        fun d(s: String) = if (s.isEmpty()) 0.0 else s.toDouble()
        return Foncier(
            lignes.sumOf { it[7].toInt() },
            Vol(lignes.sumOf { d(it[11]) }, lignes.sumOf { d(it[13]) }, lignes.sumOf { d(it[14]) }),
            lignes.size,
        )
    }

    @Test
    fun `CSV foncier - le nombre est celui des tiges nettes positionnees`() = chaque { cas, c ->
        assertEquals(cas.positionnees, foncier(c, cas.journal).nombre, "${cas.nom} : foncier")
    }

    @Test
    fun `CSV foncier - exception decidee, l'annulation orpheline n'y figure pas`() {
        // Source : journal l. 166 (Pascal, 2026-11-09). Statut = foncier + sans position (négatif).
        val cas = CAS.single { it.nom == "annulations orphelines" }
        val f = foncier(schaeffer, cas.journal)
        assertEquals(3, f.nombre)
        assertEquals(-1, TotauxMartelage(cas.journal).totalContexte())
        assertEquals(TotauxMartelage(cas.journal).totalContexte(), f.nombre + cas.sansPosition)
    }

    @Test
    fun `CSV foncier - volumes egaux a ceux des tiges nettes positionnees`() = chaque { cas, c ->
        val f = foncier(c, cas.journal)
        val net = JournalNet.tiges(cas.journal).filter { it.action == ActionTige.PLUS && it.position != null }
        // Chaque ligne est arrondie à 3 décimales par le CSV : 5e-4 par ligne.
        egal(vol(c, net), f.vol, 5e-4 * f.lignes + eps, "${cas.nom} ${c.tarif}")
        if (cas.orphelines == 0) {
            val r = ParParcelle.repartir(cas.journal, parcelleDe)
            val sans = r.volumesSansPosition(c).let { Vol(it.volumeTigeM3, it.volumeHouppierM3, it.volumeTotalM3) }
            egal(statut(c, cas.journal) - sans, f.vol, 5e-4 * f.lignes + eps, "${cas.nom} ${c.tarif} : Statut moins sans position")
        }
    }

    // ---------- Par lot ----------
    @Test
    fun `Par lot - tiges par lot calculees a la main`() = CAS.forEach { cas ->
        val b = AffouageLots.bilan(affouage, cas.journal)
        assertEquals(cas.lots.filterValues { it > 0 }, b.associate { it.lot to it.nbTiges }, cas.nom)
    }

    @Test
    fun `Par lot - sans annulation orpheline, total des lots egal Statut en nombre et en volumes`() =
        CAS.filter { it.orphelines == 0 }.forEach { cas ->
            val b = AffouageLots.bilan(affouage, cas.journal)
            assertEquals(cas.net, b.sumOf { it.nbTiges }, "${cas.nom} : nombre")
            egal(
                statut(affouage, cas.journal),
                Vol(b.sumOf { it.volumeTigeM3 }, b.sumOf { it.volumeHouppierM3 }, b.sumOf { it.volumeTigeM3 + it.volumeHouppierM3 }),
                eps, "${cas.nom} : volumes",
            )
        }

    @Test
    fun `Par lot - avec annulation orpheline, Statut egale Par lot moins les orphelines`() {
        // Écart à trancher par Pascal si ce n'est pas voulu : Par lot ne porte que des PLUS, il ne
        // voit donc pas l'orpheline que Statut et Par parcelle comptent en négatif.
        val cas = CAS.single { it.nom == "annulations orphelines" }
        val b = AffouageLots.bilan(affouage, cas.journal)
        assertEquals(cas.net, b.sumOf { it.nbTiges } - cas.orphelines)
    }

    @Test
    fun `lot courant - exception decidee, il ne redescend pas quand sa derniere tige est annulee`() {
        // Source : doc de AffouageLots.etat, mission 022. Le lot 3 n'a plus de tige nette (e annulée).
        val cas = CAS.single { it.nom == "plusieurs cases et lots" }
        assertEquals(setOf(1, 2), AffouageLots.bilan(affouage, cas.journal).mapNotNull { it.lot }.toSet())
        assertEquals(3, AffouageLots.etat(affouage, cas.journal).lot)
        assertEquals(0.0, AffouageLots.etat(affouage, cas.journal).cumulM3, eps)
    }

    // ---------- Volumes : ancrage absolu (tiges nettes écrites à la main) ----------
    @Test
    fun `volumes de Statut - egaux a la somme des tiges nettes ecrites a la main`() = contextes.forEach { c ->
        // « plusieurs cases et lots » : nets a(1) Chêne 30, b(2) Hêtre 30, c(1) Chêne 50, d(2) Hêtre 50.
        val j = CAS.single { it.nom == "plusieurs cases et lots" }.journal
        val nets = listOf(
            plus("a", 1, "Chêne", 30, 1), plus("b", 2, "Hêtre", 30, 2),
            plus("c", 3, "Chêne", 50, 1), plus("d", 4, "Hêtre", 50, 2),
        )
        egal(vol(c, nets), statut(c, j), eps, "${c.tarif}")
    }

    // ---------- CSV du journal ----------
    private fun csvJournal(c: Contexte, j: List<Tige>, net: Boolean): Triple<Map<String, String>, List<List<String>>, List<List<String>>> {
        val lignes = ExportCsv.contexteCsv(c, j, net = net).lines()
        val entete = lignes.takeWhile { it.isNotBlank() }.map { it.split(';', limit = 2) }
            .associate { it[0] to it.getOrElse(1) { "" } }
        val iT = lignes.indexOf("TOTAUX")
        val totaux = lignes.drop(iT + 2).takeWhile { it.isNotBlank() }.map { it.split(';') }
        val iJ = lignes.indexOf("JOURNAL")
        val journal = lignes.drop(iJ + 2).filter { it.isNotBlank() }.map { it.split(';') }
        return Triple(entete, totaux, journal)
    }

    @Test
    fun `CSV du journal - le tableau TOTAUX et le journal signe donnent le net, complet comme net`() = chaque { cas, c ->
        listOf(false, true).forEach { net ->
            val (entete, totaux, journal) = csvJournal(c, cas.journal, net)
            val n = "${cas.nom} net=$net"
            assertEquals(if (net) "NET" else "COMPLET", entete["Journal"])
            assertEquals(cas.net, totaux.sumOf { it[2].toInt() }, "$n : TOTAUX")
            assertEquals(cas.net, journal.sumOf { (if (it[3] == "PLUS") 1 else -1) * it[4].toInt() }, "$n : journal signé")
        }
    }

    @Test
    fun `CSV du journal - volumes d'en-tete et du journal net egaux a Statut`() = chaque { cas, c ->
        val s = statut(c, cas.journal)
        listOf(false, true).forEach { net ->
            val (entete, _, _) = csvJournal(c, cas.journal, net)
            val n = "${cas.nom} ${c.tarif} net=$net"
            // En-tête : 4 décimales.
            assertEquals(s.tige, entete.getValue("VolumeTigeTotalM3").toDouble(), 5e-5, "$n : tige")
            assertEquals(s.houppier, entete.getValue("VolumeHouppierTotalM3").toDouble(), 5e-5, "$n : houppier")
            assertEquals(s.total, entete.getValue("VolumeTotalM3").toDouble(), 5e-5, "$n : total")
        }
        // Journal NET : valeurs unitaires (6 décimales) × quantité, signées.
        val (_, _, journal) = csvJournal(c, cas.journal, true)
        fun somme(col: Int) = journal.sumOf { (if (it[3] == "PLUS") 1 else -1) * it[4].toInt() * it[col].toDouble() }
        val tol = 5e-7 * journal.sumOf { it[4].toInt() } + eps
        egal(s, Vol(somme(15), somme(16), somme(17)), tol, "${cas.nom} ${c.tarif} : journal net")
    }

    // ---------- CSV du bilan par lot ----------
    @Test
    fun `CSV du bilan par lot - la ligne Total et les lots egalent le bilan Par lot`() = CAS.forEach { cas ->
        val b = AffouageLots.bilan(affouage, cas.journal)
        val lignes = ExportBilanLots.csv(affouage, cas.journal, Locale.ROOT).lines()
        val total = lignes.single { it.startsWith("Total;") }.split(';')
        assertEquals(b.sumOf { it.nbTiges }, total[1].toInt(), "${cas.nom} : nombre")
        assertEquals(b.sumOf { it.volumeTigeM3 }, total[2].toDouble(), 5e-4, "${cas.nom} : tige")
        assertEquals(b.sumOf { it.volumeHouppierM3 }, total[3].toDouble(), 5e-4, "${cas.nom} : houppier")
        val parLot = lignes.dropWhile { !it.startsWith("Lot;") }.drop(1)
            .filter { it.isNotBlank() && !it.startsWith("Total;") }
            .map { it.split(';') }.associate { (it[0].takeIf { s -> s.isNotEmpty() }?.toInt()) to it[1].toInt() }
        assertEquals(b.associate { it.lot to it.nbTiges }, parLot, "${cas.nom} : lots")
    }

    @Test
    fun `garde-fou - le jeu couvre toutes les formes attendues`() {
        assertTrue(CAS.any { it.orphelines > 0 })
        assertTrue(CAS.any { it.sansPosition > 0 })
        assertTrue(CAS.any { it.net == 0 && it.journal.isNotEmpty() })
        assertEquals(8, CAS.size)
    }
}
