package fr.marculus.core

import fr.marculus.core.model.SatelliteGsv

/**
 * Reconstitue la liste complète des satellites en vue à partir des trames **GSV**, qui arrivent
 * en séquences (plusieurs messages) et par **constellation** (GP, GL, GA, GB…). Une séquence
 * démarre au message 1 et se termine au message `nbMessages` ; chaque constellation est suivie
 * indépendamment. Pur, sans état partagé global → testable en JVM.
 */
class AccumulateurSkyplot {
    private val enCours = HashMap<String, MutableList<SatelliteGsv>>()
    private val complets = HashMap<String, List<SatelliteGsv>>()

    /** Intègre une trame GSV ; met à jour la liste complète de sa constellation quand la séquence finit. */
    fun pousser(trame: TrameGsv) {
        val buffer = if (trame.numMessage <= 1) {
            mutableListOf<SatelliteGsv>().also { enCours[trame.systeme] = it }
        } else {
            enCours.getOrPut(trame.systeme) { mutableListOf() }
        }
        buffer.addAll(trame.satellites)
        if (trame.numMessage >= trame.nbMessages) {
            complets[trame.systeme] = buffer.toList()
        }
    }

    /** Satellites en vue, toutes constellations confondues (dernières séquences complètes). */
    fun satellites(): List<SatelliteGsv> = complets.values.flatten()
}
