# Mission 034 — la virgule décimale dans le champ « % houpp. » en français

## Mission

**En une phrase (quoi, et pourquoi) :** en français, le champ de part de houppier affiche et accepte
« 7,41 » (virgule) comme la ligne de source qui l'accompagne, au lieu de « 7.41 », pour que le
marteleur lise et saisisse les nombres comme partout ailleurs. Vu aux captures de la 030 (journal du
2026-11-10) ; choisie par Pascal pour le cycle 8.

**Redite par l'agent :** _à compléter par l'agent avant d'agir._

## Ce qui existe (preuves, à 1f66af4)

- `CreationContexteScreen.kt` l. 928 : `formatPourcent(v) = formatDecimal(v)` ; la ligne de source
  écrit « 7,4 % » (virgule). À vérifier : la saisie accepte-t-elle déjà « 7,41 » ? que fait
  l'enregistrement (`creation_erreur_houppier`) ?

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment faire ? | oui : format selon la langue de l'appli | |
| A-t-il les moyens ? | oui : émulateur | |

**Temps de la main :** appuyer (mission d'écran).

## L'Homme

- **Agent :** `afficheur` (sonnet). Captures FR et EN (« 7,41 » / « 7.41 »), avant la relecture.

## Harnais

- **Peut :** le formatage et la lecture du champ de part (et du numéro s'il a le même défaut), leurs tests.
- **Ne doit pas :** changer la valeur enregistrée (toujours une fraction, `partHouppier`), ni le CSV.

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

Aucune. Après la 033 sur l'émulateur (un seul agent d'écran à la fois).

## Tests

- Affichage FR « 7,41 », EN « 7.41 » ; saisie « 7,41 » et « 7.41 » lues pareil en FR ; valeur enregistrée inchangée.

## Terrain

- **Échantillon :** sur le téléphone, « Modifier le contexte », Autres feuillus : « % houpp. » affiche une virgule.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Arrêt :** 3 allers-retours. **Qui tranche :** Pascal.
