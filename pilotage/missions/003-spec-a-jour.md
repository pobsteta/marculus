# Mission 003 — remettre la spec à jour de ce qui est livré

## Mission

**En une phrase (quoi, et pourquoi) :** corriger dans
`docs/specs/2026-06-21-marculus-v1-spec.md` les passages en retard sur le produit livré
(v0.63.6), parce que c'est la spec en vigueur et que le brief y renvoie au lieu de la recopier.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | écarts listés ci-dessous, relevés par le `contradicteur` le 2026-10-23 |
| A-t-il les moyens ? | oui | code, `git log`, releases GitHub, specs de tranche |

**Temps de la main :** appuyer — la spec est la source de vérité : il rend la liste des
corrections, chacune prouvée par le code ou un commit, avant de les écrire.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `archiviste`
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** trancher les questions ouvertes du brief (unité du lot,
  réseau, appareil perdu) ; il les laisse telles quelles ; écrire `docs/spec.md`.

## Harnais

- **Peut :** modifier la spec v1 et les specs de tranche concernées, dans sa branche.
- **Ne doit pas :** changer une règle métier ; une contradiction de fond (par exemple tige ou
  houppier dans le cumul d'un lot : `contexte-affouage.md` l. 24-25 contre spec v1 l. 150-153)
  est signalée, pas résolue ; pas de code, pas de `main`.
- **Écarts connus :** base Room « v14 » au § 9 (l. 223) contre v17 ; « tables en attente de
  nemeton » (l. 129, 247) alors qu'elles sont livrées en v0.62.0 ; « OSM + satellite ESRI »
  (l. 28) alors que les fonds sont IGN depuis v0.57.0 ; § 10 « Reste à faire » à relire ligne à
  ligne. Il en cherche d'autres.
- **Sources autorisées :** `docs/specs/`, `docs/brief.md`, le code (lecture), `git log`,
  `gh release list`, `~/dev/briefs/traites/INDEX.md`.

## Tests

- **Comment saura-t-on que c'est juste :** chaque correction cite sa preuve (fichier et ligne,
  commit ou release) dans la description de la PR ; aucune ligne de règle métier ne change.

## Terrain

- **Échantillon que Pascal vérifiera :** le § 10 « Reste à faire » : correspond-il à ce qu'il
  sait du terrain ?

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** après le plan de corrections, avant l'écriture.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Le `contradicteur`, relancé sur la spec, ne relève plus d'écart de fait avec le produit livré.

## Décisions de Pascal (2026-10-24)

- **Feu vert sur le plan** rendu par l'archiviste (points 1 à 9, ajouts 7 et 8 compris). Le plan
  complet est dans `pilotage/journal.md` (ligne du 2026-10-24, archiviste, 003) et ci-dessous.
- **Lot d'affouage : houppier seul.** Pour un arbre avec houppier, seul le houppier compte dans
  le lot (comme le code `AffouageLots.volumes` et la spec v1 l. 150-153). Corriger
  `docs/specs/2026-10-07-contexte-affouage.md` l. 22-23 (« bois fort tige ») par une ligne datée
  qui renvoie à cette décision, sans effacer l'ancienne formulation.

### Plan validé (rappel)

1. l. 223 : Room v14 → v17 (`MarculusDatabase.kt` l. 53).
2. l. 129-130 : tables SER « en attente » → ✅ livrées en v0.62.0.
3. l. 247-248 (§ 10) : retirer « Tables IFN à recevoir ».
4. l. 28 : fonds en ligne = Plan IGN (défaut), Ortho IGN, OSM, cadastre IGN en couche (v0.57.0) ; ESRI disparu.
5. En-tête : ligne « Mise à jour 2026-10-24 » qui renvoie aux sections.
6. l. 227 : targetSdk 36.
7. § 9 : une ligne de renvoi aux bibliothèques `terrain-*`.
8. Écran 6 : citer le réglage « Estimer la hauteur (MNH) », renvoi à `couche-houppier-mnh.md`.
9. l. 244-246 : reformuler sans changer le fond (recette affouage, format 4, formats 6-7 signalés
   le 2026-10-24 par le brief `vers-nemetonshiny/2026-10-24-marculus-csv-formats-6-7.md`).

Version de référence : renvoyer aux releases GitHub plutôt que recopier un numéro (v0.64.2 au 2026-10-24).

## Remarques du relecteur — aller-retour 1/3 (2026-10-24)

Rendu : commit b5e1bce (worktree `.claude/worktrees/agent-a36ede3210423478a`). Sept points sur neuf
vérifiés exacts. À reprendre dans cette même branche :

1. **§ 10, ligne nemetonshiny : fait faux.** Le format 5 a été signalé le 2026-10-15 et traité le
   2026-10-16 (nemetonshiny `52edbfcd` lit `FormatCsv;5`, `~/dev/briefs/traites/INDEX.md` l. 191) ;
   le format 4 aussi (nemetonshiny 2026.10.1, `traites/2026-10-15-traite-nemetonshiny-format-4-affouage.md`,
   INDEX l. 151). Seuls 6 et 7 datent du brief du 2026-10-24, **urgence faible** (décision de
   Pascal), le lecteur importe déjà le format 7. Ne garder en 🔜 que la recette terrain de
   l'affouage et ce brief 6-7.
2. **§ 1, l. 32 : versions des fonds.** Ortho IGN à la place d'ESRI en **v0.47.0** (`da84eb8`,
   2026-09-24) ; Plan IGN (défaut) et cadastre IGN en **v0.57.0** (`c847712`). (L'erreur venait
   de la fiche.)
3. **`contexte-affouage.md` l. 110** (onglet « Par lot » : « volume bois fort tige ») : ajouter une
   note datée de même facture que celle des l. 26-29 (le code `AffouageLots.bilan` affiche tige,
   houppier et volume du lot).
4. **Spec v1 l. 140** (« volume maximal d'un lot (m³, bois fort tige) ») : **ne pas toucher**,
   question de sens posée à Pascal par l'orchestrateur.
5. En-tête : « v0.64.2 au 2026-10-24 » est déjà dépassé ; renvoyer aux releases sans numéro.
