# EMERGE : les trois règles de Marculus sans source (mission 040, version 3)

> **Propriétaire :** Pascal Obstétar. **Rédigé par :** agent `acquereur`, mission 040, consultation du 2026-11-27,
> version 3 (aller-retour 3/3). Version 1 : miroir rdrr.io seul, faux sur la règle 2. Version 2 : copie locale de
> gftools lue, mais présentée à tort comme « code publié ». Cette version corrige : **la copie locale n'est pas publiée**.
> **Décision éclairée :** pour chacune des trois règles du calcul EMERGE de `Cubage.kt` (`emergeVolumes`), Pascal
> décide de la garder, de la borner ou de la retirer. La mission ne corrige pas le code.
> **Statut :** constat. Règle 1 : sourcée dans gftools publié (par absence de borne). **Règle 2 : présente seulement dans
> une copie de travail non commitée de gftools** (pas dans le code publié) ; origine inconnue de l'acquéreur, question à
> Pascal. Règle 3 : seuil 1,4 sans source ; origine dans Marculus datée (commit 57fb18b). Pascal n'a pas relu ce document.

## 1. Sources consultées

| # | Source | Accès le 2026-11-27 | Valeur |
|---|---|---|---|
| G1 | **gftools, copie de travail locale** `/media/pascal/data2/digital_ocean/gftools/R/TarifsEmerge.R`, fonction `TarEmerge` (l. 139-374). `DESCRIPTION` : Version 1.2.8.9000, Date 2021-12-06 (l. 4-5). **Fichier modifié et non commité** : `git status` montre `M R/TarifsEmerge.R` (constat du relecteur, commande lancée par lui ; je n'ai pas de shell). | Lue directement (Read), l. 130-380. | Forte pour ce que dit le fichier ; **non publiée**. |
| G1b | **gftools commité**, `git show HEAD:R/TarifsEmerge.R`, commit 187ad39 du 2019-06-20 (constat du relecteur, non relu par moi). Il ne contient ni `FCT > 0.95` ni `conditionel`. | Relayé par le relecteur. | Forte (relecteur). |
| G2 | Miroir `https://rdrr.io/github/pobsteta/gftools/src/R/TarifsEmerge.R` et `.../man/ListTarEmerge.html` | WebFetch (résumés). Cohérent avec G1b : pas de règle 0,95. Sa taille de table (« 93 lignes, 15 colonnes ») est celle de l'ancienne version : **à ne pas reprendre**. | Faible, ancien. |
| G3 | `https://github.com/pobsteta/gftools` | HTTP 404 (privé ou supprimé ; confirmé par `gh` d'après l'orchestrateur). | Aucune. |
| G4 | `/media/pascal/data2/digital_ocean/gftools/data/ListTarEmerge.rda` | Chargé sous R par le relecteur : **226 lignes × 92 colonnes**. | Forte (relecteur). |
| R1 | Exécutions R du relecteur (source de `TarifsEmerge.R` + chargement de G4) ; scripts `…/scratchpad/run.R` et `grid.R` (lus par moi : appels `TarEmerge(c130 = 62.8, htot = h, ess = "HET", typevol = …)` ; grille h 2:45 × c 10:400 pas 5 sur les lignes à `code3_onf` non NA, `fct` calculée avec `hdec = 0,8·h`). Les **résultats** ci-dessous sont ceux rapportés par le relecteur ; je ne les ai pas rejoués. | Relayé. | Forte (relecteur). |
| D1 | Deleuze et al. 2014, RDVT ONF n° 44, p. 22-32 (HAL hal-01143797) | **Non lue par moi** : trois URL HAL refusent l'accès (Anubis) ; PDF NEMETON absent. Je cite la lecture NEMETON du 2026-11-26, `/home/pascal/dev/tarif-nemeton/donnees/referentiels/emerge/fiche_emerge.md` § 3-5. | Seconde main. |
| N1 | `/home/pascal/dev/tarif-nemeton/code/emerge_reference.py` l. 10, 26, 49-50 | Lue. | Forte. |
| N2 | Port R `/home/pascal/dev/nemeton/R/ifn_tarifs.R` l. 129-143 (`htot > 1.4` l. 131 ; `fct[fct > 0.95] <- 0.8` l. 140) | Lue. Son l. 184 dit « as Marculus does » : **port dérivé de Marculus, pas une source**. | Dérivée. |
| M1 | `core/src/main/kotlin/fr/marculus/core/Cubage.kt` l. 157, 169, 170, 172-175, 186, 191 (tête `6776e16`) | Relue. | Forte. |
| M2 | Commit Marculus **57fb18b du 2026-06-23** (« EMERGE = vrai volume bois fort tige ») : introduit le seuil 1,4 et la règle 0,95 ensemble, sans source citée (indiqué par l'orchestrateur ; je n'ai pas relu le commit). | Relayé. | Moyenne. |

Les l. 87, 97, 98 de la spec `2026-11-27-correction-emerge-volumes.md` sont celles de l'ancien commit `41a01f7` ;
sur la version à jour ce sont **l. 157, 169, 170**.

## 2. Règle 1 : pas de borne basse sur la part de tige `fct`

**Marculus (M1 l. 169) :** `fct = d + e·logisHdec + f·hdn + g/c13`, sans borne basse.

**gftools (G1 l. 318-322) :** même somme, `FCT <- .rowSums(.XFCT, ...)`, sans borne basse. Cette absence existe aussi
dans la version commitée G1b (elle n'a pas de règle sur `FCT` du tout). **Sourcée : Marculus reprend gftools.**

**Mesure exécutée en R (relecteur, R1), hêtre `HET`, C = 62,8 cm :**

| H (m) | `fct` brut | tige (m³) | houppier |
|---|---|---|---|
| 3 | −0,084 | −0,0173 | NA |
| 2 | −0,628 | −0,257 | NA |
| 1,35 | −1,41 | −40,45 | non rapporté |
| 1,30 | NA | — | — |

gftools rend donc une tige **négative** (et un houppier `NA`) quand `FCT < 0`, et une valeur absurde (−40 m³) à 1,35 m.
Cela confirme les −0,63 et −0,08 du brief (arrondis de −0,628 et −0,084). Mon hypothèse de la version 2 (houppier
`NaN` quand `1/FCT − 1 < 0`) est confirmée par l'exécution.

Marculus diffère : tige bornée à 0 (l. 173, `coerceAtLeast(0.0)`), houppier 0 si `unSurFct ≤ 0` ou non fini
(l. 177, 186). Il est **plus prudent que gftools**, pas fidèle.

**Quand la découpe n'est pas atteinte (G1 l. 342-361) :** pas de `NA`. `decTige <- ifelse(c13 > cDec, 1 − (cDec·corr130/c13)^3, 0)`
(l. 342), identique à Marculus (l. 172) ; gftools émet seulement un `warning` (l. 350-359). Le 0 de Marculus coïncide
avec gftools ; seul l'avertissement manque.

**Deleuze 2014 (D1, seconde main) :** pas de part de tige (« article distinct », p. 23) ; aucun domaine chiffré ; plus
petit arbre du calage : 2 m, circonférence 6-7 cm (p. 24). **Statut : sourcée (G1, G1b), hors domaine non documenté (D1).**

**Options :**
- A. Garder tel quel (négatif dans `fct`, tige et houppier ramenés à 0 par les gardes de Marculus).
- B. Borner `fct` à [0 ; 1] et déclarer « hors domaine » sous un seuil que Pascal fixe (aucune valeur publiée : calage
  à partir de 2 m, 6-7 cm).
- C. Refuser le calcul de la tige là où `fct ≤ 0` et le dire (« non calculé »).

## 3. Règle 2 : `if (fct > 0.95) fct = 0.8`

**Où elle existe.** Dans la copie de travail **non commitée** de gftools (G1 l. 324-332) :
commentaire « ajout ici de condi pour verifier que 0<FCT<1 (faut il tj un else ) ... je remonte la contrainte pour les
pins… bon faudra qu'on fasse mieux », puis
`if (any(FCT > 0.95)) { if (conditionel) { FCT[FCT > 0.95] <- 0.8 } else { FCT[FCT > 0.95] <- NA } }`,
`conditionel = TRUE` par défaut (l. 148).

**Où elle n'existe pas.** Dans gftools commité (G1b, 187ad39, 2019-06-20) et dans le miroir rdrr.io (G2) : ni
`FCT > 0.95` ni `conditionel` (relecteur). Dans Deleuze 2014 (D1) : non plus (pas de part de tige). **Aucune source
publiée.** La règle ne se trouve que dans une copie de travail de gftools (dépôt `pobsteta/gftools`, de Pascal, aujourd'hui
en 404) et dans Marculus, où elle est arrivée avec le commit 57fb18b (2026-06-23), sans source citée (M2).

- Le commentaire de l'auteur dit la règle provisoire et dirigée vers les pins ; Marculus l'applique à toutes les essences.
- Autre `0.95` dans Marculus : `PART_HOUPPIER_MAX` (l. 191) : un autre objet.
- **Effet** : fonction discontinue (0,95 reste 0,95 ; 0,951 devient 0,8) ; elle évite `1/FCT − 1 ≤ 0` du houppier.

**Mesure exécutée en R (relecteur, R1), grille H 2-45 m × C 10-400 cm (pas 5 cm), toutes essences à code ONF :**
`fct > 0,95` dans **59 435 cas sur 597 872** (9,9 %, calcul de l'acquéreur), sur des **tiges minces** ; pour le hêtre, C de
10 à 45 cm et H de 6 à 45 m. La règle ne touche donc pas un cas rare : elle joue sur des arbres courants au martelage
(jeunes et moyens bois).

**Statut : sans source publiée ; présente dans une copie de travail non commitée de gftools ; volontairement provisoire
selon son auteur ; large effet constaté.**

**Question à Pascal (une seule, non tranchée) : sais-tu d'où vient cette règle et le commentaire « faudra qu'on fasse
mieux » (une copie de travail de gftools modifiée après le commit 187ad39) : est-ce toi qui l'as ajoutée, d'après quelle
mesure ?**

**Options (sans choix de ma part) :**
- A. Garder, en l'écrivant dans la spec comme règle propre de Marculus, valeur provisoire, avec la mesure ci-dessus.
- B. Remplacer par un plafond continu `min(fct, 0.95)` (choix de Marculus, à nommer).
- C. Retirer, houppier nul quand `fct ≥ 1` (s'écarte de la copie de travail de gftools).
- D. Mesurer d'abord sur des tiges minces réelles (hêtre C 10-45 cm, H 6-45 m) le volume de tige que donne `fct = 0,8`
  contre un volume mesuré ou contre `fct` brut plafonné : le cas existe en nombre (point au sol 2) ; la mesure dira si
  0,8 est raisonnable.

## 4. Règle 3 : `htotM <= 1.4 → 0` sans le dire

**Marculus (M1 l. 157) :** `if (htotM <= 1.4 || c130cm <= 0.0) return VolumesEmerge(0.0, 0.0, 0.0, 0.0)`.

- **Origine dans Marculus :** commit 57fb18b du 2026-06-23, avec la règle 2, sans source citée (M2).
- **gftools (G1 l. 178-188) :** aucun seuil bas de hauteur : erreur si `htot < 0`, avertissement si `htot > 50`. Le
  `1.3` n'y est que dans `corr130 <- (1 − 1.3/htot)` (l. 303). Recherche de `1.4` dans tout le dépôt gftools : seulement
  des coefficients de `TarifsONF3.R`, aucun seuil. Mesure R (relecteur) : à 1,35 m, tige −40,45 m³ ; à 1,30 m, `NA`.
- **Port nemeton (N2 l. 131) :** `htot > 1,4`, copie de Marculus (N2 l. 184) : pas une source.
- **NEMETON (N1 l. 10, 49-50) :** `H ≤ 1,30` donne une erreur ; décision Q2 du 2026-11-27 « par délégation de Pascal ».
- **Deleuze 2014 (D1) :** ne traite pas H → 1,30 m ; plus petit arbre imprimé 2 m (p. 24).
- **Statut :** 1,3 sourcé par la formule ; **1,4 sans source** : le 1,4 se justifie, d'après la mesure ci-dessus, par
  l'explosion du modèle juste au-dessus de 1,3 m (−40 m³ à 1,35 m), mais c'est une lecture de l'acquéreur, pas une
  source. Le « 0 silencieux » est un choix de Marculus.

**Options :**
- A. Garder 1,4 et 0, en disant « non calculé » à l'écran (la carte du bac à sable « Statut dit combien de tiges
  EMERGE n'ont pas de volume » les compte).
- B. Passer à `H ≤ 1,30` comme NEMETON, avec refus déclaré ; la mesure de gftools à 1,35 m dit que le haut de la plage
  1,3-1,4 est de toute façon inutilisable.
- C. Refuser sous 2 m (plus petite hauteur du calage).

## 5. Écarts entre sources, notés

1. Règle 2 : copie de travail gftools (oui), gftools commité 187ad39 (non), miroir rdrr.io (non), Deleuze (non) : la règle
   n'a pas de source publiée. Les versions 1 et 2 de ce document se trompaient chacune dans un sens (« introuvable »
   puis « code publié »).
2. Règle 1 : Marculus borne tige et houppier ; gftools rend une tige négative et un houppier `NA` (mesuré).
3. Règle 3 : trois comportements (gftools sans seuil, NEMETON erreur à 1,30, Marculus 0 à 1,4).
4. Date de gftools : `DESCRIPTION` 2021-12-06 contre dernier commit 2019-06-20 : cohérent avec une copie de travail
   modifiée après le dernier commit.
5. Taille de `ListTarEmerge` : 93 × 15 (ancien miroir) contre 226 × 92 (fichier local, mesuré) : **écart de version**,
   la valeur à retenir est 226 × 92 (cohérent avec les 226 essences de `CLAUDE.md`).
6. D1 non relue ; tout ce qui touche la publication vient de la fiche NEMETON du 2026-11-26.

## 6. Trois points au sol (pour Pascal)

Les deux premiers ont été **exécutés par le relecteur sous R** ; Pascal peut rejouer avec `run.R` et `grid.R`.

1. **Hêtre C = 0,628 m à 2 m et 3 m :** `fct` −0,628 et −0,084 ; tige −0,257 et −0,0173 m³ ; houppier `NA`. Marculus
   rend 0. À voir dans l'application avant et après chaque option.
2. **Tiges minces avec `fct > 0,95` :** 59 435 cas sur 597 872 de la grille ; hêtre C 10-45 cm, H 6-45 m. Cuber dans
   Marculus un hêtre de 30 cm de circonférence et 15 m : la tige affichée suppose `fct = 0,8`. Est-ce plausible à l'œil ?
3. **Un arbre de 1,35 m :** gftools donne −40,45 m³ (tige) : le seuil 1,4 de Marculus masque une valeur aberrante.
   Vérifier dans l'application qu'un arbre de 1,35 m est bien « sans volume ».

## 7. Liste de contrôle

- 1. Sources et emplacements : cochée (fichiers, lignes, version, statut git ; les faits git et R sont ceux du relecteur,
  cités comme tels).
- 2. Décomptes : 226 × 92 (relecteur, R) ; 59 435 / 597 872 (relecteur, R) ; pourcentage 9,9 % calculé par moi
  (59 435 / 597 872 = 0,0994). Trois règles traitées. Le décompte « 93 × 15 » est retiré.
- 3. Termes : lignes recopiées de G1 et M1 relus ; les mesures R sont rapportées, non rejouées par moi.
- 4. Statut : constat ; la règle 2 est dite « sans source publiée », pas « sourcée » ; une question à Pascal, non tranchée.
- **Non cochés :** (a) gftools et `grid.R` non exécutés par moi (pas de R ni de shell) ; (b) Deleuze 2014 non relue
  directement ; (c) `git status`, `git show HEAD:…` et le commit 57fb18b relus par le relecteur ou l'orchestrateur, pas
  par moi ; (d) origine de la règle 2 dans la copie de travail : **ouverte, remonte à Pascal**.

## 8. Décisions de Pascal (2026-11-27)

Source : `pilotage/journal.md`, lignes « 2026-11-27 · Pascal · EMERGE règle 1 / 2 / 3 ».

| Règle | Décision | Conséquence |
|---|---|---|
| 1. `fct` sans borne basse | **A** : garder ; tige et houppier ramenés à 0, écrit comme choix de Marculus | aucun changement de code |
| 2. `fct > 0,95` → 0,8 | **B** : plafond continu `min(fct, 0,95)`, choix de Marculus, **0,95 provisoire** | mission 042 (`Cubage.kt`) |
| 3. H ≤ 1,4 m → 0 | **A** : garder ; « non calculé » à l'écran au cycle 11, avec « hauteur impossible » de NEMETON | aucun changement de code |

Les trois règles sont écrites dans la spec v1 § 5 comme choix de Marculus.
