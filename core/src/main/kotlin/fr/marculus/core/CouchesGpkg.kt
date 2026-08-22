package fr.marculus.core

/** Rôle d'une table vecteur du GPKG, déduit de son nom. */
enum class TypeCouche {
    /** Parcelle forestière : rattachement spatial des tiges, dessin sur la carte. */
    PARCELLE,

    /** Houppier issu d'un MNH : estimation de la hauteur de la tige (attribut `h_max`). */
    HOUPPIER,

    /** Desserte : routes, pistes et chemins de la forêt (lignes), affichés sur la carte. */
    DESSERTE,
}

/**
 * Un GPKG de martelage porte plusieurs couches vecteur dont le **nom de table** dit le rôle.
 * Toute table non reconnue reste une **parcelle** : c'est le comportement historique, et un
 * GPKG ne contenant que des parcelles se lit exactement comme avant.
 *
 * Le classement par nom est volontairement borné à une liste fermée : deviner le rôle d'après la
 * géométrie serait pire (une desserte peut être surfacique, un houppier n'est pas distinguable
 * d'une parcelle par sa seule forme).
 */
object CouchesGpkg {

    private val HOUPPIER = setOf("houppier", "houppiers", "crown", "crowns")

    private val DESSERTE = setOf(
        "desserte", "dessertes", "voirie", "route", "routes",
        "piste", "pistes", "chemin", "chemins",
    )

    /** Attribut de hauteur d'un houppier, en mètres : nom canonique en tête, alias ensuite. */
    val ALIAS_HAUTEUR = listOf("h_max", "hmax", "hauteur_max", "hauteur", "height")

    /** Libellé d'une desserte (nom de la route ou de la piste). */
    val ALIAS_NOM = listOf("nom", "name", "libelle", "libellé", "toponyme")

    /** Nature d'une desserte (route empierrée, piste, cloisonnement…). */
    val ALIAS_TYPE = listOf("type", "nature", "categorie", "catégorie", "revetement", "revêtement")

    /** Rôle de la table `nom`, insensible à la casse et aux espaces de bordure. */
    fun type(nom: String): TypeCouche {
        val n = nom.trim().lowercase()
        return when {
            n in HOUPPIER -> TypeCouche.HOUPPIER
            n in DESSERTE -> TypeCouche.DESSERTE
            else -> TypeCouche.PARCELLE
        }
    }

    /** Première valeur non vide parmi les colonnes portant l'un des `alias` (insensible à la casse). */
    fun attribut(attributs: Map<String, String>, alias: List<String>): String? =
        alias.firstNotNullOfOrNull { a ->
            attributs.entries.firstOrNull { it.key.equals(a, ignoreCase = true) && it.value.isNotBlank() }?.value
        }
}
