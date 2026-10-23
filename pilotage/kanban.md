# Kanban de Marculus

> Cinq colonnes (fiche n° 8). Chaque carte porte l'Homme (qui), la Mission (quoi, quand,
> combien) et les Moyens (comment, où, avec qui), et renvoie à sa fiche dans
> `pilotage/missions/`. Elle note aussi depuis quel cycle de `/boucle` elle est dans sa
> colonne ; on change ce numéro chaque fois qu'elle change de colonne.
> **Une carte qui ne bouge pas est souvent la mauvaise nouvelle à dire tout de suite.**

**Dernier cycle :** 0 _(augmenté de 1 à la fin de chaque `/boucle`, même quand elle n'a
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

- Thème : `ColorScheme` clair = jetons de `terrain-style`, avec un test (blanc sur `VertVif`
  = 5,17:1 < 7:1) · même brief § 2 · **bloqué** : `terrain-style` n'est pas encore publié
  (brief `surculus/pilotage/briefs/vers-terrain/2026-10-23-terrain-style.md`) · depuis le cycle 0
- Lire l'échelon **forêt** de `tarifsProposes` (lots Nemeton) : il prime sur la table
  embarquée, tout autre niveau est ignoré · décision de Pascal du 2026-10-23 · réponse à
  nemetonshiny : `~/dev/briefs/vers-nemetonshiny/2026-10-23-marculus-tarifs-proposes-niveau-foret.md`
  (§ 4 : combien d'essences atteignent la forêt ?) · depuis le cycle 0
- Recettes terrain : affouage, dictée (7 cas, bruit, gants) · spec § 10 · depuis le cycle 0
- Traçabilité des hauteurs estimées (MNH) contre mesurées · spec § 10 · depuis le cycle 0
- Synchro réseau, foncier structuré, bouton Flic 2 · spec § 10 · hors périmètre actuel

## Validé

- [001](missions/001-ci-tests-data.md) · depuis le cycle 0
  - Homme : dev (sonnet), appuyer
  - Mission : faire lancer par la CI les tests JVM de `:data`
  - Moyens : `.github/workflows/ci.yml`, une PR
- [002](missions/002-readme-terrain.md) · depuis le cycle 0
  - Homme : archiviste (sonnet), appuyer
  - Mission : README : ne nommer comme utilisateurs de `terrain` que les applications qui la déclarent
  - Moyens : `README.md` + statut en tête du brief Surculus, une PR
- [003](missions/003-spec-a-jour.md) · depuis le cycle 0
  - Homme : archiviste (sonnet), appuyer, plan d'abord
  - Mission : remettre la spec v1 à jour du livré (v0.63.6)
  - Moyens : `docs/specs/`, preuves par commit ou release, une PR
- [004](missions/004-brief-formats-6-7.md) · depuis le cycle 0
  - Homme : archiviste (sonnet), appuyer, brouillon relu
  - Mission : brief à `nemetonshiny` sur `FormatCsv;6` et `;7`
  - Moyens : `ExportCsv.kt` et ses tests ; dépôt par l'orchestrateur après accord de Pascal, pas de PR

## Planifié

## Terminé

## Abandonné

_Avec la raison, en une ligne._

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
