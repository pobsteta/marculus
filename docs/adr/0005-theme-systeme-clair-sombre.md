# ADR 0005 — Thème : choix Système / Clair / Sombre ; jetons `terrain-style` annulés

- **Date :** 2026-10-24
- **Statut :** **décision acceptée par Pascal** pour le choix du thème (v0.64.6, v0.65.0) ; la réadoption de `terrain-style` est **suspendue**, pas refusée ; **texte de l'ADR proposé**, à valider par Pascal à la lecture de la PR
- **Proposée par :** non écrit ; annulation décidée par Pascal (journal l. 65)
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
| **F** | Est-ce la solution la plus simple qui tienne ? Réparable avec les moyens du bord ? | Retour aux couleurs v0.63 (revert, journal l. 65) | Pas de palette commune pour l'instant | Non écrit |
| **R** | Qui fait quoi, et chacun a-t-il les moyens de sa part ? | `terrain` produit la palette, Marculus l'essaie (journal l. 65, 99) | Un brief de plus | Non écrit |
| **A** | Que se passe-t-il si ce qu'on n'a pas prévu arrive ? De qui ou de quoi dépendons-nous ? | Lisibilité au soleil : essai daté 2026-10-30 (journal l. 99) ; **fait et clos le 2026-10-24** : thèmes clair et sombre actuels jugés conformes par Pascal (journal l. 91 et ligne du 2026-10-24 « essai au soleil clos ») | | |
| **N** | Avons-nous écouté toutes les idées, même les plus étranges ? | Non écrit | | |
| **C** | Que coûte cette décision à l'ensemble ? Y a-t-il une dépense inutile ? | Non écrit | | |
| **É** | Que réutilisons-nous, et qui pourra réutiliser ce que nous faisons ? | Palette commune visée, reportée (journal l. 65) | | |

## Valeurs sacrifiées

La cohérence visuelle immédiate avec les autres applications qui adopteront `terrain-style` : sacrifiée parce que le vert avait disparu (journal l. 63, 65). Autre raison : non écrit.

## Sources

- `pilotage/journal.md` l. 62-67, 70, 75, 83, 90-92, 99 ; PR #9, #12, #15, #20 ; releases v0.64.0, v0.64.2, v0.64.6, v0.65.0.

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ? Réponse de Pascal : non écrite.

## Révision du 2026-10-24 — la palette commune est celle de Marculus

Décision de Pascal (journal, ligne `cap` du 2026-10-24 « la palette de `terrain-style` est celle de
Marculus v0.65.0 ») : Marculus, Groma et Surculus utilisent `terrain-style`, dont les jetons
deviennent les couleurs actuelles de Marculus ; elles s'imposent à Groma et Surculus. La règle 7:1
tombe comme règle commune (blanc sur `VertVif` = 5,17:1, jugé conforme au soleil par Pascal).
Marculus réadoptera `terrain-style` (v0.6.1) sans changement visible (mission 006).

| Rôle | Clair | Sombre |
|---|---|---|
| `primary` | `VertVif #3E7A40` | `VertVif #3E7A40` |
| `onPrimary` | blanc `#FFFFFF` | blanc `#FFFFFF` |
| `primaryContainer` | `VertMarculus #374742` | `VertMarculus #374742` |
| `onPrimaryContainer` | `Creme #F5EBD8` | `Creme #F5EBD8` |
| `secondary` | `VertMarculus #374742` | `Creme #F5EBD8` |
| les 43 autres rôles | valeurs par défaut de `lightColorScheme()` | valeurs par défaut de `darkColorScheme()` |

Source : `app/src/main/kotlin/io/github/pobsteta/marculus/ui/theme/Theme.kt` au tag `v0.65.0` de Marculus
(l. 10-28), Compose BOM `2026.02.01` (`gradle/libs.versions.toml` l. 5) pour les valeurs par défaut.
