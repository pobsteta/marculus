# Mission 009 — tests : numéro de tarif à 0 hors Schaeffer

## Mission

**En une phrase (quoi, et pourquoi) :** écrire, avant le code et sans le lire, les tests JVM
qui exigent un numéro de tarif à 0 pour `AUCUN` et `EMERGE`, dans le modèle et dans le CSV
exporté, parce qu'un passage de Schaeffer à EMERGE laisse aujourd'hui l'ancien numéro
(`Hêtre=EMERGE:9`), ce qui trompe le lecteur du CSV (relecture de la mission 004, 2026-10-24).

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | contrat ci-dessous ; idiome de `core/src/test/.../export/ExportCsvTest.kt` |
| A-t-il les moyens ? | oui | `:core:test` en local et en CI |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## Contrat (que la mission 010 doit fournir)

- Dans `core/src/main/kotlin/fr/marculus/core/model/Modeles.kt` : `fun TarifEssence.normalise(): TarifEssence`,
  qui garde `numero` si `tarif.estSchaeffer`, et le met à 0 sinon (`AUCUN`, `EMERGE`).
  (`TarifCubage.estSchaeffer` existe déjà.)
- Le CSV exporté (`ExportCsv`) n'écrit jamais un numéro non nul hors Schaeffer, même si le
  contexte enregistré en porte un (données d'avant le correctif) : ni dans `TarifsEssence`
  (`Hêtre=EMERGE:0`), ni dans `TarifNumero` quand `Tarif` vaut `EMERGE` ou `AUCUN`.

## L'Homme

- **Agent :** `testeur` · **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** l'écran de création du contexte, l'import `.marsync`
  (code Android, vérifiés par le relecteur et par Pascal) ; faire passer les tests.

## Harnais

- **Peut :** créer `core/src/test/kotlin/fr/marculus/core/model/TarifEssenceNormaliseTest.kt` et
  ajouter des cas dans `core/src/test/kotlin/fr/marculus/core/export/ExportCsvTest.kt`, dans sa branche.
- **Ne doit pas :** lire ni modifier `core/src/main/`, `data/src/main/`, `app/src/main/` ; toucher à `main`.
- **Sources autorisées :** cette fiche, `core/src/test/` (idiome et appels existants de `ExportCsv`).

## Tests

- **Comment saura-t-on que c'est juste :** `normalise` sur les 6 valeurs de `TarifCubage` avec un
  numéro non nul ; un export d'un contexte dont une essence est `EMERGE` avec numéro 9 donne
  `EMERGE:0`, et `Tarif;EMERGE` est suivi de `TarifNumero;0` ; un Schaeffer garde son numéro.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal (une seule PR avec 010).
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Aucun CSV exporté ne contient plus `EMERGE:` suivi d'autre chose que 0.
