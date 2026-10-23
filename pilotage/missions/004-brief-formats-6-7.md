# Mission 004 — signaler à nemetonshiny les formats CSV 6 et 7

## Mission

**En une phrase (quoi, et pourquoi) :** écrire le brief qui décrit à `nemetonshiny` les formats
`FormatCsv;6` et `;7` de Marculus, parce qu'il ne lit que les formats 2 à 5 et que les
contextes exportés depuis la v0.62 portent des clés qu'il ne connaît pas.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | modèle : `~/dev/briefs/traites/2026-10-16-traite-marculus-csv-format-5-houppier.md` |
| A-t-il les moyens ? | oui | `core/.../export/ExportCsv.kt`, `ExportCsvTest.kt`, specs `2026-10-16-tarifs-ser.md` et v1 § 7 ; côté lecteur : `~/dev/nemetonshiny/R/service_marculus_import.R` (lecture seule) |

**Temps de la main :** appuyer — un format d'échange est un contrat : il rend le brief à
l'orchestrateur, qui le relit avec Pascal avant dépôt.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `archiviste`
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** modifier le format, le code de Marculus ou celui de
  `nemetonshiny`.

## Harnais

- **Peut :** écrire un brouillon de brief dans son dossier de travail, et le rendre.
- **Ne doit pas :** déposer lui-même dans `~/dev/briefs/vers-nemetonshiny/` ; c'est
  l'orchestrateur qui dépose, après accord de Pascal. Pas de PR dans Marculus.
- **Contenu attendu :** les lignes d'en-tête et colonnes ajoutées par le format 6 (tarif par
  essence, stères) et le format 7 (`CommuneInsee`, `Ser`, `MillesimeTarifs`), avec la version
  de Marculus qui les a introduites et un extrait réel issu de `ExportCsvTest.kt`.

## Tests

- **Comment saura-t-on que c'est juste :** chaque clé du brief existe dans `ExportCsv.kt`, et
  aucune clé émise par les formats 6 et 7 n'y manque (comparaison avec le format 5).

## Terrain

- **Échantillon que Pascal vérifiera :** un CSV exporté de son téléphone, mis en regard du brief.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (sur le brouillon), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** à la remise du brouillon.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

`nemetonshiny` importe un CSV au format 7 sans le refuser, et son aide dit « FormatCsv;2 à 7 ».
