# Mission 018 — la liste des contextes compte les tiges nettes, comme Statut

## Mission

**En une phrase (quoi, et pourquoi) :** faire afficher par la liste des contextes le nombre
**net** de tiges (PLUS moins ANNULATION, pondérés par `quantite`), le même que Statut, la feuille
et la boîte « Ouvrir avec » de la 016. Aujourd'hui la carte compte les **lignes du journal**
(`SELECT contexteId, COUNT(*) … FROM tige GROUP BY contexteId`,
`data/src/main/kotlin/io/github/pobsteta/marculus/data/db/Daos.kt` l. 41-42), annulations
comprises et `quantite` ignorée. Le même contexte affiche donc deux nombres différents selon
l'écran : vu par le relecteur de la 016 (journal du 2026-11-09, P1). Décision de Pascal du
2026-11-09 : « oui pour aligner la liste ».

**Redite par l'agent :** _reconstituée le 2026-11-10 d'après le journal (2026-11-09 et 2026-11-10, lignes 018), l'archiviste n'ayant pas rédigé de redite à l'époque._ Faire afficher par la liste des contextes le nombre net de tiges, le même que Statut, en gardant le nombre d'événements pour le verrouillage et l'état Kanban. Livrée par la PR #42 (07a2245) après deux allers-retours (aide du changement de mode revenue à `nbEvenements > 0`). Redite de l'agent d'origine : non retrouvée.

## Ce qui existe (preuves)

- **La règle du net :** `TotauxMartelage.totalContexte()`
  (`core/src/main/kotlin/fr/marculus/core/TotauxMartelage.kt` l. 26) : somme des `quantite` des
  PLUS moins celle des ANNULATION. C'est **la seule règle** : ne pas en écrire une seconde
  (en SQL, l'équivalent est `SUM(CASE action WHEN 'PLUS' THEN quantite ELSE -quantite END)` ;
  si le dev le choisit, un test vérifie l'égalité avec `TotauxMartelage` sur un même journal).
- **Le compte actuel :** `ResumeContexte.nbEvenements`
  (`data/.../MartelageRepository.kt` l. 29-31), qui sert à **deux choses** :
  - **affichage** : carte de la liste (`liste_carte_resume`, `ListeContextesScreen.kt` l. 700-708),
    détail (`liste_detail_tiges` « Tiges enregistrées : %1$d », l. 466 et l. 655), aide du
    changement de mode (`liste_mode_aide` « Les %1$d tiges gardent leur classe… », l. 523-525) ;
  - **logique** : verrouillage (`verrouille = nbEvenements > 0 && !contexte.exporte`, l. 31) et
    état Kanban (`r.nbEvenements > 0 -> EtatKanban.PLANIFIEE`, `ListeContextesScreen.kt` l. 594).

## Règles

- **Les nombres affichés passent au net**, partout où la liste montre un nombre de tiges.
- **La logique garde le compte des événements.** Un contexte où l'on a marqué puis annulé une
  tige a un net de 0 mais un journal non vide : il reste verrouillé tant qu'il n'est pas exporté,
  et son état Kanban ne change pas. Ne pas remplacer `nbEvenements` : ajouter le net à côté
  (par ex. `ResumeContexte.nbTiges`).
- `liste_detail_tiges` (« Tiges enregistrées ») : afficher le net. Si le dev juge utile de montrer
  aussi le nombre d'événements, il le propose sans le faire.
- Un net négatif n'est pas attendu (une annulation suit une tige) ; s'il se présente (journal
  fusionné incomplet), l'afficher tel quel, ne pas le masquer, et le dire dans le rendu.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | une requête ou un calcul de plus dans `MartelageRepository`, et quatre appels d'affichage |
| A-t-il les moyens ? | oui | `Daos.kt`, `MartelageRepository.kt`, `ListeContextesScreen.kt`, tests Room de `:data` |

**Temps de la main :** appuyer. Correctif d'un nombre affiché, sans changement de mise en page ;
capture sur le téléphone après la release (règle reprise de Surculus, journal du 2026-11-09).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **`testeur` (sonnet), d'abord :** tests d'acceptation depuis cette fiche, sans lire le code.
- **`dev` (sonnet), ensuite :** réalise dans sa branche, tests compris.

## Harnais

- **Peut :** ajouter une requête au `TigeDao` ou un calcul dans `MartelageRepository`, un champ à
  `ResumeContexte`, changer les quatre affichages.
- **Ne doit pas :** changer `TotauxMartelage`, le verrouillage, l'état Kanban, le journal, le
  `.marsync`, les CSV ; toucher à `main`.
- **Attention :** la liste observe un `Flow` : le net doit suivre les ajouts et annulations sans
  relancer l'écran.

## Tests

- **Unitaires (`.claude/test-command`) :**
  - contexte avec +1, +1, annulation → la liste annonce 1, le journal compte 3 événements ;
  - `quantite` = 3 (incrément) → la liste annonce 3 ;
  - +1 puis annulation → la liste annonce 0, le contexte reste verrouillé (non exporté) ;
  - plusieurs contextes : chacun son net ;
  - le net de la liste égale `TotauxMartelage.totalContexte()` sur le même journal.
- **Comment saura-t-on que c'est juste :** CI verte ; le relecteur vérifie que le verrouillage et
  l'état Kanban lisent toujours le compte des événements.

## Terrain

- **Échantillon que Pascal vérifiera :** sur le téléphone, le contexte « Affouage » : la carte de la
  liste annonce le même nombre que Statut (22 tiges dans Statut sur la v0.67.0 ; le nombre de la
  carte n'a pas été relevé) ; capture faite par l'orchestrateur après la release.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Un contexte affiche le même nombre de tiges dans la liste, dans Statut et dans la boîte de fusion.
