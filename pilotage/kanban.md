# Kanban de Marculus

> Cinq colonnes (fiche n° 8). Chaque carte porte l'Homme (qui), la Mission (quoi, quand,
> combien) et les Moyens (comment, où, avec qui), et renvoie à sa fiche dans
> `pilotage/missions/`. Elle note aussi depuis quel cycle de `/boucle` elle est dans sa
> colonne ; on change ce numéro chaque fois qu'elle change de colonne.
> **Une carte qui ne bouge pas est souvent la mauvaise nouvelle à dire tout de suite.**

**Dernier cycle :** 4 _(augmenté de 1 à la fin de chaque `/boucle`, même quand elle n'a
rien lancé. Une carte est immobile quand dernier cycle − N ≥ 2.)_

_Modèle de carte :_

```
- [NNN-titre](missions/NNN-titre.md) · depuis le cycle N
  - Homme : qui
  - Mission : quoi, quand, combien
  - Moyens : comment, où, avec qui
```

## Bac à sable

_Relevé du 2026-10-23 dans les boîtes de briefs (`~/dev/briefs/vers-marculus/` est vide ; le
travail en attente est arrivé par `~/dev/surculus/pilotage/briefs/vers-marculus/`) et dans la
spec, § 10. Pas encore de fiche de mission : elles viendront de `/imperatrice:mission`._

- Lire l'échelon **forêt** de `tarifsProposes` (lots Nemeton) : il prime sur la table
  embarquée, tout autre niveau est ignoré · décision de Pascal du 2026-10-23 · réponse à
  nemetonshiny : `~/dev/briefs/vers-nemetonshiny/2026-10-23-marculus-tarifs-proposes-niveau-foret.md`
  (§ 4 : combien d'essences atteignent la forêt ?) · depuis le cycle 0
- Recettes terrain : affouage, dictée (7 cas, bruit, gants) · spec § 10 · depuis le cycle 0
- Traçabilité des hauteurs estimées (MNH) contre mesurées · spec § 10 · depuis le cycle 0
- Synchro réseau, foncier structuré, bouton Flic 2 · spec § 10 · hors périmètre actuel

## Validé








## Planifié

- [006](missions/006-theme-terrain-style.md) · depuis le cycle 4
  - Homme : testeur puis dev (sonnet), appuyer
  - Mission : adopter `terrain-style` v0.6.1 = couleurs actuelles de Marculus, sans changement visible (cap du 2026-10-24)
  - Moyens : attend `terrain-style` v0.6.1 (brief `vers-terrain`, révision du 2026-10-24) ; capture avant / après

- Couleurs écrites en dur hors du thème (19 `Color(0x…)`) : à passer aux jetons ou à `reussi`/`alerte` · décision de Pascal du 2026-10-24 : hors de 006 · essai au soleil clos le 2026-10-24 (thèmes actuels conformes) : la carte attend la palette `terrain-style` · depuis le cycle 1

## Terminé

- [015](missions/015-release-apres-tests.md) · depuis le cycle 4
  - Homme : dev (sonnet), appuyer
  - Mission : la release attend les tests verts de `main`
  - Moyens : `release.yml`, `ci.yml`
  - Livré : PR #27 fusionnée le 2026-10-24, release v0.65.6 après CI verte

- [013](missions/013-test-migration-theme.md) · depuis le cycle 4
  - Homme : testeur (sonnet), appuyer
  - Mission : test de la reprise de l'ancienne clé `theme_sombre`
  - Moyens : `data/src/test/`, une PR
  - Livré : PR #25 fusionnée le 2026-10-24

- [014](missions/014-docs-a-jour.md) · depuis le cycle 4
  - Homme : archiviste (sonnet), appuyer
  - Mission : README « Déployer et publier », brief à jour, ligne du plugin dans CLAUDE.md
  - Moyens : README, `docs/brief.md`, `release.yml`
  - Livré : PR #23 fusionnée le 2026-10-24

- [012](missions/012-adr-decisions.md) · depuis le cycle 4
  - Homme : archiviste (sonnet), appuyer
  - Mission : ADR 0001 à 0006 des décisions déjà prises
  - Moyens : journal, specs, gabarit `adr-france.md`
  - Livré : PR #24 fusionnée le 2026-10-24

- [011](missions/011-theme-liste-deroulante.md) · depuis le cycle 3
  - Homme : dev (sonnet), appuyer, sans testeur
  - Mission : choix du thème en liste déroulante, choix courant coché
  - Livré : PR #20 fusionnée le 2026-10-24, release v0.65.0

- [010](missions/010-numero-hors-schaeffer.md) · depuis le cycle 3
  - Homme : dev (sonnet), appuyer
  - Mission : numéro à 0 hors Schaeffer à l'enregistrement, à l'import et à l'export
  - Moyens : `Modeles.kt`, `ExportCsv.kt`, `CreationContexteScreen.kt`, `SauvegardeRepository.kt`
  - Livré : PR #18 fusionnée le 2026-10-24, release v0.64.9

- [009](missions/009-test-numero-hors-schaeffer.md) · depuis le cycle 3
  - Homme : testeur (sonnet), appuyer
  - Mission : tests du numéro de tarif à 0 hors Schaeffer (modèle et CSV)
  - Moyens : `:core:test`, une PR commune avec 010
  - Livré : PR #18 fusionnée le 2026-10-24, release v0.64.9

- [003](missions/003-spec-a-jour.md) · depuis le cycle 3
  - Homme : archiviste (sonnet), appuyer, plan d'abord
  - Mission : remettre la spec v1 à jour du livré (v0.63.6)
  - Moyens : `docs/specs/`, preuves par commit ou release, une PR
  - Livré : PR #17 fusionnée le 2026-10-24

- [008](missions/008-choix-theme.md) · depuis le cycle 3
  - Homme : dev (sonnet), appuyer
  - Mission : remplacer la case « Thème sombre » par Système / Clair / Sombre
  - Moyens : `Reglages.kt`, `ReglagesRepository.kt`, `ParametresScreen.kt`, `MainActivity.kt`
  - Livré : PR #15 fusionnée le 2026-10-24, release v0.64.6

- [007](missions/007-test-choix-theme.md) · depuis le cycle 3
  - Homme : testeur (sonnet), appuyer
  - Mission : tests du choix du thème Système / Clair / Sombre
  - Moyens : `:core:test`, une PR commune avec 008
  - Livré : PR #15 fusionnée le 2026-10-24, release v0.64.6



- [004](missions/004-brief-formats-6-7.md) · depuis le cycle 1
  - Homme : archiviste (sonnet), appuyer, brouillon relu
  - Mission : brief à `nemetonshiny` sur `FormatCsv;6` et `;7`
  - Moyens : `ExportCsv.kt` et ses tests ; dépôt par l'orchestrateur après accord de Pascal, pas de PR
  - Livré : déposé dans `~/dev/briefs/vers-nemetonshiny/2026-10-24-marculus-csv-formats-6-7.md` le 2026-10-24

- [002](missions/002-readme-terrain.md) · depuis le cycle 1
  - Homme : archiviste (sonnet), appuyer
  - Mission : README : ne nommer comme utilisateurs de `terrain` que les applications qui la déclarent
  - Moyens : `README.md` + statut en tête du brief Surculus, une PR
  - Livré : PR #8 fusionnée le 2026-10-24

- [001](missions/001-ci-tests-data.md) · depuis le cycle 1
  - Homme : dev (sonnet), appuyer
  - Mission : faire lancer par la CI les tests JVM de `:data`
  - Moyens : `.github/workflows/ci.yml`, une PR
  - Livré : PR #7 fusionnée le 2026-10-24

## Abandonné

_Avec la raison, en une ligne._

- [005](missions/005-test-theme-terrain-style.md) · depuis le cycle 2
  - Homme : testeur (sonnet), appuyer
  - Mission : test d'égalité des `ColorScheme` clair et sombre avec `terrain-style` v0.6.0
  - Moyens : JitPack, `~/dev/terrain/terrain-style/`, une PR commune avec 006
  - Raison : test retiré avec le revert de #9 (#12) ; à réécrire avec 006 quand la v0.6.1 sera là

## Livraisons

_Une ligne par cycle, ajoutée sous la précédente à la fin de chaque `/boucle`, et une
par `/bilan` : la livraison que Pascal a retenue, pour qui, et ce qu'elle apporte de plus
que la précédente du même rythme. Un cycle sans livraison neuve livre sa mauvaise
nouvelle, car
« une mauvaise nouvelle est une livraison sur laquelle on apprend » (Pascal) : on
l'inscrit avec ce qu'elle nous apprend._

```
- cycle N · livraison · pour qui · ce qu'elle apporte de plus que la précédente
- cycle N · mauvaise nouvelle : … · pour qui · ce qu'elle nous apprend
- bilan du AAAA-MM-JJ · livraison · pour qui · ce qu'elle apporte de plus que la précédente
- bilan du AAAA-MM-JJ · mauvaise nouvelle : … · pour qui · ce qu'elle nous apprend
```

- cycle 1 · contrat des CSV `FormatCsv;6` et `;7` (brief du 2026-10-24) · pour nemetonshiny, consommateur des données · première description de ces formats envoyée ; rien de neuf pour les marteleurs sur le téléphone
- cycle 2 · Marculus v0.64.0 : thème aux jetons de `terrain-style` (texte 7:1, mêmes couleurs que Surculus et Groma) · pour les marteleurs · première livraison sur le téléphone depuis la méthode ; à juger au soleil le 2026-10-30 (proposée par l’orchestrateur)
- cycle 2 · mauvaise nouvelle : le thème `terrain-style` v0.6.0 efface le vert (vu par Pascal sur le téléphone), annulé en v0.64.2 · pour les marteleurs · un test d'égalité ne voit pas la couleur : le nuancier se montre à Pascal avant de publier, et l'aspect se juge à l'écran avant la fusion
- cycle 3 · Marculus v0.64.6 : choix du thème Système / Clair / Sombre · pour les marteleurs · le thème clair au soleil même sur un téléphone en sombre (proposée par l’orchestrateur)
- cycle 3 · Marculus v0.64.9 et v0.65.0 : CSV sans faux numéro de tarif (`EMERGE:0`), thème en liste déroulante · pour nemetonshiny et les marteleurs · données justes en aval ; v0.65.0 jugée « parfaite » au soleil par Pascal
- bilan du 2026-10-24 · Marculus v0.65.0 lisible au soleil (clair et sombre vérifiés par Pascal), unité du lot lisible en portrait, thème en deux appuis ; contrat des formats 6-7 et CSV justes · pour les marteleurs et nemetonshiny · premier bilan : pas de livraison de bilan antérieure
