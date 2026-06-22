package fr.marculus.core

import fr.marculus.core.model.Contexte
import fr.marculus.core.model.ModeMesure
import fr.marculus.core.model.TarifCubage
import kotlin.math.PI

/**
 * Cubage par tarif à une entrée de Schaeffer (volume bois fort tige), vérifié contre les
 * tables ONF. D = diamètre à 1,30 m en cm ; M = volume de l'arbre de 45 cm = 0,8 + 0,1·N.
 *
 *  - Rapide : V = (M/1400)·(D−5)·(D−10)
 *  - Lent   : V = (M/1800)·D·(D−5)
 *
 * Volume négatif (très petits diamètres) ramené à 0.
 */
object Cubage {

    /** Volume (m³) d'une tige de diamètre `dCm` (cm à 1,30 m) selon le tarif et son numéro. */
    fun volume(tarif: TarifCubage, numero: Int, dCm: Double): Double {
        if (tarif == TarifCubage.AUCUN) return 0.0
        val m = 0.8 + 0.1 * numero
        val v = when (tarif) {
            TarifCubage.SCHAEFFER_RAPIDE -> m / 1400.0 * (dCm - 5.0) * (dCm - 10.0)
            TarifCubage.SCHAEFFER_LENT -> m / 1800.0 * dCm * (dCm - 5.0)
            TarifCubage.AUCUN -> 0.0
        }
        return v.coerceAtLeast(0.0)
    }

    /**
     * Volume (m³) d'une tige d'une classe donnée, en prenant le centre de classe et en
     * convertissant la circonférence en diamètre (D = C/π) si le contexte est en circonférence.
     */
    fun volumeUnitaire(contexte: Contexte, classe: Int): Double {
        if (contexte.tarif == TarifCubage.AUCUN) return 0.0
        val centre = classe + contexte.axe.pas / 2.0
        val dCm = if (contexte.mode == ModeMesure.CIRCONFERENCE) centre / PI else centre
        return volume(contexte.tarif, contexte.tarifNumero, dCm)
    }
}
