# Mission 001 — tests de `:data` en CI

## Mission

**En une phrase (quoi, et pourquoi) :** faire lancer par la CI les tests JVM de `:data`, pour
que la migration de la base et la fusion des réglages du terrain soient vérifiées à chaque PR,
pas seulement sur le poste de Pascal.

**Redite par l'agent :** faire lancer par la CI les tests JVM de `:data` (migration Room, fusion des réglages du terrain), pour qu'une PR qui les casse échoue en CI. _(reportée par l'orchestrateur, 2026-10-24)_

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | étape Gradle existante à étendre (`.github/workflows/ci.yml`, l. 36-37) |
| A-t-il les moyens ? | oui | 3 tests existants (`ReglagesTerrainTest`, `GpkgRepositoryTest`, `MigrationTest`), passés en local le 2026-10-23 |

**Temps de la main :** appuyer — tâche simple, mais première mission de l'agent.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `dev`
- **Modèle :** sonnet.
- **Pas de `testeur` associé :** la mission ne crée pas de comportement, elle fait tourner des
  tests qui existent déjà.
- **Ce qui ne lui est pas demandé :** écrire de nouveaux tests, toucher au badge de couverture
  de `:core`, modifier `release.yml`.

## Harnais

- **Peut :** modifier `.github/workflows/ci.yml` (ajouter `:data:testDebugUnitTest`), dans sa
  branche.
- **Ne doit pas :** toucher au code de production, à `main`, ni désactiver un test qui échoue :
  s'il échoue en CI, il le signale.
- **Sources autorisées :** `.github/workflows/ci.yml`, `.claude/test-command`,
  `data/build.gradle.kts`, `data/src/test/`.

## Tests

- **Comment saura-t-on que c'est juste :** le job `build-test` de la PR affiche
  `:data:testDebugUnitTest` et passe ; les rapports de test de `:data` sont visibles dans les
  logs.

## Terrain

- **Échantillon que Pascal vérifiera :** la page du run CI de la PR.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** quand la CI de la PR a fini.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

La prochaine PR qui casse une migration Room échoue en CI.
