# Mission 015 — la release attend les tests de `main`

## Mission

**En une phrase (quoi, et pourquoi) :** faire que `release.yml` ne fabrique et ne publie l'APK
signé que si les tests de `ci.yml` sont verts sur le commit de `main`, parce qu'aujourd'hui les deux
workflows partent en parallèle au merge (`release.yml` n'a ni `needs` ni `workflow_run`, l. 3-5 et
l. 71) et qu'une release peut sortir sur un `main` dont les tests échouent (relecture de la 014 ;
décision de Pascal du 2026-10-24 : « oui lance la CI »).

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | deux voies publiées : déclencher `release.yml` par `workflow_run` sur la réussite de `CI` sur `main`, ou y lancer `.claude/test-command` avant `assembleRelease` ; choisir la plus simple et le dire |
| A-t-il les moyens ? | oui | `.github/workflows/ci.yml`, `release.yml` ; la PR elle-même déclenche `ci.yml` |

**Temps de la main :** appuyer (CI : genre réussi une fois, 001).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `dev` · **Modèle :** sonnet. · **Pas de `testeur` :** la mission ne change que le
  déclenchement de la release ; la preuve est dans les runs GitHub.

## Harnais

- **Peut :** modifier `.github/workflows/release.yml` (et `ci.yml` seulement si la voie
  `workflow_run` exige un nom de workflow stable) ; dans sa branche.
- **Ne doit pas :** changer le calcul de version, la signature, les secrets, le nom de l'APK ;
  toucher à `main`.
- **Attention :** `workflow_run` lance le workflow depuis la branche par défaut et sur le
  `head_sha` du run de CI : la release doit construire **ce** commit (`ref: ${{ github.event.workflow_run.head_sha }}`)
  et ne partir que si `conclusion == 'success'` et la branche est `main`.

## Tests

- **Comment saura-t-on que c'est juste :** CI de la PR verte ; après le merge, l'onglet Actions
  montre `Release` démarré **après** la fin verte de `CI` sur le même commit, et la release publiée.
  Le relecteur vérifie qu'un `CI` rouge sur `main` n'aurait pas déclenché de release (lecture des
  conditions).

## Terrain

- **Échantillon que Pascal vérifiera :** la release qui suit le merge de cette PR s'installe sur le
  téléphone comme d'habitude, environ 3 minutes plus tard qu'avant.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Aucune release ne sort d'un `main` dont les tests sont rouges.
