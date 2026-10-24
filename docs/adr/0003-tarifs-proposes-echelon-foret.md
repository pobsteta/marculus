# ADR 0003 — `tarifsProposes` : seul l'échelon `foret` est lu

- **Date :** 2026-10-23
- **Statut :** **décision acceptée par Pascal** (ligne `cap`, `pilotage/journal.md` l. 22) ; **texte de l'ADR proposé**, à valider par Pascal à la lecture de la PR ; **pas encore dans le code** (voir Contexte)
- **Proposée par :** position initiale : brief Marculus du 2026-10-16 (`~/dev/briefs/traites/2026-10-16-traite-marculus-tarifs-ser.md` § 2, « elle prime ») ; amendement : Pascal (journal l. 22)
- **Tranchée par :** Pascal

## La décision en une phrase

Dans `tarifsProposes`, Marculus ne lit que le niveau `foret` ; `ser`, `greco` et `national` sont ignorés.

## Contexte

Ancien : `tarifsProposes` « prime en entier ». Nouveau : seul `foret` est lu (journal l. 22). Pourquoi : `ser`, `greco` et `national` font double emploi avec les tables IFN embarquées en v0.62.0 ; même règle et mêmes données des deux côtés, seul l'échelon forêt manque au téléphone (journal l. 22 ; tables : `docs/specs/2026-10-16-tarifs-ser.md`). Fait : brief `~/dev/briefs/vers-nemetonshiny/2026-10-23-marculus-tarifs-proposes-niveau-foret.md`, carte au bac à sable (fichier hors dépôt ; § 1 cite le brief Marculus du 2026-10-16 et le double emploi des niveaux `ser`, `greco`, `national`).

État : une recherche de `tarifsProposes` dans les sources Kotlin du dépôt ne trouve rien le 2026-10-24 ; la lecture n'est donc pas livrée. Le même jour, Pascal garde l'échelon forêt au bac à sable « malgré l'immobilité » (journal l. 72).

Alternative : lire les quatre échelons (position initiale du brief). Autres : non écrit.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | Est-ce la solution la plus simple qui tienne ? Réparable avec les moyens du bord ? | Un seul échelon à lire ; le reste existe déjà sur le téléphone (journal l. 22) | Non écrit | Non écrit |
| **R** | Qui fait quoi, et chacun a-t-il les moyens de sa part ? | nemetonshiny émet, Marculus lit ; nemetonshiny relit `ser` et `millesimeTarifs` (journal l. 79) | | |
| **A** | Que se passe-t-il si ce qu'on n'a pas prévu arrive ? De qui ou de quoi dépendons-nous ? | Non écrit | | |
| **N** | Avons-nous écouté toutes les idées, même les plus étranges ? | Non écrit | | |
| **C** | Que coûte cette décision à l'ensemble ? Y a-t-il une dépense inutile ? | Évite de dupliquer des règles (journal l. 22) | | |
| **É** | Que réutilisons-nous, et qui pourra réutiliser ce que nous faisons ? | Réutilise les tables v0.62.0 | | |

## Valeurs sacrifiées

Une surcharge de nemetonshiny sur les échelons SER, GRECO et national : refusée car doublon. Pas d'autre sacrifice écrit.

## Sources

- `pilotage/journal.md` l. 22, 72, 79 ; `docs/specs/2026-10-16-tarifs-ser.md`.

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ? Réponse de Pascal : non écrite.
