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
}
