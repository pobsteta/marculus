# Mission 011 — choix du thème en liste déroulante

## Mission

**En une phrase (quoi, et pourquoi) :** dans Paramètres, remplacer les trois boutons radio du
thème par une liste déroulante (une ligne qui montre le choix courant ; un appui ouvre la liste
Système / Clair / Sombre, le choix courant marqué d'une coche ✓), à la demande de Pascal du
2026-10-24 (« mettre les choix dans une liste déroulante par un check »), pour un écran plus court.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | `ChoixThemeReglage` dans `ParametresScreen.kt` l. 437 ; idiome des menus déroulants déjà présents (`SectionRtk.kt`, `CreationContexteScreen.kt`) |
| A-t-il les moyens ? | oui | logique inchangée, déjà couverte par `ChoixThemeTest` (missions 007-008) |

**Temps de la main :** appuyer.

**Pas de `testeur` :** la mission ne change que la présentation ; la logique (`ChoixTheme`,
`estSombre`, `choixThemeDepuis`) et sa persistance ne bougent pas et restent testées.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `dev` · **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** changer la logique, les clés DataStore, les libellés
  existants (titre, description, Système / Clair / Sombre en fr et en).

## Harnais

- **Peut :** modifier `ChoixThemeReglage` dans `app/.../ui/parametres/ParametresScreen.kt` ;
  ajouter au plus une chaîne d'accessibilité (fr et en) si besoin ; dans sa branche.
- **Ne doit pas :** toucher à `core/`, `data/`, `MainActivity.kt` ; toucher à `main`.
- **Exigences d'écran (brief) :** cible ≥ 48 dp pour la ligne et pour chaque entrée du menu ;
  la coche ✓ est une icône (`Icons.Default.Check`) avec une description pour le lecteur d'écran ;
  le choix courant reste lisible sans ouvrir la liste.

## Tests

- **Comment saura-t-on que c'est juste :** `.claude/test-command` vert,
  `:app:mergeReleaseResources` vert, CI de la PR verte ; titre de PR en commit conventionnel (`feat:`).

## Terrain

- **Échantillon que Pascal vérifiera :** Paramètres → ligne « Thème » montre le choix courant ;
  appui → liste Système / Clair / Sombre avec ✓ sur le choix courant ; choisir Clair → l'appli
  passe en clair et la liste se ferme.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Pascal change de thème en deux appuis, sans faire défiler l'écran.
