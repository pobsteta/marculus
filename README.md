# Marculus

[![CI](https://github.com/pobsteta/marculus/actions/workflows/ci.yml/badge.svg)](https://github.com/pobsteta/marculus/actions/workflows/ci.yml)
[![Release](https://github.com/pobsteta/marculus/actions/workflows/release.yml/badge.svg)](https://github.com/pobsteta/marculus/actions/workflows/release.yml)
[![Version](https://img.shields.io/github/v/release/pobsteta/marculus?sort=semver&label=version)](https://github.com/pobsteta/marculus/releases)
![Couverture](.github/badges/jacoco.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-min%2026%20%C2%B7%20cible%2036-3DDC84?logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)

Application Android de **martelage forestier** (smartphones et tablettes), inspirée des
compteurs multi-catégories mais transposée en véritable **feuille de martelage** : une
matrice **essences × classes de diamètre/circonférence**, traçable via un journal
d'événements *append-only* (chaque tige possède un UUID).

---

## Fonctionnalités

### Saisie & feuille de martelage
- Matrice **essences × classes** défilable ; cellule `valeur / − / + / H / Q`.
- Incrément configurable, **annulation** conservée dans le journal (traçabilité).
- **Hauteur** par tige (analyse tolérante : `27-6AB4CD`) et **qualité** (chips de référentiel).
- Saisie/édition d'une **tige libre** (essence hors matrice) depuis le menu et l'historique.
- **Comptage par boutons de volume** ; **retour sensoriel** (vibration / son).
- **Annonces vocales** (TTS, voix choisie) : nombre, étiquette, et **alertes d'avis −/+**
  (« limite inférieure non atteinte » / « limite supérieure dépassée »).
- Affichage du **code essence ONF (3 car.) en gros** (auto-dimensionné) pour la lisibilité.

### Cubage (volumes)
- **Schaeffer** rapide / lent (une entrée, diamètre ; numéro de tarif).
- **EMERGE** (deux entrées, projet ANR/IGN, d'après C. Deleuze – RDV Techniques ONF 44) :
  **volume bois fort tige**, **houppier** et **total aérien** affichés séparément.
  Table complète des **226 essences** (coefficients générés depuis le package R `gftools`),
  repli par groupe feuillus/résineux, puis **coefficient de forme** réglable par contexte.
- **Surface terrière** (g = π/4·D²) à tous les niveaux.

### Carte & foncier (GeoPackage)
- Import d'un **GeoPackage** par contexte (parcelles + orthophoto reprojetée en Web Mercator).
- Fonds **OSM / satellite / ortho**, contrôles de zoom Material, sur-zoom.
- **Rattachement spatial** point-dans-polygone (figé dans la tige au martelage).
- **Acquisition GNSS** continue ou **ponctuelle** (au clic) ; permission demandée à l'exécution.
  Une tige hors parcelle reste **affichée** sur la carte (comptée « Hors parcelle »).

### Statut, historique & exports
- Statut par essence (donut), **par parcelle** hiérarchique
  (Propriétaire → Forêt → Parcelle → Essence) avec nombre, surface terrière et volumes.
- Historique éditable (la position GNSS est préservée en correction).
- Exports **CSV** (contexte et foncier, colonnes G et volumes tige/houppier/total).

### Organisation des contextes
- **Recherche** (nom / commentaire / date) et **date de martelage** par contexte.
- **Vue Kanban** (activable dans les Paramètres) à 5 colonnes
  **Proposée · Validée · Planifiée · Réalisée · Abandonnée**, **glisser-déposer**.
  Règle de plancher : ≥ 1 tige ⇒ mini *Planifiée*, export ⇒ mini *Réalisée* (sans rétrogradation).
- Liste triée par **date de martelage décroissante**.

### Synchronisation multi-opérateurs
- Partage **par contexte** d'un fichier `<contexte>-<opérateur>-<horodatage>.marsync`
  (Android share / Quick Share). Identité opérateur = UUID de l'appareil (ou nom saisi).
- Fusion par **union sur l'UUID** des tiges, idempotente ; conflit résolu en
  **« dernière écriture gagne »** via l'horodatage `modifie`.

### Internationalisation
- Français (défaut) et anglais, bascule de langue dans les Paramètres.

---

## Architecture

| Module    | Rôle |
|-----------|------|
| **`:core`** | Domaine Kotlin pur (testé JVM) : modèle, axe des classes, cubage (Schaeffer + EMERGE), géodésie (aires), attribution spatiale, analyse des hauteurs. |
| **`:data`** | Persistance **Room** (journal `tige`, `contexte`, configs) + **DataStore** (réglages), `GpkgRepository`, sauvegarde/synchro JSON. |
| **`:app`**  | UI **Jetpack Compose / Material 3** : liste, création, feuille, carte (osmdroid), statut, paramètres. |

Pile : Gradle 9.4.1 · AGP 9.2.1 (Kotlin intégré) · Kotlin 2.2.10 · Compose BOM 2026.02.01 ·
Room 2.8.1 · osmdroid 6.1.20 · NGA geopackage-android 6.7.4 · proj4j · minSdk 26 / compileSdk 36.

---

## Construire

Le JDK « système » peut être un JRE sans `javac` : pointer Gradle sur un JDK 21 complet.

```bash
export JAVA_HOME=/chemin/vers/un/jdk-21
./gradlew :core:test            # tests du domaine (JVM) + couverture JaCoCo
./gradlew :app:assembleDebug    # APK debug
./gradlew :app:assembleRelease  # APK release minifié (R8, signé avec la clé debug)
```

Rapport de couverture : `core/build/reports/jacoco/html/index.html`.

---

## Versionnement & releases automatiques

À **chaque push sur `main`**, la CI (GitHub Actions) :

1. lance les **tests** et génère la **couverture** (badge mis à jour) ;
2. calcule la **prochaine version SemVer** à partir des messages de commit
   ([Conventional Commits](https://www.conventionalcommits.org)) ;
3. compile l'**APK release** (version injectée) et publie une **release GitHub** avec l'APK.

Le type de bump dépend du commit :

| Préfixe de commit            | Incrément          | Exemple              |
|------------------------------|--------------------|----------------------|
| `fix: …`                     | **patch** (x.y.**z**) | `0.3.1 → 0.3.2`   |
| `feat: …`                    | **minor** (x.**y**.0) | `0.3.1 → 0.4.0`   |
| `feat!: …` / `BREAKING CHANGE` | **major** (**x**.0.0) | `0.3.1 → 1.0.0` |
| autre                        | patch (défaut)     |                      |

> **Secrets / prérequis** : `GITHUB_TOKEN` (fourni automatiquement) suffit pour les tags et
> releases. Le badge de couverture est généré et commité par la CI (aucun service externe requis).

---

## Documentation

Spécification v1 : [`docs/specs/2026-06-21-marculus-v1-spec.md`](docs/specs/2026-06-21-marculus-v1-spec.md).
