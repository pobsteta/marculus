# EMERGE : les trois règles de Marculus sans source (mission 040, version 2)

> **Propriétaire :** Pascal Obstétar. **Rédigé par :** agent `acquereur`, mission 040, consultation du 2026-11-27,
> version 2 (aller-retour 2/3 : la copie locale de gftools a été lue ; la version 1 reposait sur un miroir rdrr.io
> incomplet et se trompait sur la règle 2).
> **Décision éclairée :** pour chacune des trois règles du calcul EMERGE de `Cubage.kt` (`emergeVolumes`), Pascal
> décide de la garder, de la borner ou de la retirer. La mission ne corrige pas le code.
> **Statut :** constat. Règles 1 et 2 **sourcées dans gftools** (code publié, avec l'aveu de son auteur que la
> règle 2 est provisoire). Règle 3 : seuil 1,4 **introuvable** à la source. Pascal n'a pas relu ce document.

## 1. Sources consultées

| # | Source | Accès le 2026-11-27 | Valeur |
|---|---|---|---|
| G1 | **gftools**, copie locale `/media/pascal/data2/digital_ocean/gftools/R/TarifsEmerge.R`, fonction `TarEmerge` (l. 139-374). `DESCRIPTION` : Version 1.2.8.9000, Date 2021-12-06 (l. 4-5). L'orchestrateur donne le dernier commit 187ad39 du 20/06/2019 : je n'ai pas pu relire le commit (pas d'outil shell), écart de date noté. | Lue directement (Read), l. 130-380. | Forte. |
| G2 | gftools, miroir `https://rdrr.io/github/pobsteta/gftools/src/R/TarifsEmerge.R` et `.../man/ListTarEmerge.html` (table 93 lignes, 15 colonnes) | WebFetch (résumés). **Version plus ancienne** : elle ne montrait aucune règle `FCT > 0.95`. Écartée comme preuve ; je ne la retiens que pour la taille de la table. | Faible, écart de version. |
| G3 | `https://github.com/pobsteta/gftools` | HTTP 404 (privé ou supprimé). | Aucune. |
| D1 | Deleuze et al. 2014, RDVT ONF n° 44, p. 22-32 (HAL hal-01143797) | **Non lue par moi** : trois URL HAL refusent l'accès (Anubis) ; le PDF local de NEMETON est absent. Je cite la lecture NEMETON du 2026-11-26, `/home/pascal/dev/tarif-nemeton/donnees/referentiels/emerge/fiche_emerge.md` § 3-5. | Seconde main. |
| N1 | `/home/pascal/dev/tarif-nemeton/code/emerge_reference.py` l. 10, 26, 49-50 | Lue. | Forte. |
| N2 | Port R `/home/pascal/dev/nemeton/R/ifn_tarifs.R` l. 129-143 (`htot > 1.4` l. 131 ; `fct[fct > 0.95] <- 0.8` l. 140) | Lue. Son commentaire l. 184 dit « as Marculus does » : **c'est un port dérivé de Marculus, pas une source indépendante**. | Dérivée. |
| M1 | `core/src/main/kotlin/fr/marculus/core/Cubage.kt` l. 157, 169, 170, 172-175, 186, 191 (tête `6776e16`) | Relue. | Forte. |

Les l. 87, 97, 98 de la spec `2026-11-27-correction-emerge-volumes.md` sont celles de l'ancien commit `41a01f7` ;
sur la version à jour ce sont **l. 157, 169, 170**.

## 2. Règle 1 : pas de borne basse sur la part de tige `fct`

**Marculus (M1 l. 169) :** `fct = d + e·logisHdec + f·hdn + g/c13`, sans borne basse.

**gftools (G1 l. 318-322) :** même somme, `FCT <- .rowSums(.XFCT, ...)`, sans borne basse ; le seul contrôle porte sur
le haut (règle 2). **Sourcée : Marculus reprend gftools.** Hêtre C = 0,628 m : −0,63 à 2 m, −0,08 à 3 m (valeurs du
brief, non recalculées ici).

**Ce que gftools fait quand `FCT ≤ 0` (l. 333-361) :**
- `formTige <- formTot * FCT` (l. 333) : négatif. `volTige` (l. 344/347/349) n'est **pas bornée à 0** : un volume de
  tige négatif est rendu.
- Houppier : `Cmax` (l. 336) contient `(1/FCT − 1)` élevé à 1/3 ; avec `FCT ≤ 0`, `1/FCT − 1` est négatif pour
  `FCT < 0` (ou infini à 0), la puissance 1/3 d'un négatif donne `NaN` en R, donc `volHoup` est `NA`/`NaN` (déduction à
  la lecture, **non exécutée en R**) et le total `volTige + volHoup` aussi.
- Marculus diffère : tige bornée à 0 (l. 173, `coerceAtLeast(0.0)`), houppier 0 si `unSurFct ≤ 0` ou non fini
  (l. 177, 186). Donc **plus prudent que gftools**, pas fidèle.

**Quand la découpe n'est pas atteinte (l. 342-361) :** corrige la version 1. Ce n'est **pas** `NA` : `decTige <-
ifelse(c13 > cDec, 1 − (cDec·corr130/c13)^3, 0)` (l. 342), identique à Marculus (l. 172). Avec `outrepasse = FALSE`, si
aucun arbre ne vérifie `0.75 * c130/pi > dec`, `volTige` est calculé de la même façon (l. 349) et gftools émet
seulement un `warning` (l. 350-359 : « dec is too large for valuable calculation »). Le 0 de Marculus pour un arbre
sous la découpe coïncide donc avec gftools (`decTige = 0`) ; seul l'avertissement manque.

**Deleuze 2014 (D1, seconde main) :** pas de part de tige (« article distinct », p. 23) ; aucun domaine chiffré ; plus
petit arbre du calage : 2 m, circonférence 6-7 cm (p. 24). **Statut : sourcée (G1), hors domaine non documenté (D1).**

**Options :**
- A. Garder tel quel (négatif conservé dans `fct`, tige et houppier ramenés à 0 par les gardes de Marculus).
- B. Borner `fct` à [0 ; 1] et déclarer « hors domaine » sous un seuil que Pascal fixe (aucune valeur publiée : le calage
  commence à 2 m, 6-7 cm).
- C. Refuser le calcul de la tige là où `fct ≤ 0` et le dire (« non calculé »).

## 3. Règle 2 : `if (fct > 0.95) fct = 0.8`

**Sourcée dans gftools (G1 l. 324-332)** : commentaire de l'auteur « ajout ici de condi pour verifier que 0<FCT<1 (faut
il tj un else ) ... je remonte la contrainte pour les pins… bon faudra qu'on fasse mieux », puis
`if (any(FCT > 0.95)) { if (conditionel) { FCT[FCT > 0.95] <- 0.8 } else { FCT[FCT > 0.95] <- NA } }`.
- `conditionel = TRUE` par défaut (l. 148) : c'est la branche que Marculus reprend. La branche `conditionel = FALSE`
  (valeur `NA`) n'existe pas dans Marculus.
- **Provisoire de l'aveu de son auteur** (« faudra qu'on fasse mieux ») ; la contrainte vise les pins. Pas dans
  Deleuze 2014 (D1) : propre à gftools, sans justification publiée.
- Dans Marculus, le seul autre `0.95` (l. 191, `PART_HOUPPIER_MAX`) est un autre objet.
- **Effet** : fonction discontinue (0,95 reste 0,95 ; 0,951 devient 0,8) ; elle évite `1/FCT − 1 ≤ 0` du houppier.
- Écart de version : le miroir rdrr.io (G2) ne montrait pas la règle ; la copie locale (1.2.8.9000) la contient.

**Statut : sourcée (gftools, défaut `conditionel = TRUE`), non fondée scientifiquement (provisoire).**

**Options :**
- A. Garder, en citant G1 l. 324-332 dans la spec (règle héritée de gftools, provisoire).
- B. Remplacer par un plafond continu `min(fct, 0.95)` : choix de Marculus, qui s'écarte de gftools ; à nommer.
- C. Retirer et traiter `fct ≥ 1` comme houppier nul : s'écarte de gftools.
- D. Mesurer d'abord : chercher un arbre réel où `fct > 0,95` (point au sol 2). Sans cas, A suffit.

## 4. Règle 3 : `htotM <= 1.4 → 0` sans le dire

**Marculus (M1 l. 157) :** `if (htotM <= 1.4 || c130cm <= 0.0) return VolumesEmerge(0.0, 0.0, 0.0, 0.0)`.

- **gftools (G1 l. 178-188) :** aucun seuil bas de hauteur. Contrôles : erreur si `c130` ou `htot` est non numérique
  ou négatif (`any(htot < 0)`), avertissement si `htot > 50`. Le `1.3` n'y est que dans `corr130 <- (1 − 1.3/htot)`
  (l. 303). Recherche de `1.4` dans tout `/media/pascal/data2/digital_ocean/gftools` (fichiers `.R`, `.Rd`, `.md`,
  `.txt`) : seulement des coefficients de `TarifsONF3.R`, aucun seuil. Pour `htot = 1,3` : division par zéro dans
  `corr130` (non exécuté).
- **Port nemeton (N2 l. 131) :** `htot > 1.4`, mais ce port copie Marculus (N2 l. 184) : ce n'est pas une source.
- **NEMETON (N1 l. 10, 49-50) :** `H ≤ 1,30` donne une erreur ; décision Q2 du 2026-11-27 « par délégation de Pascal »,
  non issue de la publication.
- **Deleuze 2014 (D1) :** ne traite pas H → 1,30 m ; plus petit arbre imprimé 2 m (p. 24).
- **Statut :** 1,3 sourcé par la formule ; **1,4 introuvable** à la source (gftools, Deleuze) ; le « 0 silencieux » est
  un choix de Marculus (gftools n'a pas de seuil, NEMETON lève une erreur).

**Options :**
- A. Garder 1,4 et 0, en disant « non calculé » à l'écran (la carte du bac à sable « Statut dit combien de tiges
  EMERGE n'ont pas de volume » les compte).
- B. Passer à `H ≤ 1,30` comme NEMETON, avec refus déclaré.
- C. Refuser sous 2 m (plus petite hauteur du calage) : plus prudent, mais exclut des semis que gftools calcule.

## 5. Écarts entre sources, notés

1. Règle 2 : G1 la contient (copie locale 1.2.8.9000), le miroir G2 non : écart de version de gftools. La version 1 de
   ce document concluait « introuvable » sur G2 et était fausse.
2. Règle 1 : Marculus borne tige et houppier là où gftools ne le fait pas ; en cas de `FCT < 0`, gftools rend un
   volume de tige négatif et un houppier `NA` (déduction, non exécutée).
3. Règle 3 : trois comportements (gftools sans seuil, NEMETON erreur à 1,30, Marculus 0 à 1,4).
4. Date de gftools : `DESCRIPTION` dit 2021-12-06, le dernier commit annoncé est du 2019-06-20 ; non rapproché.
5. D1 non relue ; tout ce qui touche la publication vient de la fiche NEMETON du 2026-11-26.

## 6. Trois points au sol (pour Pascal)

1. **Hêtre C = 0,628 m à 2 m et 3 m :** exécuter `gftools::TarEmerge(c130 = 62.8, htot = 2, ess = <code hêtre>,
   typevol = "tige")` puis `"houp"` (et à 3 m) : confirmer −0,63 / −0,08 pour `fct`, le signe de la tige et le `NA`
   du houppier ; comparer à Marculus (0).
2. **Un arbre où `fct` dépasse 0,95 :** chercher sur une grille H × C130 dans `ListTarEmerge` ; mesurer la tige avec
   `conditionel = TRUE` et `FALSE`. Sans cas réel, la règle ne compte pas.
3. **Un arbre de 1,35 m :** `TarEmerge` rend-il un volume, et de quel ordre ? C'est ce qui dira si le seuil 1,4 cache
   une valeur aberrante.

## 7. Liste de contrôle

- 1. Sources et emplacements : cochée (fichiers, lignes, version 1.2.8.9000 relues le 2026-11-27).
- 2. Décomptes : trois règles traitées ; table `ListTarEmerge` 93 lignes × 15 colonnes (rapport rdrr.io, non recompté).
- 3. Termes : lignes recopiées de G1 et M1 relus ; les effets sur `FCT ≤ 0` sont des **déductions non exécutées**.
- 4. Statut : constat, rien tranché ; question de Pascal : quelle option pour chaque règle.
- **Non cochés :** (a) gftools non exécuté (pas de R ici) ; (b) Deleuze 2014 non relue directement ; (c) commit de
  gftools non relu (pas d'outil shell), date 2019 contre 2021 ; (d) historique git de Marculus non interrogé pour
  l'origine du 1,4.
