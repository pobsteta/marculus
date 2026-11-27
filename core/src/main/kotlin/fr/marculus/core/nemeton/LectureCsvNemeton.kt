package fr.marculus.core.nemeton

/**
 * Lecteur CSV RFC 4180 minimal (séparateur virgule, guillemets doubles, `""` pour un guillemet, retour à la ligne
 * permis dans un champ entre guillemets). Lignes terminées par LF ou CRLF ; une ligne vide finale est ignorée.
 */
internal object LectureCsvNemeton {

    fun lire(texte: String): List<List<String>> {
        val lignes = mutableListOf<List<String>>()
        var champs = mutableListOf<String>()
        val champ = StringBuilder()
        var entreGuillemets = false
        var ligneEntamee = false
        var i = 0
        while (i < texte.length) {
            val c = texte[i]
            if (entreGuillemets) {
                if (c == '"') {
                    if (i + 1 < texte.length && texte[i + 1] == '"') {
                        champ.append('"'); i++
                    } else {
                        entreGuillemets = false
                    }
                } else {
                    champ.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        require(champ.isEmpty()) { "guillemet au milieu d'un champ (caractère $i)" }
                        entreGuillemets = true; ligneEntamee = true
                    }
                    ',' -> { champs.add(champ.toString()); champ.clear(); ligneEntamee = true }
                    '\r' -> {}
                    '\n' -> {
                        champs.add(champ.toString()); champ.clear()
                        lignes.add(champs); champs = mutableListOf(); ligneEntamee = false
                    }
                    else -> { champ.append(c); ligneEntamee = true }
                }
            }
            i++
        }
        require(!entreGuillemets) { "guillemet non fermé en fin de fichier" }
        if (ligneEntamee || champ.isNotEmpty()) {
            champs.add(champ.toString()); lignes.add(champs)
        }
        return lignes
    }
}
