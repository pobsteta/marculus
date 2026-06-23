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

    // Normalisation identique à celle utilisée pour générer EMERGE_COEFS (EmergeCoefs.kt) :
    // minuscules, sans accent, caractères non alphanumériques → espace, espaces compactés.
    private fun normaliser(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex(" +"), " ")
            .trim()

    private fun coefEmerge(essence: String): CoefEmerge? {
        val n = normaliser(essence)
        if (n.isEmpty()) return null
        EMERGE_COEFS[n]?.let { return it }
        // Correspondance partielle : l'essence saisie commence par / est préfixe d'un nom de la table.
        EMERGE_COEFS.entries.firstOrNull { it.key.startsWith(n) || n.startsWith(it.key) }?.let { return it.value }
        EMERGE_COEFS.entries.firstOrNull { it.key.contains(n) || n.contains(it.key) }?.let { return it.value }
        // Repli par grand groupe.
        if (n.contains("resineux") || n.contains("conifere")) return EMERGE_RESINEUX
        if (n.contains("feuillu")) return EMERGE_FEUILLUS
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
                    else -> volumeForme(dCm, h, contexte.coefficientForme)
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
