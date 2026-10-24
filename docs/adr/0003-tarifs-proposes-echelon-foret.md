# ADR 0003 — `tarifsProposes` : seul l'échelon `foret` est lu

- **Date :** 2026-10-23
- **Statut :** acceptée par Pascal (ligne `cap`, `pilotage/journal.md` l. 22) ; **pas encore dans le code** (voir Contexte)
- **Proposée par :** brief de nemetonshiny du 2026-10-16 (« prime en entier ») ; amendement : Pascal
- **Tranchée par :** Pascal

## La décision en une phrase

Dans `tarifsProposes`, Marculus ne lit que le niveau `foret` ; `ser`, `greco` et `national` sont ignorés.

## Contexte

Ancien : `tarifsProposes` « prime en entier ». Nouveau : seul `foret` est lu (journal l. 22). Pourquoi : `ser`, `greco` et `national` font double emploi avec les tables IFN embarquées en v0.62.0 ; même règle et mêmes données des deux côtés, seul l'échelon forêt manque au téléphone (journal l. 22 ; tables : `docs/specs/2026-10-16-tarifs-ser.md`). Fait : brief `~/dev/briefs/vers-nemetonshiny/2026-10-23-marculus-tarifs-proposes-niveau-foret.md`, carte au bac à sable (fichier hors dépôt, non revérifié ici).

État : une recherche de `tarifsProposes` dans les sources Kotlin du dépôt ne trouve rien le 2026-10-24 ; la lecture n'est donc pas livrée. Le même jour, Pascal garde l'échelon forêt au bac à sable « malgré l'immobilité » (journal l. 72).

Alternative : lire les quatre échelons (position initiale du brief). Autres : non écrit.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | Plus simple ? | Un seul échelon à lire ; le reste existe déjà sur le téléphone (journal l. 22) | Non écrit | Non écrit |
| **R** | Qui fait quoi ? | nemetonshiny émet, Marculus lit ; nemetonshiny relit `ser` et `millesimeTarifs` (journal l. 79) | | |
| **A** | Imprévu ? | Tables IFN embarquées : fonctionne sans réseau | | |
| **N** | Toutes les idées ? | Non écrit | | |
| **C** | Coût ? | Évite de dupliquer des règles (journal l. 22) | | |
| **É** | Réutilisation ? | Réutilise les tables v0.62.0 | | |

## Valeurs sacrifiées

Une surcharge de nemetonshiny sur les échelons SER, GRECO et national : refusée car doublon. Pas d'autre sacrifice écrit.

## Sources

- `pilotage/journal.md` l. 22, 72, 79 ; `docs/specs/2026-10-16-tarifs-ser.md`.

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ? Réponse de Pascal : non écrite.
