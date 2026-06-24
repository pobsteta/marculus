package fr.marculus.core

/**
 * Reconstitue des trames NMEA complètes à partir d'un flux reçu **par morceaux** (lecture
 * Bluetooth ou réseau fragmentée) : accumule les octets jusqu'aux délimiteurs CR/LF et restitue
 * chaque ligne terminée. Les fragments incomplets sont conservés pour le morceau suivant.
 * État interne minimal, sans E/S → testable en JVM.
 */
class NmeaDecoupeur(private val tailleMax: Int = 1024) {
    private val tampon = StringBuilder()

    /**
     * Ajoute un morceau reçu et renvoie les trames qu'il **termine** (sans les délimiteurs).
     * Un `$` redémarre une trame (resynchronisation : tout fragment partiel en cours est abandonné).
     * Une ligne dépassant [tailleMax] sans fin de ligne est vidée (garde-fou anti-emballement sur
     * un flux binaire ou corrompu).
     */
    fun pousser(morceau: String): List<String> {
        val trames = mutableListOf<String>()
        for (c in morceau) {
            when (c) {
                '\n', '\r' -> if (tampon.isNotEmpty()) {
                    trames += tampon.toString()
                    tampon.setLength(0)
                }
                '$' -> { tampon.setLength(0); tampon.append('$') }
                else -> {
                    tampon.append(c)
                    if (tampon.length > tailleMax) tampon.setLength(0)
                }
            }
        }
        return trames
    }
}
