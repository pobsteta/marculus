# Mission 037 — Les essences de Marculus et leurs codes IFN pour NEMETON

## Mission

**En une phrase (quoi, et pourquoi) :** proposer à Pascal, dans une table de revue, le code IFN (nomenclature
ESS 2022) de chaque nom d'essence que Marculus connaît, avec le repli prévu quand le code n'est pas dans le paquet
NEMETON (genre, puis classe « autres »). Le paquet NEMETON prend un code IFN comme clé de calcul, alors que Marculus
travaille sur des noms (« Chêne », « Hêtre »…). Décision de Pascal du 2026-11-27 : intégrer NEMETON dès le paquet
provisoire (option b, journal du 2026-11-27).

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Ce qui existe (preuves)

- **La clé du paquet :** `code_ifn` = code ESS de l'IFN 2022, un code par ligne
  (`~/dev/tarif-nemeton/livrable/datapackage.json`, clé `cle_essence`).
- **Les codes couverts par le paquet 0.2.2-provisoire :** 2 et 3 (Quercus_spec), 9 (hêtre), 11 (charme), 12 (bouleau),
  15 (grand érable), 17 (frêne), 29 et 49 (autres feuillus), 52 (pin sylvestre), 61 (sapin), 62 (épicéa), 63 (mélèze),
  64 (douglas) ; non couverts : 68 et 75 (autres conifères) (`datapackage.json`, `essences_non_couvertes` et
  `limites_connues`).
- **Rattachements à trancher, signalés par le paquet :** code 15 (A. pseudoplatanus et platanoides) et code 17
  (F. excelsior, oxyphylla et ornus) ; les codes rattachés au tarif par le seul genre ne sont pas dans le paquet et
  « le repli de la spec § 4 est à appliquer par Marculus, sur décision de Pascal (D5) » (`limites_connues`).
- **Le référentiel IFN :** `~/dev/tarif-nemeton/donnees/referentiels/ifn/essences_ifn_ess.csv`.
- **Les noms d'essence de Marculus :** essences par défaut et 39 noms courts rattachés à la main (38 écrit à la création de la fiche ; 39 comptés par l'acquereur et le relecteur, journal du 2026-11-27)
  (`core/src/main/kotlin/fr/marculus/core/Cubage.kt` l. 52, `RATTACHEMENT_ESSENCES`) ; leur table de revue EMERGE
  `docs/specs/2026-11-27-rattachement-essences.md` (même forme attendue ici) ; codes ONF à 3 caractères
  (`EmergeCoefs.kt`, table des codes).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | même forme que la table de revue EMERGE du 2026-11-27 |
| A-t-il les moyens ? | oui | référentiel IFN et paquet en local, lecture seule |

**Temps de la main :** donner. Un référentiel : l'agent propose, Pascal valide chaque ligne avant tout code.

**Degré de la main :** le chef tranche (Pascal valide la table).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `acquereur`
- **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** écrire du code ; décider seul d'un rattachement ambigu (il le signale).

## Harnais

- **Peut (périmètre) :** `docs/specs/2026-11-27-essences-nemeton.md` (nouveau), une ligne au journal par rendu.
- **Câblé (ne peut pas) :** pas de `main` ; journal en ajout seul.
- **Ne doit pas (écrit) :** inventer un code IFN ; toucher au dépôt `tarif-nemeton` ; modifier `Cubage.kt`.
- **Sources autorisées :** les fichiers cités ci-dessus ; la nomenclature IFN publiée (inventaire-forestier.ign.fr) pour
  recouper. Comparer au moins deux sources pour chaque ligne douteuse.

## Journal

- **Ce que l'agent inscrit :** une ligne par rendu : nombre de noms traités, rattachements sûrs, ambigus, sans code.

## Valeur provisoire (chiffre, réglage)

- **Valeur provisoire :** la règle de repli (genre, puis classe « autres » 29/49 ou aucune pour les conifères), tant
  que la décision D5 de NEMETON n'est pas prise.
- **Mesurée par :** Pascal, à la revue de la table.
- **Gel :** avant le branchement de NEMETON dans le modèle de données (mission à venir).

## Dépend de / rouge attendu jusqu'à

- aucune.

## Tests

- **Comment saura-t-on que c'est juste :** chaque ligne de la table donne le nom saisi, le code IFN, son libellé IFN,
  la ligne du paquet utilisée (ou « repli : … », ou « non couvert ») et la source ; aucune ligne sans source.

## Terrain

- **Échantillon que Pascal vérifiera :** les lignes « Chêne », « Érable », « Frêne », « Pin » et « Autres résineux ».

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Pascal valide la table ligne à ligne ; le moteur (mission 039) et son branchement s'en servent sans autre choix.
