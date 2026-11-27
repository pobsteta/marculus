package fr.marculus.core.nemeton

import java.security.MessageDigest

/**
 * Chargement du paquet NEMETON (mission 039) : `constantes.csv` + manifeste `datapackage.json`, tels que livrés par
 * le dépôt `tarif-nemeton` (`code/paquet_livrable.py`) et embarqués octet pour octet dans
 * `core/src/main/resources/nemeton/`.
 *
 * Le chargement :
 * 1. vérifie que le SHA-256 des octets UTF-8 de [charger]`(constantes)` est le `hash` (`sha256:…`) que le manifeste
 *    déclare pour la ressource de `path` `constantes.csv` ; sinon [IllegalStateException] ;
 * 2. lit la version (clé `version` de premier niveau du manifeste), jamais écrite dans le code ;
 * 3. valide les constantes : en-tête, 7 colonnes, valeurs numériques finies, aucune clé (code ou groupe, modèle,
 *    terme) en double, un seul libellé d'essence par code, et pour chaque code couvert le jeu complet de constantes
 *    de son type (voir [TarifNemeton]) ; sinon [IllegalArgumentException].
 *
 * Un paquet incomplet est donc refusé au chargement, pas au premier arbre.
 */
object PaquetNemeton {

    /** Nom de la ressource des constantes dans le manifeste. */
    const val CHEMIN_CONSTANTES = "constantes.csv"

    private val EN_TETE = listOf("essence", "code_ifn", "modele", "terme", "valeur", "unite", "source")

    fun charger(constantes: String, manifeste: String): TarifNemeton {
        val m = LectureJsonNemeton.lire(manifeste) as? Map<*, *>
            ?: throw IllegalArgumentException("manifeste : objet JSON attendu")
        val attendu = hashDeclare(m)
        val obtenu = sha256(constantes.toByteArray(Charsets.UTF_8))
        check(obtenu == attendu) {
            "empreinte de $CHEMIN_CONSTANTES : $obtenu, le manifeste déclare $attendu (paquet altéré ou incomplet)"
        }
        val version = m["version"] as? String
            ?: throw IllegalArgumentException("manifeste : clé « version » absente ou non textuelle")
        require(version.isNotBlank()) { "manifeste : version vide" }
        return construire(version, constantes)
    }

    internal fun sha256(octets: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(octets).joinToString("") { "%02x".format(it) }

    private fun hashDeclare(m: Map<*, *>): String {
        val ressources = m["resources"] as? List<*>
            ?: throw IllegalArgumentException("manifeste : tableau « resources » absent")
        val r = ressources.filterIsInstance<Map<*, *>>().filter { it["path"] == CHEMIN_CONSTANTES }
        require(r.size == 1) { "manifeste : ${r.size} ressource(s) de path $CHEMIN_CONSTANTES (1 attendue)" }
        val h = r[0]["hash"] as? String
            ?: throw IllegalArgumentException("manifeste : hash de $CHEMIN_CONSTANTES absent")
        require(h.startsWith("sha256:")) { "manifeste : hash de $CHEMIN_CONSTANTES non SHA-256 ($h)" }
        val hex = h.removePrefix("sha256:").lowercase()
        require(hex.length == 64 && hex.all { it in '0'..'9' || it in 'a'..'f' }) { "manifeste : hash malformé ($h)" }
        return hex
    }

    internal fun construire(version: String, constantes: String): TarifNemeton {
        val lignes = LectureCsvNemeton.lire(constantes)
        require(lignes.isNotEmpty() && lignes[0] == EN_TETE) { "constantes : en-tête attendu $EN_TETE" }
        val valeurs = HashMap<Cle, Double>()
        val essences = LinkedHashMap<String, String>()
        for ((n, l) in lignes.withIndex().drop(1)) {
            require(l.size == EN_TETE.size) { "constantes l. ${n + 1} : ${l.size} colonnes (7 attendues)" }
            val (essence, code, modele, terme, texte) = l
            require(essence.isNotEmpty() && modele.isNotEmpty() && terme.isNotEmpty()) { "constantes l. ${n + 1} : champ vide" }
            val v = texte.toDoubleOrNull()
            require(v != null && v.isFinite()) { "constantes l. ${n + 1} : valeur « $texte » non numérique" }
            // Clé de calcul : le code IFN pour une ligne d'essence, le nom du groupe (feuillu, conifere, tous) sinon.
            val cle = Cle(if (code.isNotEmpty()) code else GROUPE + essence, modele, terme)
            require(valeurs.put(cle, v) == null) { "constantes l. ${n + 1} : constante en double $cle" }
            if (code.isNotEmpty()) {
                val deja = essences.putIfAbsent(code, essence)
                require(deja == null || deja == essence) { "constantes : code $code porte deux libellés ($deja, $essence)" }
            }
        }
        return TarifNemeton(version, valeurs, essences)
    }

    /** Préfixe des clés de groupe, pour qu'un nom de groupe ne puisse jamais valoir un code IFN. */
    internal const val GROUPE = "#"
}

/** Clé d'une constante : code IFN (ou `#` + groupe), modèle, terme. */
internal data class Cle(val cle: String, val modele: String, val terme: String)
