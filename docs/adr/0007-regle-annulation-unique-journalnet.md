# ADR 0007 — Une seule règle d'annulation, dans `JournalNet`

- **Date :** 2026-11-10
- **Statut :** **décision acceptée par Pascal** (« Je valide les 4 », puis « (a) », journal du 2026-11-10) ; **texte de l'ADR proposé**, à valider par Pascal à la lecture de la PR ; livrée par la PR #49 (76bd939)
- **Proposée par :** relecteur de la 019 (copies de la règle, journal du 2026-11-10) ; troisième copie trouvée par le dev de la 022
- **Tranchée par :** Pascal

## La décision en une phrase

La règle « une annulation retire la dernière tige de sa case » n'est écrite qu'une fois, dans `JournalNet.tiges` ; tout calcul au net (volumes, lots, Par parcelle, CSV, boîte « Ouvrir avec ») en part.

## Contexte

La règle existait en trois copies : `VolumesMartelage.kt` l. 84-102 et `JournalNet.kt` l. 18-40 (relecture de la 019, journal du 2026-11-10), puis `AffouageLots.etat` (`AffouageLots.kt` l. 91-104, trouvée par le dev de la 022). Une correction dans l'une aurait laissé les autres : Statut aurait divergé de Par lot, Par parcelle et des CSV (fiche 022). Deux nombres différents pour un même contexte avaient déjà été vus aux missions 018 et 019.

Résultat : `VolumesMartelage.totaux` (0 ; PR #49, d381c40, +5/−21) puis `AffouageLots.etat` (0109aa8, +7/−26) partent de `JournalNet.tiges`. `grep removeLast|ArrayDeque` dans `core/src/main` ne trouve plus que `JournalNet.kt` (journal du 2026-11-10, dev 022). La règle elle-même est décrite dans la spec § 5.

Comportement gardé tel quel, signalé à Pascal par le testeur : le lot courant ne redescend pas quand on annule la seule tige d'un lot (lot 2, cumul 0).

Alternatives : laisser les copies « équivalentes » et les tester ; écartée, sans autre raison écrite que le risque de divergence (fiche 022). Filtres `VIEW`/`SEND` : pas d'ADR sans accord de Pascal.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | La plus simple qui tienne ? | Une pile, un fichier (`JournalNet.kt`) | Tout calcul dépend de lui | Oui (« (a) », journal du 2026-11-10) |
| **R** | Qui fait quoi ? | Testeur (16 puis 18 tests d'égalité, d6d4f75), dev (d381c40, 0109aa8), relecteur (prêt) | | |
| **A** | Et si l'imprévu arrive ? | Tests d'égalité `VolumesMartelage.totaux` = somme des cubages de `JournalNet.tiges`, verts avant et après | | |
| **N** | Idées étranges écoutées ? | Non écrit | | |
| **C** | Coût pour l'ensemble ? | Non écrit | | |
| **É** | Que réutilisons-nous ? | `JournalNet.tiges`, déjà utilisé par `AffouageLots` et `ExportCsv` (journal du 2026-11-09, testeur 019) | | |

## Valeurs sacrifiées

Non écrit. Réserve du relecteur : l'oracle de `EtatLotUneRegleTest` (l. 45-51) est recopié en production, valable parce qu'il est passé sur l'ancien code.

## Sources

- `pilotage/journal.md` du 2026-11-09 (testeur 019, décision technique) et du 2026-11-10 (022, « (a) », relecteur) ; PR #49 (76bd939).

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ? Réponse de Pascal : non écrite.
