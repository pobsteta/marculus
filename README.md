# Marculus

Application Android de **martelage forestier** (smartphones et tablettes), inspirée des
compteurs multi-catégories mais transposée en véritable **feuille de martelage** : une
matrice **essences × classes de diamètre/circonférence**, traçable via un journal
d'événements *append-only*.

## État (v1 « comptage seul »)

- **`:core`** — domaine Kotlin pur (testé en JVM) : axe des classes, totaux dérivés du
  journal, analyse tolérante du texte de hauteur (`27-6AB4CD`).
- **`:data`** — persistance Room (journal `tige` + `contexte`) et `MartelageRepository`.
- **`:app`** — UI Jetpack Compose / Material 3 : liste des contextes → création d'un
  contexte → **feuille de martelage** (matrice défilable, cellule `valeur / − / + / H / Q`).

Reporté en v2 : carte (import GPKG des parcelles), rattachement GPS, GNSS RTK/NTRIP,
synchro multi-opérateurs. Voir la spec : [`docs/specs/2026-06-21-marculus-v1-spec.md`](docs/specs/2026-06-21-marculus-v1-spec.md).

## Construire

Le JDK « système » peut être un JRE sans `javac`. Pointer Gradle sur un JDK complet :

```bash
export JAVA_HOME=/chemin/vers/un/jdk-21
./gradlew :core:test          # tests du domaine (JVM)
./gradlew :app:assembleDebug  # APK debug
```

Pile : Gradle 9.4.1 · AGP 9.2.1 (Kotlin intégré) · Kotlin 2.2.10 · Compose BOM 2026.02.01 ·
Room 2.8.1 · minSdk 26 / compileSdk 36.
