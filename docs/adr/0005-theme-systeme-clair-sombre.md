# ADR 0005 — Thème : choix Système / Clair / Sombre ; jetons `terrain-style` annulés

- **Date :** 2026-10-24
- **Statut :** acceptée par Pascal pour le choix du thème (v0.64.6, v0.65.0) ; la réadoption de `terrain-style` est **suspendue**, pas refusée
- **Proposée par :** orchestrateur (missions 007-008) ; annulation décidée par Pascal (journal l. 65-66)
- **Tranchée par :** Pascal

## La décision en une phrase

Le thème se choisit entre Système, Clair et Sombre ; les jetons `terrain-style` v0.6.0 sont retirés (v0.64.2) et ne reviennent qu'avec une palette qui reste verte et un essai au soleil.

## Contexte

Les jetons `terrain-style` v0.6.0 (PR #9) ont été adoptés (v0.64.0, journal l. 62). Vu sur le téléphone : plus de vert du tout, bandeau et bouton « + » vert-gris pâle en sombre, presque noir en clair (journal l. 63). Cap de Pascal : revert (PR #12, v0.64.2), avec un brief à `terrain` : palette qui reste verte, nuancier montré à Pascal avant publication, `reussi` distinct de `primary` (journal l. 65, brief `~/dev/briefs/vers-terrain/2026-10-24-marculus-palette-reste-verte.md`). Le changement de couleur était un coût accepté d'avance (journal l. 63) ; la couleur n'avait pas été regardée à l'écran avant la fusion (leçon, journal l. 67).

Second défaut : le réglage « Thème sombre » ne pouvait que forcer le sombre (`MainActivity.kt` l. 63, journal l. 64). Remplacé par l'enum `ChoixTheme` (clé `theme`, ancienne clé `theme_sombre` lue seulement), PR #15, v0.64.6 (journal l. 70, 75) ; liste déroulante avec coche, PR #20, v0.65.0 (journal l. 83, 90).

Au soleil, Pascal juge les thèmes de la v0.65.0 « parfaits » (journal l. 91). Le critère du brief Surculus « blanc sur `VertVif` (5,17:1) mal lisible » reste à confirmer par l'essai du 2026-10-30 (journal l. 92, 99).

Alternatives : garder `terrain-style` v0.6.0 (écarté, journal l. 65) ; autres : non écrit.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | Plus simple ? | Retour aux couleurs v0.63 (revert, journal l. 65) | Pas de palette commune pour l'instant | Oui, par le cap |
| **R** | Qui fait quoi ? | `terrain` produit la palette, Marculus l'essaie (journal l. 65, 99) | Un brief de plus | Non écrit |
| **A** | Imprévu ? | Lisibilité au soleil : essai daté 2026-10-30 (journal l. 99) | | |
| **N** | Toutes les idées ? | Non écrit | | |
| **C** | Coût ? | Non écrit | | |
| **É** | Réutilisation ? | Palette commune visée, reportée (journal l. 65) | | |

## Valeurs sacrifiées

La cohérence visuelle immédiate avec les autres applications qui adopteront `terrain-style` : sacrifiée contre l'identité verte et la lisibilité au soleil (journal l. 63, 65). Raison détaillée au-delà : non écrit.

## Sources

- `pilotage/journal.md` l. 62-67, 70, 75, 83, 90-92, 99 ; PR #9, #12, #15, #20 ; releases v0.64.0, v0.64.2, v0.64.6, v0.65.0.

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ? Réponse de Pascal : non écrite.
