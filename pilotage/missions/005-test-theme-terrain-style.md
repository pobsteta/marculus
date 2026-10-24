# Mission 005 — test d'égalité du thème avec `terrain-style`

## Mission

**En une phrase (quoi, et pourquoi) :** écrire, avant le code et sans le lire, le test JVM qui
vérifie que les `ColorScheme` clair et sombre de Marculus sont égaux, rôle par rôle, aux jetons
`StyleTerrain.clair` et `StyleTerrain.sombre` de `terrain-style` v0.6.0, pour que le critère
d'acceptation de l'issue #2 (§ 2) soit vérifié à chaque PR et pas seulement à l'œil.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | contrat ci-dessous ; README de `terrain` § « Couleurs : `terrain-style` » |
| A-t-il les moyens ? | oui | `com.github.pobsteta.terrain:terrain-style:v0.6.0` servi par JitPack (vérifié le 2026-10-24) ; source lisible dans `~/dev/terrain/terrain-style/` |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## Contrat (ce que le test suppose, que la mission 006 doit fournir)

- Dans `app/src/main/kotlin/io/github/pobsteta/marculus/ui/theme/Theme.kt` (paquet
  `io.github.pobsteta.marculus.ui.theme`) : `internal val SchemaClair: ColorScheme` et
  `internal val SchemaSombre: ColorScheme`, ceux que `MarculusTheme` utilise.
- Pour chacun des 48 rôles de `fr.nemeton.terrain.style.Schema` (de `primary` à
  `onTertiaryFixedVariant`) : `SchemaClair.<rôle> == Color(StyleTerrain.clair.<rôle>)`, et de
  même en sombre. Un échec doit nommer le rôle fautif.
- `reussi`, `alerte` et `interditsEnTexte` ne sont pas des rôles de `ColorScheme` : hors test.

## L'Homme

- **Agent :** `testeur`
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** toucher à `Theme.kt` ou à un écran ; faire passer le test
  (il doit échouer, ou ne pas compiler, tant que 006 n'est pas livrée : il le dit).

## Harnais

- **Peut :** créer `app/src/test/kotlin/io/github/pobsteta/marculus/ui/theme/ThemeTerrainStyleTest.kt` ;
  ajouter à `app/build.gradle.kts` les dépendances de test JVM (JUnit, comme `:core` et `:data`)
  et `terrain-style` en `testImplementation` ; ajouter `terrain-style` à `gradle/libs.versions.toml`
  (même `version.ref = "terrain"` si possible, sinon le dire) ; dans sa branche.
- **Ne doit pas :** lire ni modifier `app/src/main/` ; toucher à `main`.
- **Sources autorisées :** ce qui précède, `~/dev/terrain/README.md`, `~/dev/terrain/terrain-style/src/main/`,
  `core/build.gradle.kts` et `data/build.gradle.kts` (pour l'idiome des tests).

## Tests

- **Comment saura-t-on que c'est juste :** le test couvre les 48 rôles × 2 schémas (le relecteur
  compte), compile contre `terrain-style` v0.6.0, et échoue sur le `Theme.kt` actuel en nommant
  `primary` (`#3E7A40` ≠ jeton).

## Terrain

- **Échantillon que Pascal vérifiera :** rien sur le terrain ; c'est la 006 qui se voit au soleil.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal (avec la PR de 006, une seule PR).
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** test rendu, avant la livraison de 006.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Une PR qui change une couleur du thème sans passer par `terrain-style` échoue en test.
