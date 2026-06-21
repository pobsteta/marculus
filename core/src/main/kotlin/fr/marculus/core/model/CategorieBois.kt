package fr.marculus.core.model

/**
 * Catégorie de grosseur des bois (seuils de diamètre à 1,30 m, convention forestière) :
 *  - PB  : petits bois        (diamètre < 27,5 cm)
 *  - BM  : bois moyens        (27,5 ≤ d < 47,5)
 *  - GB  : gros bois          (47,5 ≤ d < 67,5)
 *  - TGB : très gros bois     (d ≥ 67,5)
 *
 * En mode circonférence, la classe est convertie en diamètre (÷ π) avant le classement.
 */
enum class CategorieBois(val code: String, val libelle: String) {
    PB("PB", "Petits bois"),
    BM("BM", "Bois moyens"),
    GB("GB", "Gros bois"),
    TGB("TGB", "Très gros bois"),
    ;

    companion object {
        fun pour(classe: Int, mode: ModeMesure): CategorieBois {
            val diametre = if (mode == ModeMesure.CIRCONFERENCE) classe / Math.PI else classe.toDouble()
            return when {
                diametre < 27.5 -> PB
                diametre < 47.5 -> BM
                diametre < 67.5 -> GB
                else -> TGB
            }
        }
    }
}
