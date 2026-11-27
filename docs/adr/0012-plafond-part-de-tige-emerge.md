# ADR 0012 — Part de tige EMERGE : plafond continu `min(fct, 0,95)`, 0,95 provisoire ; deux autres règles gardées comme choix de Marculus

- **Date :** 2026-11-27
- **Statut :** **décision acceptée par Pascal** (journal l. 370, 371, 372, cap l. 398) ; **texte de l'ADR proposé**, à valider par Pascal à la lecture de la PR ; règle 2 livrée par la PR #90 (795079e, missions 041 et 042, release v2026.11.6) ; règles 1 et 3 : aucun changement de code
- **Proposée par :** acquereur (mission 040, options A à D de la règle 2, `docs/specs/2026-11-27-emerge-regles-sans-source.md` § 3), puis Pascal choisit « B »
- **Tranchée par :** Pascal

Règle du bilan n° 3 : chaque fait cité dit l'état de sa source. Journal lu dans `pilotage/journal.md` à `0c6b889` (`main`, 2026-11-27) ; clé d'une ligne = date · auteur · mission.

## La décision en une phrase

Dans le cubage EMERGE (`Cubage.emergeVolumes`), la part de tige `fct` est plafonnée de façon continue à **0,95** (`fct = min(fct, 0,95)`), à la place de l'ancienne règle « si `fct` > 0,95 alors `fct` = 0,8 » ; **0,95 est provisoire** ; les deux autres règles du calcul qui n'ont pas de source publiée (pas de borne basse sur `fct` ; hauteur ≤ 1,4 m donne des volumes à 0) sont **gardées** et écrites comme des choix de Marculus.

## Pourquoi : trois règles, aucune publiée

Les trois règles se lisent dans `Cubage.kt` et n'avaient aucune source citée ; la mission 040 les a cherchées (`docs/specs/2026-11-27-emerge-regles-sans-source.md`, sur `main` à `0c6b889`, version 3 après 3 allers-retours, journal l. 334-345).

| Règle | Source trouvée | Preuve |
|---|---|---|
| 1. `fct` sans borne basse | gftools **publié** (par absence de borne) ; Marculus plus prudent : tige et houppier ramenés à 0 là où gftools rend une tige négative et un houppier `NA` (mesuré sous R par le relecteur) | spec règles sans source § 2 ; `Cubage.kt` l. 170 (la spec des règles cite l. 169, ancienne numérotation) |
| 2. `fct > 0,95 → 0,8` | **aucune source publiée** : présente seulement dans une copie de travail non commitée de gftools (`M R/TarifsEmerge.R`, absente du commit 187ad39), avec le commentaire de l'auteur « bon faudra qu'on fasse mieux » ; arrivée dans Marculus au commit 57fb18b (2026-06-23) sans source citée | spec règles sans source § 3 ; journal l. 340, 344, 345 |
| 3. `H ≤ 1,4 m → 0` | 1,3 sourcé par la formule ; **1,4 sans source** (commit 57fb18b) ; NEMETON refuse `H ≤ 1,30` | spec règles sans source § 4 ; `Cubage.kt` l. 158 |

**L'origine de la règle 2 est inconnue.** La question a été posée à Pascal, qui a répondu : « je ne sais pas répondre à cette question » (journal 2026-11-27 · Pascal · 040, l. 352).

L'effet de la règle 2 n'était pas marginal : `fct > 0,95` dans 59 435 cas sur 597 872 de la grille H 2-45 m × C 10-400 cm (9,9 %), sur des tiges minces (hêtre C 10-45 cm ; **C = circonférence à 1,30 m**, D = C/π, dans tout cet ADR sauf mention contraire) ; mesure exécutée sous R par le relecteur, pourcentage calculé par l'acquéreur (spec règles sans source § 3). La règle faisait en outre **sauter** le volume : hêtre C 25 cm / H 20 m, tige 0,0177 m³ avec 0,8, 0,0210 m³ avec 0,95 (journal 2026-11-27 · testeur · 041, l. 382 ; relecteur, l. 385).

## Les décisions de Pascal

| Règle | Décision | Preuve (journal, clé) |
|---|---|---|
| 1 | **A** : garder ; tige et houppier ramenés à 0, écrit comme choix de Marculus | l. 370 (2026-11-27 · Pascal · EMERGE règle 1) |
| 2 | **B** : plafond continu `min(fct, 0,95)`, choix de Marculus, 0,95 provisoire | l. 371 (2026-11-27 · Pascal · EMERGE règle 2) ; cap l. 398 (2026-11-27 · Pascal · 042 · cap) |
| 3 | **A** : garder ; « non calculé » à l'écran au cycle 11, avec « hauteur impossible » de NEMETON | l. 372 (2026-11-27 · Pascal · EMERGE règle 3) |

Reprise des trois dans `docs/specs/2026-11-27-emerge-regles-sans-source.md` § 8 et dans `docs/specs/2026-06-21-marculus-v1-spec.md` § 5 (l. 140-148), sur `main` à `0c6b889`.

Le cap du journal (l. 398) donne le **pourquoi** : « règle sans source publiée, saut de volume (mission 040) ». La ligne cite « fct > 0,95 → 0,8 » remplacé par « min(fct, 0,95) ».

## Pourquoi 0,95

Ce que les sources disent : 0,95 est **le seuil déjà présent** de l'ancienne règle (`Cubage.kt` avant la PR #90 ; spec règles sans source § 3), et l'option B de l'acquéreur était écrite `min(fct, 0.95)` (§ 3, option B). Le plafond continu garde ce seuil et ne change que la valeur au-dessus : plus de saut en passant 0,95.

Ce que les sources **ne disent pas** : pourquoi 0,95 plutôt qu'une autre valeur. Aucune mesure, aucune publication ne la fonde (spec règles sans source § 3 : « Aucune source publiée » ; origine inconnue, journal l. 352). D'où le mot **provisoire**, porté à trois endroits : commentaire de `Cubage.kt` l. 171-172 (« 0,95 est PROVISOIRE : à mesurer sur des cubages réels de tiges minces »), spec v1 § 5 l. 144-146, journal l. 371 et l. 398.

Constante : `FCT_PLAFOND = 0.95` (`Cubage.kt` l. 195), appliquée à `Cubage.kt` l. 174 (sur `main` à `0c6b889`).

## Ce qui est livré, avec les preuves

| Où | Quoi | Preuve |
|---|---|---|
| Code | `fct = min(fct, FCT_PLAFOND)` | PR #90 (795079e) ; `Cubage.kt` l. 174, 195 ; journal l. 384 (2026-11-27 · dev · 042) |
| Tests | `EmergePlafondFctTest`, 14 cas ; rouge constaté 8 sur 14 avant le code (règle 2 seule), règles 1 et 3 et non-régression verts ; valeurs recalculées sous R | `core/src/test/kotlin/fr/marculus/core/EmergePlafondFctTest.kt` ; journal l. 382 (testeur · 041), l. 385 (relecteur · 041+042 : `test-command` exit 0, `core` 1790, `data` 86, `app` 29) |
| Livraison | fusion de la PR #90 ; release v2026.11.6 ; note de version corrigée par Pascal (« seules les perches très élancées changent… ») | journal l. 389 (2026-11-27 · Pascal · 041+042), l. 395 (2026-11-27 · Pascal · cycle 10) |
| Contrôle terrain | Prévu : hêtre C 25 cm (circonférence) à H 18 m puis H 19 m (0,0203 → 0,0207 m³) et H 20 m (0,0210 m³) ; le contrôle d'abord prévu (C 24 / C 26 à H 20) ne montrait rien et avait été remplacé (l. 386). **Contrôle réel de Pascal** : il a saisi la **classe de diamètre 25** (centre 27,5 cm), pas une circonférence de 25 cm (D ≈ 8 cm, une perche) ; à D 27,5 cm, `fct` ≈ 0,8 : la règle ne joue pas, volumes inchangés (tige 0,522 / 0,550 / 0,579 m³ à H 18 / 19 / 20, recalculés par l'orchestrateur, identiques à l'écran). Décision : un contrôle terrain s'écrit dans l'unité de l'écran. Le `% houpp.` grisé en EMERGE (12,6 % sur le hêtre) est ignoré en EMERGE, gardé pour un retour en Schaeffer. Note de version corrigée : « seules les perches très élancées changent (hêtre de 8 cm de diamètre à 20 m : tige 0,018 → 0,021 m³) ; les classes de martelage courantes ne bougent pas » | journal l. 386 (écart orchestrateur), l. 392 (installation), **l. 393** (écart : classe de diamètre contre circonférence), **l. 394** (% houpp. ignoré en EMERGE), **l. 395** (note de version corrigée) |

## Ce qui reste ouvert

- **« Non calculé » à l'écran** pour les tiges sous 1,4 m (règle 3) : au cycle 11 (journal l. 372) ; la carte « Statut dit combien de tiges EMERGE n'ont pas de volume » du bac à sable les compte (journal l. 328).
- **Valeur 0,95** : à mesurer sur des cubages réels de tiges minces ; qui mesure, quand : non écrit.
- **Volume aérien total négatif** pour une perche très élancée (hêtre C 10 cm / H 45 m, −0,00488 m³) : porté au bac à sable (journal l. 383), non traité ; houppier de 0,054 m³ figé à C 10 / H 1,41, artefact de la règle 3 (journal l. 385).
- **Brief vers `nemeton`** : son port R copie les règles 1,4 m et 0,95 → 0,8 de Marculus (`~/dev/nemeton/R/ifn_tarifs.R` l. 131 et 140, cité par `pilotage/kanban.md` l. 41 sur `main` à `0c6b889`) ; à écrire, non écrit au `0c6b889`.
- **Réserves de la mission 040 non levées** : source Deleuze et al. 2014 non lue directement (accès HAL refusé) ; faits git et R rapportés par le relecteur et non rejoués par l'acquéreur (spec règles sans source § 7, « Non cochés »).

## Alternatives

Pour la règle 2, options présentées à Pascal (spec règles sans source § 3) : A garder `0,95 → 0,8` écrit comme règle propre ; **B plafond continu (retenue)** ; C retirer (houppier nul quand `fct ≥ 1`) ; D mesurer d'abord sur des tiges réelles. Raison de B contre A, C, D : le saut de volume (journal l. 398) ; contre C : s'écarte de gftools (spec § 3) ; contre D : non écrit. Pour la règle 3 : B passer à 1,30 avec refus, C refuser sous 2 m (options de la spec règles sans source § 4), écartées par le choix « A » de Pascal (journal l. 372), raison non écrite. Pour la règle 1 : B borner `fct` à [0 ; 1], C refuser le calcul (options de la spec règles sans source § 2), écartées par le choix « A » (journal l. 370), raison non écrite.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | La plus simple qui tienne ? | Une ligne de code (`min`), une constante nommée | 0,95 sans fondement mesuré | Non écrit ; déduit du choix « B » de Pascal (l. 371) |
| **R** | Qui fait quoi ? | acquereur (040), testeur (041), dev sonnet (042), relecteur ; Pascal tranche | | |
| **A** | Et si l'imprévu arrive ? | Règles 1 et 3 gardent les volumes à 0 plutôt qu'un volume négatif ; contrôle sous R à chaque valeur attendue | Volumes de perches très élancées changés (note de version, l. 395) | Non écrit ; déduit de la note de version validée par Pascal (l. 395) |
| **N** | Idées étranges écoutées ? | Non écrit | | |
| **C** | Coût pour l'ensemble ? | Un cubage qui change pour les tiges minces, dit dans la note de version | Port R de `nemeton` copie encore l'ancienne règle (écart à signaler) | Non écrit |
| **É** | Que réutilisons-nous ? | gftools (règle 1), la constante et le seuil existants (règle 2) | | |

## Valeurs sacrifiées

- **La fidélité à la copie de gftools** : Marculus s'écarte de gftools (volume négatif ou `NA` sous `fct < 0`, saut à 0,95) ; assumé comme « choix de Marculus » (journal l. 370, 371).
- **Une valeur fondée** : 0,95 reste provisoire tant qu'aucune mesure ne la fonde.

## Sources

- `pilotage/journal.md` à `0c6b889` : l. 328, 334-345, 352, 370-372, 382-386, 389, 392-395, 398.
- PR #86 (b4695ac, spec des règles sans source, fusionnée), PR #90 (795079e, plafond).
- `docs/specs/2026-11-27-emerge-regles-sans-source.md`, `docs/specs/2026-06-21-marculus-v1-spec.md` § 5, `core/src/main/kotlin/fr/marculus/core/Cubage.kt` l. 158, 170-174, 195 (sur `main` à `0c6b889`).
- Fiches `pilotage/missions/040-emerge-points-secondaires.md` à `042-plafond-fct-emerge.md`.
- Lien avec l'ADR 0011 : la règle 3 et « hauteur impossible » (NEMETON) partagent l'écran « non calculé » du cycle 11.

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ? Réponse de Pascal : non écrite.
