# Mission 041 — Tests d'acceptation du plafond continu de la part de tige EMERGE

## Mission

**En une phrase (quoi, et pourquoi) :** écrire, sans lire le code qui l'implémente, les tests qui prouvent que le
cubage EMERGE plafonne la part de tige à `fct = min(fct, 0,95)` au lieu de la ramener à 0,8 au-delà de 0,95, et
qu'il garde les règles 1 et 3 telles quelles, pour que la décision de Pascal du 2026-11-27 (règle 2 : option B) ne
s'auto-évalue pas.

**Redite par l'agent :** _reconstituée le 2026-11-27 d'après le journal (lignes 369-373, 382, 385), l'agent n'ayant pas consigné sa redite à l'époque._ Écrire, sans lire le code, les tests du plafond `min(fct, 0,95)` et de la non-régression des règles 1 et 3 : `EmergePlafondFctTest`, 14 cas, 8 rouges avant le code (règle 2 seule). Livrée avec la 042 par la PR #90 (795079e, v2026.11.6). Redite de l'agent d'origine : non retrouvée.

## Ce qui existe (preuves)

- **Les trois règles et leurs options :** `docs/specs/2026-11-27-emerge-regles-sans-source.md` § 2-4 (mission 040,
  PR #86, avec l'erratum du relecteur en tête de la PR : le point au sol 2 se fait sur un **hêtre C 25 cm, H 20 m**,
  pas C 30 cm H 15 m, qui a `fct` = 0,893).
- **Les décisions de Pascal du 2026-11-27** (`pilotage/journal.md`, lignes de ce jour) : règle 1 → **A** (garder :
  `fct` sans borne basse, tige et houppier ramenés à 0) ; règle 2 → **B** (plafond continu `min(fct, 0,95)`, choix
  de Marculus nommé) ; règle 3 → **A** (garder `H ≤ 1,4 m → 0`, sans message à l'écran pour ce cycle).
- **Les formules EMERGE** (part de tige, houppier, découpe) : `docs/specs/2026-11-27-correction-emerge-volumes.md`
  et Deleuze et al. 2014 (`docs/ONF_RDVT_44.pdf`, p. 22-32) ; les coefficients : `EmergeCoefs.kt` (généré, données
  seules).
- **Interface publique inchangée :** `Cubage.volumesEmerge(essence, c130cm, htotM)` et
  `Cubage.volumeEmergeTige(essence, c130cm, htotM)` (`core/src/main/kotlin/fr/marculus/core/Cubage.kt` l. 149-154 :
  signatures seulement, à ne pas lire plus loin).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | formules publiées et règle décidée, écrites ci-dessus |
| A-t-il les moyens ? | oui | R et la copie de gftools (`/media/pascal/data2/digital_ocean/gftools`) pour un calcul indépendant ; pas de donnée mesurée pour juger 0,95 (valeur provisoire, ci-dessous) |

**Temps de la main :** donner. **Degré de la main :** le chef tranche.
**Net ou lignes du journal ?** Sans objet : volume d'une tige, calculé par `Cubage`, sans décompte de tiges.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `testeur` · **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** changer le code de `Cubage` ; trancher une autre règle ; changer un écran.

## Harnais

- **Peut (périmètre) :** un fichier de test **nouveau**, `core/src/test/kotlin/fr/marculus/core/EmergePlafondFctTest.kt`.
- **Câblé (ne peut pas) :** pas de `main` ; pas de « terminé » sans le rouge annoncé ; journal en ajout seul.
- **Ne doit pas (écrit) :** lire le corps de `emergeVolumes` dans `Cubage.kt` (au-delà des signatures) ; modifier
  `CorrectionEmergeTest.kt` ou un autre test existant ; inventer un coefficient.
- **Sources autorisées :** cette fiche, les deux specs EMERGE du 2026-11-27, `docs/ONF_RDVT_44.pdf`, `EmergeCoefs.kt`,
  la copie de gftools (lecture et exécution R ; **copie de travail non commitée**, à citer comme telle).

## Journal

- **Ce que l'agent inscrit :** une ligne par rendu : cas écrits, valeurs attendues et comment elles sont calculées
  (formule recopiée, R), rouge attendu.

## Valeur provisoire (chiffre, réglage)

- **Valeur provisoire :** le plafond **0,95** de la part de tige EMERGE (choix de Marculus, aucune source publiée ;
  décision de Pascal du 2026-11-27).
- **Mesurée par :** un cubage réel de tiges minces (hêtre C 10-45 cm, H 6-45 m) ; aucune donnée dans les moyens
  du projet, ni date.
- **Gel :** pas de gel : marquée « provisoire » dans la spec par la mission 042.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune ; branche `test/041-plafond-fct` partie de `main`.
- **Ordre de fusion :** groupée avec 042 (une PR).
- **PR en brouillon jusqu'à :** la fin de 042.
- **CI rouge attendue :** les cas de la règle 2 de `EmergePlafondFctTest` (hêtre C 25 / H 20 et continuité) sont
  rouges tant que 042 n'a pas remplacé la règle 0,8 ; les cas des règles 1 et 3 et de non-régression sont verts dès
  la 041.

## Tests

- **Comment saura-t-on que c'est juste :**
  1. **Hêtre C 25 cm, H 20 m** (`fct` brut > 0,95) : tige et houppier égaux au calcul avec `fct = 0,95`, valeurs
     écrites dans le test avec leur calcul (et l'écart avec l'ancien 0,8, noté dans le rapport) ;
  2. **Continuité :** pour une essence et une hauteur où `fct` brut franchit 0,95, le volume de tige varie peu entre
     les deux côtés du seuil (aucun saut de l'ordre de 0,8 / 0,95) ;
  3. **Houppier jamais négatif ni infini** quand `fct` brut ≥ 1 (plafonné : `1/0,95 − 1 > 0`) ;
  4. **Règle 1 gardée :** hêtre C 62,8 cm à H 2 m et 3 m → tige 0 et houppier 0 ;
  5. **Règle 3 gardée :** H = 1,40 m et 1,35 m → tous volumes 0 ; H = 1,41 m → pas d'exception ;
  6. **Non-régression :** un arbre courant où `fct` ≤ 0,95 (par exemple hêtre C 150 cm, H 30 m) garde ses volumes
     d'aujourd'hui.

## Terrain

- **Échantillon que Pascal vérifiera :** voir la mission 042 (hêtre C 25 cm, H 20 m, avant et après, sur le téléphone).

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal
  (changement de règle métier : jamais lâché).

## Comment saura-t-on que ça marche ?

Sans 042 : rouge sur les cas 1 et 2 seulement ; avec 042 : tout vert.
