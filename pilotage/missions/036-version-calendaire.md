# Mission 036 — des numéros de version calendaires, comme nemeton

## Mission

**En une phrase (quoi, et pourquoi) :** la CI de release numérote chaque version `vAAAA.MM.N`
(année, mois, compteur du mois qui repart à 1 chaque mois) au lieu du SemVer `v0.Y.Z`, pour que
Marculus se numérote comme nemeton et qu'on lise d'un coup d'œil de quand date une version.
Décision de Pascal du 2026-11-11 (compteur par mois).

**Redite par l'agent :** _à compléter par l'agent avant d'agir._

## Ce qui existe (preuves, à 44ca151)

- `.github/workflows/release.yml` l. 44-57 : `mathieudutour/github-tag-action@v6.2`, SemVer d'après les
  commits conventionnels (`default_bump: patch`, `tag_prefix: v`, `commit_sha` = `head_sha`) ;
  l. 96-99 : `-PappVersionName=new_version`, `-PappVersionCode=github.run_number` ; l. 105 : nom de l'APK
  avec `new_tag` ; l. 109-116 : release GitHub avec `changelog`.
- `.github/scripts/release-necessaire.sh` l. 41 : base = `git describe --tags --abbrev=0 --match 'v*'` ;
  tests `.github/scripts/test-release-necessaire.sh` (tags `v0.*`, rejoue de vrais commits).
- Modèle : `~/dev/nemeton` tags `v2026.10.1` … `v2026.11.22` (version tirée de DESCRIPTION, à la main ;
  ici elle doit être calculée par la CI).
- `versionCode` = `github.run_number` : ne dépend pas du nom, les mises à jour restent possibles.

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment faire ? | oui : calcul du tag dans un script testable, comme `release-necessaire.sh` | |
| A-t-il les moyens ? | oui : script et tests shell rejoués en CI | |

**Temps de la main :** lâcher (genre CI, lâché par Pascal : 023, 031).

## L'Homme

- **Agent :** `dev` (sonnet).

## Harnais

- **Peut :** un script `.github/scripts/version-calendaire.sh` (tags existants + date UTC → prochain tag),
  ses tests, son branchement dans `release.yml` à la place de `github-tag-action` (tag annoté sur
  `head_sha`, notes de version générées), le README (section versions, badge), la ligne de pré-fusion
  sur le titre de PR qui « fixe la version ».
- **Ne doit pas :** toucher à la signature, au `versionCode`, à la règle de la 031 (release nécessaire
  ou non) ; renommer ni supprimer les anciens tags `v0.*`.
- **À tenir :** `git describe --match 'v*'` doit continuer de trouver le dernier tag, anciens et
  nouveaux mélangés (l'ordre est celui de l'historique, pas du nom) ; le compteur se lit parmi les tags
  `vAAAA.MM.*` du mois courant (UTC) ; deux releases le même mois → `.1`, `.2` ; un changement de mois
  → `.1`.

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

Aucune. La première version publiée après la fusion sera `v2026.11.1` (si elle part en novembre).

## Tests

- Premier tag calendaire après des `v0.*` → `v2026.11.1` ; `v2026.11.1` existe → `v2026.11.2` ;
  `v2026.11.9` existe → `v2026.11.10` (tri numérique) ; mois suivant → `v2026.12.1` ; un tag d'un autre
  mois n'influe pas ; `release-necessaire.sh` trouve un tag calendaire comme base.

## Terrain

- **Échantillon :** la release suivant la fusion s'appelle `Marculus v2026.11.N` ; l'APK
  `marculus-v2026.11.N-….apk` s'installe par-dessus la v0.73.x sur le téléphone de Pascal.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis fusion par l'orchestrateur (lâché, CI verte). **Arrêt :**
  3 allers-retours. **Qui tranche :** Pascal.
