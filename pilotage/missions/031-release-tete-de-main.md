# Mission 031 — une version seulement pour la tête de `main`, et pas pour des tests seuls

## Mission

**En une phrase (quoi, et pourquoi) :** la release ne part que si le commit est encore la tête de
`main` et que l'APK a changé (les fichiers de test ne comptent pas), pour qu'une CI relancée sur un
ancien commit ne publie jamais une version plus haute contenant moins de code, et qu'une PR de tests
seuls ne publie pas un APK identique. Décision de Pascal du 2026-11-10 (journal, ligne
« Pascal · 024 · consigne nuancée … corriger le risque lui-même par une mission CI »).

**Redite par l'agent :** _à compléter par l'agent avant d'agir._

## Ce qui existe (preuves, à 1f66af4)

- `.github/scripts/release-necessaire.sh` l. 5 (exclus : `pilotage/`, `docs/`, `*.md`,
  `.github/badges/`), l. 13 (`git describe` sur le tag atteignable depuis le commit), l. 22.
- `.github/workflows/release.yml` l. 48-57 (version calculée depuis le dernier tag du dépôt),
  l. 99 (`versionCode = run_number`) ; `ci.yml` l. 13 (`cancel-in-progress`).
- Faits : 07a2245, 02fc671, a800bb8 restés sans tag (CI annulées) ; la #55 (tests seuls) a publié
  v0.70.5 = même APK que v0.70.4 ; la 026 seule a publié v0.71.0 quelques minutes avant v0.72.0.
- Tests : `.github/scripts/test-release-necessaire.sh` (18 vérifications, lancé par `ci.yml`).

## Règles

1. Pas de release si le commit n'est plus la tête de `origin/main` au moment de la décision
   (dire « pas de release : commit dépassé par <tête> »).
2. Ne comptent pas, en plus des exclusions actuelles : `**/src/test/**`, `**/src/androidTest/**`,
   `**/src/test/resources/**`, `.github/scripts/test-*.sh`.
3. Dans le doute, on publie (règle de la 023 inchangée).
4. Le README (consigne de relance) et l'ADR 0008 sont mis à jour par renvoi daté si la consigne
   change de sens.

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment faire ? | oui : script et tests de la 023 | |
| A-t-il les moyens ? | oui : historique des tags, script de test | |

**Temps de la main :** **lâcher** (genre CI, réussi trois fois : 001, 015, 023 ; `pilotage/equipe.md`).

## L'Homme

- **Agent :** `dev` (sonnet), tests compris dans le script de test.

## Harnais

- **Peut :** `.github/scripts/`, `.github/workflows/release.yml`, `ci.yml`, README (renvoi daté), ADR 0008 (ajout daté).
- **Ne doit pas :** changer le calcul de version ni la signature, toucher au code de l'appli.

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

Aucune ; CI verte attendue.

## Tests

- Cas ajoutés au script de test : commit dépassé → pas de release ; PR de tests seuls → pas de
  release ; test + code → release ; historique rejoué (07a2245, #55, 026 seule).

## Terrain

- **Échantillon :** la prochaine PR de tests seuls ne publie aucune version (vérifié par l'orchestrateur).

## Qui veille, qui tranche

- **Relecteur :** `relecteur` ; fusion par l'orchestrateur si « prêt » et CI verte (lâché).
- **Arrêt :** 3 allers-retours. **Qui tranche :** Pascal.
