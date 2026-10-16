package fr.marculus.core

import fr.marculus.core.model.TarifCubage
import java.text.Normalizer
import kotlin.math.roundToInt

/**
 * Numéros de tarif Schaeffer proposés à partir de l'Inventaire forestier national, par
 * sylvoécorégion (SER), avec repli sur la grande région écologique (GRECO) puis la France.
 * Voir `docs/specs/2026-10-16-tarifs-ser.md`.
 *
 * Les tables sont produites par nemeton (`data-raw/build_ifn_tarifs_schaeffer.R`, méthode
 * PPtools) et lues ici depuis leur texte : ce module ne touche ni aux fichiers ni à Android.
 * L'essence d'un contexte est rapprochée d'une ligne IFN par le code ONF de son nom
 * ([Cubage.codeEssence]), appliqué aux deux côtés : pas de nouvelle table de noms.
 */
class TarifsIfn private constructor(
    private val lignes: Map<Cle, LigneTarif>,
    private val communesParInsee: Map<String, List<SerPart>>,
    private val communes: List<Commune>,
    private val sers: Map<String, Ser>,
    /** Millésime des tables (ex. `IFN 2005-2025`), plusieurs séparés par `, ` ; vide si aucun. */
    val millesime: String,
) {
    // Noms normalisés une fois pour toutes : la recherche tourne à chaque frappe sur ~35 000 communes.
    private val nomsNormalises: List<String> = communes.map { normaliser(it.nom) }

    /** Niveau d'agrégation d'une proposition, du plus local au plus large. */
    enum class Niveau { SER, GRECO, FRANCE }

    /** Type de tarif des tables IFN et son équivalent Marculus. */
    enum class TypeIfn(val tarif: TarifCubage) {
        SCHR(TarifCubage.SCHAEFFER_RAPIDE),
        SCHI(TarifCubage.SCHAEFFER_INTERMEDIAIRE),
        SCHL(TarifCubage.SCHAEFFER_LENT),
        SCHTL(TarifCubage.SCHAEFFER_TRES_LENT),
        ;

        companion object {
            fun de(tarif: TarifCubage): TypeIfn? = entries.firstOrNull { it.tarif == tarif }
        }
    }

    /** Part [part] (0..1) de la surface d'une commune située dans la SER [ser]. */
    data class SerPart(val ser: String, val part: Double, val greco: String)

    data class Commune(val insee: String, val nom: String, val departement: String)

    data class Ser(val code: String, val nom: String, val greco: String)

    /**
     * Numéro proposé : [numero] est [numeroBrut] arrondi, borné à 1..20. [code] est celui du
     * [niveau] retenu (code SER, lettre GRECO ou `FR`) ; [cv] en %.
     */
    data class PropositionTarif(
        val numero: Int,
        val numeroBrut: Double,
        val niveau: Niveau,
        val code: String,
        val nArbres: Int,
        val cv: Double,
        val millesime: String,
    )

    private data class Cle(val niveau: Niveau, val code: String, val essence: String, val type: TypeIfn)

    private data class LigneTarif(val numero: Double, val nArbres: Int, val cv: Double, val millesime: String)

    /** SER de la commune, de la plus grande part à la plus petite ; vide si commune inconnue. */
    fun serDeCommune(insee: String): List<SerPart> = communesParInsee[insee.trim()].orEmpty()

    fun commune(insee: String): Commune? = communes.firstOrNull { it.insee == insee.trim() }

    fun ser(code: String): Ser? = sers[code]

    /**
     * Communes dont le nom commence par [saisie] (accents, casse et ponctuation ignorés), puis
     * celles qui le contiennent ; au plus [max]. Un code INSEE complet trouve sa commune.
     */
    fun rechercherCommunes(saisie: String, max: Int = 20): List<Commune> {
        val n = normaliser(saisie)
        if (n.isEmpty()) return emptyList()
        val parInsee = communes.filter { it.insee == saisie.trim() }
        val debut = communes.indices.filter { nomsNormalises[it].startsWith(n) }.map { communes[it] }
        val dedans = communes.indices.filter { nomsNormalises[it].contains(n) }.map { communes[it] }
        return (parInsee + debut + dedans).distinct().take(max)
    }

    /**
     * Communes portant exactement le nom [nom] (normalisé) : sert à reconnaître la commune d'une
     * parcelle du GeoPackage, qui ne porte qu'un nom. Plusieurs résultats = homonymes.
     */
    fun communesNommees(nom: String): List<Commune> {
        val n = normaliser(nom)
        return if (n.isEmpty()) emptyList() else communes.indices.filter { nomsNormalises[it] == n }.map { communes[it] }
    }

    /**
     * Proposition pour [essence] dans la SER [ser] au tarif [tarif] : la ligne SER si elle compte au
     * moins [SEUIL_ARBRES] arbres, sinon celle de sa GRECO, sinon celle de la France, sinon null.
     * Null aussi pour un tarif sans numéro (AUCUN, EMERGE) ou une essence sans code ONF.
     */
    fun proposer(ser: String, essence: String, tarif: TarifCubage): PropositionTarif? {
        val type = TypeIfn.de(tarif) ?: return null
        val code = Cubage.codeEssence(essence) ?: return null
        val greco = sers[ser]?.greco ?: ser.take(1)
        return listOf(Niveau.SER to ser, Niveau.GRECO to greco, Niveau.FRANCE to CODE_FRANCE)
            .firstNotNullOfOrNull { (niveau, c) ->
                lignes[Cle(niveau, c, code, type)]?.takeIf { it.nArbres >= SEUIL_ARBRES }?.let { l ->
                    PropositionTarif(
                        numero = l.numero.roundToInt().coerceIn(NUMERO_MIN, NUMERO_MAX),
                        numeroBrut = l.numero,
                        niveau = niveau,
                        code = c,
                        nArbres = l.nArbres,
                        cv = l.cv,
                        millesime = l.millesime,
                    )
                }
            }
    }

    /**
     * Type de Schaeffer au plus faible CV pour [essence] dans [ser] (chaque type avec son propre
     * repli), ou null si aucun n'a de proposition. Indicatif : rien n'est imposé.
     */
    fun typeConseille(ser: String, essence: String): TarifCubage? =
        TypeIfn.entries
            .mapNotNull { t -> proposer(ser, essence, t.tarif)?.let { t.tarif to it.cv } }
            .minByOrNull { it.second }?.first

    companion object {
        /** En dessous, l'échantillon d'un niveau est trop maigre : on passe au niveau supérieur. */
        const val SEUIL_ARBRES = 30
        const val CODE_FRANCE = "FR"
        private const val NUMERO_MIN = 1
        private const val NUMERO_MAX = 20

        /**
         * Lit les trois tables (séparateur `;`, en-tête obligatoire, colonnes trouvées par leur nom) :
         *  - [tarifsCsv] `niveau;code;espar;essence_libelle;type;numero;n_arbres;cv;millesime` ;
         *  - [communesCsv] `insee;nom;departement;ser;part;greco` ;
         *  - [serCsv] `code;nom;greco`.
         * Les lignes illisibles sont ignorées. À deux lignes pour un même code ONF (deux espèces IFN
         * rapprochées du même code), la plus fournie en arbres l'emporte.
         */
        fun charger(tarifsCsv: String, communesCsv: String, serCsv: String): TarifsIfn {
            val lignes = mutableMapOf<Cle, LigneTarif>()
            val codes = mutableMapOf<String, String?>() // ~180 libellés pour ~15 000 lignes
            lire(tarifsCsv).forEach { r ->
                val niveau = runCatching { Niveau.valueOf(r["niveau"].orEmpty()) }.getOrNull() ?: return@forEach
                val type = runCatching { TypeIfn.valueOf(r["type"].orEmpty()) }.getOrNull() ?: return@forEach
                val libelle = r["essence_libelle"].orEmpty()
                val code = codes.getOrPut(libelle) { Cubage.codeEssence(libelle) } ?: return@forEach
                val numero = r["numero"]?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return@forEach
                val n = r["n_arbres"]?.toIntOrNull() ?: return@forEach
                val l = LigneTarif(numero, n, r["cv"]?.toDoubleOrNull() ?: Double.NaN, r["millesime"].orEmpty())
                val cle = Cle(niveau, r["code"].orEmpty(), code, type)
                if ((lignes[cle]?.nArbres ?: -1) < n) lignes[cle] = l
            }
            val parts = mutableMapOf<String, MutableList<SerPart>>()
            val communes = linkedMapOf<String, Commune>()
            lire(communesCsv).forEach { r ->
                val insee = r["insee"]?.takeIf { it.isNotBlank() } ?: return@forEach
                val ser = r["ser"]?.takeIf { it.isNotBlank() } ?: return@forEach
                val part = r["part"]?.toDoubleOrNull() ?: return@forEach
                parts.getOrPut(insee) { mutableListOf() }.add(SerPart(ser, part, r["greco"] ?: ser.take(1)))
                communes.getOrPut(insee) { Commune(insee, r["nom"].orEmpty(), r["departement"].orEmpty()) }
            }
            val sers = lire(serCsv).mapNotNull { r ->
                val code = r["code"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                code to Ser(code, r["nom"].orEmpty(), r["greco"] ?: code.take(1))
            }.toMap()
            return TarifsIfn(
                lignes,
                parts.mapValues { (_, l) -> l.sortedByDescending { it.part } },
                communes.values.toList(),
                sers,
                lignes.values.map { it.millesime }.filter { it.isNotBlank() }.distinct().sorted().joinToString(", "),
            )
        }

        /** Lignes d'un CSV `;` en dictionnaires colonne → valeur (BOM et guillemets retirés). */
        private fun lire(csv: String): List<Map<String, String>> {
            val l = csv.removePrefix("﻿").lineSequence().filter { it.isNotBlank() }.toList()
            if (l.isEmpty()) return emptyList()
            val entete = l.first().split(';').map { it.trim().trim('"') }
            return l.drop(1).map { ligne ->
                val v = ligne.split(';').map { it.trim().trim('"') }
                entete.indices.associate { entete[it] to v.getOrElse(it) { "" } }
            }
        }

        // Comme Cubage : minuscules, sans accent, ponctuation → espace, espaces compactés.
        private fun normaliser(s: String): String =
            Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
                .replace(Regex("[^a-z0-9 ]"), " ")
                .replace(Regex(" +"), " ")
                .trim()
    }
}
