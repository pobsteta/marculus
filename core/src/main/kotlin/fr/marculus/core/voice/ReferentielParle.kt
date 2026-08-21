package fr.marculus.core.voice

import fr.marculus.core.Cubage
import java.text.Normalizer

/**
 * Génération du référentiel *parlé* d'un contexte de martelage : à partir des libellés réels
 * (essences de la matrice, qualités du référentiel), produit les formes à dicter et le code
 * métier renvoyé par le parseur.
 *
 * Deux règles structurent la génération :
 *
 *  1. **Forme à deux mots imposée** pour tout couple d'essences partageant le premier mot
 *     (« chêne sessile » / « chêne pédonculé ») ou dont les premiers mots sont quasi-homophones
 *     (chêne/frêne, orme/charme, aulne/orme, pin/sapin). Une seule syllabe de différence sur un
 *     mot isolé ne suffit pas en forêt ; deux mots donnent deux endroits où le décodeur tranche.
 *  2. Quand le libellé est **d'un seul mot** et qu'il entre en conflit, le second mot est la
 *     lettre discriminante du code ONF en alphabet radio (« chêne » CHE / « frêne » FRE →
 *     « chene charlie » / « frene foxtrot »). Aucune espèce n'est inventée : la forme parlée est
 *     un alias de dictée, le libellé enregistré dans la tige reste celui du contexte.
 *
 * Les qualités suivent la même logique : un code en lettres est épelé en alphabet radio
 * (« A » → « alpha », « AB » → « alpha bravo »), un libellé en toutes lettres se dicte tel quel
 * (« Chablis » → « chablis »).
 */
object ReferentielParle {

    /** Une essence du contexte, telle qu'elle s'enregistre et telle qu'elle se dicte. */
    data class EssenceParlee(
        /** Libellé exact de la colonne du contexte — c'est lui qui part dans la tige. */
        val nom: String,
        /** Clé métier renvoyée par le parseur (code ONF quand il est connu), unique par contexte. */
        val codeOnf: String,
        /** Forme parlée, en jetons minuscules sans accents. */
        val spoken: List<String>,
    ) {
        fun versGrammaire(): SpokenEssence = SpokenEssence(codeOnf, spoken)
    }

    /** Alphabet radio de l'OTAN, limité aux lettres latines. */
    val ALPHABET_RADIO: Map<Char, String> = mapOf(
        'A' to "alpha", 'B' to "bravo", 'C' to "charlie", 'D' to "delta", 'E' to "echo",
        'F' to "foxtrot", 'G' to "golf", 'H' to "hotel", 'I' to "india", 'J' to "juliett",
        'K' to "kilo", 'L' to "lima", 'M' to "mike", 'N' to "november", 'O' to "oscar",
        'P' to "papa", 'Q' to "quebec", 'R' to "romeo", 'S' to "sierra", 'T' to "tango",
        'U' to "uniform", 'V' to "victor", 'W' to "whisky", 'X' to "xray", 'Y' to "yankee",
        'Z' to "zoulou",
    )

    /**
     * Familles de premiers mots acoustiquement voisins : deux essences d'une même famille sont
     * traitées comme si elles partageaient leur premier mot.
     */
    private val FAMILLES_VOISINES: List<Set<String>> = listOf(
        setOf("chene", "frene"),
        setOf("orme", "charme"),
        setOf("aulne", "orme"),
        setOf("pin", "sapin"),
    )

    /** Nombre maximal de mots d'une forme parlée (borne la fenêtre d'appariement du parseur). */
    private const val MOTS_MAX = 3

    // Normalisation identique à celle de Cubage/EMERGE : minuscules, sans accent,
    // caractères non alphanumériques → espace, espaces compactés.
    private fun normaliser(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex(" +"), " ")
            .trim()

    /**
     * Construit le référentiel parlé des essences d'un contexte. Seules les essences passées
     * en argument sont dictables : la grammaire fermée ne contient jamais les 226 essences ONF.
     */
    fun essences(noms: List<String>): List<EssenceParlee> {
        val bases = noms.filter { it.isNotBlank() }.distinct().map { nom ->
            Base(nom = nom, mots = motsDe(nom), code = codeDe(nom))
        }
        val codes = codesUniques(bases)
        val enConflit = conflits(bases)

        val formes = bases.mapIndexed { i, base ->
            val code = codes[i]
            val mots = when {
                !enConflit[i] -> base.mots
                base.mots.size >= 2 -> base.mots
                // Un seul mot en conflit : on impose un second mot discriminant.
                else -> base.mots + radio(lettreDiscriminante(codes, enConflit, i))
            }
            EssenceParlee(nom = base.nom, codeOnf = code, spoken = mots)
        }
        return leverDoublons(formes, codes)
    }

    /**
     * Construit le référentiel parlé des qualités depuis les libellés du référentiel de
     * l'application. [SpokenQualite.code] est le libellé d'origine : c'est lui qui est
     * enregistré dans la tige, sans conversion.
     */
    fun qualites(libelles: List<String>): List<SpokenQualite> {
        val vues = mutableSetOf<String>()
        return libelles.filter { it.isNotBlank() }.distinct().mapNotNull { libelle ->
            val parle = if (estCodeLettres(libelle)) {
                libelle.uppercase().mapNotNull { ALPHABET_RADIO[it] }.joinToString(" ")
            } else {
                normaliser(libelle).split(" ").take(MOTS_MAX).joinToString(" ")
            }
            if (parle.isBlank() || !vues.add(parle)) null else SpokenQualite(libelle, parle)
        }
    }

    /** Un code de qualité est une suite courte de lettres majuscules (« A », « AB », « CD »). */
    private fun estCodeLettres(libelle: String): Boolean {
        val t = libelle.trim()
        return t.length <= MOTS_MAX && t.isNotEmpty() && t.all { it in 'A'..'Z' }
    }

    private data class Base(val nom: String, val mots: List<String>, val code: String)

    private fun motsDe(nom: String): List<String> {
        val mots = normaliser(nom).split(" ").filter { it.isNotEmpty() }.take(MOTS_MAX)
        return mots.ifEmpty { listOf("essence") }
    }

    /**
     * Code ONF de l'essence si la table gftools la reconnaît, sinon un code de repli lisible
     * dérivé du libellé (« Autres feuillus » → AUF, « Autres résineux » → AUR).
     */
    private fun codeDe(nom: String): String {
        Cubage.codeEssence(nom)?.let { return it }
        val mots = normaliser(nom).split(" ").filter { it.isNotEmpty() }
        val brut = when {
            mots.isEmpty() -> nom
            mots.size == 1 -> mots[0].take(3)
            else -> mots[0].take(2) + mots[1].take(1)
        }
        return brut.uppercase().padEnd(3, 'X')
    }

    /** Rend les codes uniques dans le contexte (deux libellés peuvent viser la même entrée gftools). */
    private fun codesUniques(bases: List<Base>): List<String> {
        val vus = mutableSetOf<String>()
        return bases.map { base ->
            var code = base.code
            var n = 2
            while (!vus.add(code)) code = "${base.code}$n".also { n++ }
            code
        }
    }

    /** Marque les essences dont le premier mot est partagé ou voisin de celui d'une autre. */
    private fun conflits(bases: List<Base>): List<Boolean> = bases.map { base ->
        bases.any { autre ->
            autre !== base && (
                autre.mots[0] == base.mots[0] || FAMILLES_VOISINES.any {
                    base.mots[0] in it && autre.mots[0] in it
                }
                )
        }
    }

    /**
     * Lettre du code qui sépare le mieux l'essence [i] des autres essences en conflit : la
     * première position où les codes du groupe ne sont pas tous identiques.
     */
    private fun lettreDiscriminante(codes: List<String>, enConflit: List<Boolean>, i: Int): Char {
        val groupe = codes.filterIndexed { j, _ -> enConflit[j] }
        val code = codes[i].filter { it.isLetter() }.uppercase().padEnd(3, 'X')
        val position = (0 until 3).firstOrNull { p ->
            groupe.map { it.filter(Char::isLetter).uppercase().padEnd(3, 'X')[p] }.distinct().size > 1
        } ?: 0
        return code[position]
    }

    private fun radio(lettre: Char): String = ALPHABET_RADIO[lettre] ?: "unite"

    /**
     * Filet de sécurité : si deux formes parlées restent identiques (libellés distincts par les
     * seuls accents, ou par un quatrième mot), on suffixe chacune de sa lettre de code radio.
     */
    private fun leverDoublons(
        formes: List<EssenceParlee>,
        codes: List<String>,
    ): List<EssenceParlee> {
        val comptes = formes.groupingBy { it.spoken }.eachCount()
        if (comptes.values.all { it == 1 }) return formes
        val ambigues = formes.indices.filter { comptes.getValue(formes[it].spoken) > 1 }
        val marquees = formes.indices.map { it in ambigues }
        return formes.mapIndexed { i, forme ->
            if (!marquees[i]) forme
            else forme.copy(spoken = forme.spoken + radio(lettreDiscriminante(codes, marquees, i)))
        }
    }
}
