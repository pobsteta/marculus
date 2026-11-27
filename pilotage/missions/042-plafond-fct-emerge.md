# Mission 042 — Plafond continu de la part de tige EMERGE

## Mission

**En une phrase (quoi, et pourquoi) :** remplacer dans `Cubage.emergeVolumes` la règle sans source
`if (fct > 0.95) fct = 0.8` par le plafond continu `fct = min(fct, 0,95)`, et écrire les trois règles EMERGE dans la
spec comme choix de Marculus, pour supprimer le saut de volume des tiges minces (9,9 % de la grille d'essai) décidé
par Pascal le 2026-11-27.

**Redite par l'agent :** _reconstituée le 2026-11-27 d'après le journal (lignes 383-386, 389, 398), l'agent n'ayant pas consigné sa redite à l'époque._ Remplacer « fct > 0,95 → 0,8 » par `fct = min(fct, FCT_PLAFOND)` (0,95 provisoire) et écrire les trois règles EMERGE dans la spec comme choix de Marculus. Livrée par la PR #90 (795079e, v2026.11.6), commit 21fe2b3 ; le contrôle terrain de la fiche a été remplacé (ligne 386) puis la note de version corrigée (lignes 393-395). Redite de l'agent d'origine : non retrouvée.

## Ce qui existe (preuves)

- **La règle :** `core/src/main/kotlin/fr/marculus/core/Cubage.kt` l. 170 (`if (fct > 0.95) fct = 0.8`) ; règles 1
  et 3 : l. 157, 169, 173, 186.
- **Le constat et les options :** `docs/specs/2026-11-27-emerge-regles-sans-source.md` (PR #86 et son erratum).
- **Les décisions de Pascal du 2026-11-27** (`pilotage/journal.md`) : règle 1 → A, règle 2 → B, règle 3 → A.
- **Les tests d'acceptation :** mission 041, `EmergePlafondFctTest.kt`, branche `test/041-plafond-fct`.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | une ligne de code, règle décidée |
| A-t-il les moyens ? | oui | tests de la 041, `.claude/test-command` |

**Temps de la main :** donner. **Degré de la main :** le chef tranche. Changement de règle métier : la PR remonte à
Pascal (jamais lâché, `pilotage/equipe.md`).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `dev` · **Modèle :** sonnet (une règle, déjà tranchée ; la justesse est portée par les tests de la 041)
- **Ce qui ne lui est pas demandé :** changer les règles 1 et 3 ; un message à l'écran (« non calculé » : cycle 11,
  avec le branchement de NEMETON) ; toucher à `PART_HOUPPIER_MAX` (autre objet, l. 191).

## Harnais

- **Peut (périmètre) :** `core/src/main/kotlin/fr/marculus/core/Cubage.kt` (la règle 2 et son commentaire) ;
  `docs/specs/2026-06-21-marculus-v1-spec.md` § 5 (EMERGE : les trois règles, avec « choix de Marculus » et
  « 0,95 provisoire ») ; une section « Décisions de Pascal » à la fin de
  `docs/specs/2026-11-27-emerge-regles-sans-source.md` ; les **valeurs attendues** d'un test existant que le plafond
  change, chacune nommée dans le rapport avec l'ancienne et la nouvelle valeur.
- **Câblé (ne peut pas) :** pas de `main` ; pas de « terminé » sans tests verts ; journal en ajout seul.
- **Ne doit pas (écrit) :** modifier `EmergePlafondFctTest.kt` (mission 041) ; éditer `EmergeCoefs.kt` (généré).
- **Sources autorisées :** cette fiche, la 041, les deux specs EMERGE du 2026-11-27.

## Journal

- **Ce que l'agent inscrit :** une ligne par rendu : ligne changée, tests verts, tests existants dont la valeur a
  changé (ancienne → nouvelle).

## Valeur provisoire (chiffre, réglage)

- **Valeur provisoire :** le plafond **0,95** (voir la 041) ; écrit « provisoire » dans la spec et dans le
  commentaire du code.
- **Mesurée par :** cubage réel de tiges minces ; aucune donnée ni date. **Gel :** aucun.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 041 ; branche `feat/042-plafond-fct` partie de `test/041-plafond-fct`.
- **Ordre de fusion :** groupée avec 041 (une PR).
- **PR en brouillon jusqu'à :** la fin de 042 et la relecture.
- **CI rouge attendue :** aucune à la fin de 042.

## Tests

- **Comment saura-t-on que c'est juste :** les six cas de la 041 verts ; `.claude/test-command` vert ; tout test
  existant changé est listé avec sa raison.

## Terrain

- **Échantillon que Pascal vérifiera :** sur le téléphone, un **hêtre C 25 cm, H 20 m** en EMERGE : volume de tige
  et de houppier avant (v2026.11.5) et après la release ; le rapport donne les deux valeurs attendues.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Point de suivi :** à la
  fin de la 041, puis à la PR. · **Qui tranche :** Pascal. Les volumes de tiges minces changent pour tous les
  marteleurs qui cubent en EMERGE : la note de version le dit.

## Comment saura-t-on que ça marche ?

Après la release, le hêtre C 25 / H 20 montre la tige calculée avec 0,95, et un hêtre de C 24 ou 26 cm ne fait pas
de saut de volume.
