# Numéro de tarif Schaeffer proposé par sylvoécorégion (IFN)

*Spec du 16 octobre 2026. Brief d'origine (traité) : `~/dev/briefs/traites/2026-10-16-traite-marculus-tarifs-ser.md`.
Tables produites par nemeton : `~/dev/briefs/vers-nemeton/2026-10-16-tarifs-schaeffer-ser.md`.*

## But

Quand l'utilisateur indique la commune du martelage, Marculus propose pour chaque essence en
Schaeffer le numéro de tarif calculé sur les arbres de l'Inventaire forestier national (IFN) de sa
**sylvoécorégion** (SER). Le numéro reste modifiable. Tout fonctionne hors ligne.

La SER plutôt que la commune : une commune compte rarement assez d'arbres IFN par essence ; une
SER en compte des centaines à des milliers, dans des conditions écologiques homogènes.

## Tables (assets `data/src/main/assets/tarifs-ifn/`)

Produites par nemeton (`data-raw/build_ifn_tarifs_schaeffer.R`), séparateur `;`, UTF-8, colonnes
lues par leur nom (l'ordre importe peu, une colonne en trop est ignorée) :

| Fichier | Colonnes |
|---|---|
| `tarifs_ser.csv` | `niveau;code;espar;essence_libelle;type;numero;n_arbres;cv;millesime;part_houppier` |
| `communes_ser.csv` | `insee;nom;departement;ser;part;greco` |
| `ser.csv` | `code;nom;greco` |

- `niveau` ∈ `SER`, `GRECO`, `FRANCE` (code `FR`) ; `type` ∈ `SCHR`, `SCHI`, `SCHL`, `SCHTL`
  (rapide, intermédiaire, lent, très lent) ; `numero` décimal à 0,1 ; `cv` en %.
- `part_houppier` (nemeton 2026.10.9) : part du bois fort total qui est du houppier, en % au
  dixième, modélisée par EMERGE (`hdec = 0,8·htot`) sur les arbres IFN à hauteur mesurée —
  un modèle, l'IFN ne mesure pas les branches. Même valeur pour les 4 types d'un groupe ; vide
  sous 3 arbres à hauteur mesurée. Colonne absente ou vide = pas de proposition.
- `communes_ser.csv` : une ligne par couple commune × SER ; `part` = part de la **surface totale**
  de la commune dans la SER (somme 1 par commune).
- Méthode (PPtools, M. Bruciamacchie) : arbres vifs cubés, C13 ≥ 50 cm, 17,5 ≤ D ≤ 80 cm ;
  numéro = moyenne pondérée par le volume des numéros individuels, CV = écart type pondéré /
  moyenne.

Sans ces fichiers dans l'APK, `TarifsIfnRepository.referentiel()` vaut null : la section
« Commune et sylvoécorégion » n'apparaît pas, rien d'autre ne change.

## Règles

- **Essence ↔ ligne IFN** : par le code ONF du nom (`Cubage.codeEssence`), appliqué au libellé
  IFN et au nom de l'essence du contexte. Essence sans code → pas de proposition. Deux espèces IFN
  rapprochées du même code : la plus fournie en arbres l'emporte.
- **Repli** : la ligne SER si elle compte au moins `TarifsIfn.SEUIL_ARBRES` = 30 arbres, sinon la
  GRECO de la SER, sinon la France ; aucune ligne au seuil → pas de proposition.
- **Numéro** : `numeroBrut` arrondi à l'entier le plus proche (x,5 vers le haut), borné à 1..20.
- **Part de houppier** (`proposerPartHouppier`) : même cascade, un niveau n'est retenu que s'il a
  ≥ 30 arbres **et** une part renseignée ; indépendante du type de Schaeffer.
- **Type conseillé** : le type au plus faible CV (chacun avec son repli) ; affiché s'il diffère du
  type choisi, jamais imposé.
- AUCUN et EMERGE : rien n'est proposé.

## Écran « Nouveau / Modifier le contexte »

- Champ **Commune** (nom ou code INSEE, accents et casse ignorés) ; une fois choisie : nom
  (département), bouton « Changer ».
- Préremplissage : commune la plus fréquente des parcelles du GeoPackage rattaché, retenue d'office
  si un seul nom correspond, sinon placée dans la recherche (homonymes). Seulement à la création ou
  quand on rattache un autre GeoPackage.
- **SER** : code + nom ; commune à cheval → puces triées par part, la principale retenue.
- Chaque essence cochée en Schaeffer : n° et % houppier préremplis, source en dessous
  (« SER C20 — 1 240 arbres, CV 38 % · houppier 12,1 % (France, EMERGE) », « repli GRECO C — … »).
- **Jamais d'écrasement d'une saisie** (numéro comme part de houppier) : une proposition ne
  remplit qu'un champ vide ou encore égal à la dernière proposition. « Reproposer les numéros » réapplique tout. Un contexte existant n'est
  jamais recalculé à son ouverture.
- `Contexte.tarifNumero` reste le numéro par défaut des essences sans tarif propre.

## Données enregistrées

- `Contexte.communeInsee`, `ser`, `millesimeTarifs` (millésime des tables utilisées, gardé tant
  que la SER ne change pas) : base **v17** (`MIGRATION_16_17`, colonnes TEXT nullables, test
  `MigrationTest` sous Robolectric depuis les schémas exportés `data/schemas/`).
- `.marsync` : clés `communeInsee`, `ser`, `millesimeTarifs` ; réglages du terrain
  (`ReglagesTerrain`) : clé absente = valeur locale gardée.
- CSV **format 7** : lignes d'en-tête `CommuneInsee`, `Ser`, `MillesimeTarifs` après
  `SteresParM3`. Pas de colonne par tige : `Cubage` porte déjà `TARIF:numéro`.
- À propos : « données de l'Inventaire forestier national (millésime) © IGN — Licence Ouverte
  Etalab 2.0 ; méthode PPtools », affiché quand les tables sont embarquées.

## Tables embarquées

nemeton **2026.10.9** (part de houppier ajoutée ; 2026.10.8 pour la première livraison), millésime IFN **2011-2025** : 14 752 lignes de tarifs (11 476 SER,
2 748 GRECO, 528 France), 41 550 couples commune × SER pour 34 791 communes, 86 SER. 2,1 Mo de CSV,
~0,5 Mo compressés dans l'APK. `tarifs_ser.csv` = `ifn_tarif_schaeffer_ser.csv` de nemeton renommé.
Contrôles : `TarifsIfnReelsTest` (Gevrey-Chambertin C20 : Chêne sessile SchL 7,8 → 8, Hêtre 8,5 → 9).

Livré en v0.62.0, affichage vérifié sur le téléphone le 16 octobre 2026.

## Reste à faire

- Option GNSS (SER depuis la position, `AttributionSpatiale`) : seulement si un contour simplifié
  des SER est fourni.
