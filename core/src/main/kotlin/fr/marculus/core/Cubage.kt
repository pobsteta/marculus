package fr.marculus.core

import fr.marculus.core.model.Contexte
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import java.text.Normalizer
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Cubage des tiges.
 *
 *  - **Schaeffer** (une entrée, diamètre) : V = (M/1400)(D−5)(D−10) [rapide] / (M/1800)·D·(D−5) [lent],
 *    M = 0,8 + 0,1·N. Vérifié contre les tables ONF.
 *  - **EMERGE** (deux entrées, projet ANR/IGN) : **volume bois fort tige** (découpe 7 cm) calculé selon
 *    le modèle de C. Deleuze (RDV Techniques ONF 44, 2014 ; portage R `gftools::TarEmerge`). Entrées :
 *    circonférence à 1,30 m + hauteur totale ; hdec (décrochement) = 0,8·htot par défaut. Coefficients
 *    par essence (a,b,c pour le volume total ; d,e,f,g pour la part de tige). Repli **coefficient de
 *    forme** (f = 0,5) si essence non couverte ; tige sans hauteur → non cubable (0).
 */
object Cubage {

    /** Coefficient de forme par défaut pour le repli deux entrées. */
    const val COEFFICIENT_FORME = 0.5

    private const val DECOUPE_BOIS_FORT_CM = 7.0

    /** Coefficients EMERGE : a,b,c (volume total) et d,e,f,g (part de tige). Source : gftools::ListTarEmerge. */
    private data class CoefEmerge(
        val a: Double, val b: Double, val c: Double,
        val d: Double, val e: Double, val f: Double, val g: Double,
    )

    private val EMERGE_FEUILLUS = CoefEmerge(0.52196, 0.66107, -0.00166, 0.75738, 0.08902, -3.62468, 0.06723)
    private val EMERGE_RESINEUX = CoefEmerge(0.35639, 1.75592, 0.00173, 0.73796, 0.07128, -2.46468, 0.04462)

    private val EMERGE = mapOf(
        "chene" to CoefEmerge(0.56104, 0.66107, -0.00235, 0.90955, 0.06998, -4.24117, 0.02852),
        "chene sessile" to CoefEmerge(0.56104, 0.66107, -0.00235, 0.90955, 0.06998, -4.24117, 0.02852),
        "chene pedoncule" to CoefEmerge(0.56104, 0.66107, -0.00235, 0.89821, 0.06695, -4.05873, 0.02452),
        "hetre" to CoefEmerge(0.54187, 0.66107, -0.00150, 0.87721, 0.06488, -4.11764, 0.02298),
        "charme" to CoefEmerge(0.53321, 0.66107, -0.00150, 0.72119, 0.13227, -3.93900, 0.08058),
        "frene" to CoefEmerge(0.50864, 0.66107, -0.00097, 0.85751, 0.06899, -3.38930, 0.03003),
        "chataignier" to CoefEmerge(0.52196, 0.66107, -0.00166, 0.66167, 0.10312, -2.54073, 0.08169),
        "bouleau" to CoefEmerge(0.49335, 0.66107, -0.00159, 0.70973, 0.12597, -4.29785, 0.10620),
        "sapin" to CoefEmerge(0.39790, 1.75592, 0.00168, 0.88412, 0.05671, -4.03079, 0.03243),
        "epicea" to CoefEmerge(0.30342, 1.75592, 0.00390, 0.80828, 0.08902, -3.39626, 0.04733),
        "douglas" to CoefEmerge(0.23501, 1.75592, 0.00389, 0.71074, 0.18144, -3.06943, 0.10053),
        "meleze" to CoefEmerge(0.37658, 1.75592, 0.00107, 0.83717, 0.06521, -4.27466, 0.06594),
        "pin sylvestre" to CoefEmerge(0.37161, 1.75592, 0.00064, 0.84501, 0.05908, -3.00829, 0.03301),
        "pin maritime" to CoefEmerge(0.39581, 1.75592, -0.00186, 1.07374, 0.05706, -6.30588, 0.08522),
    )

    private fun normaliser(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    private fun coefEmerge(essence: String): CoefEmerge? {
        val n = normaliser(essence)
        EMERGE[n]?.let { return it }
        if (n.contains("resineux") || n.contains("conifere") || n.startsWith("pin") ||
            n.contains("epicea") || n.contains("sapin") || n.contains("cedre") || n.contains("if ")
        ) {
            EMERGE.entries.firstOrNull { n.contains(it.key) }?.let { return it.value }
            return EMERGE_RESINEUX
        }
        if (n.contains("feuillu")) return EMERGE_FEUILLUS
        EMERGE.entries.firstOrNull { n.startsWith(it.key) || n.contains(it.key) }?.let { return it.value }
        return null
    }

    /** Volume (m³) Schaeffer d'une tige de diamètre `dCm` selon le tarif et son numéro. */
    fun volume(tarif: TarifCubage, numero: Int, dCm: Double): Double {
        val m = 0.8 + 0.1 * numero
        val v = when (tarif) {
            TarifCubage.SCHAEFFER_RAPIDE -> m / 1400.0 * (dCm - 5.0) * (dCm - 10.0)
            TarifCubage.SCHAEFFER_LENT -> m / 1800.0 * dCm * (dCm - 5.0)
            else -> 0.0
        }
        return v.coerceAtLeast(0.0)
    }

    /** Volume bois fort tige (m³, découpe 7 cm) — modèle EMERGE pour une essence couverte. */
    fun volumeEmergeTige(essence: String, c130cm: Double, htotM: Double): Double {
        val k = coefEmerge(essence) ?: return 0.0
        return emergeTige(k, c130cm, htotM)
    }

    private fun emergeTige(k: CoefEmerge, c130cm: Double, htotM: Double): Double {
        if (htotM <= 1.4 || c130cm <= 0.0) return 0.0
        val hdec = 0.8 * htotM
        val logisHdec = ln(hdec / (htotM - hdec)) // = ln(4) avec hdec = 0,8·htot
        val c13 = c130cm / 100.0 // circonférence à 1,30 m, en m
        val cDec = DECOUPE_BOIS_FORT_CM * PI / 100.0 // circonférence de la découpe bois fort
        val hdn = sqrt(c13) / htotM
        val hsurd = htotM / c13
        val corr130 = 1.0 - 1.3 / htotM
        val volCyl = c13 * c13 * htotM / (4.0 * PI * corr130 * corr130)
        val formTot = k.a + k.b * hdn + k.c * hsurd
        var fct = k.d + k.e * logisHdec + k.f * hdn + k.g * (1.0 / c13)
        if (fct > 0.95) fct = 0.8
        val formTige = formTot * fct
        val decTige = if (c13 > cDec) 1.0 - (cDec * corr130 / c13).pow(3) else 0.0
        return (formTige * volCyl * decTige).coerceAtLeast(0.0)
    }

    /** Repli coefficient de forme : V = f·(π/4)·D²·H. */
    fun volumeForme(dCm: Double, hM: Double, f: Double = COEFFICIENT_FORME): Double {
        if (hM <= 0.0 || dCm <= 0.0) return 0.0
        val dM = dCm / 100.0
        return f * (PI / 4.0) * dM * dM * hM
    }

    /** Volume (m³) d'une tige individuelle selon le tarif du contexte, sa classe et sa hauteur saisie. */
    fun volumeUnitaireTige(contexte: Contexte, essence: String, classe: Int, hauteurTexte: String?): Double {
        if (contexte.tarif == TarifCubage.AUCUN) return 0.0
        val centre = classe + contexte.axe.pas / 2.0
        val circ = contexte.mode == ModeMesure.CIRCONFERENCE
        val dCm = if (circ) centre / PI else centre
        val cCm = if (circ) centre else centre * PI
        return when (contexte.tarif) {
            TarifCubage.SCHAEFFER_RAPIDE, TarifCubage.SCHAEFFER_LENT ->
                volume(contexte.tarif, contexte.tarifNumero, dCm)
            TarifCubage.EMERGE -> {
                val h = HauteurParser.parse(hauteurTexte ?: "").hauteurTotale
                when {
                    h == null -> 0.0
                    coefEmerge(essence) != null -> volumeEmergeTige(essence, cCm, h)
                    else -> volumeForme(dCm, h)
                }
            }
            TarifCubage.AUCUN -> 0.0
        }
    }

    /** Volume d'une tige sur sa classe seule (tarifs une entrée), pour compatibilité. */
    fun volumeUnitaire(contexte: Contexte, classe: Int): Double =
        volumeUnitaireTige(contexte, "", classe, null)

    /** Surface terrière (m²) d'une tige : g = π/4·D² (D = diamètre 1,30 m au centre de classe). */
    fun surfaceTerriereUnitaire(contexte: Contexte, classe: Int): Double {
        val centre = classe + contexte.axe.pas / 2.0
        val dCm = if (contexte.mode == ModeMesure.CIRCONFERENCE) centre / PI else centre
        val dM = dCm / 100.0
        return PI / 4.0 * dM * dM
    }
}
