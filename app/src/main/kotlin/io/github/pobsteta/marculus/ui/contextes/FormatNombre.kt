package io.github.pobsteta.marculus.ui.contextes

import java.text.DecimalFormatSymbols
import java.util.Locale

/** Pourcentage sans zéros inutiles, au séparateur de la langue : 30.0 → « 30 », 7.41 → « 7,41 » (fr) ou « 7.41 » (en). */
internal fun formatPourcent(v: Double, locale: Locale = Locale.getDefault()): String = formatDecimal(v, 2, locale)

/**
 * Nombre sans zéros inutiles, arrondi à [decimales], au séparateur de la langue :
 * 6.6667 → « 6,667 » (fr, 3) ou « 6.667 » (en), 10.0 → « 10 ». La saisie accepte les deux séparateurs
 * (voir [lireDecimal]).
 */
internal fun formatDecimal(v: Double, decimales: Int = 2, locale: Locale = Locale.getDefault()): String {
    val brut = "%.${decimales}f".format(Locale.ROOT, v)
    val net = if ('.' in brut) brut.trimEnd('0').trimEnd('.') else brut
    return net.replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)
}

/** Lit une saisie décimale avec point ou virgule ; null si illisible. */
internal fun lireDecimal(saisie: String): Double? = saisie.replace(',', '.').toDoubleOrNull()
