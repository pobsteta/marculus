# ADR 0010 — Proposition IFN pour « Autres feuillus » et « Autres résineux » ; les lignes au numéro ≤ 0 sont écartées

- **Date :** 2026-11-10
- **Statut :** **décision acceptée (Pascal, 2026-11-10, `pilotage/journal.md` l. 228, 229, 233 et 245) ; texte proposé**, à valider par Pascal à la lecture de la PR ; livrée par la PR #62 (609910f, mission 030, v0.73.0)
- **Proposée par :** Pascal, après la note de l'acquéreur sur `29AF` et `68CE` (journal l. 224 et 228)
- **Tranchée par :** Pascal

## La décision en une phrase

« Autres feuillus » et « Autres résineux » reçoivent une proposition de tarif IFN **calculée** (moyenne pondérée par le nombre d'arbres des feuillus, resp. résineux, de la SER hors essences listées à part dans le contexte), et non un lien vers un code IFN ; toute ligne de la table au numéro de tarif ≤ 0 est écartée partout.

## La règle (source unique)

Les autres documents renvoient ici. Le code est `core/src/main/kotlin/fr/marculus/core/TarifsIfn.kt` (sur `main` à 609910f).

**Pas de lien à `29AF` / `68CE`.** Ce sont, d'après l'IGN, « AUTRE FEUILLU EXOTIQUE » et « AUTRE CONIFERE EXOTIQUE » : des résidus exotiques, pas les « autres » au sens de Marculus (`docs/sources/2026-11-10-ifn-29AF-68CE.md`, Q1 et Q2, **une seule source** : IGN `documentation_2.txt`, ni titre ni date). Décision de Pascal : pas de lien ; « moyenne des feuillus pour autres feuillus et moyenne des résineux pour autres résineux » (journal l. 228).

**Reconnaissance.** Le nom de l'essence, casse et accents ignorés, égal à « autres feuillus » ou « autres resineux » (`Regroupement`, `TarifsIfn.kt` l. 85-105, `de` l. 100-103).

**Feuillu ou résineux.** Deux premiers caractères du code ESPAR : < 50 feuillu, ≥ 50 résineux (`Regroupement.contient`, l. 94-97). L'if et les genévriers sont des « Autres résineux », comme chez l'IGN (journal l. 233 ; le testeur a lu `documentation_2.txt` en entier, l. 241).

**Lignes retenues (`lignesAutres`, l. 193-202).** Les lignes du regroupement au niveau considéré, **hors essences du contexte** : un nom du contexte exclut les espèces IFN dont le libellé normalisé est ce nom ou commence par ce nom suivi d'une espace (mot entier). Exemples décidés : « Chêne » exclut tous les chênes, « Pin laricio » les deux laricios, « Cerisier » ses quatre libellés ; « Chêne sessile » n'exclut que le chêne sessile ; seule exception, « Charme » n'exclut pas « Charme houblon » (journal l. 229 et 233 ; tests `TarifsIfnAutresTest.kt` l. 356-475).

**Calcul du numéro (`proposerAutres`, l. 215-236).**

- Moyenne des numéros **pondérée par le nombre d'arbres**, sur les lignes du type de Schaeffer demandé ; arrondie à l'entier, bornée à 1..20 (l. 223 et 227) ; le CV est pondéré de même sur les lignes qui en ont un (l. 224-232) ; millésimes listés (l. 233).
- Même cascade **SER → GRECO → France** que les essences ordinaires ; seuil de 30 arbres (`SEUIL_ARBRES`, l. 281) appliqué **au total retenu**, non à chaque ligne (l. 219-222) ; sous le seuil à tous les niveaux : pas de proposition (journal l. 229).
- Tarif sans numéro (Aucun, EMERGE) : pas de proposition (l. 218).

**Part de houppier (`proposerPartHouppierAutres`, l. 243-254).** Mêmes règles ; seules comptent les lignes à part renseignée ; les quatre types d'une même espèce ne se cumulent pas, on retient le plus fourni (l. 248-250) ; moyenne pondérée par les arbres, seuil de 30 sur le total (l. 251-253). Ligne sans part, ou au numéro ≤ 0 : écartée (journal l. 233).

**Aiguillage.** `proposerPour`, `proposerPartHouppierPour` et `typeConseillePour` (l. 257-268) appellent la version « Autres » pour un regroupement et la version ordinaire sinon ; c'est ce que l'écran appelle (`CreationContexteScreen.kt` l. 183, 194, 997-1008, dans `app/src/main/kotlin/io/github/pobsteta/marculus/ui/contextes/`).

**Écran.** Le total d'arbres et le CV des « Autres » dépendent des essences cochées : `modifierSelection` (`CreationContexteScreen.kt` l. 202-205) est le point unique de modification de la sélection et repropose ensuite toutes les essences cochées ; un champ retouché à la main n'est pas réécrit. Vu sur l'émulateur : ajout de Douglas, « Autres résineux » passe de 10 à 9 (286 arbres), saisie manuelle conservée (journal l. 260).

**Numéro ≤ 0 écarté (au chargement, `TarifsIfn.charger`, l. 303-304).** Une ligne au numéro ≤ 0 ou non fini n'est pas un tarif : elle n'est ni proposée, ni moyennée, ni comptée pour le seuil ; on passe au niveau suivant. Vaut pour **toutes** les propositions, essences ordinaires comprises (journal l. 229 : négatives ; l. 233 : zéro écarté comme les négatifs ; commentaire `proposer`, l. 147).

## Contexte

Avant : « Autres feuillus » et « Autres résineux » étaient des regroupements sans code ONF, donc sans proposition (journal l. 223). La note de l'acquéreur (journal l. 224, 225, 227) montre que `29AF` et `68CE` ne conviennent pas, et que la table compte 44 lignes `29AF` / `68CE`, dont 24 négatives, sans libellé donc jamais chargées. Le relecteur a relevé en outre 276 lignes négatives à 30 arbres ou plus qui étaient ramenées à 1 sans signal (journal l. 225 et 227) ; Pascal a décidé de les écarter et de faire un brief à nemetonshiny sur leur origine (journal l. 229).

## Effet accepté sur la table embarquée

Écarter les lignes ≤ 0 change des propositions existantes : **3 663 propositions qui valaient 1 deviennent « aucune donnée », et 344 parts par essence disparaissent** ; comptes gonflés par le repli France répété (relecteur, journal l. 245). Pascal l'a accepté (« Fusionne tout, je valide tout », journal l. 245). Le brief à nemetonshiny sur l'origine de ces lignes : non écrit dans le journal comme livré.

## Ce qui est livré, avec les preuves

| Où | Quoi | Preuve |
|---|---|---|
| Noyau | `Regroupement`, `proposerAutres`, `proposerPartHouppierAutres`, `proposerPour`, `proposerPartHouppierPour`, `typeConseillePour` | `TarifsIfn.kt` l. 85-105, 188-268 ; PR #62 (609910f) |
| Chargement | numéro ≤ 0 écarté partout ; lignes brutes gardées avec libellé et ESPAR | `TarifsIfn.kt` l. 303-313 |
| Écran | propositions et parts via les fonctions `…Pour` ; `modifierSelection` | `CreationContexteScreen.kt` l. 183, 194, 202-205, 387, 593, 997-1008 |
| Tests | 45 tests : moyenne pondérée, seuil 29 / 30, cascade, négatifs et zéro, préfixes, Charme houblon, part, fonctions `…Pour` | `core/src/test/kotlin/fr/marculus/core/TarifsIfnAutresTest.kt` (45 `@Test`) ; journal l. 241, 247 |
| Vu | Dijon, SER C51 : Autres feuillus 8 (4 862 arbres), Autres résineux 10 (512) ; version 0.73.0 vue sur le téléphone | journal l. 242, 259, 263 |

Limite connue : la logique de l'écran n'a pas de test automatique, seules les fonctions `…Pour` du noyau sont testées (relecteur, journal l. 260).

## Alternatives

Relier à `29AF` / `68CE` : écartée par Pascal (journal l. 228), ces codes étant des résidus exotiques. Moyenne non pondérée, ou seuil par ligne : non écrit dans le journal ; la fiche (`pilotage/missions/030-tarif-ifn-autres-feuillus-resineux.md`) en dit le détail.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | La plus simple qui tienne ? | Une moyenne pondérée au lieu d'un code ; même cascade et même seuil que les essences ordinaires | Une table de lignes brutes gardée en mémoire (`brutes`) | Oui (journal l. 229) |
| **R** | Qui fait quoi ? | Testeur (37 puis 45 tests), dev (PR #62, 609910f), afficheur (captures), relecteur (prêt au 2e tour) | | |
| **A** | Et si l'imprévu arrive ? | Aucune ligne au seuil : pas de proposition, saisie manuelle ; mutations « ≤ 0 », Charme, préfixe, regroupement ignoré détectées (journal l. 246, 260) | Logique d'écran non testée automatiquement | Non écrit |
| **N** | Idées étranges écoutées ? | Non écrit | | |
| **C** | Coût pour l'ensemble ? | 3 663 propositions qui valaient 1 deviennent nulles ; 344 parts disparaissent (journal l. 245) ; aucune attente de test existante changée (journal l. 242) | | Oui (journal l. 245) |
| **É** | Que réutilisons-nous ? | La cascade, `SEUIL_ARBRES`, `PropositionTarif`, `PropositionPart`, la table nemeton 2026.10.9 | | |

## Valeurs sacrifiées

Non écrit.

## Sources

- `pilotage/journal.md` du 2026-11-10 : l. 223-229 (029), 232-233 (fiche et cas de règle), 241-248 (testeur, dev, relecteur), 259-263 (captures, PR #62, essai de Pascal).
- PR #62 (609910f, v0.73.0).
- Note de sources `docs/sources/2026-11-10-ifn-29AF-68CE.md` (IGN `documentation_2.txt`, une seule source).
- Fiche `pilotage/missions/030-tarif-ifn-autres-feuillus-resineux.md` ; spec `docs/specs/2026-10-16-tarifs-ser.md`.
