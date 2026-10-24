# Mission 010 — numéro de tarif à 0 hors Schaeffer

## Mission

**En une phrase (quoi, et pourquoi) :** remettre à 0 le numéro de tarif d'une essence (et du
tarif par défaut) dès qu'il n'est pas Schaeffer, à l'enregistrement du contexte, à l'import
`.marsync` et à l'export CSV, pour que `Hêtre=EMERGE:9` n'existe plus (décision de Pascal du
2026-10-24 ; le cubage n'est pas touché, EMERGE n'utilise pas ce numéro).

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | lieux relevés par le relecteur : `CreationContexteScreen.kt` l. 381, 506, 509, 538 ; `SauvegardeRepository.kt` l. 220 ; `ExportCsv.kt` l. 151-154 |
| A-t-il les moyens ? | oui | tests de la mission 009 |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## Contrat

Celui de la fiche `009-test-numero-hors-schaeffer.md`, section « Contrat ».

## L'Homme

- **Agent :** `dev` · **Modèle :** sonnet. · **`testeur` associé :** mission 009.
- **Ce qui ne lui est pas demandé :** migrer la base (les anciens numéros restent en base,
  l'export les normalise) ; changer le cubage ; changer le format CSV (toujours `FormatCsv;7`).

## Harnais

- **Peut :** ajouter `normalise()` dans `Modeles.kt` ; l'appliquer dans `ExportCsv.kt`,
  `CreationContexteScreen.kt` (enregistrement : `tarifsFinaux` et tarif par défaut) et
  `SauvegardeRepository.kt` (import) ; dans la branche qui porte les tests de 009.
- **Ne doit pas :** modifier les tests de 009 ; toucher à `main`.

## Tests

- **Comment saura-t-on que c'est juste :** tests de 009 verts, `.claude/test-command` vert, CI verte.

## Terrain

- **Échantillon que Pascal vérifiera :** un contexte où une essence passe de Schaeffer lent 11
  à EMERGE, puis export CSV : la ligne `TarifsEssence` montre `EMERGE:0`.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Le lecteur de nemetonshiny n'a plus à ignorer le numéro hors Schaeffer.
