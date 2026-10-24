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
