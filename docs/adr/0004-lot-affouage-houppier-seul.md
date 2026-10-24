# ADR 0004 — Lot d'affouage : houppier seul quand l'arbre en a un

- **Date :** 2026-10-24
- **Statut :** acceptée par Pascal (lignes `cap`, `pilotage/journal.md` l. 68, 73, 78) ; livrée par la PR #17 (6103049)
- **Proposée par :** archiviste (contradiction relevée entre la spec v1 et `contexte-affouage.md`)
- **Tranchée par :** Pascal

## La décision en une phrase

Le volume d'un lot d'affouage est, pour chaque tige, le volume houppier si l'arbre a un houppier (part de houppier > 0, ou EMERGE couvrant l'essence), sinon le volume de la tige entière.

## Contexte

Ancien : « bois fort tige » (`docs/specs/2026-10-07-contexte-affouage.md`). Nouveau : houppier seul. Pourquoi : c'est ce que fait le code et ce que dit la spec v1 l. 150-153 (journal l. 100).

Règle de Pascal, mot pour mot : « le volume total d'un lot est la somme du volume total de chaque tige si le pourcentage de houppier est à 0, sinon c'est le volume houppier si il y a un pourcentage houppier renseigné » (journal l. 73). En EMERGE, pas de pourcentage, coefficient propre : houppier seul dès qu'EMERGE couvre l'essence, sinon tige entière (confirmé, journal l. 78 ; `AffouageLots.volumes`). Texte en vigueur : spec v1 §5 bis (l. 141-146).

Alternative écartée : conserver « bois fort tige ». Autres : non écrit.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | Plus simple ? | La spec s'aligne sur le code existant (journal l. 100) | Aucun code à changer | Non écrit |
| **R** | Qui fait quoi ? | Pascal tranche, l'archiviste aligne les documents (journal l. 68, 71) | | |
| **A** | Imprévu ? | Sans essence cubée, aucun lot ne se ferme (spec §5 bis) | | |
| **N** | Toutes les idées ? | Non écrit | | |
| **C** | Coût ? | Non écrit | | |
| **É** | Réutilisation ? | Non écrit | | |

## Valeurs sacrifiées

Le bois fort de la tige entière pour les arbres à houppier : non compté dans la borne du lot. Raison : non écrite au-delà de la conformité au code.

## Sources

- `pilotage/journal.md` l. 68, 71, 73, 78, 100 ; `docs/specs/2026-06-21-marculus-v1-spec.md` l. 141-146 ; `docs/specs/2026-10-07-contexte-affouage.md` (note datée) ; PR #17.

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ? Réponse de Pascal : non écrite.
