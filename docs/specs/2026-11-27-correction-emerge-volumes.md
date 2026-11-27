> **Brief transmis par la session NEMETON** (dépôt `pobsteta/tarif-nemeton`, fichier `docs/correction-marculus-emerge.md`, commit 8a984d1), versé ici le 27/11/2026 à la demande de Pascal.
> Les chemins `code/…`, `resultats/…` et `tests/donnees/…` cités ci-dessous renvoient au dépôt `pobsteta/tarif-nemeton`.
> Rappel Marculus : `EmergeCoefs.kt` est généré depuis `gftools::ListTarEmerge` et ne s'édite pas à la main ; la table de rattachement « Chêne » est à créer à part.
> Décisions de Pascal (27/11/2026) intégrées en fin de document : quatre volumes affichés ; « Chêne » rattaché au chêne sessile / pédonculé.

# Fiche de correction — EMERGE dans Marculus

> Mission 010 (27/11/2026). Rédigée par l'agent `dev` à partir de l'audit 005 (`resultats/constat_emerge/audit_marculus.md`).
> **Non relue par Pascal.** À transmettre à la session Marculus par Pascal ; la fiche propose, Pascal et la session Marculus décident.
> Clone lu : `pobsteta/marculus`, commit `41a01f7`. Fichiers lus : `core/src/main/kotlin/fr/marculus/core/` (`Cubage.kt`, `EmergeCoefs.kt`, `VolumesMartelage.kt`, `CsvFoncier.kt`, `ParParcelle.kt`, `export/ExportCsv.kt`), `core/src/test/kotlin/fr/marculus/core/CubageTest.kt`, `app/.../ui/statut/StatutHistoriqueScreen.kt`, `app/src/main/res/values/strings.xml`, `data/.../SauvegardeRepository.kt`. Rien n'a été modifié dans Marculus.
> Aucun compilateur Kotlin : les valeurs « Marculus actuel » viennent d'une **traduction Python ligne à ligne** (`code/audit_marculus_traduction.py`), pas d'une exécution Kotlin (voir « Ce qui reste à confirmer »).

## En deux phrases

Marculus appelle « total » un volume qui est en réalité le **bois fort (tige + houppier, découpe 7 cm)**, donc il sous-estime le volume aérien total EMERGE de 27 % à 59 % sur les petits arbres (2 à 10 m). Et le nom « Chêne » est rattaché par préfixe au **chêne rouge** (arbre américain) au lieu d'un chêne indigène, sans que rien ne le signale.

La formule du volume total, elle, est **juste** (le facteur `1 − 1,3/H` va dans le bon sens, comme dans Deleuze et al. 2014 p. 26) : il n'y a pas d'erreur de calcul à chercher, seulement un mauvais nom et une mauvaise correspondance.

## Défaut 1 — le « total » n'est pas le volume total

**Où.** `Cubage.kt`, `emergeVolumes` (l. 86-116) ; le commentaire l. 79 et le test `CubageTest.kt` l. 61 (« total aérien »).

**Ce que fait le code.**
- l. 95-96 : `volCyl` et `formTot` donnent `formTot · volCyl`, **c'est le volume aérien total publié** (à l'arrondi des coefficients près : 5 décimales dans Marculus contre 3 dans la publication, soit −1,5 % à +1,4 % sur nos arbres tests).
- l. 100 et 107-108 : `decTige` et `decHoup` retirent le bois sous la découpe de 7 cm (`DECOUPE_BOIS_FORT_CM`, l. 29).
- l. 115 : `VolumesEmerge(tige, houppier, tige + houppier)` : le `total` rendu est donc **tige bois fort + houppier bois fort**. Le volume `formTot · volCyl` n'est jamais rendu.

**Conséquence.** `total` / volume publié : 0,41 (2 m), 0,49 (3 m), 0,61 (5 m), 0,73 (10 m) ; sur le hêtre de 62,8 cm : 0,78 à 10 m. L'écart vient de la **découpe** (et, tout en bas, de `fct`, voir points secondaires), pas du facteur de forme. Le calcul de la hauteur de décrochement (0,8 × H) déplace le total de ±4 à 5 % seulement : ce n'est pas la cause.

**Correction proposée** (à décider par la session Marculus) :
1. Ajouter un champ `volumeTotal = formTot * volCyl` (valeur des l. 95-96) à `VolumesEmerge`, sous un nom distinct de `total`.
2. Renommer l'actuel `total` en `boisFortTotal` (« bois fort total, découpe 7 cm ») ; corriger le commentaire l. 79, le commentaire de `volumesUnitaire` (l. 165) et le test l. 61.
3. Les sorties qui affichent ou emportent `total` doivent dire quel volume elles montrent. Le `total` part de `VolumesMartelage.kt` l. 65 (`CubageTige(v.tige, v.houppier, v.total, …)`) et est cumulé l. 78, puis il se retrouve dans :
   - l'écran `StatutHistoriqueScreen.kt` : l. 237 (`volTot = volumes.volumeTotalM3`), l. 269 (libellé `statut_volume_aerien`, « Volume total aérien : … », `strings.xml` l. 193, alors que la valeur est le bois fort) et l. 477 (somme directe de `v.total`) ;
   - les exports : `ExportCsv.kt` l. 74 (en-tête `VolumeTotalM3`) et l. 149 (colonne `VolumeTotalM3` par ligne), `CsvFoncier.kt` l. 56 (`vTot += v.total * quantité`), `SauvegardeRepository.kt` l. 195 et 272 (`volumeTotalM3` sauvegardé), `ParParcelle.kt` l. 39 (`vtot += c.volumeTotalM3 * n`).
   Le nom `VolumeTotalM3` des fichiers étant déjà échangé, le renommage demande de décider s'il change aussi dans les formats (CSV, sauvegarde) : question de compatibilité pour la session Marculus.

**Question laissée à Pascal : quelle valeur Marculus affiche-t-il par défaut ?** Le bois fort (ce que l'on vend, découpe 7 cm) ou le volume aérien total (ce que donne EMERGE publié et ce que NEMETON compare) ? Chacun a un sens ; la fiche ne tranche pas, elle demande seulement que les deux soient exposés et nommés.

## Défaut 2 — « Chêne » tombe sur le chêne rouge

**Où.** `Cubage.kt`, `coefEmerge` (l. 40-50), en particulier l. 45 (préfixe) et l. 46 (sous-chaîne) ; table `EmergeCoefs.kt` (« chene rouge » l. 406, avant « chene vert », « chene pedoncule », « chene sessile »).

**Ce que fait le code.** « Chêne » est normalisé en `chene` ; aucune clé exacte ; `firstOrNull { startsWith }` retient la **première** clé de la table commençant par `chene` : `chene rouge`. Le résultat dépend de l'ordre de la table. Même mécanisme pour `codeEssence` (l. 61-62). Le test `CubageTest.kt` l. 42-45 passe (1,108 est dans 0,9-1,8) sans prouver que la bonne essence est utilisée.

**Conséquence.** Pour « Chêne » 125 cm de circonférence, 20 m : tige 1,108 m³ avec le chêne rouge contre 1,257 avec le chêne sessile (tige −12 %).

**Correction proposée.**
- Supprimer la correspondance par préfixe et par sous-chaîne (l. 45-46). Correspondance **explicite** : par code IFN / ONF (`EMERGE_CODES` existe déjà, ex. « CHS » pour chêne sessile) ou par une table de rattachement écrite à la main (« chêne » → ?, « chêne sessile », « chêne pédonculé », etc.).
- Pour un nom qui ne correspond à rien : soit **refus** (volume non calculé, message à l'utilisateur), soit **repli sur le modèle gris** (feuillus ou résineux) **déclaré** à l'écran. Le repli déjà prévu l. 48-49 (mots « feuillu », « résineux ») va dans ce sens ; il faut le rendre visible.
- Dans la publication (Tab. 1), « chêne sessile » et « chêne pédonculé » partagent **une seule ligne** (`Quercus robur_petraea`) ; le chêne rouge (`Quercus rubra`) a la sienne.

**Question laissée à Pascal : que doit devenir le nom générique « Chêne » ?** Rattachement au chêne sessile/pédonculé (`Quercus robur_petraea`, le plus courant en France), ou refus qui force l'utilisateur à choisir ? La fiche propose le refus ou le rattachement explicite ; le choix est celui de Pascal.

## Points secondaires (non traités ici, à signaler)

1. **Part de tige `fct` (l. 97) extrapolée et négative aux petites hauteurs.** Hêtre C = 0,628 m : `fct` = −0,63 à 2 m et −0,08 à 3 m (hors domaine réaliste) ; la tige tombe à 0. Arbre A (0,20 m, 2 m) : `fct` = 0,16, tige 0. Il n'y a pas de borne basse.
2. **Règle `if (fct > 0.95) fct = 0.8` (l. 98)** : une part de tige supérieure à 0,95 est ramenée à 0,8. **Aucune source** n'est donnée ; à documenter ou à retirer.
3. **`H ≤ 1,4 m → 0` (l. 87).** La référence NEMETON refuse `H ≤ 1,30 m` (erreur) ; Marculus rend 0 sans le dire. Écart de seuil (1,3 / 1,4) et de comportement (0 silencieux / erreur).
4. **Coefficients d, e, f, g, α, β** : absents de Deleuze 2014 (qui ne publie que a, b, c) ; ils viennent de `gftools::ListTarEmerge`. **Non vérifiables d'ici**, donc non vérifiés ; la fiche n'invente aucun coefficient.
5. **Aucun test Marculus ne compare à une valeur publiée** (`CubageTest.kt` : bornes larges 0,9-1,8 et cohérence interne). Les arbres tests ci-dessous comblent ce manque.
6. **Sens du facteur** : correct dans Marculus ; **ne pas le « corriger »** vers la forme de la méthodologie NEMETON v2.24 (qui donnerait 0,30 du publié à 5 m).

## Arbres tests

Unités : **C130 = circonférence en cm** à l'entrée de Marculus (`c130cm`), **H en m**, volumes en m³. Le diamètre vaut C/π. Les valeurs « avant » sont celles de la traduction Python du code actuel ; les valeurs « après » sont `formTot · volCyl` avec les coefficients de Marculus (ce que le code corrigé exposerait), à comparer au **publié** (`code/emerge_reference.py`, coefficients à 3 décimales). Tolérance proposée : ±2 % (arrondi des coefficients).

### Les deux arbres du terrain (Pascal les cube avant et après)

| Arbre | Essence saisie → clé retenue | C130 (cm) | H (m) | Avant : tige / houppier / « total » | Après : volume total (Marculus corrigé) | Publié (référence) |
|---|---|---|---|---|---|---|
| Hêtre | « hetre » → `hetre` | 62,8 (D ≈ 20 cm) | 10 | 0,1557 / 0,0251 / **0,1808** | **0,2365** | 0,2332 (rapport 1,014) |
| « Chêne » | « Chêne » → **`chene rouge`** (défaut 2) | 125 (D ≈ 40 cm) | 20 | 1,1082 / 0,2113 / **1,3196** | voir ci-dessous | voir ci-dessous |

Pour « Chêne » 125 / 20, la valeur « après » dépend de la réponse de Pascal sur le rattachement :

| Rattachement | Volume total Marculus corrigé (`formTot·volCyl`) | Publié | Bois fort actuel (tige + houppier) |
|---|---|---|---|
| `chene rouge` (comportement actuel) | 1,4835 | 1,4677 (`Quercus rubra`) | 1,3196 |
| `chene sessile` (`Quercus robur_petraea`) | **1,5941** | **1,6099** | 1,4395 (tige 1,2574 / houppier 0,1822) |

Soit, pour « Chêne » : 1,3196 affiché aujourd'hui contre 1,5941 à 1,6099 attendu avec le chêne sessile (+21 %).

### Les quatre arbres de la mission 003

| Arbre | Clé Marculus | C130 (cm) | H (m) | Avant : tige / houppier / « total » | Après : `formTot·volCyl` | Publié | Avant / publié | Après / publié |
|---|---|---|---|---|---|---|---|---|
| A Fagus sylvatica | `hetre` | 20 | 2 | 0,000000 / 0,014247 / 0,014247 | 0,0351 | 0,034809 | 0,41 | 1,007 |
| B Picea abies | `epicea commun` | 25 | 3 | 0,014525 / 0,000225 / 0,014750 | 0,0299 | 0,029909 | 0,49 | 0,999 |
| C Quercus robur_petraea | `chene sessile` | 30 | 5 | 0,020805 / 0,003089 / 0,023895 | 0,0389 | 0,039241 | 0,61 | 0,990 |
| D Pinus sylvestris | `pin sylvestre` | 50 | 10 | 0,098451 / 0,000865 / 0,099315 | 0,1337 | 0,135670 | 0,73 | 0,985 |

(Valeurs « après » : 0,035063 ; 0,029872 ; 0,038863 ; 0,133673 avant arrondi à 4 décimales.)

Ces valeurs se lisent aussi comme un **test de non-régression à écrire** côté Marculus : `volumeTotal` à ±2 % du publié pour ces **six arbres** (hêtre 62,8 / 10 ; « Chêne » 125 / 20 ; les quatre arbres A à D), soit **sept cas** si « Chêne » compte deux fois (chêne rouge actuel et chêne sessile), et un test qui vérifie qu'un nom générique ne retombe plus sur une essence arbitraire.

## Rejouer l'audit

Depuis la racine du dépôt NEMETON (Python dans `.venv/`, clone Marculus en lecture seule) :

```
.venv/bin/python code/audit_marculus_traduction.py /home/user/pobsteta/marculus
```

Le script traduit `emergeVolumes` ligne à ligne (chaque instruction Python cite la ligne Kotlin), lit la table `EmergeCoefs.kt`, et imprime l'échantillon hêtre 62,8 cm / 10 m, le test « Chêne » 125 cm / 20 m (clé retenue, chêne rouge puis chêne sessile) et les quatre arbres de la mission 003 avec le publié. Les valeurs « publié » viennent de `code/emerge_reference.py` (Deleuze et al. 2014 p. 26, Tab. 1 p. 28, coefficients dans `donnees/referentiels/emerge/coefficients_emerge.csv`). Les colonnes « après » se retrouvent par le champ `full` du détail rendu par `emerge_volumes` (`formtot × volcyl`).

## Ce qui reste à confirmer

- **La traduction Python n'est pas une exécution Kotlin.** Le cubage réel du hêtre 62,8 cm / 10 m dans Marculus doit donner tige 0,1557, houppier 0,0251, total 0,1808 ; sinon la traduction est fausse et les « avant » ci-dessus aussi.
- Coefficients d, e, f, g, α, β non vérifiés (hors publication).
- « Sur écorce » non vérifié dans le code de Marculus.
- Les valeurs « après » supposent que la session Marculus expose `formTot · volCyl` ; si elle choisit un autre calcul, elle doit rejouer ces arbres.

## Décision de Pascal (27/11/2026) : quatre volumes affichés

Marculus affiche et exporte quatre volumes, chacun sous son nom :

| Libellé | Contenu | Dans `Cubage.kt` | Hêtre C130 = 62,8 cm, H = 10 m |
|---|---|---|---|
| Volume bois fort tige (découpe 7 cm) | tige jusqu'à 7 cm | `tige` (l. 101) | 0,1557 m³ |
| Volume houppier (découpe 7 cm) | branches jusqu'à 7 cm | `houppier` (l. 112) | 0,0251 m³ |
| Volume bois fort total = tige + houppier | ce que Marculus nomme aujourd'hui « total » / « Volume total aérien » | `tige + houppier` (l. 115) | 0,1808 m³ |
| Volume aérien total (sans découpe, EMERGE publié) | tout le bois aérien, menu bois compris | `formTot * volCyl` (l. 95-96) | 0,2365 m³ |

Conséquences pour la session Marculus : renommer l'actuel « total » en « bois fort total » partout où il s'affiche ou
s'exporte (liste au défaut 1) ; ajouter le volume aérien total ; les deux questions de compatibilité (nom de colonne
`VolumeTotalM3` dans les CSV et les sauvegardes) restent à trancher dans la session Marculus.

## Décision de Pascal (27/11/2026) : nom générique « Chêne »

« Chêne » est rattaché explicitement à « Quercus robur_petraea » (chêne sessile ou pédonculé, Deleuze 2014 Tab. 1),
comme dans NEMETON ; les autres chênes (rouge, vert, pubescent…) ne sont pris que s'ils sont saisis explicitement. La
recherche par préfixe ou sous-chaîne (Cubage.kt l. 40-50) est remplacée par une table de rattachement explicite.
Arbre test : « Chêne » C130 = 125 cm, H = 20 m → volume aérien total 1,5941 m³ (coefficients Marculus du chêne
sessile ; publié 1,6099 m³), au lieu de 1,4835 m³ aujourd'hui (chêne rouge).
