# ADR 0002 — Briques communes dans la bibliothèque `terrain` (JitPack) : on corrige là-bas

- **Date :** 2026-10-17 (premier déplacement, commit 3377464) ; consignée le 2026-10-24
- **Statut :** acceptée par Pascal (règle en mémoire de l'orchestrateur, « Bibliothèque terrain » ; application dans le dépôt : `README.md` l. 107-116)
- **Proposée par :** brief Surculus du 2026-10-15 « bibliothèques communes », traité le 2026-10-17 (`pilotage/journal.md` l. 23)
- **Tranchée par :** Pascal

## La décision en une phrase

Le NMEA, le NTRIP, la géodésie, la fusion du journal, la lecture GPKG, la dictée et les briques de carte vivent dans `github.com/pobsteta/terrain`, publiée par tag sur JitPack ; une correction de ces briques se fait là-bas, pas dans Marculus.

## Contexte

Marculus, Groma et Surculus doivent partager des briques de terrain (`README.md` l. 107-116 ; seul Marculus l'utilise aujourd'hui, Groma et Surculus « doivent l'adopter », journal l. 34). Modules repris : `terrain-gnss` (3377464, 2026-10-17), `terrain-journal` (86f88d4, v0.2.0), `terrain-gpkg` (95ed602), `terrain-voix` (6622d3d, v0.4.0), `terrain-carte` (87c3cf4, v0.5.0). Version dans `gradle/libs.versions.toml` l. 18 (`terrain = "v0.5.0"`) ; dépôt Maven dans `settings.gradle.kts` l. 20-21, limité au groupe `com.github.pobsteta.terrain`.

Alternatives (copie dans chaque application, module local) : non écrit. Le miroir Google contre le code 429 de JitPack est noté en mémoire de l'orchestrateur, non versé au dépôt.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | Plus simple ? | Une version par tag, un seul endroit où corriger | Non écrit | Non écrit |
| **R** | Qui fait quoi ? | Les corrections vont chez `terrain` (brief `vers-terrain/2026-10-24-marculus-palette-reste-verte.md`, journal l. 65) | Un aller-retour entre dépôts | Non écrit |
| **A** | Dépendances ? | JitPack (429 constaté, miroir Google, mémoire de l'orchestrateur) | Disponibilité d'un service tiers | Non écrit |
| **N** | Toutes les idées ? | Non écrit | | |
| **C** | Coût ? | Non écrit | | |
| **É** | Réutilisation ? | C'est le but : trois applications, un code (`README.md` l. 107-116) | | Non écrit |

## Valeurs sacrifiées

Corriger vite dans Marculus seul : l'autonomie d'une application est cédée à la cohérence entre applications. Justification chiffrée : non écrit.

## Sources

- `README.md` l. 107-116 ; `gradle/libs.versions.toml` l. 18, 29-33 ; `settings.gradle.kts` l. 20-21.
- Commits 3377464, 86f88d4, 95ed602, 6622d3d, 87c3cf4 ; `pilotage/journal.md` l. 23, 34, 65.

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ? Réponse de Pascal : non écrite.
