# Mission 019 — « Par parcelle » et le CSV foncier retirent les tiges annulées

## Mission

**En une phrase (quoi, et pourquoi) :** faire compter à l'onglet « Par parcelle » de Statut /
historique et au CSV foncier les tiges **nettes**, en retirant chaque annulation avec la règle de
Statut. Aujourd'hui les deux ne gardent que les `PLUS` et ignorent les annulations :
`val plus = journal.filter { it.action == ActionTige.PLUS }`
(`app/src/main/kotlin/io/github/pobsteta/marculus/ui/statut/StatutHistoriqueScreen.kt` l. 430)
et `journal.filter { it.action == ActionTige.PLUS && it.position != null }` (même fichier, l. 586,
`csvFoncier`). Constaté sur le téléphone de Pascal, v0.67.0, contexte « Affouage » : « Par
parcelle » annonce 23 tiges et 13,47 m³ quand Statut et Par lot annoncent 22 et 13,39 m³
(journal du 2026-11-09). Demande de Pascal du 2026-11-09 : « oui, écris la fiche du décompte
Par parcelle ».

**Redite par l'agent :** _reconstituée le 2026-11-10 d'après le journal (2026-11-09 et 2026-11-10, lignes 019), l'archiviste n'ayant pas rédigé de redite à l'époque._ Faire compter à « Par parcelle » et au CSV foncier les tiges nettes, en retirant les annulations avec la règle de Statut (réutilisation de `JournalNet.tiges`, décision de l'orchestrateur du 2026-11-09). Livrée par la PR #43 (53e446c) après deux allers-retours (volume d'une annulation orpheline retiré de « sans position », décision de Pascal « Oui au 3 »). Redite de l'agent d'origine : non retrouvée.

## Ce qui existe (preuves)

- **La règle d'annulation :** `VolumesMartelage.totaux`
  (`core/src/main/kotlin/fr/marculus/core/VolumesMartelage.kt` l. 13-17 et l. 69-107). Le journal
  est trié par `horodatage` puis `uuid` ; une annulation retire, **dans la même case** (essence ×
  classe), les dernières tiges comptées avant elle et pas encore annulées (pile par case). Une
  annulation sans tige à retirer vaut sa propre saisie, en négatif.
- **Le nombre net :** `TotauxMartelage.totalContexte()`
  (`core/src/main/kotlin/fr/marculus/core/TotauxMartelage.kt` l. 26).
- **Le rattachement à la parcelle** se fait par la **position** de la tige (point dans polygone,
  `AttributionSpatiale.contient`, l. 434-435 et l. 587). Une annulation est saisie sans position :
  elle ne peut pas être rattachée seule. C'est la tige qu'elle retire qui porte la parcelle.

## Règles

- **Une seule règle d'annulation.** Extraire de `VolumesMartelage.totaux` une fonction pure de
  `:core` qui rend les tiges `PLUS` **restantes** (avec leur quantité restante) après application
  des annulations, par exemple `JournalNet.tigesRestantes(journal): List<Tige>`, et la faire
  utiliser par `VolumesMartelage.totaux` **sans en changer les résultats** (ses tests existants
  restent verts tels quels).
- « Par parcelle » et `csvFoncier` partent de ces tiges restantes, au lieu du filtre `PLUS`. Le
  reste (rattachement, regroupement, volumes, surface terrière, colonnes du CSV) ne change pas.
- **Annulation sans tige à retirer** (journal fusionné dans le désordre) : elle n'a ni tige ni
  position, donc ni parcelle. Le dev propose comment la montrer (ligne « hors parcelle » en
  négatif, ou rien) et **le dit dans son rendu** ; il ne tranche pas seul.
- La somme des tiges de « Par parcelle », toutes lignes comprises (rattachées, hors parcelle, sans
  position), égale le nombre net de Statut (`TotauxMartelage.totalContexte()`), hors le cas
  ci-dessus.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | extraire la pile d'annulation existante, puis changer deux filtres |
| A-t-il les moyens ? | oui | `VolumesMartelage.kt` et ses tests, `StatutHistoriqueScreen.kt` ; le contexte « Affouage » du téléphone comme échantillon |

**Temps de la main :** appuyer. Le changement touche une règle de calcul partagée
(`VolumesMartelage`) : jamais lâché (`pilotage/equipe.md`).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **`testeur` (sonnet), d'abord :** tests d'acceptation depuis cette fiche, sans lire le code.
- **`dev` (sonnet), ensuite :** réalise dans sa branche, tests compris.

## Harnais

- **Peut :** ajouter la fonction pure dans `:core` et la faire utiliser par `VolumesMartelage` ;
  sortir `csvFoncier` et le calcul de « Par parcelle » de l'écran vers `:core` s'il le faut pour
  les tester ; dans sa branche.
- **Ne doit pas :** changer les résultats de `VolumesMartelage.totaux` ni de `TotauxMartelage`, le
  journal, le `.marsync`, le CSV d'échange, les colonnes du CSV foncier ; toucher à `main`.
- **Attention :** le tri du journal (`horodatage`, puis `uuid`) fait partie de la règle ; une tige
  annulée retirée en partie (`quantite` > 1) garde sa quantité restante.

## Tests

- **Unitaires (`.claude/test-command`) :**
  - +1 dans la parcelle A, +1 dans la parcelle B (même case), annulation → c'est la **dernière**
    (B) qui est retirée : A = 1, B = 0 ;
  - +3 (`quantite` 3) puis annulation de 1 → il en reste 2 dans la parcelle ;
  - cases différentes : une annulation de CHE 30 ne retire rien à HET 30 ;
  - une tige sans position annulée disparaît des « sans position » ;
  - somme de « Par parcelle » = `TotauxMartelage.totalContexte()` ; volumes de « Par parcelle »
    sommés = `VolumesMartelage.totaux` ;
  - CSV foncier : la colonne `Nombre` suit le net ;
  - `VolumesMartelage` : tests existants inchangés et verts.
- **Comment saura-t-on que c'est juste :** CI verte ; le relecteur vérifie qu'il n'existe plus
  qu'une seule règle d'annulation.

## Terrain

- **Échantillon que Pascal vérifiera :** sur le téléphone, contexte « Affouage » : « Par parcelle »
  annonce 22 tiges et 13,39 m³, comme Statut ; le CSV foncier exporté donne le même total.
  Capture par l'orchestrateur après la release.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Statut, Par lot, Par parcelle et le CSV foncier donnent le même nombre de tiges et le même volume.
