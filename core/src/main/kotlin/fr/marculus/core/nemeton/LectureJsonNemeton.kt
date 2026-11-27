package fr.marculus.core.nemeton

/**
 * Lecteur JSON minimal (RFC 8259) pour le manifeste `datapackage.json` du paquet NEMETON, sans dépendance.
 * Objet -> `Map<String, Any?>`, tableau -> `List<Any?>`, chaîne -> `String`, nombre -> `Double`,
 * `true`/`false` -> `Boolean`, `null` -> `null`.
 */
internal class LectureJsonNemeton private constructor(private val s: String) {
    private var i = 0

    companion object {
        fun lire(texte: String): Any? {
            val l = LectureJsonNemeton(texte)
            l.blancs()
            val v = l.valeur()
            l.blancs()
            require(l.i == texte.length) { "JSON : contenu après la valeur (caractère ${l.i})" }
            return v
        }
    }

    private fun blancs() {
        while (i < s.length && s[i] in " \t\r\n") i++
    }

    private fun attendre(c: Char) {
        require(i < s.length && s[i] == c) { "JSON : '$c' attendu (caractère $i)" }
        i++
    }

    private fun valeur(): Any? {
        require(i < s.length) { "JSON : fin inattendue" }
        return when (val c = s[i]) {
            '{' -> objet()
            '[' -> tableau()
            '"' -> chaine()
            't' -> mot("true", true)
            'f' -> mot("false", false)
            'n' -> mot("null", null)
            else -> {
                require(c == '-' || c.isDigit()) { "JSON : valeur inattendue '$c' (caractère $i)" }
                nombre()
            }
        }
    }

    private fun mot(m: String, v: Any?): Any? {
        require(s.startsWith(m, i)) { "JSON : '$m' attendu (caractère $i)" }
        i += m.length
        return v
    }

    private fun objet(): Map<String, Any?> {
        attendre('{')
        val m = LinkedHashMap<String, Any?>()
        blancs()
        if (i < s.length && s[i] == '}') { i++; return m }
        while (true) {
            blancs()
            val k = chaine()
            blancs(); attendre(':'); blancs()
            require(k !in m) { "JSON : clé '$k' en double" }
            m[k] = valeur()
            blancs()
            require(i < s.length) { "JSON : objet non fermé" }
            if (s[i] == ',') { i++; continue }
            attendre('}')
            return m
        }
    }

    private fun tableau(): List<Any?> {
        attendre('[')
        val l = mutableListOf<Any?>()
        blancs()
        if (i < s.length && s[i] == ']') { i++; return l }
        while (true) {
            blancs()
            l.add(valeur())
            blancs()
            require(i < s.length) { "JSON : tableau non fermé" }
            if (s[i] == ',') { i++; continue }
            attendre(']')
            return l
        }
    }

    private fun chaine(): String {
        attendre('"')
        val b = StringBuilder()
        while (true) {
            require(i < s.length) { "JSON : chaîne non fermée" }
            val c = s[i++]
            when {
                c == '"' -> return b.toString()
                c == '\\' -> {
                    require(i < s.length) { "JSON : échappement incomplet" }
                    when (val e = s[i++]) {
                        '"' -> b.append('"'); '\\' -> b.append('\\'); '/' -> b.append('/')
                        'b' -> b.append('\b'); 'f' -> b.append('\u000C'); 'n' -> b.append('\n')
                        'r' -> b.append('\r'); 't' -> b.append('\t')
                        'u' -> {
                            require(i + 4 <= s.length) { "JSON : \\u incomplet" }
                            b.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4
                        }
                        else -> throw IllegalArgumentException("JSON : échappement inconnu \\$e")
                    }
                }
                c < ' ' -> throw IllegalArgumentException("JSON : caractère de contrôle dans une chaîne")
                else -> b.append(c)
            }
        }
    }

    private fun nombre(): Double {
        val debut = i
        while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
        return s.substring(debut, i).toDoubleOrNull()
            ?: throw IllegalArgumentException("JSON : nombre invalide '${s.substring(debut, i)}'")
    }
}
