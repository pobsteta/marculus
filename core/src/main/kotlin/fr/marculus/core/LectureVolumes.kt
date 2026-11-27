package fr.marculus.core

/**
 * Relecture des totaux de volume d'un ancien fichier (mission 094, décision de Pascal du 2026-11-27 :
 * « renommer les colonnes et relire l'ancien nom »).
 *
 * Avant la correction EMERGE, `VolumeTotalM3` (CSV, format 8 et antérieurs) et `volumeTotalM3`
 * (sauvegarde `.marsync`) désignaient **tige + houppier à la découpe de 7 cm**, ce que le fichier
 * nouveau appelle `VolumeBoisFortTotalM3` / `volumeBoisFortTotalM3`. L'ancien nom est donc lu comme
 * bois fort total, jamais comme volume aérien total (absent des anciens fichiers).
 */
object LectureVolumes {

    private const val NOM_BOIS_FORT = "VolumeBoisFortTotalM3"
    private const val NOM_ANCIEN = "VolumeTotalM3"
    private const val NOM_AERIEN = "VolumeAerienTotalM3"

    /** Champs « clé;valeur » de l'en-tête d'un CSV de contexte (jusqu'à la première ligne vide). */
    fun entete(csv: String): Map<String, String> {
        val champs = LinkedHashMap<String, String>()
        for (ligne in csv.lineSequence()) {
            if (ligne.isBlank()) break
            val i = ligne.indexOf(';')
            if (i > 0) champs.putIfAbsent(ligne.substring(0, i), ligne.substring(i + 1))
        }
        return champs
    }

    /** Bois fort total (tige + houppier, découpe 7 cm), sous son nom actuel ou, à défaut, l'ancien ; la casse de la première lettre est indifférente (JSON). */
    fun boisFortTotal(champs: Map<String, String>): Double? =
        lire(champs, NOM_BOIS_FORT) ?: lire(champs, NOM_ANCIEN)

    /** Volume aérien total EMERGE ; null dans un fichier d'avant la correction, ou sans EMERGE. */
    fun aerienTotal(champs: Map<String, String>): Double? = lire(champs, NOM_AERIEN)

    private fun lire(champs: Map<String, String>, nom: String): Double? =
        champs.entries.firstOrNull { it.key.equals(nom, ignoreCase = true) }?.value?.trim()?.toDoubleOrNull()
}
