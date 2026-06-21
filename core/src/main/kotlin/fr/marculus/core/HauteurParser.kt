package fr.marculus.core

/** Un segment de découpe : une longueur associée à une qualité bois (ex. 6 m de « AB »). */
data class SegmentDecoupe(val longueur: Double, val qualiteBois: String)

/** Résultat de l'analyse, tolérante, du texte de hauteur. Le texte brut est toujours conservé. */
data class HauteurParsee(
    val brut: String,
    val hauteurTotale: Double?,
    val segments: List<SegmentDecoupe>,
)

/**
 * Analyse le texte de hauteur saisi librement : la hauteur totale d'abord, puis (optionnel)
 * le séparateur « - » et une découpe en segments « longueur + qualité bois ».
 *
 *  - « 27 »          → 27 m, aucune découpe
 *  - « 27-6AB4CD »   → 27 m, dont 6 m de qualité « AB » et 4 m de qualité « CD »
 *
 * Aucune validation de cohérence : un texte non interprétable est conservé tel quel
 * (hauteurTotale nulle, segments vides).
 */
object HauteurParser {
    private val nombreEnTete = Regex("""^\s*(\d+(?:[.,]\d+)?)""")
    private val segment = Regex("""(\d+(?:[.,]\d+)?)\s*([A-Za-z]+)""")

    fun parse(texte: String): HauteurParsee {
        val brut = texte.trim()
        val sep = brut.indexOf('-')
        val partieHauteur = if (sep >= 0) brut.substring(0, sep) else brut
        val partieDecoupe = if (sep >= 0) brut.substring(sep + 1) else ""

        val totale = nombreEnTete.find(partieHauteur)
            ?.groupValues?.get(1)
            ?.replace(',', '.')
            ?.toDoubleOrNull()

        val segments = segment.findAll(partieDecoupe).map { m ->
            SegmentDecoupe(
                longueur = m.groupValues[1].replace(',', '.').toDouble(),
                qualiteBois = m.groupValues[2].uppercase(),
            )
        }.toList()

        return HauteurParsee(brut = brut, hauteurTotale = totale, segments = segments)
    }
}
