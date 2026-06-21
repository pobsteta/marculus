package fr.marculus.core

/** Listes prédéfinies mais modifiables par l'utilisateur (valeurs par défaut au premier lancement). */
object Referentiels {
    val ESSENCES_DEFAUT: List<String> = listOf(
        "Chêne", "Hêtre", "Autres feuillus", "Sapin", "Épicéa", "Autres résineux",
    )

    val QUALITE_ARBRE_DEFAUT: List<String> = listOf(
        "Sec", "Chablis", "Volis", "Malade",
    )

    /** Qualités bois : lettres simples et combinaisons (employées dans le texte de hauteur). */
    val QUALITE_BOIS_DEFAUT: List<String> = listOf(
        "A", "B", "C", "D", "AB", "BC", "CD",
    )

    /** Couleurs de cellule par défaut (ARGB) : fond bleu, texte blanc, comme l'app de référence. */
    const val COULEUR_FOND_DEFAUT: Int = 0xFF1FA0EC.toInt()
    const val COULEUR_TEXTE_DEFAUT: Int = 0xFFFFFFFF.toInt()

    /** Palette proposée pour le choix des couleurs (ordre proche de l'app de référence). */
    val PALETTE: List<Int> = listOf(
        0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF1E88E5,
        0xFF1FA0EC, 0xFF00ACC1, 0xFF00897B, 0xFF43A047, 0xFF7CB342, 0xFFC0CA33,
        0xFFFDD835, 0xFFFFB300, 0xFFFB8C00, 0xFFF4511E, 0xFF6D4C41, 0xFF757575,
        0xFF000000, 0xFFFFFFFF,
    ).map { it.toInt() }
}
