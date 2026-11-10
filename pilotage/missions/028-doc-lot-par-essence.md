# Mission 028 — la nouvelle règle du lot écrite une fois, et annoncée à nemetonshiny

## Mission

**En une phrase (quoi, et pourquoi) :** écrire la règle « le lot compte les parties cochées de
chaque essence » dans la spec affouage (en remplaçant la règle du 2026-10-24 par un ajout daté),
un ADR 0009, le format CSV 8 dans la spec v1, et un brief `vers-nemetonshiny`, pour que le
successeur et le consommateur des données lisent la même règle que le code des 026 et 027.

**Redite par l'agent :** _à compléter par l'agent avant d'agir._

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment faire ? | oui : 024 pour la forme, ADR 0007 pour le modèle | |
| A-t-il les moyens ? | après la fusion de 026 et 027 (code et captures à citer) | part après elles |

**Temps de la main :** appuyer, plan d'abord.

## L'Homme

- **Agent :** `archiviste` (sonnet).

## Harnais

- **Peut :** `docs/specs/`, `docs/adr/`, `README.md`, un brief dans `~/dev/briefs/vers-nemetonshiny/`.
- **Ne doit pas :** effacer la règle du 2026-10-24 (règle du sommier : ajout daté), toucher au code.

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 026 et 027 fusionnées. **CI rouge attendue :** aucune.

## Tests

- Le relecteur recontrôle chaque preuve ; une seule source pour la règle, les autres renvoient.

## Terrain

- **Échantillon :** nemetonshiny lit l'en-tête `FormatCsv;8` d'un export d'« Affouage ».

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Arrêt :** 3 allers-retours. **Qui tranche :** Pascal.
