# Mission 033 — « 1 tige » au singulier sur la carte de la liste

## Mission

**En une phrase (quoi, et pourquoi) :** la carte d'un contexte dans la liste écrit « 1 tige » et
« 1 essence » (FR), « 1 stem » et « 1 species » (EN) au singulier, pour une lecture juste sur le
terrain. Vu à l'essai A → B du 2026-11-10 (journal) ; choisie par Pascal pour le cycle 8.

**Redite par l'agent :** _à compléter par l'agent avant d'agir._

## Ce qui existe (preuves, à 1f66af4)

- `app/src/main/res/values/strings.xml` l. 46 : `liste_carte_resume` = « … · %5$d essences · %6$d tiges »,
  sans pluriel ; `values-en` l. 44 de même ; la boîte de fusion a déjà un `plurals` (`marsync_tiges`, l. 313).

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment faire ? | oui : `plurals` Android, modèle `marsync_tiges` | |
| A-t-il les moyens ? | oui : émulateur | |

**Temps de la main :** appuyer (mission d'écran).

## L'Homme

- **Agent :** `afficheur` (sonnet). Captures FR/EN, clair/sombre, 384/360 dp, avec 0, 1 et 2 tiges,
  jointes **avant** la relecture.

## Harnais

- **Peut :** `strings.xml` FR/EN, l'endroit qui compose la carte, un test du formatage si possible.
- **Ne doit pas :** changer ce qui est compté (`ResumeContexte.nbTiges`, net), toucher au téléphone.

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

Aucune. Ne pas utiliser l'émulateur en même temps que la 032 ou la 034.

## Tests

- Formatage 0 / 1 / 2 tiges et 1 / 2 essences en FR et EN (le français met le singulier à 0 et 1).

## Terrain

- **Échantillon :** la carte de la 1116 (1 tige) sur le téléphone de Pascal.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Arrêt :** 3 allers-retours. **Qui tranche :** Pascal.
