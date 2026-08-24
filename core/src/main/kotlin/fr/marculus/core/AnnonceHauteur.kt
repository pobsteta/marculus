package fr.marculus.core

/**
 * Mise en mots d'un texte de hauteur pour l'annonce vocale. « 12-6A3AB » se dit
 * « 12 » **et** « 6 A, 3 A B » : la découpe fait partie de ce qui a été saisi, donc de ce qui
 * doit être relu — c'est la seule façon de vérifier à l'oreille ce que le décodeur a compris.
 *
 * Les lettres de qualité bois sont **détachées** (« A B » et non « AB ») : un synthéthiseur
 * français lit « ab » comme un mot, ce qui rend la relecture inutilisable pour la contrôler.
 */
object AnnonceHauteur {

    /** Hauteur totale, telle qu'on la dit : « 12-6A » → « 12 ». */
    fun totale(texte: String): String = texte.substringBefore('-').trim()

    /**
     * Découpe, telle qu'on la dit : « 12-6A3AB » → « 6 A, 3 A B ». `null` s'il n'y a pas de
     * découpe. Un texte de découpe que l'analyseur ne sait pas relire est rendu **tel quel**
     * plutôt que tu : mieux vaut une annonce maladroite qu'une annonce muette.
     */
    fun decoupe(texte: String): String? {
        val brut = texte.substringAfter('-', "").trim()
        if (brut.isEmpty()) return null
        val segments = HauteurParser.parse(texte).segments
        if (segments.isEmpty()) return brut
        return segments.joinToString(", ") { s ->
            longueur(s.longueur) + " " + s.qualiteBois.toCharArray().joinToString(" ")
        }
    }

    /** 6.0 → « 6 » ; 6.5 → « 6.5 ». Un entier ne se dit pas « six virgule zéro ». */
    private fun longueur(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}
