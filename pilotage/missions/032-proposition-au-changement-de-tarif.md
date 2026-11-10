# Mission 032 — changer le tarif après la commune repropose le numéro

## Mission

**En une phrase (quoi, et pourquoi) :** à l'écran du contexte, choisir ou changer le tarif d'une
essence après avoir choisi la commune doit proposer son numéro IFN et sa part de houppier comme au
choix de la commune, pour que le marteleur n'ait pas à appuyer sur « Reproposer ». Carte du bac à
sable du 2026-11-10 (vu par le dev de la 030, journal) ; choisie par Pascal pour le cycle 8.

**Redite par l'agent :** _à compléter par l'agent avant d'agir._

## Ce qui existe (preuves, à 1f66af4)

- `CreationContexteScreen.kt` l. 391 : `onTarif = { tarifs[essence] = it; appliquerPropositions(listOf(essence)) }`
  — le code semble déjà reproposer ; l. 178-184 : `suivre` n'écrit que si le champ est vide ou égal
  au dernier proposé ; l. 344 « Reproposer » force.
- **Le défaut n'est donc pas encore reproduit** : première tâche, le reproduire (quelles étapes,
  quel champ reste vide) avant toute correction.

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment faire ? | non tant que le défaut n'est pas reproduit | appuyer, reproduction d'abord |
| A-t-il les moyens ? | oui : émulateur, uiautomator | testeur d'abord |

**Temps de la main :** appuyer.

## L'Homme

- **Agents :** `testeur` (sonnet) : reproduit et écrit le test (logique sortie de l'écran si besoin,
  sans lire l'implémentation au-delà des signatures) ; puis `dev` (sonnet).

## Harnais

- **Peut :** `CreationContexteScreen.kt`, une petite logique testable sortie de l'écran, ses tests ;
  l'émulateur `emulator-5554` (un seul agent d'écran à la fois).
- **Ne doit pas :** changer `TarifsIfn` ni la règle « champ retouché non réécrit », toucher au téléphone.

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

Aucune ; test rouge jusqu'au commit du dev, même branche.

## Tests

- Le scénario reproduit devient un test (commune puis tarif → numéro proposé ; champ retouché → non réécrit).
- Captures avant / après (FR, 384 dp), jointes avant la relecture.

## Terrain

- **Échantillon :** Pascal choisit la commune, puis met « Autres feuillus » en Schaeffer lent : le numéro s'affiche aussitôt.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Arrêt :** 3 allers-retours. **Qui tranche :** Pascal.
