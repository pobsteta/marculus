package fr.marculus.core

import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.TarifCubage
import fr.marculus.core.model.Tige

/**
 * Volumes d'un martelage, tels que l'écran Statut les affiche et que les exports (`.marsync`,
 * CSV format 3) les transmettent : Marculus est la **seule source** de ces chiffres.
 *
 * **Annulation** : elle retire, dans la même case (essence × classe), les dernières tiges
 * comptées avant elle et pas encore annulées — donc **leur** volume. Elle est saisie sans
 * hauteur : en EMERGE, lui calculer un volume propre retirerait 0 m³ et laisserait le volume de
 * la tige annulée dans le total. Une annulation sans tige à retirer (journal fusionné dans le
 * désordre) retire le volume calculé sur sa propre saisie.
 */
object VolumesMartelage {

    /** Méthode de cubage effectivement appliquée à une tige (clé `cubage` des exports). */
    object Methode {
        const val AUCUN = "AUCUN"
        const val EMERGE = "EMERGE"
        const val NON_CUBABLE = "NON_CUBABLE"
        fun schaeffer(tarif: TarifCubage, numero: Int) = "${tarif.name}:$numero"
        fun forme(f: Double) = "FORME:$f"
    }

    /**
     * Valeurs **unitaires** d'une tige (pour une tige, avant multiplication par `quantite`).
     * [volumeBoisFortTotalM3] = tige + houppier (découpe 7 cm) ; [volumeAerienTotalM3] = volume aérien
     * total EMERGE (sans découpe), 0 hors méthode EMERGE.
     */
    data class CubageTige(
        val volumeTigeM3: Double,
        val volumeHouppierM3: Double,
        val volumeBoisFortTotalM3: Double,
        val volumeAerienTotalM3: Double,
        val surfaceTerriereM2: Double,
        val cubage: String,
    ) {
        val nonCubee: Boolean get() = cubage == Methode.NON_CUBABLE
        /** Cubée par EMERGE, donc porteuse d'un volume aérien total. */
        val aerien: Boolean get() = cubage == Methode.EMERGE
    }

    /** Totaux **nets** d'un contexte, annulations déduites. */
    data class Totaux(
        val volumeTigeM3: Double,
        val volumeHouppierM3: Double,
        val volumeBoisFortTotalM3: Double,
        val volumeAerienTotalM3: Double,
        val surfaceTerriereM2: Double,
        val nbTigesNonCubees: Int,
        /**
         * Tiges nettes cubées par EMERGE : seules elles entrent dans [volumeAerienTotalM3] (contexte mixte
         * EMERGE + Schaeffer : somme EMERGE seule, signalée « sur [nbTiges] »).
         */
        val nbTigesAerien: Int = 0,
        /** Toutes les tiges nettes, cubées ou non. */
        val nbTiges: Int = 0,
    )

    /** Cubage d'une tige sur **sa propre saisie** (classe, essence, hauteur), avec le tarif de son essence. */
    fun cubage(contexte: Contexte, tige: Tige): CubageTige {
        val g = Cubage.surfaceTerriereUnitaire(contexte, tige.classe)
        val t = Cubage.tarifDe(contexte, tige.essence)
        val methode = when (t.tarif) {
            TarifCubage.AUCUN -> Methode.AUCUN
            TarifCubage.SCHAEFFER_RAPIDE, TarifCubage.SCHAEFFER_INTERMEDIAIRE,
            TarifCubage.SCHAEFFER_LENT, TarifCubage.SCHAEFFER_TRES_LENT ->
                Methode.schaeffer(t.tarif, t.numero)
            TarifCubage.EMERGE -> when {
                HauteurParser.parse(tige.hauteurTexte ?: "").hauteurTotale == null -> Methode.NON_CUBABLE
                Cubage.estCouverteEmerge(tige.essence) -> Methode.EMERGE
                else -> Methode.forme(contexte.coefficientForme)
            }
        }
        val v = Cubage.volumesUnitaire(contexte, tige.essence, tige.classe, tige.hauteurTexte)
        return CubageTige(v.tige, v.houppier, v.boisFortTotal, v.volumeAerienTotal, g, methode)
    }

    /** Totaux nets du journal, à partir de [JournalNet.tiges] (voir en-tête). */
    fun totaux(contexte: Contexte, journal: List<Tige>): Totaux {
        var vTige = 0.0
        var vHoup = 0.0
        var vTot = 0.0
        var vAer = 0.0
        var g = 0.0
        var nonCubees = 0
        var nbAerien = 0
        var nbTiges = 0
        fun ajouter(c: CubageTige, n: Int) {
            vTige += c.volumeTigeM3 * n
            vHoup += c.volumeHouppierM3 * n
            vTot += c.volumeBoisFortTotalM3 * n
            vAer += c.volumeAerienTotalM3 * n
            g += c.surfaceTerriereM2 * n
            if (c.nonCubee) nonCubees += n
            if (c.aerien) nbAerien += n
            nbTiges += n
        }
        // Une seule règle d'annulation : JournalNet. Une tige PLUS compte ; une annulation
        // restante (orpheline) retire le cubage de sa propre saisie.
        JournalNet.tiges(journal).forEach { t ->
            val c = cubage(contexte, t)
            ajouter(c, if (t.action == ActionTige.PLUS) t.quantite else -t.quantite)
        }
        return Totaux(vTige, vHoup, vTot, vAer, g, nonCubees.coerceAtLeast(0), nbAerien.coerceAtLeast(0), nbTiges.coerceAtLeast(0))
    }
}
