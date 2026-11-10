# Mission 027 — les cases « tige » et « houppier » à l'écran du contexte

## Mission

**En une phrase (quoi, et pourquoi) :** quand « Affouage » est coché, l'écran de création et de
modification du contexte montre, pour chaque essence, deux cases « tige » et « houppier » (cochées
par défaut), pour que le marteleur règle sur le terrain ce qui compte dans le lot (mission 026).

**Redite par l'agent :** _à compléter par l'agent avant d'agir._

## Ce qui existe (preuves, à 4dd7526)

- Réglages par essence du même écran : part de houppier et tarif
  (`app/.../ui/contextes/CreationContexteScreen.kt` l. 126-189).

## Règles

- Cases visibles seulement si « Affouage » est coché ; à côté des réglages de l'essence.
- Les deux décochées : permis, une ligne dit « cette essence ne compte pas dans le lot ».
- Textes FR et EN ; libellés sans mot coupé à 360 dp (comme la 020).

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment faire ? | oui : les réglages voisins de l'essence | |
| A-t-il les moyens ? | non avant la fusion de la 026 (champs du modèle) | part après la 026 |

**Temps de la main :** appuyer (mission d'écran).

## L'Homme

- **Agent :** `afficheur` (sonnet). Captures clair et sombre, FR et EN, 384 et 360 dp jointes au
  rapport **avant** la relecture (`pilotage/pre-fusion.md` point 4).

## Harnais

- **Peut :** l'écran du contexte, `strings.xml` FR et EN, un test d'écran s'il en existe le modèle.
- **Ne doit pas :** changer la règle ou le modèle (026), toucher à `main`.

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 026 fusionnée ; part de `origin/main` après elle.
- **CI rouge attendue :** aucune.

## Tests

- Le réglage écrit puis relu dans le contexte (cases cochées / décochées) ; captures.

## Terrain

- **Échantillon :** sur le téléphone, « Affouage » de Daniel, Autres feuillus : décocher « tige »,
  voir Par lot passer de 15,75 à 2,36 m³.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Arrêt :** 3 allers-retours. **Qui tranche :** Pascal.
