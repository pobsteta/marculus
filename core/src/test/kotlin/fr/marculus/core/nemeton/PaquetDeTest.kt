package fr.marculus.core.nemeton

import java.security.MessageDigest

/** Accès au paquet NEMETON embarqué (core/src/main/resources/nemeton/) et au jeu de contrôle. */
object PaquetDeTest {
    const val VERSION = "0.2.2-provisoire"

    /** Tolérance retenue : max(1e-6 relatif, demi-unité de la 8e décimale). Mesurée sur jeu_controle.csv le 2026-11-27 : 927 valeurs à 8 décimales, 94 à 7, 11 à 6 (zéros finaux omis) ; 1e-6 relatif seul ne couvre pas l'arrondi des valeurs inférieures à 0,005. */
    const val TOL_RELATIVE = 1e-6
    const val TOL_ABSOLUE = 5.0001e-9

    private fun octets(nom: String): ByteArray =
        PaquetDeTest::class.java.getResourceAsStream("/nemeton/$nom")?.use { it.readBytes() }
            ?: error("ressource /nemeton/$nom absente (paquet non embarqué)")

    val constantesOctets: ByteArray by lazy { octets("constantes.csv") }
    val jeuOctets: ByteArray by lazy { octets("jeu_controle.csv") }
    val manifesteOctets: ByteArray by lazy { octets("datapackage.json") }

    val constantes: String by lazy { constantesOctets.toString(Charsets.UTF_8) }
    val manifeste: String by lazy { manifesteOctets.toString(Charsets.UTF_8) }

    fun sha256(b: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    fun charger(): TarifNemeton = PaquetNemeton.charger(constantes, manifeste)

    /** Hash déclaré par le manifeste pour le fichier [chemin]. */
    fun hashDuManifeste(chemin: String): String =
        Regex("\"path\":\\s*\"" + Regex.escape(chemin) + "\",[^}]*?\"hash\":\\s*\"sha256:([0-9a-f]{64})\"", RegexOption.DOT_MATCHES_ALL)
            .find(manifeste)?.groupValues?.get(1) ?: error("hash de $chemin introuvable dans le manifeste")

    class Ligne(
        val essence: String,
        val code: String,
        val d: Double,
        val h: Double,
        val sortie: String,
        val texteValeur: String,
    ) {
        val valeur: Double get() = texteValeur.toDouble()
        val nom: String get() = "$essence code=$code D=$d H=$h $sortie"
    }

    /** Les 1 032 lignes du jeu de contrôle, dans l'ordre du fichier. */
    val jeu: List<Ligne> by lazy {
        jeuOctets.toString(Charsets.UTF_8).lines().drop(1).filter { it.isNotBlank() }.map {
            val c = it.split(",")
            check(c.size == 7) { "ligne mal formée : $it" }
            Ligne(c[0], c[1], c[2].toDouble(), c[3].toDouble(), c[4], c[5])
        }
    }

    fun tolerance(attendu: Double): Double =
        maxOf(TOL_RELATIVE * kotlin.math.abs(attendu), TOL_ABSOLUE)
}
