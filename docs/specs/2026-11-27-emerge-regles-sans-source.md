# EMERGE : les trois règles de Marculus sans source (mission 040)

> **Propriétaire :** Pascal Obstétar. **Rédigé par :** agent `acquereur`, mission 040, consultation du 2026-11-27.
> **Décision éclairée :** pour chacune des trois règles du calcul EMERGE de `Cubage.kt` (`emergeVolumes`), Pascal
> décide de la garder, de la borner ou de la retirer. La mission ne corrige pas le code.
> **Statut :** constat. **Aucune des trois règles n'est sourcée.** Une est contredite en partie (règle 3, seuil).
> Pascal n'a pas relu ce document.

## 1. Sources consultées, et ce qui n'a pas pu l'être

| # | Source | Accès le 2026-11-27 | Valeur de la lecture |
|---|---|---|---|
| G1 | `gftools` (pobsteta), `R/TarifsEmerge.R`, fonction `TarEmerge`, miroir `https://rdrr.io/github/pobsteta/gftools/src/R/TarifsEmerge.R` | Lue par WebFetch (un petit modèle résume la page ; je n'ai pu obtenir que des lignes isolées de moins de 125 caractères, pas le source complet). Version : celle du miroir rdrr.io, sans numéro ni commit lisible. | Moyenne. À rejouer sur le dépôt par Pascal (point au sol 1). |
| G2 | `gftools`, `https://rdrr.io/github/pobsteta/gftools/man/TarifsEmerge.html` (aide de `TarEmerge`) et `.../man/ListTarEmerge.html` (table : 93 lignes, 15 colonnes ; a1,b1,c1 = volume total ; a2,b2,c2,d2 = tige bois fort ; a3,b3 = houppier) | Lues par WebFetch (résumé). | Moyenne. |
| G3 | `https://github.com/pobsteta/gftools` et `raw.githubusercontent.com/pobsteta/gftools/master/R/TarifsEmerge.R` | **HTTP 404** (dépôt privé ou renommé, comme déjà noté dans `fiche_emerge.md` S3). | Aucune. |
| D1 | Deleuze et al. 2014, RDVT ONF n° 44, p. 22-32 (HAL hal-01143797) | **Non lue par moi** : `hal.science/hal-01143797/document`, `hal.science/hal-01143797v1/file/...` et `hal.inrae.fr/hal-01143797/document` répondent « Access Denied » (barrière Anubis). Le PDF local cité dans `fiche_emerge.md` (`donnees/nemeton_densite_sources/docs/deleuze2014_hal-01143797.pdf`) est **absent** de `/home/pascal/dev/tarif-nemeton` (dossier exclu de git). | Seconde main : je cite la lecture faite par la session NEMETON le 2026-11-26 dans `/home/pascal/dev/tarif-nemeton/donnees/referentiels/emerge/fiche_emerge.md` § 4, § 5 (lue sur images, 12 pages). |
| N1 | `/home/pascal/dev/tarif-nemeton/code/emerge_reference.py` l. 10, l. 26, l. 49-50 | Lue en entier pour ces lignes. | Forte. |
| M1 | `core/src/main/kotlin/fr/marculus/core/Cubage.kt` l. 157, 169, 170, 175, 191 (version de ce worktree, tête `6776e16`) | Relue. | Forte. |

Les numéros de ligne de la spec `2026-11-27-correction-emerge-volumes.md` (l. 87, 97, 98) sont ceux de l'ancien commit
`41a01f7` ; sur la version à jour ce sont **l. 157, 169, 170**.

## 2. Règle 1 — pas de borne basse sur la part de tige `fct`

**Code (M1 l. 169) :** `var fct = k.d + k.e * logisHdec + k.f * hdn + k.g * (1.0 / c13)`.

- **gftools (G1) :** la même somme, sans borne. Lignes lues : `FCT <- .rowSums(.XFCT, ...)` puis
  `formTige <- formTot * FCT`. Aucun `pmin`, `pmax`, `if` ni `ifelse` sur `FCT` n'a été vu ; seuls `Cmax` et
  `decHoup` utilisent `FCT`. **Marculus fait donc comme gftools : sourcée, au sens « fidèle au code publié ».**
- **Ce que gftools fait en plus et que Marculus ne fait pas :** si aucun arbre n'atteint le diamètre de découpe (`dec`,
  7 cm) et que `perchebrin = FALSE`, `volTige <- NA` avec un `warning()` ; Marculus rend 0 (`decTige = 0`, l. 172).
- **Deleuze 2014 (D1, seconde main) :** la part de tige n'y figure pas. La tige est renvoyée à « un article distinct »
  (p. 23, colonne de droite, d'après `fiche_emerge.md` § 5). Aucun domaine de validité chiffré par essence (§ 4 de la
  fiche) ; plus petit arbre du calage : 2 m de haut, circonférence 6-7 cm (p. 24).
- **Le négatif :** il vient donc des coefficients d, e, f, g de `ListTarEmerge`, calés sur des arbres plus grands ; pas
  d'une règle de Marculus. Hêtre C = 0,628 m : −0,63 à 2 m, −0,08 à 3 m (valeurs du brief, **non recalculées ici**).
- **Statut : sourcée (G1, absence de borne), hors domaine non documenté (D1).**

**Options pour Pascal :**
- A. Garder (fidèle à gftools) ; le `coerceAtLeast(0)` de la tige (l. 173) rend déjà 0.
- B. Borner `fct` à [0 ; 1] et dire « hors domaine » sous un seuil de hauteur ou de diamètre que Pascal fixe (aucune
  valeur publiée à reprendre : la plus petite donnée de calage est 2 m, 6-7 cm).
- C. Refuser le calcul de la tige sous la hauteur où `fct` devient négatif (comme gftools renvoie NA quand la découpe n'est pas atteinte).

## 3. Règle 2 — `if (fct > 0.95) fct = 0.8`

**Code (M1 l. 170).**

- **gftools (G1) :** **introuvable.** Ni `0.95` ni `0.8` comme constante de remplacement de `FCT` dans `TarEmerge` ;
  l'extraction cible ne trouve aucun test de `FCT` contre une borne. Le seul `0.8` possible, `hdec`, n'est pas une
  constante dans gftools (`hdec = NULL` par défaut, aucune ligne à 0.8 relevée). **Contredite ? Non : absente.**
- **Deleuze 2014 (D1) :** part de tige hors de la publication (voir règle 1) ; **introuvable**.
- **Dans Marculus :** le seul autre `0.95` du fichier est `PART_HOUPPIER_MAX` (l. 191), plafond du réglage « part de
  houppier » saisi par l'utilisateur : autre chose, sans lien établi avec la règle.
- **Effet constaté sur le code (lecture, non mesuré) :** fonction discontinue et non monotone : `fct = 0,95` reste
  0,95, `fct = 0,951` devient 0,8 (la tige passe de 95 % à 80 % du total). Hypothèse non vérifiée : garde-fou contre
  `1/fct − 1 ≤ 0` du houppier (l. 175), qui s'annule à `fct = 1`. Je n'ai trouvé aucune trace écrite de l'origine
  (journal, spec, ADR : recherche `0.95` / `fct` dans `docs/` et `pilotage/`, rien d'autre que le brief). L'historique git n'a pas pu être interrogé (pas d'outil shell dans cette mission).
- **Statut : introuvable.** Lu : G1 (fonction `TarEmerge`), G2, D1 (seconde main), `docs/` et `pilotage/` de Marculus.

**Options pour Pascal :**
- A. Retirer la règle (aligner sur gftools) et traiter le houppier seul quand `fct ≥ 1` (houppier = 0).
- B. Remplacer par une borne continue `fct = min(fct, 0.95)` (choix de Marculus, à nommer comme tel, sans source).
- C. Garder tel quel et l'écrire dans la spec comme règle propre de Marculus, valeur provisoire, à mesurer sur des arbres où `fct > 0,95`.

## 4. Règle 3 — `htotM <= 1.4 → 0` sans le dire

**Code (M1 l. 157) :** `if (htotM <= 1.4 || c130cm <= 0.0) return VolumesEmerge(0.0, 0.0, 0.0, 0.0)`.

- **gftools (G1) :** pas de seuil sur la hauteur. Les seuls contrôles sur `htot` : erreur si non numérique ou
  négative ; `warning` si `htot > 50`. Le `1.3` n'y est que la constante de `corr130 <- (1 - 1.3 / htot)`.
  **Contredite :** gftools calcule pour `1,3 < H ≤ 1,4` (et pour `H ≤ 1,3` : division par zéro ou valeur négative,
  non traité).
- **NEMETON (N1 l. 10, 26, 49-50) :** `H ≤ 1,30 m` → `ValueError` (« décision Q2, 27/11/2026, par délégation de
  Pascal »). Choix de NEMETON, pas de la publication.
- **Deleuze 2014 (D1, seconde main) :** ne traite pas H → 1,30 m ; le plus petit arbre imprimé fait 2 m (p. 24,
  `fiche_emerge.md` § 3 et § 4).
- **Origine du 1,3 :** mathématique, `1 − 1,3/H` s'annule à H = 1,3 m (volCyl, l. 164-165). Le 1,4 de Marculus n'a pas
  de source : c'est une marge au-dessus de la singularité. Entre 1,3 et 2 m, le modèle est hors du calage.
- **Statut :** seuil 1,3 : **sourcé (formule)** ; seuil 1,4 : **introuvable** ; comportement « 0 silencieux » :
  **contredit** par NEMETON (erreur) et par gftools (NA pour les valeurs manquantes, pas de 0).

**Options pour Pascal :**
- A. Garder 0 et dire « non calculé » dans l'interface (la carte du bac à sable « Statut dit combien de tiges EMERGE
  n'ont pas de volume » compte alors ces tiges).
- B. Passer à H ≤ 1,30 m comme NEMETON, avec refus déclaré.
- C. Refuser sous la plus petite hauteur du calage (2 m) : plus prudent, mais exclut des semis que gftools calcule.

## 5. Écarts entre sources, notés

1. Règle 2 : ni gftools ni Deleuze ne contiennent la règle ; elle est propre à Marculus.
2. Règle 3 : trois valeurs de seuil selon la source (gftools aucun ; NEMETON 1,30 ; Marculus 1,4) et trois comportements.
3. Marculus rend 0 là où gftools rend `NA` (tige sous la découpe) : écart de comportement, pas de valeur.
4. D1 n'a pu être relue ; tout ce que je dis de la publication vient de la fiche NEMETON du 2026-11-26.

## 6. Trois points au sol (pour Pascal)

1. **Hêtre C = 0,628 m à 2 m et 3 m** : lire dans l'application la tige affichée (attendue 0 selon le brief) et, dans R,
   `gftools::ListTarEmerge` ligne hêtre : calculer `d2 + e·ln(4) + f·sqrt(c)/H + g/c` pour vérifier −0,63 et −0,08.
2. **Un arbre où `fct` dépasse 0,95** : chercher dans R (`ListTarEmerge`, toutes essences, grille H × C) un cas ; mesurer
   la tige avec et sans la règle. Sans cas réel, la règle ne sert à rien.
3. **Un arbre de 1,35 m** (entre 1,3 et 1,4) : gftools donne-t-il un volume, et quel ordre de grandeur ? C'est ce qui
   dira si le seuil 1,4 cache un volume aberrant.

## 7. Liste de contrôle

- 1. Sources et emplacements : cochée. Version de gftools non lisible (miroir rdrr.io, sans commit) : dit en § 1.
- 2. Décomptes : les trois règles traitées ; ListTarEmerge = 93 lignes, 15 colonnes (rapport de G2, non recompté en R).
- 3. Termes : bornes recopiées de `Cubage.kt` (relu), pas de mémoire.
- 4. Statut : constat, rien tranché.
- **Non cochés :** (a) le source complet de `TarifsEmerge.R` n'a pas été obtenu verbatim (résumés seulement) ;
  (b) Deleuze 2014 non relue directement (accès refusé) ; (c) l'origine de la règle 2 dans l'historique git non
  interrogée (pas d'outil shell). Dépôt gftools privé : Pascal peut lever (a) et (c) en deux minutes.
