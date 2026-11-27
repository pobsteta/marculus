# ADR 0013 — Numéros de version calendaires `vAAAA.MM.N`, compteur par mois, à la place du SemVer `v0.Y.Z`

- **Date :** 2026-11-11 (décision) ; texte écrit le 2026-11-27
- **Statut :** **décision acceptée par Pascal** (cap du 2026-11-11, journal l. 307), livrée par la PR #77 (a28dc2d, mission 036, première release `v2026.11.1`) ; **texte de l'ADR proposé**, à valider par Pascal à la lecture de la PR
- **Proposée par :** Pascal (le cap est de lui, journal l. 307)
- **Tranchée par :** Pascal

Règle du bilan n° 3 : chaque fait cité dit l'état de sa source. Journal lu dans `pilotage/journal.md` à `0c6b889` (`main`, 2026-11-27) ; clé d'une ligne = date · auteur · mission.

## La décision en une phrase

La CI de release numérote chaque version `vAAAA.MM.N` (année et mois UTC de la release, `N` = compteur du mois qui repart à 1 chaque mois), calculée à partir des tags existants, au lieu de déduire un numéro SemVer `v0.Y.Z` des types de commits ; ce cap défait le SemVer (journal l. 396, bilan n° 3 : « 220 et 307 défont des règles de Pascal, aucune décision déléguée défaite »).

## Contexte

- Avant : `mathieudutour/github-tag-action@v6.2`, SemVer d'après les commits conventionnels (`default_bump: patch`, `tag_prefix: v`) ; relevé dans la fiche à `44ca151` (`pilotage/missions/036-version-calendaire.md`, « Ce qui existe »). Ce que la fiche cite de `release.yml` est de l'ancienne version du fichier ; l'ancien contenu n'est pas relu ici.
- Le cap : « numéros SemVer `v0.Y.Z` → calendaires `vAAAA.MM.N`, compteur par mois comme nemeton » ; pourquoi : « même forme que nemeton (`~/dev/nemeton`, v2026.10.1 → v2026.11.22) » (journal 2026-11-11 · Pascal · —, l. 307). La fiche ajoute le but : « qu'on lise d'un coup d'œil de quand date une version » (fiche 036, l. 5-7).
- Raison de préférer le calendaire au SemVer au-delà de la ressemblance avec nemeton : **non écrite** dans le journal. Raison de choisir un compteur **par mois** (et non par année ou global) : « décision de Pascal du 2026-11-11 (compteur par mois) » (fiche 036, l. 8), raison non écrite.
- `versionCode` Android : `github.run_number` (`release.yml` l. 100), indépendant du nom de la version ; les mises à jour par-dessus une `v0.73.x` restent possibles (README l. 213-214 ; fiche 036).

## La règle (source unique)

Le script `.github/scripts/version-calendaire.sh` calcule le prochain tag (sur `main` à `0c6b889`) :

- mois = `AAAA.MM` UTC du jour (ou argument) ; seuls comptent les tags `vAAAA.MM.<entier>` de ce mois ; tri numérique (`.9` puis `.10`) ; prochain `N` = plus grand + 1, ou 1 ; les anciens `v0.*` et les autres mois n'influent pas (script, 12 lignes : en-tête l. 3-5, mois invalide l. 6-10, calcul l. 11-12).
- `release.yml` l. 46-58 : le tag est **annoté**, posé sur `head_sha` du run de CI, poussé ; `versionName` = tag sans le `v` (l. 58, 99).
- Test : `.github/scripts/test-version-calendaire.sh` (13 vérifications, dépôt temporaire et tags synthétiques), lancé par `ci.yml` l. 27.
- Dite une fois pour les humains dans `README.md` l. 208-214.
- Effet attendu : « Le type de commit (`fix`, `feat`) n'y change rien » (README l. 211-212) : on ne lit plus dans le numéro la gravité du changement ; on la lit dans la note de version.

Lien avec l'ADR 0008 (release conditionnelle) : `release-necessaire.sh` choisit **si** une release part, `version-calendaire.sh` choisit **quel nom** elle porte. `release-necessaire.sh` cherche le dernier tag avec `--match 'v*'` (l. 41), qui trouve les tags `v0.*` et `vAAAA.MM.N` mélangés (README l. 211-213).

## Ce qui est livré, avec les preuves

| Où | Quoi | Preuve |
|---|---|---|
| Script et tests | `version-calendaire.sh`, 13 tests | PR #77 (a28dc2d) ; journal l. 309 (2026-11-11 · dev · 036, commit 2df9998) |
| CI | remplace `github-tag-action` dans `release.yml` ; tag annoté sur `head_sha` ; notes générées | journal l. 309 ; `release.yml` l. 46-58 |
| Documentation | README et `pilotage/pre-fusion.md` mis à jour | journal l. 309 ; README l. 208-214 |
| Relecture | prêt ; à signaler : `web/index.html` l. 44 et spec l. 305 encore en SemVer (hors harnais) ; tag sans release si le build échoue après l'envoi du tag (défaut hérité) renvoyé au bac à sable | journal l. 310 (2026-11-11 · relecteur · 036) ; à `0c6b889`, le README ne dit plus « SemVer », mais `web/index.html` l. 44 garde `?sort=semver` (paramètre du badge de version, non vérifié quant à son effet sur l'affichage des tags calendaires) ; la spec v1 l. 314 dit encore « releases SemVer automatiques » (trou de mémoire relevé, hors périmètre de la 046, à corriger par la 045) |
| Première release | `Marculus v2026.11.1`, tag annoté sur a28dc2d ; installée sur le téléphone par-dessus v0.73.2 (`versionName=2026.11.1`, `versionCode=183`) | journal l. 311 (2026-11-11 · Claude (orchestrateur) · 036) |
| Suite | v2026.11.2 (PR #76) ; tags `v2026.11.1` à `v2026.11.6` sur `main` à `0c6b889` (`git tag --list 'v2026*'`) | journal l. 312 ; commande lancée le 2026-11-27 |

Le dernier tag SemVer est `v0.73.3` (journal l. 304) : les `v0.*` restent en place (README l. 211).

## Conséquences et risques nommés

- **Le numéro ne dit plus la gravité** : un correctif et une fonction nouvelle se suivent de la même façon. Compensation : la note de version (fiche 047 sur son titre ; journal l. 391 pour la note de v2026.11.6).
- **Le compteur est lu dans les tags** : un tag supprimé ou posé à la main change le prochain numéro. Le test couvre `.9` → `.10`, le changement de mois, les anciens tags `v0.*` ; la reprise après un tag supprimé n'est pas écrite.
- **Tag sans release** si le build échoue après l'envoi du tag : défaut hérité, laissé au bac à sable (journal l. 310).
- **Les ADR et specs écrites avant le 2026-11-11 citent des `v0.Y.Z`** (ex. ADR 0008, 0010) : ce sont les noms réels de ces versions, pas des erreurs ; elles ne se réécrivent pas.
- **Android compare `versionCode`** (`run_number`), pas `versionName` : c'est pourquoi le `versionCode` n'a pas changé de règle et que les mises à jour par-dessus `v0.73.x` restent possibles (README l. 213-214 ; installation de `v2026.11.1` par-dessus `v0.73.2`, journal l. 311).

## Alternatives

- Garder le SemVer `v0.Y.Z` : écartée par Pascal (journal l. 307), raison au-delà de la ressemblance avec nemeton non écrite.
- Compteur par année ou global : non écrit ; Pascal a décidé « par mois » (fiche 036, l. 8).
- Numéro posé à la main, comme nemeton (« version tirée de DESCRIPTION, à la main », fiche 036) : écartée, la version « doit être calculée par la CI » (fiche 036).

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | La plus simple qui tienne ? | Un script shell de 12 lignes, un tag annoté | Le numéro ne porte plus la gravité | Non écrit ; déduit du cap de Pascal (l. 307) |
| **R** | Qui fait quoi ? | dev sonnet (036, lâché), relecteur ; Pascal tranche le cap | | |
| **A** | Et si l'imprévu arrive ? | Mois invalide : sortie 2 (script l. 6-10) ; aucun tag du mois : `N` = 1 ; `.9` → `.10` testé | Tag sans release si le build échoue ensuite (défaut hérité) | Non écrit ; déduit du renvoi au bac à sable (l. 310) |
| **N** | Idées étranges écoutées ? | Non écrit | | |
| **C** | Coût pour l'ensemble ? | Une montée de version qui ne rompt aucune mise à jour (`versionCode` inchangé) | Documents anciens en `v0.*` à lire comme tels | Non écrit |
| **É** | Que réutilisons-nous ? | Le modèle de nemeton ; le même genre de script testé que `release-necessaire.sh` (lâchers 023, 031) | | |

## Valeurs sacrifiées

- **Le SemVer** : un numéro qui dit « correctif » ou « fonction nouvelle ». Contrepartie : lire la date d'un coup d'œil, comme nemeton.

## Sources

- `pilotage/journal.md` à `0c6b889` : l. 304, 307, 309-312, 391, 396.
- PR #77 (a28dc2d), PR #76 (309b83e, v2026.11.2).
- `.github/scripts/version-calendaire.sh`, `.github/scripts/test-version-calendaire.sh`, `.github/workflows/release.yml` l. 46-58 et 99-100, `.github/workflows/ci.yml` l. 27, `README.md` l. 208-214 (sur `main` à `0c6b889`).
- Fiche `pilotage/missions/036-version-calendaire.md`. Voir aussi l'ADR 0008.

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ? Réponse de Pascal : non écrite.
