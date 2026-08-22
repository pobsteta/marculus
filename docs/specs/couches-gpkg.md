# Ce que Marculus attend dans le GPKG

Un contexte de martelage pointe **un** GeoPackage, importé une fois et copié dans le stockage
privé de l'application. Ce fichier peut porter plusieurs couches : c'est le **nom de la table**
qui dit le rôle de chacune, pas sa géométrie.

| Table | Rôle | Géométrie | Lu par |
|---|---|---|---|
| *(tout autre nom)* | **Parcelles** | POLYGON / MULTIPOLYGON | carte, rattachement des tiges, statut |
| `houppier` | **Houppiers MNH** → estimation de la hauteur | POLYGON / MULTIPOLYGON | feuille de martelage |
| `desserte` | **Routes, pistes, chemins** | LINESTRING / MULTILINESTRING | carte |
| *(table de tuiles)* | **Ortho** hors-ligne | tuiles | carte |

Le CRS est quelconque : l'app reprojette tout en **WGS84 / EPSG:4326**. Les noms de tables et de
colonnes sont insensibles à la casse.

> **Règle de repli** : toute table vecteur dont le nom n'est pas reconnu est traitée comme une
> couche de **parcelles**. Un GPKG ne contenant que des parcelles se lit donc exactement comme
> avant l'ajout de ces conventions.

## Parcelles

Aucun nom de table imposé. Attributs cherchés (premier trouvé, insensible à la casse) :

| Information | Colonnes acceptées |
|---|---|
| Propriétaire | `proprietaire`, `propriétaire`, `owner`, `prop` |
| Forêt | `foret`, `forêt`, `forest` |
| Commune | `commune`, `nom_com`, `ville` |
| Parcelle | `section` + `numero` (ou `num`, `n_parcelle`, `parcelle`, `id_parcelle`, `idu`) |

L'**indivision** s'écrit dans le champ propriétaire avec `;`, `/` ou `&` pour séparer les
co-propriétaires. La **surface est calculée depuis la géométrie** (aire géodésique) : aucun
attribut de surface n'est lu, aucun n'est nécessaire. Toutes les autres colonnes restent
accessibles en attributs bruts.

## `houppier` — estimation de la hauteur

Voir `couche-houppier-mnh.md` pour la production en amont (lidR/QGIS) et les limites.

| Élément | Valeur attendue |
|---|---|
| Nom de table | `houppier` (aussi `houppiers`, `crown`, `crowns`) |
| Géométrie | un polygone par houppier |
| Hauteur | **`h_max`**, réel, en **mètres** (alias : `hmax`, `hauteur_max`, `hauteur`, `height`) |

Une entité sans hauteur lisible est ignorée. Les hauteurs hors de **1–70 m** sont rejetées : un
`h_max` à 0 (houppier vide) ou en centimètres n'écrira rien plutôt qu'une absurdité au journal.

## `desserte` — routes, pistes et chemins

| Élément | Valeur attendue |
|---|---|
| Nom de table | `desserte` (aussi `dessertes`, `voirie`, `route(s)`, `piste(s)`, `chemin(s)`) |
| Géométrie | LINESTRING / MULTILINESTRING (une desserte cartographiée en surface est acceptée : on en trace le contour) |
| Libellé | `nom`, `name`, `libelle`, `libellé`, `toponyme` — facultatif |
| Nature | `type`, `nature`, `categorie`, `catégorie`, `revetement`, `revêtement` — facultatif |

La desserte est **purement informative** : tracée en ocre sur la carte, au-dessus des parcelles
et sous les tiges, libellé et nature au toucher. Elle sert à trouver l'accès au chantier et le
point de dépôt ; elle n'entre dans aucun calcul, et aucune tige ne lui est rattachée.

## Ortho

La **première table de tuiles** du GPKG sert de fond de carte hors-ligne ; elle est reprojetée en
Web Mercator au premier affichage (le résultat est écrit dans le GPKG, `<table>_wm`). N'en mettre
**qu'une** : avec plusieurs tables de tuiles, celle qui est retenue n'est pas prévisible.

## Pièges

- Ne pas livrer une couche de houppiers sous un autre nom que `houppier` : elle deviendrait une
  couche de parcelles, et chaque houppier deviendrait candidat au **rattachement spatial** des
  tiges — le journal se remplirait de parcelles fantômes.
- Un MNH **raster** n'est pas lu (cf. `couche-houppier-mnh.md`) : la hauteur se déduit du
  polygone de houppier, pas d'un échantillonnage au pied du tronc.
