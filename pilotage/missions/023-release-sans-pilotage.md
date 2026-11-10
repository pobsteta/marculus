# Mission 023 — pas de release quand l'application ne change pas

## Mission

**En une phrase (quoi, et pourquoi) :** ne publier une release que si quelque chose qui entre dans
l'APK a changé depuis la dernière release, pour que chaque numéro de version que Pascal installe
apporte réellement du neuf. Aujourd'hui, toute fusion sur `main` dont la CI est verte publie une
version (`.github/workflows/release.yml` l. 5-25) : sur les 10 dernières, 5 ne changent que
`pilotage/` (v0.68.1 à v0.68.4, v0.70.1) et une que le badge de couverture (v0.69.1,
`.github/badges/jacoco.svg`) — relevé par `git diff --name-only <tag précédent> <tag>` le
2026-11-10. Décision de Pascal du 2026-11-10 : « oui fais les 2 ».

**Redite par l'agent :** ne créer tag, release et APK que si un fichier hors `pilotage/**`, `docs/**`, `*.md`, `.github/badges/**` a changé entre le dernier tag `v*` et le commit testé, pour que chaque version installée apporte du neuf.

## Ce qui existe (preuves)

- **Déclencheur :** `workflow_run` sur la fin de « CI », push de `main`, conclusion `success`
  (`release.yml` l. 5-9 et l. 22-25).
- **Version :** `mathieudutour/github-tag-action@v6.2`, `default_bump: patch`, sur le `head_sha`
  du run de CI (l. 35-43) ; les étapes suivantes ne tournent que si `steps.tag.outputs.new_tag`
  n'est pas vide (l. 46 et suivantes).
- **La CI de `main` annule ses runs en cours** (`ci.yml`, `concurrency: ci-${{ github.ref }}`,
  `cancel-in-progress: true`) : le 2026-11-10, les CI de 07a2245, 02fc671 et a800bb8 ont été
  annulées, et c'est la fusion de pilotage suivante (6041e2d) qui a publié leur code en v0.70.0.

## Règles

- **Comparer à la dernière release, jamais au seul commit précédent :** une fusion de pilotage
  qui suit un code non encore publié (CI annulée) **doit** publier.
- **Ne compte pas comme changement de l'application :** `pilotage/**`, `docs/**`, les `*.md`, et
  `.github/badges/**`. Tout le reste compte (code, ressources, Gradle, workflows) ; en cas de
  doute, on publie.
- Pas de tag, pas de release, pas de construction d'APK quand rien ne compte ; le job se termine
  vert et dit pourquoi dans son résumé.
- Le calcul de version (conventional commits) et la signature ne changent pas.
- La règle de décision vit dans un script versionné et testé (par exemple
  `.github/scripts/`), appelé par `release.yml`, pour pouvoir l'essayer hors de GitHub.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | genre CI déjà réussi deux fois (001, 015) |
| A-t-il les moyens ? | oui | `release.yml`, l'historique des tags pour les cas d'essai |

**Temps de la main :** lâcher (genre CI réussi deux fois, `pilotage/equipe.md` l. 16-18) : fusion
par l'orchestrateur si relecteur « prêt », CI verte et `pilotage/pre-fusion.md` coché.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **`dev` (sonnet)** seul, tests du script compris.

## Harnais

- **Peut :** modifier `release.yml`, ajouter un script et ses tests, et lancer ces tests dans
  `ci.yml` ; dans sa branche.
- **Ne doit pas :** changer le calcul de version, la signature, `default_bump`, ni toucher à
  `main` ; supprimer ou renommer des tags ou releases existants.

## Tests

- Le script, rejoué sur l'historique : il dit « pas de release » pour v0.68.1 à v0.68.4, v0.69.1
  et v0.70.1, et « release » pour v0.68.0, v0.69.0, v0.70.0 (code arrivé par une fusion de
  pilotage après des CI annulées) et v0.70.2.
- **Comment saura-t-on que c'est juste :** après la fusion, la prochaine PR de pilotage ne crée pas
  de version ; la prochaine PR de code en crée une.

## Terrain

- **Échantillon que Pascal vérifiera :** la page Releases ne montre plus de version « docs(pilotage) ».

## Qui veille, qui tranche

- **Relecteur :** `relecteur`. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Chaque nouvelle version de Marculus sur la page Releases change quelque chose dans l'application.
