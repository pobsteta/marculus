package fr.marculus.core

import fr.marculus.core.TarifsIfn.Niveau
import fr.marculus.core.TarifsIfn.Regroupement.AUTRES_FEUILLUS
import fr.marculus.core.TarifsIfn.Regroupement.AUTRES_RESINEUX
import fr.marculus.core.model.TarifCubage
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 030 : proposition de tarif IFN pour « Autres feuillus » et « Autres résineux » (moyenne
 * pondérée par le nombre d'arbres des feuillus / résineux de la SER hors essences du contexte).
 * Tests d'acceptation écrits d'après la fiche, sans lire l'implémentation ; valeurs calculées à la
 * main (détail en commentaire de chaque test). Table : `tarifs-ifn/tarifs_ser_autres.csv`
 * (SER C20, C51, B70 de ser.csv ; D30, D31, E10, F10, F11, P10, P12, T01..T25, Z99 absentes de ser.csv :
 * la GRECO est alors la 1re lettre du code, comme le fait déjà `proposer`).
 *
 * API attendue (signatures proposées au dev) :
 *  - `enum class TarifsIfn.Regroupement { AUTRES_FEUILLUS, AUTRES_RESINEUX }` avec, dans son companion,
 *    `fun de(essence: String): Regroupement?` (libellé « Autres feuillus » / « Autres résineux », casse et accents ignorés) ;
 *  - `fun proposerAutres(ser: String, regroupement: Regroupement, essencesDuContexte: Collection<String>, tarif: TarifCubage): PropositionTarif?` ;
 *  - `fun proposerPartHouppierAutres(ser: String, regroupement: Regroupement, essencesDuContexte: Collection<String>): PropositionPart?`.
 * `PropositionTarif.numeroBrut` = moyenne pondérée brute, `nArbres` = total des arbres retenus au niveau.
 */
class TarifsIfnAutresTest {

    private fun ressource(nom: String) =
        requireNotNull(javaClass.getResource("/tarifs-ifn/$nom")) { nom }.readText()

    private val ifn = TarifsIfn.charger(
        ressource("tarifs_ser_autres.csv"), ressource("communes_ser.csv"), ressource("ser.csv"),
    )

    /** Un contexte type : les six essences par défaut, avec Chêne précisé en Chêne sessile. */
    private val contexte = listOf("Chêne sessile", "Hêtre", "Autres feuillus", "Sapin pectiné", "Épicéa commun", "Autres résineux")
    private val lent = TarifCubage.SCHAEFFER_LENT

    // --- Cas nominal : moyenne pondérée, exclusion des essences du contexte -------------------

    /**
     * SER C20, lent, feuillus : Chêne sessile 8,0/100 et Hêtre 9,0/50 sont au contexte (écartés) ;
     * restent Charme 6,0/20, Tremble 10,0/10, Robinier 3,0/10.
     * (6*20 + 10*10 + 3*10) / 40 = 250 / 40 = 6,25 → 6. Non pondérée : 6,333 (distinct).
     * Aucune ligne n'atteint 30 arbres seule : le seuil porte sur le total (40).
     */
    @Test
    fun `autres feuillus - moyenne ponderee hors contexte, seuil sur le total`() {
        val p = assertNotNull(ifn.proposerAutres("C20", AUTRES_FEUILLUS, contexte, lent))
        assertEquals(Niveau.SER, p.niveau)
        assertEquals("C20", p.code)
        assertEquals(40, p.nArbres)
        assertEquals(6.25, p.numeroBrut, 1e-9)
        assertEquals(6, p.numero)
    }

    /** Même SER, rapide : Charme 4,0/20, Tremble 8,0/10, Robinier 3,0/10 → 190/40 = 4,75 → 5. Seules les lignes du type demandé comptent. */
    @Test
    fun `autres feuillus - seules les lignes du type demande comptent`() {
        val p = assertNotNull(ifn.proposerAutres("C20", AUTRES_FEUILLUS, contexte, TarifCubage.SCHAEFFER_RAPIDE))
        assertEquals(4.75, p.numeroBrut, 1e-9)
        assertEquals(5, p.numero)
        assertEquals(40, p.nArbres)
    }

    /**
     * SER C20, lent, résineux : Sapin pectiné 12/400 et Épicéa commun 11/200 au contexte (écartés) ;
     * restent Pin sylvestre 7/12, Douglas 9/20, Cyprès de Lawson 14/8.
     * (84 + 180 + 112) / 40 = 376 / 40 = 9,4 → 9. Non pondérée : 10,0 (distinct). Aucun feuillu ne fuit.
     */
    @Test
    fun `autres resineux - moyenne ponderee hors contexte`() {
        val p = assertNotNull(ifn.proposerAutres("C20", AUTRES_RESINEUX, contexte, lent))
        assertEquals(Niveau.SER, p.niveau)
        assertEquals(40, p.nArbres)
        assertEquals(9.4, p.numeroBrut, 1e-9)
        assertEquals(9, p.numero)
    }

    /**
     * Contexte vide : plus rien n'est écarté. Feuillus C20 lent : 8*100 + 9*50 + 6*20 + 10*10 + 3*10
     * = 1500 pour 190 arbres → 7,894736… → 8.
     */
    @Test
    fun `sans essence au contexte - tous les feuillus de la SER comptent`() {
        val p = assertNotNull(ifn.proposerAutres("C20", AUTRES_FEUILLUS, emptyList(), lent))
        assertEquals(190, p.nArbres)
        assertEquals(1500.0 / 190, p.numeroBrut, 1e-9)
        assertEquals(8, p.numero)
    }

    /** L'essence du contexte est reconnue par son code ONF : sans accent ni majuscule, même résultat que le premier test. */
    @Test
    fun `exclusion par code ONF - casse et accents sans effet`() {
        val p = assertNotNull(ifn.proposerAutres("C20", AUTRES_FEUILLUS, listOf("chene SESSILE", "hetre"), lent))
        assertEquals(40, p.nArbres)
        assertEquals(6.25, p.numeroBrut, 1e-9)
    }

    // --- Cascade SER -> GRECO -> France, seuil de 30 sur le total ------------------------------

    /**
     * SER C51, lent : Hêtre 9,4/871 est au contexte (ne compte pas pour le seuil) ; Charme 7/12 + Tremble 9/10 = 22 < 30
     * → GRECO C : Hêtre 9/500 écarté ; Charme 6/20, Tremble 10/15 = 35 ≥ 30 ; (120 + 150) / 35 = 7,714285… → 8.
     */
    @Test
    fun `repli sur la GRECO quand le total retenu du SER est sous 30`() {
        val p = assertNotNull(ifn.proposerAutres("C51", AUTRES_FEUILLUS, contexte, lent))
        assertEquals(Niveau.GRECO, p.niveau)
        assertEquals("C", p.code)
        assertEquals(35, p.nArbres)
        assertEquals(270.0 / 35, p.numeroBrut, 1e-9)
        assertEquals(8, p.numero)
    }

    /** SER B70, lent : Charme 5/15 + Tremble 7/15 = 30 pile → SER ; (75 + 105) / 30 = 6,0. */
    @Test
    fun `seuil exact - 30 arbres au total suffisent`() {
        val p = assertNotNull(ifn.proposerAutres("B70", AUTRES_FEUILLUS, contexte, lent))
        assertEquals(Niveau.SER, p.niveau)
        assertEquals(30, p.nArbres)
        assertEquals(6.0, p.numeroBrut, 1e-9)
        assertEquals(6, p.numero)
    }

    /**
     * SER C51, rapide : Charme 4/15 + Tremble 6/14 = 29 < 30 → GRECO C : Charme 5/40, Tremble 7/10 = 50 ;
     * (200 + 70) / 50 = 5,4 → 5.
     */
    @Test
    fun `seuil - 29 arbres au total ne suffisent pas`() {
        val p = assertNotNull(ifn.proposerAutres("C51", AUTRES_FEUILLUS, contexte, TarifCubage.SCHAEFFER_RAPIDE))
        assertEquals(Niveau.GRECO, p.niveau)
        assertEquals(50, p.nArbres)
        assertEquals(5.4, p.numeroBrut, 1e-9)
        assertEquals(5, p.numero)
    }

    /**
     * SER inconnue (Z99, GRECO « Z » sans ligne) → France : Hêtre 20/1000 écarté (contexte) ; Charme 5/100 + Tremble 9/100 :
     * (500 + 900) / 200 = 7,0. Résineux : Pin sylvestre 7/60 + Douglas 11/20 : (420 + 220) / 80 = 8,0.
     */
    @Test
    fun `repli sur la France - feuillus et resineux`() {
        val f = assertNotNull(ifn.proposerAutres("Z99", AUTRES_FEUILLUS, contexte, lent))
        assertEquals(Niveau.FRANCE, f.niveau)
        assertEquals("FR", f.code)
        assertEquals(200, f.nArbres)
        assertEquals(7.0, f.numeroBrut, 1e-9)
        assertEquals(7, f.numero)
        val r = assertNotNull(ifn.proposerAutres("Z99", AUTRES_RESINEUX, contexte, lent))
        assertEquals(Niveau.FRANCE, r.niveau)
        assertEquals(80, r.nArbres)
        assertEquals(8.0, r.numeroBrut, 1e-9)
        assertEquals(8, r.numero)
    }

    /** Rien nulle part : France à 20 arbres (< 30), aucun feuillu au type très lent, SER sans aucune ligne. */
    @Test
    fun `aucune proposition quand aucun niveau n atteint 30 arbres`() {
        val tl = TarifCubage.SCHAEFFER_TRES_LENT
        assertNull(ifn.proposerAutres("Z99", AUTRES_RESINEUX, contexte, tl)) // Pin sylvestre FR : 20 arbres
        assertNull(ifn.proposerAutres("Z99", AUTRES_FEUILLUS, contexte, tl))
        assertNull(ifn.proposerAutres("Y01", AUTRES_FEUILLUS, contexte, lent))
    }

    /** Comme `proposer` : pas de numéro pour EMERGE ni pour AUCUN. */
    @Test
    fun `tarif sans numero - aucune proposition`() {
        assertNull(ifn.proposerAutres("C20", AUTRES_FEUILLUS, contexte, TarifCubage.EMERGE))
        assertNull(ifn.proposerAutres("C20", AUTRES_RESINEUX, contexte, TarifCubage.AUCUN))
    }

    // --- Lignes au numéro négatif : écartées partout -------------------------------------------

    /**
     * Bouleau verruqueux, F10 (GRECO F), lent : la ligne SER (-2,0 sur 500 arbres, au-dessus du seuil) est écartée, non ramenée à 1 ;
     * on passe à la GRECO F : 4,0 sur 200 arbres → numéro 4, niveau GRECO.
     */
    @Test
    fun `proposition par essence - ligne negative ecartee, niveau suivant retenu`() {
        val p = assertNotNull(ifn.proposer("F10", "Bouleau verruqueux", lent))
        assertEquals(Niveau.GRECO, p.niveau)
        assertEquals("F", p.code)
        assertEquals(200, p.nArbres)
        assertEquals(4.0, p.numeroBrut, 1e-9)
        assertEquals(4, p.numero)
    }

    /** Érable sycomore, F11, lent : seule ligne = négative (500 arbres) → écartée, aucun autre niveau → null (et non numéro 1). */
    @Test
    fun `proposition par essence - seule ligne negative, rien n est propose`() {
        assertNull(ifn.proposer("F11", "Érable sycomore", lent))
    }

    /**
     * SER D30, lent, contexte vide : Charme 6/20, Tremble 8/15 ; Robinier -3,0/400 écarté (ni numérateur ni total).
     * (120 + 120) / 35 = 6,857142… → 7 ; nArbres 35 (et non 435).
     */
    @Test
    fun `moyenne - ligne negative ecartee du calcul`() {
        val p = assertNotNull(ifn.proposerAutres("D30", AUTRES_FEUILLUS, emptyList(), lent))
        assertEquals(Niveau.SER, p.niveau)
        assertEquals(35, p.nArbres)
        assertEquals(240.0 / 35, p.numeroBrut, 1e-9)
        assertEquals(7, p.numero)
    }

    /** Résineux, D30, lent, contexte vide : Pin sylvestre 7/20, Douglas 9/15 ; Épicéa -5,0/300 écarté. (140 + 135) / 35 = 7,857142… → 8. */
    @Test
    fun `moyenne resineux - ligne negative ecartee du calcul`() {
        val p = assertNotNull(ifn.proposerAutres("D30", AUTRES_RESINEUX, emptyList(), lent))
        assertEquals(35, p.nArbres)
        assertEquals(275.0 / 35, p.numeroBrut, 1e-9)
        assertEquals(8, p.numero)
    }

    /**
     * SER D31 : Charme 6/20 + Robinier -3/400. La négative ne compte pas pour le seuil : 20 < 30 → GRECO D :
     * Charme 5/40 → 5,0 sur 40 arbres (sans l'écart, le SER passerait à 420 arbres et une moyenne négative).
     */
    @Test
    fun `ligne negative ne compte pas pour le seuil - repli sur la GRECO`() {
        val p = assertNotNull(ifn.proposerAutres("D31", AUTRES_FEUILLUS, emptyList(), lent))
        assertEquals(Niveau.GRECO, p.niveau)
        assertEquals("D", p.code)
        assertEquals(40, p.nArbres)
        assertEquals(5.0, p.numeroBrut, 1e-9)
    }

    /**
     * SER E10 (GRECO E) : Charme -4/300 écarté, Tremble 8/10 → 10 < 30 → France (Hêtre 20/1000 au contexte) :
     * Charme 5/100 + Tremble 9/100 → 7,0 sur 200 arbres.
     */
    @Test
    fun `negative au niveau GRECO - passage a la France comme si la ligne n existait pas`() {
        val p = assertNotNull(ifn.proposerAutres("E10", AUTRES_FEUILLUS, contexte, lent))
        assertEquals(Niveau.FRANCE, p.niveau)
        assertEquals(200, p.nArbres)
        assertEquals(7.0, p.numeroBrut, 1e-9)
    }

    // --- Part de houppier ------------------------------------------------------------------------

    /**
     * SER P10 (quatre types par essence, comme nemeton) : Hêtre 30 %/100 et Sapin pectiné 40 %/300 au contexte ;
     * feuillus restants Charme 12/20, Tremble 18/10, Robinier 15/10 : (240 + 180 + 150) / 40 = 14,25 %, 40 arbres
     * (non 160 : les quatre types d'une même essence ne se cumulent pas).
     * Résineux : Pin sylvestre 10/20, Douglas 20/10, Cyprès de Lawson 25/10 : (200 + 200 + 250) / 40 = 16,25 %.
     */
    @Test
    fun `part de houppier - moyenne ponderee hors contexte`() {
        val f = assertNotNull(ifn.proposerPartHouppierAutres("P10", AUTRES_FEUILLUS, contexte))
        assertEquals(Niveau.SER, f.niveau)
        assertEquals("P10", f.code)
        assertEquals(40, f.nArbres)
        assertEquals(14.25, f.pourcent, 1e-9)
        val r = assertNotNull(ifn.proposerPartHouppierAutres("P10", AUTRES_RESINEUX, contexte))
        assertEquals(40, r.nArbres)
        assertEquals(16.25, r.pourcent, 1e-9)
    }

    /** SER P12 : Charme 12/12 + Tremble 18/10 = 22 < 30 → GRECO P : Charme 10/20 + Tremble 20/10 = 30 ; 400 / 30 = 13,3333… %. */
    @Test
    fun `part de houppier - repli sur la GRECO, seuil sur le total`() {
        val p = assertNotNull(ifn.proposerPartHouppierAutres("P12", AUTRES_FEUILLUS, contexte))
        assertEquals(Niveau.GRECO, p.niveau)
        assertEquals("P", p.code)
        assertEquals(30, p.nArbres)
        assertEquals(400.0 / 30, p.pourcent, 1e-9)
    }

    // --- Partage feuillu / résineux (règle 5), table T01..T23 : une ligne de 50 arbres par essence --

    private data class Cas(val ser: String, val essence: String, val espar: String, val feuillu: Boolean)

    /**
     * Source : IGN, `documentation_2.txt`, donnée ESSPRE (lue le 2026-11-10 : 70 lignes) ; le code ESS = les deux premiers
     * caractères du code ESPAR (vérifié : les 185 ESPAR du fichier ont un ESSPRE) ; ESS 01..49 = feuillus
     * (43 codes), ESS 51..77 = conifères (27 codes). Les deux cas ambigus de la table (T24 If 67, T25 Genévrier 69JC)
     * ne sont pas testés : question à Pascal.
     */
    private val partage = listOf(
        Cas("T01", "Chêne sessile", "03", true), Cas("T02", "Hêtre", "09", true),
        Cas("T03", "Robinier faux acacia", "14", true), Cas("T04", "Tremble", "24", true),
        Cas("T05", "Olivier d'Europe", "28", true), Cas("T06", "Eucalyptus", "36", true),
        Cas("T07", "Arbousier", "40", true), Cas("T08", "Alisier torminal", "41", true),
        Cas("T09", "Aubépine monogyne", "49AM", true), Cas("T10", "Houx", "49IA", true),
        Cas("T11", "Bouleau verruqueux", "12V", true),
        Cas("T12", "Pin maritime", "51", false), Cas("T13", "Pin sylvestre", "52", false),
        Cas("T14", "Pin à crochets", "58", false), Cas("T15", "Pin cembro", "59", false),
        Cas("T16", "Sapin pectiné", "61", false), Cas("T17", "Épicéa commun", "62", false),
        Cas("T18", "Mélèze d'Europe", "63", false), Cas("T19", "Douglas", "64", false),
        Cas("T20", "Cèdre de l'Atlas", "65", false), Cas("T21", "Cyprès de Provence", "66", false),
        Cas("T22", "Cyprès de Lawson", "68CL", false), Cas("T23", "Sapin de Nordmann", "71", false),
    )

    /** Chaque SER Txx n'a qu'une ligne (50 arbres, numéro 7,0) : elle est reprise par un seul des deux regroupements. */
    @Test
    fun `partage feuillu resineux selon le code ESS des deux premiers caracteres de l ESPAR`() {
        assertEquals(23, partage.size)
        for (c in partage) {
            assertNotNull(ifn.proposer(c.ser, c.essence, lent), "ligne ${c.ser} ${c.essence} non chargee")
            val f = ifn.proposerAutres(c.ser, AUTRES_FEUILLUS, emptyList(), lent)
            val r = ifn.proposerAutres(c.ser, AUTRES_RESINEUX, emptyList(), lent)
            val attendu = if (c.feuillu) f else r
            val autre = if (c.feuillu) r else f
            val nom = "${c.essence} (${c.espar})"
            assertNotNull(attendu, "$nom devrait être ${if (c.feuillu) "feuillu" else "résineux"}")
            assertEquals(7.0, attendu.numeroBrut, 1e-9, nom)
            assertEquals(50, attendu.nArbres, nom)
            assertNull(autre, "$nom ne doit pas être dans l'autre regroupement")
        }
    }

    /** Les libellés de la liste par défaut (Referentiels.ESSENCES_DEFAUT) reconnaissent les deux regroupements, et eux seuls. */
    @Test
    fun `reconnaissance des deux regroupements par leur libelle`() {
        assertEquals(AUTRES_FEUILLUS, TarifsIfn.Regroupement.de("Autres feuillus"))
        assertEquals(AUTRES_RESINEUX, TarifsIfn.Regroupement.de("Autres résineux"))
        assertEquals(AUTRES_RESINEUX, TarifsIfn.Regroupement.de("autres resineux"))
        for (e in listOf("Chêne", "Hêtre", "Sapin", "Épicéa", "Pin sylvestre", "")) {
            assertNull(TarifsIfn.Regroupement.de(e), e)
        }
    }

    // --- Terrain : contexte type, table embarquée ------------------------------------------------

    /**
     * Chêne, Hêtre, Autres feuillus, Sapin, Épicéa, Autres résineux (Referentiels.ESSENCES_DEFAUT) sur les SER C20 et B70
     * de la table embarquée : chacune reçoit une proposition. Invariants seulement (valeurs non calculables à la main sur
     * 14 752 lignes) : numéro 1..20, au moins 30 arbres, brut non négatif. Les 44 lignes 29AF/68CE ont un libellé vide et
     * ne sont pas chargées.
     */
    @Test
    fun `terrain - les six essences par defaut recoivent une proposition sur la table embarquee`() {
        fun table(nom: String) = File("../data/src/main/assets/tarifs-ifn/$nom").readText()
        val reel = TarifsIfn.charger(table("tarifs_ser.csv"), table("communes_ser.csv"), table("ser.csv"))
        val defaut = Referentiels.ESSENCES_DEFAUT
        for (ser in listOf("C20", "B70")) {
            for (essence in defaut) {
                val regroupement = TarifsIfn.Regroupement.de(essence)
                val p = if (regroupement == null) reel.proposer(ser, essence, lent)
                else reel.proposerAutres(ser, regroupement, defaut, lent)
                val vu = assertNotNull(p, "$ser / $essence")
                assertTrue(vu.numero in 1..20, "$ser / $essence : numéro ${vu.numero}")
                assertTrue(vu.nArbres >= TarifsIfn.SEUIL_ARBRES, "$ser / $essence : ${vu.nArbres} arbres")
                assertTrue(vu.numeroBrut >= 0.0, "$ser / $essence : brut ${vu.numeroBrut} (négatif écarté)")
            }
        }
    }
}
