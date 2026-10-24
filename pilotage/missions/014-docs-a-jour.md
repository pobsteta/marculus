# Mission 014 — README « Déployer et publier », brief et pilotage remis à jour

## Mission

**En une phrase (quoi, et pourquoi) :** ajouter au README une section « Déployer et publier »
(compiler, tester avec `.claude/test-command`, release au merge de la PR, installation sur le
téléphone par `gh release download` + `adb install -r`, JDK complet, retour arrière par revert) et
corriger par lignes datées les faits périmés de `docs/brief.md`, pour qu'un successeur sache
installer sans la mémoire de Claude (bilan n° 1, test du successeur).

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | procédure ci-dessous ; `.github/workflows/release.yml` ; README actuel |
| A-t-il les moyens ? | oui | |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## La procédure à écrire (aujourd'hui seulement dans la mémoire de Claude)

- Le téléphone tourne sur l'APK **release** signé par la CI ; un build debug local ne s'installe
  pas par-dessus (signature différente, `versionCode` plus bas).
- Release : merge d'une PR sur `main` → `release.yml` calcule la version depuis les commits
  conventionnels (`feat:` → mineur, `fix:` → patch ; titre de PR conventionnel), publie l'APK.
- Installation : `gh release download vX.Y.Z -R pobsteta/marculus -p '*.apk'` puis
  `adb install -r marculus-vX.Y.Z-*.apk` (données conservées).
- Compilation locale : JDK complet (Adoptium 21) via `JAVA_HOME`, le JRE système n'a pas `javac` ;
  avant de pousser des chaînes, `:app:mergeReleaseResources` (apostrophes).
- Sur le téléphone de Pascal : `adb install` et captures seulement, jamais de taps à l'aveugle.
- Clé de signature : secrets CI ; **emplacement de la sauvegarde : à fournir par Pascal** (laisser
  un « à compléter par Pascal » visible, ne rien inventer).

## Corrections (lignes datées, sans effacer : règle du sommier)

- README : « release à chaque push sur main » → au merge de la PR ; tests : citer `.claude/test-command`
  (`:core`, `:data`, `:app`) ; pile technique : renvoyer à `gradle/libs.versions.toml`.
- `docs/brief.md` § Ce que fait Marculus : version → « voir les releases » ; écarts de spec corrigés
  (PR #17) ; formats 6-7 signalés le 2026-10-24 (brief déposé), lecteur R tolérant ; question du
  lot → règle de Pascal du 2026-10-24 (renvoi à l'ADR 0004 quand elle existe).
- CLAUDE.md : une ligne « le plugin `imperatrice` (pobsteta) porte la méthode, les agents et les
  hooks ; sans lui on lit `pilotage/` mais on ne rejoue pas la boucle ».

## L'Homme

- **Agent :** `archiviste` · **Modèle :** sonnet.

## Harnais

- **Peut :** modifier `README.md`, `docs/brief.md`, `CLAUDE.md` (la seule ligne ci-dessus) ; dans sa branche.
- **Ne doit pas :** toucher au code, à `pilotage/` (tenu par l'orchestrateur), à `main`.
- **Règle de preuve :** chaque fait cité porte sa preuve.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Un successeur installe la dernière release sur un téléphone en suivant le README seul.

## Remarques du relecteur — aller-retour 1/3 (2026-10-24)

Rendu : commit f3d1f6e (worktree `.claude/worktrees/agent-af5c7f5a4c271d86c`). Procédure conforme à
`release.yml`, à `.claude/test-command` et à `app/build.gradle.kts` ; clé sans invention. À reprendre :

1. **« tables en attente »** : la preuve existe, la mettre : spec v1 l. 19-20 et l. 135-136,
   journal l. 36 ; retirer « n'a pas été revérifié ici ».
2. **Règle du sommier** : dans `docs/brief.md` § Échanges avec Nemeton, la phrase du 2026-10-23
   (« `nemetonshiny` ne lit que les formats 2 à 5 … ») a été réécrite sur place : la garder telle
   quelle, ou la citer en entier dans la ligne datée. Même chose pour « Version installée :
   **v0.63.6** (2026-10-23) » : citer sa forme exacte.
3. **README l. 162, fait faux** : `release.yml` ne lance aucun test (ni `needs` ni `workflow_run`) ;
   les tests sont dans `ci.yml`. Corriger le fait ; ne pas trancher la conséquence (question posée à
   Pascal par l'orchestrateur).
4. **Preuves « journal 2026-10-24 »** : donner les numéros de ligne (urgence l. 72, règle du lot
   l. 73, dépôt l. 57, constat du relecteur l. 43 — à recontrôler).
5. **Règle du lot** : la nuance EMERGE n'est plus ouverte : **confirmée par Pascal** (journal,
   ligne « règle EMERGE confirmée (« oui pour EMERGE ») ») : houppier seul dès qu'EMERGE couvre
   l'essence, sinon tige entière. L'écrire comme décidée, avec sa ligne.
