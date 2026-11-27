package fr.marculus.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mission 041 : acceptation du plafond continu de la part de tige EMERGE, `fct = min(fct, 0,95)`
 * (décision de Pascal du 2026-11-27, règle 2 option B, `docs/specs/2026-11-27-emerge-regles-sans-source.md` § 3),
 * avec les règles 1 (option A) et 3 (option A) gardées. Le plafond 0,95 est une VALEUR PROVISOIRE de Marculus,
 * sans source publiée ni mesure sur tiges réelles (fiche 041, « Valeur provisoire »).
 *
 * Écrit sans lire le corps de `emergeVolumes`. Les valeurs attendues viennent d'un calcul indépendant, sous R,
 * de la formule de `gftools::TarEmerge` (copie de travail NON commitée de `/media/pascal/data2/digital_ocean/gftools`,
 * `R/TarifsEmerge.R` l. 285-361 ; `ListTarEmerge.rda` chargé, essence `HET`) :
 *  - « ancien » = la copie telle quelle (`FCT[FCT > 0.95] <- 0.8`) ;
 *  - « nouveau » = la même copie où la seule ligne changée est `FCT[FCT > 0.95] <- 0.95` (copie en scratchpad,
 *    hors dépôt), c'est-à-dire `min(fct, 0,95)` ;
 *  - recoupé par un calcul Python de la formule avec les coefficients arrondis à 5 décimales de `EmergeCoefs.kt`
 *    (hêtre : a 0,54187 b 0,66107 c −0,00150 d 0,87721 e 0,06488 f −4,11764 g 0,02298 alpha 0,20006 beta 2,53350) ;
 *    l'écart R / Python est de 0,02 % sur la tige du hêtre C 25 / H 20.
 * Formule : c13 = C/100 ; hdec = 0,8·H ; logis = ln(hdec/(H−hdec)) ; hdn = √c13/H ; corr = 1−1,3/H ;
 *   volCyl = c13²·H/(4π·corr²) ; formTot = a + b·hdn + c·H/c13 ; fct = d + e·logis + f·hdn + g/c13 ;
 *   tige = formTot·fct·volCyl·(1−(7π/100·corr/c13)³) ; houppier = formTot·(1−fct)·volCyl·découpe_houppier.
 * Unités : C130 en cm de circonférence, H en m, volumes en m³. Tolérance : 0,5 % relatif (arrondi des coefficients).
 */
class EmergePlafondFctTest {

    private fun vol(c130: Double, h: Double): Cubage.VolumesEmerge =
        assertNotNull(Cubage.volumesEmerge("Hêtre", c130, h), "hêtre : coefficients EMERGE attendus")

    private fun proche(attendu: Double, obtenu: Double, msg: String, relatif: Double = 0.005) =
        assertTrue(
            abs(obtenu - attendu) <= relatif * abs(attendu),
            "$msg : attendu $attendu, obtenu $obtenu (écart > ${relatif * 100} %)",
        )

    // --- 1. Hêtre C 25 cm, H 20 m : fct brut 0,9561 > 0,95 -----------------------------------------------

    @Test
    fun `hetre 25 cm 20 m - tige avec fct plafonne a 0,95`() {
        // R « nouveau » : 0,021018459 m³. L'ancien (fct = 0,8) donnait 0,017699755 : écart de 18,8 %.
        val v = vol(25.0, 20.0)
        proche(0.021018459, v.tige, "tige hêtre 25 / 20 (fct = 0,95)")
    }

    @Test
    fun `hetre 25 cm 20 m - la tige n est plus celle de l ancien fct 0,8`() {
        val v = vol(25.0, 20.0)
        assertTrue(abs(v.tige - 0.017699755) > 0.1 * 0.017699755, "tige encore à la valeur de l'ancienne règle 0,8 : ${v.tige}")
    }

    @Test
    fun `hetre 25 cm 20 m - houppier nul avec fct 0,95 comme avec 0,8`() {
        // R : 0 dans les deux cas (la découpe de 7 cm n'est pas atteinte par le houppier). Le bois fort total = la tige.
        val v = vol(25.0, 20.0)
        assertEquals(0.0, v.houppier, 1e-12, "houppier hêtre 25 / 20")
        proche(0.021018459, v.boisFortTotal, "bois fort total hêtre 25 / 20")
    }

    @Test
    fun `hetre 40 cm 35 m - le houppier de l ancienne regle disparait avec le plafond`() {
        // fct brut 0,9502. R « nouveau » : tige 0,16429845, houppier 0. R « ancien » (0,8) : tige 0,13835659, houppier 0,001669947.
        val v = vol(40.0, 35.0)
        proche(0.16429845, v.tige, "tige hêtre 40 / 35 (fct = 0,95)")
        assertEquals(0.0, v.houppier, 1e-12, "houppier hêtre 40 / 35 (l'ancienne règle 0,8 donnait 0,00167)")
    }

    @Test
    fun `hetre 25 cm 40 m - fct brut au dessus de 1 - tige plafonnee`() {
        // fct brut 1,008. R « nouveau » : tige 0,02400472.
        val v = vol(25.0, 40.0)
        proche(0.02400472, v.tige, "tige hêtre 25 / 40 (fct brut > 1)")
    }

    // --- 2. Continuité autour du seuil -------------------------------------------------------------------

    @Test
    fun `hetre 25 cm - la tige ne saute pas quand fct franchit 0,95`() {
        // fct brut = 0,95 à H ≈ 18,8757 m (bissection, Python). L'ancienne règle fait tomber la tige de 15,8 %
        // entre 18,8657 et 18,8857 m (0,020671 → 0,017413) ; le plafond continu la laisse varier de 0,04 %.
        val avant = vol(25.0, 18.80).tige
        val apres = vol(25.0, 18.95).tige
        assertTrue(avant > 0.0 && apres > 0.0, "tiges attendues positives : $avant, $apres")
        val rapport = apres / avant
        assertTrue(rapport in 0.99..1.02, "saut de la tige au franchissement de 0,95 : $avant → $apres (rapport $rapport)")
    }

    @Test
    fun `hetre 25 cm - balayage de H de 15 a 26 m sans saut de la tige`() {
        var precedent = vol(25.0, 15.0).tige
        var h = 15.05
        while (h <= 26.0 + 1e-9) {
            val t = vol(25.0, h).tige
            val saut = abs(t - precedent) / precedent
            assertTrue(saut < 0.01, "saut de ${saut * 100} % de la tige entre ${h - 0.05} et $h m ($precedent → $t)")
            precedent = t
            h += 0.05
        }
    }

    // --- 3. Houppier jamais négatif, jamais infini, y compris quand fct brut >= 1 ------------------------

    @Test
    fun `houppier et tige finis et positifs ou nuls quand fct brut depasse 1`() {
        // (le volume aérien total, sans fct, n'est pas contrôlé ici : il est négatif à C 10 / H 45, hors règle 2.)
        // fct brut > 1 : (C 10, H 20) = 1,132 ; (C 20, H 30) = 1,021 ; (C 25, H 40) = 1,008 ; (C 10, H 45) = 1,168.
        for ((c, h) in listOf(10.0 to 20.0, 20.0 to 30.0, 25.0 to 40.0, 10.0 to 45.0, 15.0 to 45.0)) {
            val v = vol(c, h)
            for ((nom, x) in listOf("tige" to v.tige, "houppier" to v.houppier, "bois fort" to v.boisFortTotal)) {
                assertTrue(x.isFinite(), "$nom non fini pour C $c / H $h : $x")
                assertTrue(x >= 0.0, "$nom négatif pour C $c / H $h : $x")
            }
        }
    }

    @Test
    fun `balayage hetre C 10 a 60 cm et H 2 a 45 m - aucun volume negatif ni non fini`() {
        var c = 10.0
        while (c <= 60.0) {
            var h = 2.0
            while (h <= 45.0) {
                val v = vol(c, h)
                for (x in listOf(v.tige, v.houppier, v.boisFortTotal)) {
                    assertTrue(x.isFinite() && x >= 0.0, "volume invalide $x pour C $c / H $h")
                }
                h += 1.0
            }
            c += 5.0
        }
    }

    // --- 4. Règle 1 gardée : pas de borne basse sur fct, tige et houppier ramenés à 0 -------------------

    @Test
    fun `regle 1 - hetre 62,8 cm a 2 m et 3 m - tige et houppier nuls`() {
        // fct brut −0,628 et −0,084 ; gftools rend une tige de −0,2566 et −0,0173 m³ et un houppier NA ; Marculus rend 0.
        for (h in listOf(2.0, 3.0)) {
            val v = vol(62.8, h)
            assertEquals(0.0, v.tige, 1e-12, "tige hêtre 62,8 / $h")
            assertEquals(0.0, v.houppier, 1e-12, "houppier hêtre 62,8 / $h")
        }
    }

    // --- 5. Règle 3 gardée : H <= 1,4 m -> tous les volumes à 0 ------------------------------------------

    @Test
    fun `regle 3 - H 1,40 m et 1,35 m - les quatre volumes sont nuls`() {
        for (c in listOf(10.0, 62.8)) {
            for (h in listOf(1.40, 1.35)) {
                val v = vol(c, h)
                assertEquals(0.0, v.tige, 0.0, "tige C $c / H $h")
                assertEquals(0.0, v.houppier, 0.0, "houppier C $c / H $h")
                assertEquals(0.0, v.boisFortTotal, 0.0, "bois fort total C $c / H $h")
                assertEquals(0.0, v.volumeAerienTotal, 0.0, "aérien total C $c / H $h")
            }
        }
    }

    @Test
    fun `regle 3 - H 1,41 m n est plus une exception - le calcul a lieu`() {
        // Hêtre C 10 cm, H 1,41 m : R « nouveau » rend tige 0 (découpe non atteinte) et houppier 0,05442803 m³ :
        // un 0 ici serait la règle 3 appliquée au-dessus de 1,4 m. Au passage : ni exception, ni NaN.
        val v = vol(10.0, 1.41)
        assertTrue(v.tige.isFinite() && v.tige >= 0.0, "tige : ${v.tige}")
        assertTrue(v.houppier.isFinite() && v.houppier >= 0.0, "houppier : ${v.houppier}")
        proche(0.05442803, v.houppier, "houppier hêtre 10 / 1,41", relatif = 0.02)
    }

    // --- 6. Non-régression : fct <= 0,95, volumes inchangés ---------------------------------------------

    @Test
    fun `non regression - hetre 150 cm 30 m avec fct brut 0,814 garde ses volumes`() {
        // R, identique avec l'ancienne et la nouvelle règle : tige 2,5683202, houppier 0,27972402.
        val v = vol(150.0, 30.0)
        proche(2.5683202, v.tige, "tige hêtre 150 / 30")
        proche(0.27972402, v.houppier, "houppier hêtre 150 / 30")
    }

    @Test
    fun `la fonction de tige seule suit le meme plafond`() {
        proche(0.021018459, Cubage.volumeEmergeTige("Hêtre", 25.0, 20.0), "volumeEmergeTige hêtre 25 / 20")
    }
}
