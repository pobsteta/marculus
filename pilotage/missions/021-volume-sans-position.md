# Mission 021 — « Par parcelle » donne le volume des tiges sans position

## Mission

**En une phrase (quoi, et pourquoi) :** afficher, sur la ligne « sans position GNSS » de
l'onglet « Par parcelle », le volume des tiges non rattachées (net des annulations), pour que le
marteleur retrouve dans « Par parcelle » tout le volume que donne Statut. Aujourd'hui la ligne ne
donne que le nombre. Décision de Pascal du 2026-11-10 : « Je pars sur (c) » (journal du
2026-11-10, option (c) après le constat sur c924db7).

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Ce qui existe (preuves)

- **La ligne « sans position » :** nombre seul, `Text(stringResource(R.string.statut_tiges_sans_position_gnss, sansPosition))`,
  affichée seulement si `sansPosition > 0`
  (`app/src/main/kotlin/io/github/pobsteta/marculus/ui/statut/StatutHistoriqueScreen.kt`
  l. 575-583 sur c924db7) ; libellé FR « %1$d tige(s) sans position GNSS (non rattachées). »
  (`app/src/main/res/values/strings.xml` l. 175) et son équivalent EN.
- **Le calcul est prêt :** `ParParcelle.Repartition.volumesSansPosition(contexte)` rend des
  `VolumesMartelage.Totaux` (tige, houppier, total) : tiges sans position, moins le cubage de la
  saisie de chaque annulation orpheline (`core/src/main/kotlin/fr/marculus/core/ParParcelle.kt`
  l. 20-43, commit c924db7, test `ParParcelleVolumesOrphelineTest`). Il n'est appelé nulle part.
- **La forme des autres volumes de l'onglet :** `volumeTexte(...)` (l. 591), qui ajoute les
  stères « m³ (st) » en affouage à maximum renseigné (mission 017), et `LigneVolumesEmerge` pour
  houppier et total (l. 627).
- **Dépend de la 019** (PR #43) : la mission part de `main` une fois la #43 fusionnée.

## Règles

- **Même forme que les lignes de parcelle :** volume tige avec `volumeTexte`, et houppier / total
  avec `LigneVolumesEmerge` quand `Cubage.aHouppier(contexte)` ; pas de volume si
  `Cubage.aTarif(contexte)` est faux (comme `essencesAvecVolume`, l. 442-459).
- **Somme juste :** volumes des parcelles + volume « sans position » = volumes de Statut
  (`VolumesMartelage.totaux`), y compris avec une annulation orpheline.
- **Décidé par Pascal le 2026-11-10 (« Je valide les 4 ») :** la ligne s'affiche dès qu'il y a une
  tige sans position ou une annulation orpheline (aujourd'hui : seulement si le nombre net
  est > 0) ; à nombre net ≤ 0, elle garde le nombre tel quel.
- **Libellé :** le texte du nombre ne change pas sans l'accord de Pascal ; le volume va sur une
  ligne à part, sous le nombre, comme pour les parcelles.
- Ne rien changer au CSV foncier ni aux autres onglets.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | la fonction de calcul existe, l'écran a déjà la forme des lignes de volume |
| A-t-il les moyens ? | oui | émulateur `Medium_Phone_API_36.1`, contexte de test avec une tige sans position |

**Temps de la main :** appuyer. Mission d'écran : capture avant la PR (`pilotage/equipe.md` l. 22),
puis capture sur le téléphone après la release.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **`afficheur` (sonnet)** seul. Le calcul est déjà testé (c924db7) ; si l'afficheur ajoute une
  règle (par exemple la condition d'affichage de la ligne), il la sort dans `core` et la teste.

## Harnais

- **Peut :** modifier la ligne « sans position » de l'onglet « Par parcelle » et ses chaînes FR /
  EN ; dans sa branche, partie de `main` après la fusion de la #43.
- **Ne doit pas :** changer le calcul de `ParParcelle`, le CSV foncier, les autres onglets ;
  toucher à `main` ; utiliser le téléphone de Pascal (`R5CW51BZA2W`).
- **Attention :** la 020 modifie la rangée d'onglets du même fichier (l. 159-171) : n'y pas toucher.

## Tests

- **Captures, avant / après**, sur l'émulateur : contexte Schaeffer sans houppier, contexte EMERGE
  (houppier et total), contexte d'affouage à maximum renseigné (stères), clair et sombre pour au
  moins un cas, FR et EN.
- **Comment saura-t-on que c'est juste :** la somme des volumes des parcelles et de « sans
  position » égale le volume de Statut, sur une capture.

## Terrain

- **Échantillon que Pascal vérifiera :** sur le téléphone, un contexte où des tiges ont été
  marquées sans GNSS : le volume de la ligne « sans position » ajouté à celui des parcelles donne
  le volume de Statut ; capture par l'orchestrateur après la release.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

« Par parcelle » rend compte de tout le volume martelé, rattaché ou non, comme Statut.
