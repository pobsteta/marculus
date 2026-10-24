# Mission 006 — thème de Marculus = jetons de `terrain-style`

## Mission

**En une phrase (quoi, et pourquoi) :** remplacer les couleurs écrites à la main de
`ui/theme/Theme.kt` par les jetons `StyleTerrain.clair` et `StyleTerrain.sombre` de
`terrain-style` v0.6.0, recopiés un pour un dans les `ColorScheme`, pour que le texte tienne
7:1 au soleil (blanc sur `VertVif` = 5,17:1 aujourd'hui) et que Marculus, Surculus et Groma
partagent les mêmes couleurs (issue #2 § 2, ADR 0008 de Surculus acceptée par Pascal le 2026-10-23).

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | à moitié | dépendance nouvelle et interface entière touchée : **plan d'abord** |
| A-t-il les moyens ? | oui | `terrain-style` v0.6.0 sur JitPack ; test d'acceptation de la mission 005 |

**Temps de la main :** **tenir** — l'agent rend un plan, l'orchestrateur le soumet à Pascal,
rien n'est écrit avant son accord.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## Décisions de Pascal (2026-10-24)

- **Mode sombre gardé**, avec `StyleTerrain.sombre`.
- **Les 19 `Color(0x…)` écrites en dur hors de `ui/theme/` restent hors de cette mission** (carte à part).
- Le bouton principal change de vert : coût accepté par Pascal (issue #2).
- `reussi` est provisoire jusqu'à l'essai au soleil du 2026-10-30 : ne pas l'utiliser ici.
- **Plan validé** (« Oui aux 2 questions ») : **toute la ligne `terrain` passe en v0.6.0** (une seule version `terrain`, l'entrée `terrainStyle` de 005 est retirée au profit de `version.ref = "terrain"`) ; `.claude/test-command` est modifié par l'orchestrateur (fait, commit dans la branche).
- **Branche :** `feat/006-theme-terrain-style`, worktree `/home/pascal/dev/marculus/.claude/worktrees/theme-006` (part du commit 944ce0b de 005).

## Contrat (fixé par la mission 005)

`internal val SchemaClair: ColorScheme` et `internal val SchemaSombre: ColorScheme` dans
`Theme.kt`, égaux rôle par rôle (48 rôles) à `StyleTerrain.clair` et `StyleTerrain.sombre`,
utilisés par `MarculusTheme`.

## L'Homme

- **Agent :** `dev`
- **Modèle :** sonnet.
- **`testeur` associé :** mission 005 (le test précède le code).
- **Ce qui ne lui est pas demandé :** toucher aux couleurs en dur des écrans, aux couleurs de la
  carte, à `reussi` / `alerte`, ni changer de version de Compose.

## Harnais

- **Peut :** modifier `Theme.kt`, `app/build.gradle.kts`, `gradle/libs.versions.toml`
  (`terrain-style` en `implementation`), `.claude/test-command` et `.github/workflows/ci.yml`
  (ajouter `:app:testDebugUnitTest`), dans la branche qui porte le test de 005.
- **Sans Android au chargement :** `SchemaClair` et `SchemaSombre` restent de simples `lightColorScheme(...)` / `darkColorScheme(...)`, chargeables sur la JVM sans Robolectric (relecture de 005).
- **Ne doit pas :** modifier le test de 005 (s'il le croit faux, il le dit) ; toucher à `main`.
- **Sources autorisées :** `app/src/main/kotlin/.../ui/theme/`, `~/dev/terrain/README.md`,
  `~/dev/terrain/terrain-style/src/main/`, issue #2.

## Tests

- **Comment saura-t-on que c'est juste :** le test de 005 passe, `.claude/test-command` (avec
  `:app:testDebugUnitTest`) passe, la CI de la PR est verte, `:app:mergeReleaseResources` passe.

## Terrain

- **Échantillon que Pascal vérifiera :** l'APK de la release sur son téléphone, au soleil
  (essai du 2026-10-30) : feuille de martelage, bouton principal, mode sombre.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** plan rendu (avant écriture) ; puis PR ouverte.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Au soleil, le texte des boutons se lit sans plisser les yeux ; et les trois applications ont le
même vert.

## Reformulation du 2026-10-24 (cap de Pascal : la palette commune est celle de Marculus)

La mission devient : **adopter `terrain-style` v0.6.1, dont les jetons sont les couleurs actuelles de
Marculus**, sans aucun changement visible. Remplace l'objectif « tenir 7:1 ».
- Moyens : `terrain-style` v0.6.1 publiée (brief `vers-terrain/2026-10-24-marculus-palette-reste-verte.md`,
  révision du 2026-10-24) ; test d'égalité de la 005 (abandonnée) à réécrire par le `testeur`.
- Exigence d'écran (liste de pré-fusion, point 4) : **capture avant / après identiques**, montrée à
  Pascal avant la PR, en clair et en sombre.
- Temps de la main : appuyer (ce n'est plus un changement d'aspect).
