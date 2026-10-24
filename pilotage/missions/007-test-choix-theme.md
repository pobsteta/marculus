# Mission 007 — tests du choix du thème (Système, Clair, Sombre)

## Mission

**En une phrase (quoi, et pourquoi) :** écrire, avant le code et sans le lire, les tests JVM
du choix du thème à trois valeurs (Système, Clair, Sombre) et de la reprise de l'ancien réglage
« Thème sombre », parce qu'aujourd'hui décocher la case ne rend jamais le thème clair quand le
téléphone est en sombre (constat de Pascal du 2026-10-24).

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | contrat ci-dessous ; idiome des tests de `:core` |
| A-t-il les moyens ? | oui | `:core:test` tourne en local et en CI |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## Contrat (ce que les tests supposent, que la mission 008 doit fournir)

Dans `core/src/main/kotlin/fr/marculus/core/model/Reglages.kt` (paquet `fr.marculus.core.model`) :

- `enum class ChoixTheme { SYSTEME, CLAIR, SOMBRE }` ;
- `fun ChoixTheme.estSombre(systemeSombre: Boolean): Boolean` : SYSTEME suit le système,
  CLAIR vaut toujours `false`, SOMBRE toujours `true` ;
- `fun choixThemeDepuis(enregistre: String?, ancienThemeSombre: Boolean?): ChoixTheme` :
  `enregistre` est le nom d'une valeur de l'enum (prioritaire s'il est valide) ; sinon reprise de
  l'ancienne case : `true` → SOMBRE, `false` ou absent → SYSTEME ; une chaîne inconnue → même
  reprise que l'absence ;
- `Reglages.theme: ChoixTheme`, défaut `SYSTEME`.

## L'Homme

- **Agent :** `testeur` · **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** l'écran, le DataStore, `MainActivity` ; faire passer les
  tests (ils ne compilent pas tant que 008 n'est pas livrée : il le dit).

## Harnais

- **Peut :** créer `core/src/test/kotlin/fr/marculus/core/model/ChoixThemeTest.kt`, dans sa branche.
- **Ne doit pas :** lire ni modifier `core/src/main/`, `data/src/main/`, `app/src/main/` ; toucher à `main`.
- **Sources autorisées :** cette fiche, `core/src/test/` (idiome), `core/build.gradle.kts`.

## Tests

- **Comment saura-t-on que c'est juste :** les 3 × 2 cas de `estSombre`, et pour
  `choixThemeDepuis` : chaque nom valide, `null` + `true`, `null` + `false`, `null` + `null`,
  chaîne inconnue + `true`, nom valide + ancienne case contraire (le nom prime).

## Terrain

- **Échantillon que Pascal vérifiera :** rien ici ; c'est 008 qui se voit sur le téléphone.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal (une seule PR avec 008).
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Un choix « Clair » donne le thème clair sur un téléphone en sombre, et le test le garde.
