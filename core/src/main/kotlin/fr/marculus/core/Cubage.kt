package fr.marculus.core

import fr.marculus.core.model.Contexte
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import java.text.Normalizer
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Cubage des tiges.
 *
 *  - **Schaeffer** (tarif à une entrée, diamètre) : V = (M/1400)(D−5)(D−10) [rapide] ou
 *    V = (M/1800)·D·(D−5) [lent], M = 0,8 + 0,1·N, D = diamètre 1,30 m (cm). Vérifié / tables ONF.
 *  - **EMERGE** (tarif à deux entrées, projet ANR/IGN, volume total aérien) :
 *    V = ( H·c²/(4π(1−1,3/H)²) )·( A + B·√c/H + C·H/c ), c = circonférence 1,30 m (m), H hauteur (m),
 *    A/B/C par essence (B commun feuillus 0,661 / résineux 1,756). Source : Deleuze et al. 2014.
 *    Repli **coefficient de forme** V = f·(π/4)·D²·H (f = 0,5) si essence non couverte ;
 *    une tige sans hauteur n'est pas cubable en deux entrées (volume 0).
 */
object Cubage {

    /** Coefficient de forme par défaut pour le repli deux entrées. */
    const val COEFFICIENT_FORME = 0.5

    private data class CoefEmerge(val a: Double, val b: Double, val c: Double)

    private val EMERGE_FEUILLUS = CoefEmerge(0.522, 0.661, -0.002)
    private val EMERGE_RESINEUX = CoefEmerge(0.356, 1.756, 0.002)

    // Clés normalisées (minuscule, sans accent). Source : VolEmerge (DataForet / projet EMERGE).
    private val EMERGE = mapOf(
        "chene" to CoefEmerge(0.561, 0.661, -0.002),
        "chene sessile" to CoefEmerge(0.561, 0.661, -0.002),
        "chene pedoncule" to CoefEmerge(0.561, 0.661, -0.002),
        "chene rouge" to CoefEmerge(0.511, 0.661, -0.002),
        "hetre" to CoefEmerge(0.542, 0.661, -0.002),
        "charme" to CoefEmerge(0.533, 0.661, -0.001),
        "frene" to CoefEmerge(0.509, 0.661, -0.001),
        "bouleau" to CoefEmerge(0.493, 0.661, -0.002),
        "sapin" to CoefEmerge(0.360, 1.756, 0.003),
        "sapin pectine" to CoefEmerge(0.398, 1.756, 0.002),
        "sapin de nordmann" to CoefEmerge(0.375, 1.756, 0.002),
        "epicea" to CoefEmerge(0.303, 1.756, 0.004),
        "epicea commun" to CoefEmerge(0.303, 1.756, 0.004),
        "douglas" to CoefEmerge(0.235, 1.756, 0.004),
        "meleze" to CoefEmerge(0.377, 1.756, 0.001),
        "meleze d'europe" to CoefEmerge(0.377, 1.756, 0.001),
        "cedre" to CoefEmerge(0.340, 1.756, 0.002),
        "pin sylvestre" to CoefEmerge(0.372, 1.756, 0.001),
        "pin maritime" to CoefEmerge(0.396, 1.756, -0.002),
        "pin noir" to CoefEmerge(0.305, 1.756, 0.003),
        "pin laricio" to CoefEmerge(0.306, 1.756, 0.003),
        "pin d'alep" to CoefEmerge(0.403, 1.756, 0.001),
        "pin weymouth" to CoefEmerge(0.356, 1.756, 0.001),
        "pin a crochets" to CoefEmerge(0.443, 1.756, -0.001),
    )

    private fun normaliser(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")

    private fun coefEmerge(essence: String): CoefEmerge? {
        val n = normaliser(essence)
        EMERGE[n]?.let { return it }
        if (n.contains("resineux") || n.contains("conifere")) return EMERGE_RESINEUX
        if (n.contains("feuillu")) return EMERGE_FEUILLUS
        // Reconnaissance partielle (ex. « chêne pubescent » → chêne).
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

    /** Volume total aérien (m³) EMERGE pour une circonférence `cCm` (cm) et une hauteur `hM` (m). */
    fun volumeEmerge(essence: String, cCm: Double, hM: Double): Double {
        if (hM <= 1.3 || cCm <= 0.0) return 0.0
        val cM = cCm / 100.0
        val k = coefEmerge(essence) ?: return 0.0
        val base = hM * cM * cM / (4.0 * PI * (1.0 - 1.3 / hM).pow(2))
        return (base * (k.a + k.b * (sqrt(cM) / hM) + k.c * (hM / cM))).coerceAtLeast(0.0)
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
                    coefEmerge(essence) != null -> volumeEmerge(essence, cCm, h)
                    else -> volumeForme(dCm, h)
                }
            }
            TarifCubage.AUCUN -> 0.0
        }
    }

    /** Volume d'une tige sur sa classe seule (tarifs une entrée), pour compatibilité. */
    fun volumeUnitaire(contexte: Contexte, classe: Int): Double =
        volumeUnitaireTige(contexte, "", classe, null)
}
