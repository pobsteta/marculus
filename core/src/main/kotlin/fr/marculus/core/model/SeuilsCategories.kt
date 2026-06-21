package fr.marculus.core.model

/**
 * Seuils (en diamètre, cm) délimitant les catégories de grosseur. Paramétrables.
 *  - PB  : diamètre < pbBm
 *  - BM  : pbBm ≤ d < bmGb
 *  - GB  : bmGb ≤ d < gbTgb
 *  - TGB : d ≥ gbTgb
 */
data class SeuilsCategories(
    val pbBm: Double = 27.5,
    val bmGb: Double = 47.5,
    val gbTgb: Double = 67.5,
) {
    fun categorie(classe: Int, mode: ModeMesure): CategorieBois {
        val diametre = if (mode == ModeMesure.CIRCONFERENCE) classe / Math.PI else classe.toDouble()
        return when {
            diametre < pbBm -> CategorieBois.PB
            diametre < bmGb -> CategorieBois.BM
            diametre < gbTgb -> CategorieBois.GB
            else -> CategorieBois.TGB
        }
    }

    companion object {
        val DEFAUT = SeuilsCategories()
    }
}
