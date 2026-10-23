# Équipe de Marculus

> Produit par l'orchestrateur avec `/imperatrice:mission`, **validé par Pascal** avant
> tout démarrage. Règle de staffing : un agent par mission qui passe les trois
> questions, jamais un agent « par principe ».

- **Statut :** validée
- **Validée par :** Pascal, le 2026-10-23 (« je valide »)

## Plafond humain

- **PR ouvertes en même temps, au plus :** 3
- **Rythme de relecture de Pascal :** _à fixer par Pascal._

## Le vivier (catalogue, pas une équipe)

| Agent | A / plan | Modèle | Écrit du code ? |
|---|---|---|---|
| `contradicteur` | Abstraction · Polos | opus | non |
| `acquereur` | Acquisition · Ponos | sonnet | non (référentiels et données) |
| `archiviste` | Archivage · Pontos | sonnet | non (docs) |
| `analyste` | Analyse · Ponos | sonnet | non |
| `afficheur` | Affichage · Ponos | sonnet | oui (interface) |
| `dev` | réalisation · Ponos | sonnet | oui |
| `testeur` | réalisation · Ponos | sonnet | oui (tests) |
| `relecteur` | contrôle · C3 | opus | non (lecture seule) |

## Missions

| Mission | Agent | Modèle | Temps de la main | Les 3 questions | Cycle |
|---|---|---|---|---|---|
| [001](missions/001-ci-tests-data.md) tests de `:data` en CI | `dev` | sonnet | appuyer | oui · oui · oui | 1 |
| [002](missions/002-readme-terrain.md) README : utilisateurs de `terrain` | `archiviste` | sonnet | appuyer | oui · oui · oui | 1 |
| [003](missions/003-spec-a-jour.md) spec à jour du livré | `archiviste` | sonnet | appuyer (plan d'abord) | oui · oui · oui | 1 |
| [004](missions/004-brief-formats-6-7.md) brief formats CSV 6 et 7 | `archiviste` | sonnet | appuyer (brouillon relu) | oui · oui · oui | 1 (pas de PR) |

**Pas encore de mission** (n'ont pas passé les trois questions) :

| Carte | Question qui bloque | Ce qu'il faut |
|---|---|---|
| Échelon forêt de `tarifsProposes` | 3 (moyens) | réponse de `nemetonshiny` (§ 4 du brief du 2026-10-23) et un lot `.zip` d'essai avec un niveau `foret` |
| Jetons `terrain-style` dans `Theme.kt` | 3 (moyens) | `terrain-style` publié |
| Unité en suffixe des champs (rouvrir la PR #3) | 1 (sens) | décision de Pascal |
| Unité d'un lot d'affouage, repli sans réseau, appareil perdu | 1 (sens) | décisions de Pascal (brief) |
| Recettes terrain affouage et dictée | — | Pascal sur le terrain, pas un agent |
| Hauteurs estimées contre mesurées | 1 (sens) | décider si la distinction est nécessaire |

