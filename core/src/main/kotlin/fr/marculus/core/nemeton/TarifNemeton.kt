package fr.marculus.core.nemeton

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Tarif NEMETON chargé depuis le paquet (mission 039) ; s'obtient par [PaquetNemeton.charger].
 *
 * Calcule les sorties d'un arbre à partir des seules constantes, comme la classe de référence `Tarif` de
 * `~/dev/tarif-nemeton/code/paquet_livrable.py` l. 269-357 (paquet 0.2.2-provisoire). Le type d'un code se lit dans
 * les données : feuillu s'il porte le modèle `volume_feuillu`, résineux s'il porte `profil_tige_resineux`. Les
 * sorties optionnelles aussi : `V_bois_fort_total_m3` si le code a `fraction_decoupe`, biomasse et carbone s'il a
 * `infradensite`. Une sortie qui n'existe pas est absente de la map, jamais 0.
 *
 * Formules (D en cm, H en m ; « k(x) » = constante du paquet) :
 * - feuillus, volume aérien total : `V = π/4·(D/100)²·H·exp(a + b·(ln D − ln D_ref) + c·(ln H − ln H_ref)
 *   + facteur_biais_sigma2·σ²)` (méthodologie NEMETON v2.25 l. 551 ; README « Principe ») ;
 * - feuillus, bois fort total : `V·min(f7/corr, 1)`, `f7 = fmax/(1 + exp(−(ln D − m)/s))`,
 *   `corr = (min(D, D_ref_pente)/D_ref_pente)^delta_pente` (méthodologie l. 643) ;
 * - résineux, tige : profil de l'éq. 5b (méthodologie l. 209-212) intégré par la méthode des trapèzes sur
 *   `n_points` hauteurs régulièrement espacées de 1,3 m à H − marge_sommet (comme `np.linspace` et
 *   `np.trapezoid`), plus le cylindre de 0 à 1,3 m ; bois fort tige = tige à la découpe `diametre_decoupe_cm` ;
 * - résineux, volume aérien total : tige × `E(D) = exp(a + b·ln(min(D, D_plafond)/D_ref))`
 *   (méthodologie l. 597, 639, 655) ;
 * - biomasse aérienne (t) : `V_aerien·ρ(D)/kg_par_t`, `ρ(D) = (rho_moyenne + rho_pente_10cm·(D − D_ref)/pas_D)
 *   ·facteur_arbre_entier` (éq. 14b, méthodologie l. 311-316, l. 609) ;
 * - carbone aérien (t) : biomasse × `fraction_carbone` du groupe feuillu ou conifere (méthodologie l. 305-307, 551).
 *
 * Domaine (hors domaine : valeurs calculées, [SortiesNemeton.horsDomaine] vrai) : feuillus
 * `D_min_calage·facteur_D_min ≤ D ≤ D_max_calage·facteur_D_max` et
 * `H_min_calage − marge_H ≤ H ≤ H_max_calage + marge_H` ; résineux `HD_min ≤ H/(D/100) ≤ HD_max`.
 */
class TarifNemeton internal constructor(
    /** Version du paquet (clé `version` du manifeste). */
    val version: String,
    valeurs: Map<Cle, Double>,
    essences: Map<String, String>,
) {
    private val modeles: Map<String, Modele>

    init {
        val v = Constantes(valeurs)
        modeles = essences.mapValues { (code, essence) -> Modele.lire(v, code, essence) }
    }

    /** Codes IFN couverts par le paquet. */
    val codes: Set<String> get() = modeles.keys

    /**
     * Sorties de l'arbre [codeIfn] de diamètre [dCm] (cm, à 1,30 m) et de hauteur [hM] (m) ; `null` si le code n'est
     * pas couvert par le paquet.
     *
     * @throws IllegalArgumentException si D ou H n'est pas un nombre fini strictement positif, ou, pour un résineux,
     *   si `H ≤ h_ref + marge_sommet` (1,3 + 0,001 = 1,301 m dans le paquet 0.2.2-provisoire) : la tige n'a alors
     *   plus de longueur à intégrer au-dessus de 1,30 m et la formule de référence rendrait un volume sans sens.
     */
    fun sorties(codeIfn: String, dCm: Double, hM: Double): SortiesNemeton? {
        val m = modeles[codeIfn] ?: return null
        require(dCm.isFinite() && dCm > 0.0) { "D = $dCm cm : nombre fini strictement positif attendu" }
        require(hM.isFinite() && hM > 0.0) { "H = $hM m : nombre fini strictement positif attendu" }
        return SortiesNemeton(m.valeurs(dCm, hM), !m.dansDomaine(dCm, hM), m.essence, version)
    }

    /** Accès aux constantes avec message explicite si l'une manque (le paquet est alors refusé au chargement). */
    internal class Constantes(private val v: Map<Cle, Double>) {
        fun a(cle: String, modele: String, terme: String): Boolean = Cle(cle, modele, terme) in v
        fun k(cle: String, modele: String, terme: String): Double =
            v[Cle(cle, modele, terme)]
                ?: throw IllegalArgumentException("constante absente : ${cle.removePrefix(PaquetNemeton.GROUPE)} / $modele / $terme")
        fun g(groupe: String, modele: String, terme: String): Double = k(PaquetNemeton.GROUPE + groupe, modele, terme)
    }

    private class Infradensite(
        val rhoMoyenne: Double, val rhoPente: Double, val facteur: Double,
        val dRef: Double, val pasD: Double, val kgParT: Double, val fractionCarbone: Double,
    ) {
        fun rho(d: Double): Double = (rhoMoyenne + rhoPente * (d - dRef) / pasD) * facteur

        fun ajouter(out: MutableMap<String, Double>, vAerien: Double, d: Double) {
            val b = vAerien * rho(d) / kgParT
            out[BIOMASSE] = b
            out[CARBONE] = b * fractionCarbone
        }

        companion object {
            fun lire(c: Constantes, code: String, groupe: String): Infradensite? =
                if (!c.a(code, "infradensite", "rho_moyenne")) null
                else Infradensite(
                    c.k(code, "infradensite", "rho_moyenne"), c.k(code, "infradensite", "rho_pente_10cm"),
                    c.k(code, "facteur_arbre_entier", "facteur"),
                    c.g("tous", "infradensite", "D_ref"), c.g("tous", "infradensite", "pas_D"),
                    c.g("tous", "conversion", "kg_par_t"), c.g(groupe, "carbone", "fraction_carbone"),
                )
        }
    }

    private sealed class Modele(val essence: String) {
        abstract fun dansDomaine(d: Double, h: Double): Boolean
        abstract fun valeurs(d: Double, h: Double): Map<String, Double>

        companion object {
            fun lire(c: Constantes, code: String, essence: String): Modele {
                val feuillu = c.a(code, "volume_feuillu", "a")
                val resineux = c.a(code, "profil_tige_resineux", "k0")
                require(feuillu != resineux) {
                    "code $code ($essence) : ni feuillu ni résineux, ou les deux (volume_feuillu / profil_tige_resineux)"
                }
                return if (feuillu) Feuillu.lire(c, code, essence) else Resineux.lire(c, code, essence)
            }
        }
    }

    private class Decoupe(val fmax: Double, val m: Double, val s: Double, val delta: Double, val dRefPente: Double) {
        fun fraction(d: Double): Double {
            val f7 = fmax / (1 + exp(-(ln(d) - m) / s))
            val corr = (min(d, dRefPente) / dRefPente).pow(delta)
            return min(f7 / corr, 1.0)
        }
    }

    private class Feuillu(
        essence: String,
        val a: Double, val b: Double, val c: Double, val sigma: Double,
        val dRef: Double, val hRef: Double, val facteurBiais: Double,
        val dMin: Double, val dMax: Double, val hMin: Double, val hMax: Double,
        val decoupe: Decoupe?, val infra: Infradensite?,
    ) : Modele(essence) {
        override fun dansDomaine(d: Double, h: Double) = d in dMin..dMax && h in hMin..hMax

        override fun valeurs(d: Double, h: Double): Map<String, Double> {
            val lf = a + b * (ln(d) - ln(dRef)) + c * (ln(h) - ln(hRef))
            val dm = d / 100
            val v = PI / 4 * (dm * dm) * h * exp(lf + facteurBiais * (sigma * sigma))
            val out = LinkedHashMap<String, Double>()
            out[V_AERIEN] = v
            if (decoupe != null) out[V_BOIS_FORT_TOTAL] = v * decoupe.fraction(d)
            infra?.ajouter(out, v, d)
            return out
        }

        companion object {
            fun lire(c: Constantes, code: String, essence: String): Feuillu {
                val vf = "volume_feuillu"; val dom = "domaine_feuillu"
                val decoupe = if (!c.a(code, "fraction_decoupe", "f7_fmax")) null else Decoupe(
                    c.k(code, "fraction_decoupe", "f7_fmax"), c.k(code, "fraction_decoupe", "f7_m"),
                    c.k(code, "fraction_decoupe", "f7_s"), c.k(code, "correction_menu_bois", "delta_pente"),
                    c.g("feuillu", "correction_menu_bois", "D_ref_pente"),
                )
                val marge = c.g("feuillu", dom, "marge_H")
                return Feuillu(
                    essence,
                    c.k(code, vf, "a"), c.k(code, vf, "b_logD"), c.k(code, vf, "c_logH"), c.k(code, vf, "sigma_log"),
                    c.g("feuillu", vf, "D_ref"), c.g("feuillu", vf, "H_ref"), c.g("feuillu", vf, "facteur_biais_sigma2"),
                    c.k(code, dom, "D_min_calage") * c.g("feuillu", dom, "facteur_D_min"),
                    c.k(code, dom, "D_max_calage") * c.g("feuillu", dom, "facteur_D_max"),
                    c.k(code, dom, "H_min_calage") - marge,
                    c.k(code, dom, "H_max_calage") + marge,
                    decoupe, Infradensite.lire(c, code, "feuillu"),
                )
            }
        }
    }

    private class Resineux(
        essence: String,
        val k0: Double, val k1: Double, val beta: Double, val lam: Double, val aK: Double, val x0: Double, val sx: Double,
        val h0: Double, val dRefProfil: Double, val nPoints: Int, val margeSommet: Double, val uMin: Double,
        val eA: Double, val eB: Double, val dPlafond: Double, val dRefExpansion: Double,
        val decoupeBoisFortM: Double, val hdMin: Double, val hdMax: Double,
        val infra: Infradensite?,
    ) : Modele(essence) {
        override fun dansDomaine(d: Double, h: Double): Boolean = (h / (d / 100)) in hdMin..hdMax

        override fun valeurs(d: Double, h: Double): Map<String, Double> {
            require(h > h0 + margeSommet) {
                "H = $h m : un résineux doit dépasser ${h0 + margeSommet} m (h_ref + marge_sommet) pour avoir une tige à intégrer"
            }
            val v = tige(d, h, 0.0) * expansion(d)
            val out = LinkedHashMap<String, Double>()
            out[V_AERIEN] = v
            out[V_BOIS_FORT_TIGE] = tige(d, h, decoupeBoisFortM)
            infra?.ajouter(out, v, d)
            return out
        }

        fun expansion(d: Double): Double = exp(eA + eB * ln(min(d, dPlafond) / dRefExpansion))

        /** Volume de tige (m³) au-dessus du diamètre de découpe [dcutM] (m) : paquet_livrable.py l. 303-319. */
        fun tige(d: Double, h: Double, dcutM: Double): Double {
            val dm = d / 100
            val termeA = aK * ln(d / dRefProfil)
            val longueur = h - h0 - margeSommet
            val pas = 1.0 / (nPoints - 1)
            var somme = 0.0
            var hPrec = 0.0
            var fPrec = 0.0
            for (i in 0 until nPoints) {
                val t = if (i == nPoints - 1) 1.0 else i * pas // np.linspace(0, 1, n) : dernier point exact
                val hg = h0 + t * longueur
                val x = ((hg - h0) / (h - h0)).coerceIn(0.0, 1.0)
                val u = max(1 - x, uMin)
                val kk = max(k0 + (k1 - k0) / (1 + exp(-(x - x0) / sx)) + termeA, 0.0)
                val dg = dm * exp(0.5 * kk * ln(u) + beta * (exp(-(hg - h0) / lam) - 1))
                val f = if (dg >= dcutM) PI * (dg * dg) / 4 else 0.0
                if (i > 0) somme += (hg - hPrec) * (f + fPrec) / 2.0 // np.trapezoid
                hPrec = hg
                fPrec = f
            }
            val cylindre = if (dm >= dcutM) h0 * PI * (dm * dm) / 4 else 0.0
            return somme + cylindre
        }

        companion object {
            fun lire(c: Constantes, code: String, essence: String): Resineux {
                val p = "profil_tige_resineux"; val e = "expansion_resineux"; val it = "integration_tige_resineux"
                val n = c.g("conifere", it, "n_points")
                require(n >= 2 && n == Math.rint(n)) { "n_points = $n : entier ≥ 2 attendu" }
                return Resineux(
                    essence,
                    c.k(code, p, "k0"), c.k(code, p, "k1"), c.k(code, p, "beta"), c.k(code, p, "lam"),
                    c.k(code, p, "a"), c.k(code, p, "x0"), c.k(code, p, "sx"),
                    c.g("conifere", p, "h_ref"), c.g("conifere", p, "D_ref"), n.toInt(),
                    c.g("conifere", it, "marge_sommet"), c.g("conifere", it, "u_min"),
                    c.k(code, e, "a"), c.k(code, e, "b"), c.k(code, e, "D_plafond"), c.g("conifere", e, "D_ref"),
                    c.g("tous", "bois_fort", "diametre_decoupe_cm") / 100,
                    c.g("conifere", "domaine_resineux", "HD_min"), c.g("conifere", "domaine_resineux", "HD_max"),
                    Infradensite.lire(c, code, "conifere"),
                )
            }
        }
    }

    companion object {
        const val V_AERIEN = "V_aerien_total_m3"
        const val V_BOIS_FORT_TOTAL = "V_bois_fort_total_m3"
        const val V_BOIS_FORT_TIGE = "V_bois_fort_tige_m3"
        const val BIOMASSE = "biomasse_aerienne_t"
        const val CARBONE = "carbone_aerien_t"
    }
}
