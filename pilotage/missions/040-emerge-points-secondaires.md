# Mission 040 — EMERGE : sources des trois règles non documentées

## Mission

**En une phrase (quoi, et pourquoi) :** chercher dans `gftools` (`ListTarEmerge` et le code R qui l'applique) et dans
Deleuze et al. 2014 la source des trois règles du calcul EMERGE de Marculus que le brief du 2026-11-27 dit sans
source, pour que Pascal décide de les garder, de les borner ou de les retirer. « Oui c'est une prochaine mission »
(Pascal, 2026-11-27).

**Redite par l'agent :** _reconstituée le 2026-11-27 d'après le journal (lignes 329, 334-336, 340, 344-346, 352), l'agent n'ayant pas consigné sa redite à l'époque._ Chercher la source des trois règles EMERGE sans source (fct sans borne basse, fct > 0,95 → 0,8, H ≤ 1,4 m → 0). Livrée par la PR #86 (b4695ac) après trois allers-retours et un erratum du relecteur en tête, plutôt qu'un quatrième : règle 2 sans source publiée (copie de travail non commitée de gftools), Pascal « ne sait pas répondre » (ligne 352). Redite de l'agent d'origine : non retrouvée.

## Ce qui existe (preuves)

`docs/specs/2026-11-27-correction-emerge-volumes.md`, « Points secondaires » :

1. **Part de tige `fct` négative aux petites hauteurs :** hêtre C = 0,628 m : `fct` = −0,63 à 2 m, −0,08 à 3 m ; pas de
   borne basse (`Cubage.kt` l. 169, `emergeVolumes`).
2. **`if (fct > 0.95) fct = 0.8`** sans source (`Cubage.kt` l. 170).
3. **`H ≤ 1,4 m → 0` sans le dire** (`Cubage.kt` l. 157), alors que la référence NEMETON refuse `H ≤ 1,30 m`.

Le point 3 touche la carte du bac à sable « Statut dit combien de tiges EMERGE n'ont pas de volume » (décision de
Pascal du 2026-11-27).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | trois règles, trois lignes de code |
| A-t-il les moyens ? | à vérifier | `gftools` n'est pas en local : accès réseau (CRAN, GitHub) ; sinon l'agent le dit et s'arrête |

**Temps de la main :** donner. **Degré de la main :** le chef tranche (Pascal, sur le constat).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `acquereur` · **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** corriger le code (une mission testeur + dev suivra la décision de Pascal).

## Harnais

- **Peut (périmètre) :** `docs/specs/2026-11-27-emerge-regles-sans-source.md` (nouveau), une ligne au journal.
- **Câblé (ne peut pas) :** pas de `main` ; journal en ajout seul.
- **Ne doit pas (écrit) :** modifier `Cubage.kt` ou `EmergeCoefs.kt` ; conclure sans source citée.
- **Sources autorisées :** `gftools` (code R publié), Deleuze et al. 2014 (HAL hal-01143797),
  `~/dev/tarif-nemeton/code/emerge_reference.py`, le brief ci-dessus. Deux sources par règle quand elles existent.

## Journal

- **Ce que l'agent inscrit :** une ligne par rendu : pour chaque règle, « sourcée (où) », « contredite (où) » ou
  « introuvable ».

## Valeur provisoire (chiffre, réglage)

- **Valeur provisoire :** les trois règles restent telles quelles (0,95 → 0,8 ; seuil 1,4 m) jusqu'à la décision de Pascal.
- **Mesurée par :** cette mission (constat), puis Pascal.
- **Gel :** avant la mission de correction.

## Dépend de / rouge attendu jusqu'à

- aucune.

## Tests

- **Comment saura-t-on que c'est juste :** chaque règle a sa source citée (fichier, fonction, ligne ou page) ou la
  mention « introuvable » avec la liste de ce qui a été lu ; pour chacune, une option proposée à Pascal.

## Terrain

- **Échantillon que Pascal vérifiera :** le hêtre C = 0,628 m à 2 m et 3 m, avant et après chaque option.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Pascal peut trancher les trois règles sur pièces, et la mission de correction se rédige sans question ouverte.
