# ADR 0006 — Numéro de tarif à 0 hors Schaeffer, perdu au retour en Schaeffer

- **Date :** 2026-10-24
- **Statut :** **décision acceptée par Pascal** (journal l. 89 ; Pascal décide la mission correctif, journal l. 72) ; **texte de l'ADR proposé**, à valider par Pascal à la lecture de la PR ; livrée par la PR #18 (15bec4c, v0.64.9)
- **Proposée par :** relecteur (écart sur la mission 004, journal l. 46)
- **Tranchée par :** Pascal

## La décision en une phrase

Un tarif autre que Schaeffer porte toujours le numéro 0 ; repasser une essence d'EMERGE à Schaeffer oblige à ressaisir le numéro.

## Contexte

Écart relevé : passer une essence de Schaeffer à EMERGE gardait l'ancien numéro (`CreationContexteScreen.kt` l. 381, 506 ; import `.marsync`, `SauvegardeRepository.kt` l. 220) : `Hêtre=EMERGE:9` possible (journal l. 46). Correction : `normalise()` à l'enregistrement, à l'import et à l'export (journal l. 82). Le relecteur a posé la question de sens : le numéro est perdu au retour en Schaeffer, la part de houppier est gardée (journal l. 85). Pascal : « le comportement me va pour l'exemple du hêtre » (journal l. 89).

Le numéro Schaeffer reste entier (`CLAUDE.md` l. 51, « numéro de tarif entier »).

Alternative : mémoriser l'ancien numéro pour le retour en Schaeffer ; écartée, sans autre raison écrite que l'exemple du hêtre.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | Est-ce la solution la plus simple qui tienne ? Réparable avec les moyens du bord ? | Une règle : hors Schaeffer, 0 (journal l. 82) | Ressaisie au retour | Oui (journal l. 89) |
| **R** | Qui fait quoi, et chacun a-t-il les moyens de sa part ? | Testeur 009 (222a792), dev 010, relecteur (journal l. 81-85) | | |
| **A** | Que se passe-t-il si ce qu'on n'a pas prévu arrive ? De qui ou de quoi dépendons-nous ? | Anciens fichiers à numéro non nul : normalisés à l'import (journal l. 82) | | |
| **N** | Avons-nous écouté toutes les idées, même les plus étranges ? | Non écrit | | |
| **C** | Que coûte cette décision à l'ensemble ? Y a-t-il une dépense inutile ? | Non écrit | | |
| **É** | Que réutilisons-nous, et qui pourra réutiliser ce que nous faisons ? | Non écrit | | |

## Valeurs sacrifiées

La commodité de retrouver le numéro après un détour par EMERGE. Raison : non écrit au-delà de l'exemple du hêtre (journal l. 89).

## Sources

- `pilotage/journal.md` l. 46, 81-82, 85-86, 89, 96 ; PR #18 (15bec4c).

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ? Réponse de Pascal : non écrite.
