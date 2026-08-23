# Couche « houppier » — estimation automatique de la hauteur des tiges (MNH)

But : pré-remplir la **hauteur** d'une tige au moment du martelage à partir d'une
segmentation des houppiers issue d'un MNH (Modèle Numérique de Hauteur / LiDAR HD).
Le calcul lourd (détection des apex + délimitation des houppiers) se fait **en amont sur PC** ;
l'app ne fait qu'un **point‑dans‑polygone** sur la position GNSS de la tige, comme pour le
rattachement aux parcelles.

## Convention de la couche dans le GPKG

| Élément | Valeur attendue |
|---|---|
| **Nom de table** | `houppier` (insensible à la casse) |
| **Type de géométrie** | `POLYGON` ou `MULTIPOLYGON` (un polygone par houppier) |
| **Colonne géométrie** | quelconque (auto‑détectée), p. ex. `geom` |
| **CRS** | quelconque (déclaré dans le GPKG) — l'app reprojette en **WGS84 / EPSG:4326** |
| **Attribut hauteur** | **`h_max`** — réel, en **mètres** = hauteur de l'apex (max du MNH dans le houppier) |

`h_max` est l'attribut canonique. Pour la robustesse, l'app accepte aussi ces alias
(insensible à la casse) : `hmax`, `hauteur_max`, `hauteur`, `height`. Toute autre table que
`houppier` reste traitée comme une couche de parcelles (inchangé) — la liste complète des
couches attendues dans le GPKG est dans `couches-gpkg.md`.

**Livré le 2026-08-22** : lecture de la couche, estimation à la saisie, et exclusion des
houppiers du rattachement spatial des tiges.

## Production en amont (exemple lidR)

```r
library(lidR)
chm <- rast("mnh.tif")                    # ou normalize_height() depuis le nuage
ttops   <- locate_trees(chm, lmf(ws = 5)) # apex (maxima locaux)
crowns  <- segment_trees(las, dalponte2016(chm, ttops)) # ou silva2016/watershed
houppier <- crown_metrics(crowns, func = ~list(h_max = max(Z)), geom = "convex")
st_write(houppier["h_max"], "peuplement.gpkg", layer = "houppier", append = TRUE)
```

Sous QGIS : « Détection de cimes » + « Délimitation de houppiers » (plugins LiDAR / r.watershed),
puis « zonal max » du MNH par houppier dans un champ `h_max`, enregistré comme couche `houppier`
du même GPKG.

## Comportement de l'app

- Réglage **« Estimer la hauteur (MNH) »** (Paramètres), décoché par défaut. Décoché, la couche
  n'est même pas lue.
- À la saisie d'une tige, si la couche `houppier` est présente **et** qu'une position est captée :
  point‑dans‑polygone → **H = `h_max` arrondi au mètre**, **modifiable** au bouton H de la cellule.
- L'estimation ne complète que les tiges **sans hauteur mesurée** : une hauteur **dictée** ou
  **saisie** prime toujours, elle n'est jamais écrasée.
- **Houppiers superposés** (segmentation par enveloppe convexe) : on retient le **plus haut**,
  celui dont l'apex domine physiquement l'opérateur.
- Position dans aucun houppier (trouée, bord, tige dominée) : **rien n'est écrit**. Pas de repli
  sur le houppier le plus proche — ce serait deviner l'arbre d'à côté.
- Hauteur hors de **1–70 m** : ignorée (`h_max` à 0, ou en centimètres).
- En **GNSS ponctuel**, la position n'arrive qu'après l'insertion : l'estimation est alors faite
  au même moment que le rattachement à la parcelle, et annotée sur la tige.
- **Ajouter une découpe à une hauteur estimée** : le bouton H rouvre la saisie **sur la valeur
  courante** (hauteur d'un côté, découpe de l'autre). Sur une tige estimée à 27 m, il suffit donc
  de taper `6AB` dans le champ découpe pour obtenir `27-6AB` — la hauteur n'est pas à retaper.
  La qualité bois ne vient **jamais** du GPKG : elle sort du référentiel de qualité bois du
  contexte et vit dans le texte de hauteur (`HauteurParser`), comme pour une hauteur mesurée.
- L'estimation est une **aide**, jamais une valeur d'autorité.

## Limites à connaître

- **Précision GNSS** : fiable en **RTK** (on tombe dans le bon houppier) ; en GNSS interne
  (3–10 m) on risque le houppier voisin.
- **Canopée visible seulement** : les tiges **dominées** (sous couvert) n'apparaissent pas dans le
  MNH → pas de houppier → pas d'estimation (cas normal au martelage).
- Sur/sous‑segmentation possible en peuplement dense : l'opérateur garde la main.
- **Traçabilité** : le schéma n'a pas de champ distinguant une hauteur estimée d'une hauteur
  mesurée. À l'export, les deux se ressemblent. Si la distinction devient nécessaire (cubage,
  contrôle), il faudra une colonne dédiée et une migration Room.
