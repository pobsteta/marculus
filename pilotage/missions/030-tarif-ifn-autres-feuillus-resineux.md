# Mission 030 — une proposition de tarif IFN pour « Autres feuillus » et « Autres résineux »

## Mission

**En une phrase (quoi, et pourquoi) :** proposer pour « Autres feuillus » (resp. « Autres
résineux ») un tarif IFN calculé comme la moyenne, pondérée par le nombre d'arbres, des feuillus
(resp. résineux) de la SER qui ne sont pas listés à part dans le contexte, en écartant les lignes au
numéro de tarif négatif, pour que ces deux regroupements reçoivent une proposition comme les autres
essences. Décisions de Pascal du 2026-11-10 (journal, lignes `Pascal · 029` et `Pascal · 030`).

**Redite par l'agent :** _à compléter par l'agent avant d'agir._

## Ce qui existe (preuves, à 25b6314)

- `TarifsIfn.proposer` (`core/src/main/kotlin/fr/marculus/core/TarifsIfn.kt` l. 112-135) : cascade
  SER → GRECO → France, seuil `SEUIL_ARBRES = 30` (l. 166), numéro borné à 1..20 (l. 124),
  `proposerPartHouppier` (l. 137-150), `typeConseille` (l. 156-161).
- Les regroupements : `Referentiels.kt` l. 6. Aucun code IFN ne leur correspond (note
  `docs/sources/2026-11-10-ifn-29AF-68CE.md`, mission 029).
- La table : `data/src/main/assets/tarifs-ifn/tarifs_ser.csv` ; 276 lignes à 30 arbres ou plus ont
  un numéro négatif, ramené à 1 sans signal (029).

## Règles (décisions de Pascal du 2026-11-10)

1. « Autres feuillus » = moyenne des **feuillus** de la table pour le niveau considéré, **sauf les
   essences listées à part dans le contexte** ; « Autres résineux » = de même avec les **résineux**.
2. Moyenne des numéros de tarif **pondérée par le nombre d'arbres** ; même cascade SER → GRECO →
   France ; le seuil de 30 arbres s'applique au **total** des arbres retenus au niveau.
3. Même calcul pour la **part de houppier** proposée.
4. Une ligne au numéro **négatif est écartée** partout (proposition par essence comme moyenne) :
   on passe au niveau suivant comme si elle n'existait pas.
5. Le partage feuillu / résineux suit le code d'espèce IFN (à établir et citer : la
   nomenclature IGN `ESSPRE`, note 029, S1) — **à confirmer par le testeur dans ses sources,
   question à Pascal si un cas est ambigu.**

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment faire ? | oui : `TarifsIfn`, note 029 | |
| A-t-il les moyens ? | oui, table embarquée + tests du testeur | testeur d'abord |

**Temps de la main :** appuyer (règle de calcul nouvelle ; Pascal relit la PR). ADR 0010 par
l'archiviste après fusion.

## L'Homme

- **Agents :** `testeur` (sonnet), puis `dev` (sonnet).

## Harnais

- **Peut :** `:core` (`TarifsIfn` et ses tests), l'écran qui affiche la proposition si l'indication
  du niveau ou de la moyenne doit changer (alors captures avant relecture).
- **Ne doit pas :** modifier la table embarquée, toucher à la 026/027.

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune ; tests rouges jusqu'au commit du dev sur la même branche.

## Tests

- Moyenne pondérée calculée à la main sur la table de test ; exclusion des essences du contexte ;
  cascade et seuil sur le total ; ligne négative écartée (proposition par essence aussi).

## Terrain

- **Échantillon :** un contexte de la SER de Pascal avec Chêne, Hêtre, Autres feuillus, Sapin,
  Épicéa, Autres résineux : chaque essence reçoit une proposition.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Arrêt :** 3 allers-retours. **Qui tranche :** Pascal.
