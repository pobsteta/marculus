# Mission 008 — choix du thème : Système, Clair, Sombre

## Mission

**En une phrase (quoi, et pourquoi) :** remplacer la case « Thème sombre » des Paramètres par
un choix à trois valeurs (Système, Clair, Sombre), en reprenant l'ancien réglage, pour que le
marteleur puisse avoir le thème clair au soleil même quand son téléphone est en sombre
(`MainActivity.kt` l. 63 : `reglages.themeSombre || isSystemInDarkTheme()` ne peut que forcer
le sombre).

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | contrat de la fiche 007 ; réglages dans `Reglages.kt`, `ReglagesRepository.kt`, `ParametresScreen.kt` |
| A-t-il les moyens ? | oui | tests de la mission 007 |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## Contrat

Celui de la fiche `007-test-choix-theme.md`, section « Contrat ».

## L'Homme

- **Agent :** `dev` · **Modèle :** sonnet. · **`testeur` associé :** mission 007.
- **Ce qui ne lui est pas demandé :** changer les couleurs (`Theme.kt` reste tel quel, le
  thème `terrain-style` attend la v0.6.1) ; toucher au `.marsync` (le thème n'est pas un réglage
  du terrain).

## Harnais

- **Peut :** modifier `core/.../model/Reglages.kt` (enum, fonctions, champ `theme` à la place de
  `themeSombre`), `data/.../ReglagesRepository.kt` (nouvelle clé texte `theme` ; lecture par
  `choixThemeDepuis(p[theme], p[theme_sombre])` ; l'ancienne clé n'est plus écrite),
  `app/.../MainActivity.kt` (`darkTheme = reglages.theme.estSombre(isSystemInDarkTheme())`),
  `app/.../ParametresScreen.kt` (trois choix à la place de la case, cibles ≥ 48 dp), les chaînes
  `values/` et `values-en/` ; dans la branche qui porte les tests de 007.
- **Ne doit pas :** modifier les tests de 007 (s'il les croit faux, il le dit) ; toucher à `main`.

## Tests

- **Comment saura-t-on que c'est juste :** tests de 007 verts, `.claude/test-command` vert,
  `:app:mergeReleaseResources` vert, CI de la PR verte.

## Terrain

- **Échantillon que Pascal vérifiera :** sur son téléphone (en sombre), Paramètres → Clair :
  l'application passe en clair ; Système : elle revient en sombre ; le choix survit à un
  redémarrage ; un ancien « Thème sombre » coché arrive sur « Sombre ».

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Au soleil, le marteleur choisit « Clair » et l'écran le reste.
