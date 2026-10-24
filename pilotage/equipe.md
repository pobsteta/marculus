# Équipe de Marculus

> Produit par l'orchestrateur avec `/imperatrice:mission`, **validé par Pascal** avant
> tout démarrage. Règle de staffing : un agent par mission qui passe les trois
> questions, jamais un agent « par principe ».

- **Statut :** validée
- **Validée par :** Pascal, le 2026-10-23 (« je valide »)

## Plafond humain

- **PR ouvertes en même temps, au plus :** 3 (confirmé par Pascal le 2026-10-24)
- **Rythme de relecture de Pascal :** _à fixer par Pascal._
- **Ce qui est lâché (décision de Pascal du 2026-10-24, « rend la main sur tout ce qui est possible ») :**
  - les PR de pilotage seul (`pilotage/`, sans code ni spec) : fusionnées par l'orchestrateur, CI verte ;
  - une mission dont le genre a déjà réussi deux fois : « lâcher », fusion par l'orchestrateur si le
    relecteur dit « prêt » et que la CI est verte ; sinon elle remonte à Pascal. Genres réussis une
    fois au 2026-10-24 : CI (001), README (002), brief vers une autre application (004, en 3 allers-retours).
  - jamais lâché : une mission « tenir » (003, 006), un changement de règle métier, de modèle de données ou de dépendance.

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
| [005](missions/005-test-theme-terrain-style.md) test d'égalité du thème | `testeur` | sonnet | appuyer | oui · oui · oui | 2 |
| [006](missions/006-theme-terrain-style.md) thème = jetons `terrain-style` | `dev` | sonnet | tenir (plan d'abord) | oui · à moitié · oui | 2, annulée ; attend v0.6.1 |
| [007](missions/007-test-choix-theme.md) tests du choix du thème | `testeur` | sonnet | appuyer | oui · oui · oui | 3 |
| [008](missions/008-choix-theme.md) choix du thème Système / Clair / Sombre | `dev` | sonnet | appuyer | oui · oui · oui | 3 |
| [009](missions/009-test-numero-hors-schaeffer.md) tests numéro hors Schaeffer | `testeur` | sonnet | appuyer | oui · oui · oui | 3 |
| [010](missions/010-numero-hors-schaeffer.md) numéro à 0 hors Schaeffer | `dev` | sonnet | appuyer | oui · oui · oui | 3 |

**Pas encore de mission** (n'ont pas passé les trois questions) :

| Carte | Question qui bloque | Ce qu'il faut |
|---|---|---|
| Échelon forêt de `tarifsProposes` | 3 (moyens) | réponse de `nemetonshiny` (§ 4 du brief du 2026-10-23) et un lot `.zip` d'essai avec un niveau `foret` |
| Couleurs en dur hors du thème (19) | 1 (sens) | après l'essai au soleil du 2026-10-30 |
| Unité en suffixe des champs (rouvrir la PR #3) | 1 (sens) | décision de Pascal |
| Unité d'un lot d'affouage, repli sans réseau, appareil perdu | 1 (sens) | décisions de Pascal (brief) |
| Recettes terrain affouage et dictée | — | Pascal sur le terrain, pas un agent |
| Hauteurs estimées contre mesurées | 1 (sens) | décider si la distinction est nécessaire |

