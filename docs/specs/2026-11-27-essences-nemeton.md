# Essences de Marculus et codes IFN pour NEMETON (mission 037)

> **À valider par Pascal, ligne à ligne, avant tout code.** Propriétaire : Pascal. Rendu de l'agent `acquereur`, consulté le 2026-11-27.
>
> **Pourquoi (la décision éclairée).** Le paquet NEMETON prend un code IFN comme clé de calcul ; Marculus travaille sur des noms. Cette table propose, pour chaque nom, le code IFN et le repli à appliquer quand ce code n'est pas dans le paquet. Pascal tranche chaque rattachement avant le branchement de NEMETON dans le modèle de données (mission 039 et suivante).

## Sources (une seule pour les codes, plusieurs pour les observations)

| Rôle | Source | Version et date | Emplacement |
|---|---|---|---|
| **Codes et libellés IFN (source unique)** | IGN, « OCRE PRO ONF, Documentation des données disponibles », rubrique « ESS : essence de l'arbre » | v2.2 du 13/12/2022 ; URL `https://inventaire-forestier.ign.fr/IMG/pdf/ocrepro_onf_documentation_v2_2.pdf`, téléchargée (WebFetch) et **pages 40-41 relues en image le 2026-11-27** | pages 40-41 (68 modalités : 1 à 49 feuillus, 51 et plus résineux) |
| Copie locale des codes (lue, pas recopiée ailleurs) | `~/dev/tarif-nemeton/donnees/referentiels/ifn/essences_ifn_ess.csv` | consulté le 2026-11-27 | lignes 15 à 82 (68 lignes) ; colonne `repli_propose` pour le repli genre |
| **Codes couverts par le paquet** | `~/dev/tarif-nemeton/livrable/datapackage.json` (version `0.2.2-provisoire`, créé 2026-11-27) et `constantes.csv` (247 lignes de données, relues le 2026-11-27) | | `datapackage.json` : `essences_non_couvertes` (68, 75) et `limites_connues` ; `constantes.csv` : lignes citées dans la table |
| Noms de Marculus | `core/src/main/kotlin/fr/marculus/core/Referentiels.kt` l. 5-7 (`ESSENCES_DEFAUT`, 6 noms) et `core/src/main/kotlin/fr/marculus/core/Cubage.kt` l. 52-95 (`RATTACHEMENT_ESSENCES`) | branche à jour au commit 6776e16 | |
| Noms exacts de la table EMERGE (hors rattachement) | `docs/specs/2026-11-27-rattachement-essences.md` l. 9 (liste « déjà exacts ») ; présence vérifiée dans `core/src/main/kotlin/fr/marculus/core/EmergeCoefs.kt` | | |

**Concordance des deux lectures des codes.** La page IGN (image, v2.2) et la copie locale donnent les mêmes 68 codes et libellés (relus code par code pour les 59 noms ci-dessous). Les deux lectures viennent du même producteur (IGN) : ce n'est pas une source indépendante. **Limite dite** : la documentation IGN 2025 n'a pas été lue ; il reste à vérifier que la liste 2022 est encore celle de la campagne 2025 (réserve déjà inscrite dans l'en-tête de la copie locale, ligne 8).

**Codes couverts par le paquet 0.2.2-provisoire** (14 codes) : 2 et 3 (`Quercus_spec`, `constantes.csv` l. 6-7), 9 (`Fagus_sylvatica`, l. 2), 11 (`Carpinus_betulus`, l. 18), 12 (`Betula_spec`, l. 26), 15 (`Acer_pseudoplatanus`, l. 14), 17 (`Fraxinus_excelsior`, l. 22), 29 et 49 (`Autres_feuillus`, l. 227-228), 52 (`Pinus_sylvestris`, l. 40), 61 (`Abies_alba`, l. 47), 62 (`Picea_abies`, l. 33), 63 (`Larix_decidua`, l. 54), 64 (`Pseudotsuga_menziesii`, l. 61) (les lignes des résineux sont celles de la première constante `profil_tige_resineux`). **Non couverts** : 68 et 75 (`datapackage.json`, `essences_non_couvertes`). Tous les autres codes sont hors paquet.

## Règle de repli (VALEUR PROVISOIRE)

Source : la règle de `docs/spec.md` § 4 de NEMETON telle que la reprend `essences_ifn_ess.csv` (colonne `repli_propose`, ligne 12 : « essence, puis genre calé, puis classe "autres" ») ; `datapackage.json` `limites_connues` précise que Marculus l'applique « sur décision de Pascal (D5) ». Je n'ai pas lu cette spec NEMETON elle-même.

1. Code couvert par le paquet : ses constantes.
2. Sinon, **genre calé** : Quercus → `Quercus_spec` (2/3), Acer → 15, Pinus → 52, Abies → 61, Picea → 62, Larix → 63 (ces genres sont ceux que la copie locale marque « à décider (D5) »).
3. Sinon **feuillu** → classe « autres feuillus » (49 indigène, 29 exotique : constantes identiques dans le paquet, `datapackage.json` `limites_connues`, donc le choix 29/49 est sans effet sur le calcul ; l'attribution indigène/exotique est une proposition de l'agent, non sourcée).
4. Sinon **conifère** → aucun repli NEMETON (68 et 75 non couverts) : Marculus garde son repli actuel.

Valeur provisoire, mesurée par Pascal à la revue de cette table, gelée avant le branchement de NEMETON dans le modèle de données (fiche 037).

## Table de revue (59 noms)

Statut : **S** rattachement sûr (le code IFN nomme l'essence, ou son nom latin figure dans la liste du code) ; **A** ambigu, Pascal tranche ; **C** pas de code IFN propre, la seule modalité possible est une classe ; **X** aucun code proposé.
Colonne « Paquet » : `couvert` = le code est dans le paquet ; `repli genre → N` ; `repli classe → 49` ; `non couvert`, aucun repli.
Source de chaque ligne : colonne « Code IFN » = IGN OCRE PRO v2.2 p. 40 (codes 1 à 42, 49) ou p. 41 (51 à 77) ; colonne « Paquet » = `datapackage.json` et `constantes.csv` comme ci-dessus ; colonne « Clé EMERGE » = `Cubage.kt` l. 52-95 (nom saisi normalisé).

### A. Essences par défaut (`Referentiels.kt` l. 5-7) : 6 noms

| Nom saisi | Code IFN | Libellé IFN | Paquet | Statut | Remarque |
|---|---|---|---|---|---|
| Chêne | 3 (ou 2) | Chêne rouvre / Chêne pédonculé | couvert : `Quercus_spec` (l. 6-7) | A | Décision de Pascal du 2026-11-27 : « Chêne » = sessile / pédonculé. Les codes 2 et 3 ont des constantes identiques (`limites_connues`) : 3 proposé pour accorder avec « Chêne sessile » côté EMERGE ; sans effet sur le calcul |
| Hêtre | 9 | Hêtre | couvert : `Fagus_sylvatica` (l. 2) | S | |
| Autres feuillus | 49 (ou 29) | Autre feuillu indigène / exotique | couvert : `Autres_feuillus` (l. 227-228) | A | Classe, pas une essence ; 29 et 49 mêmes constantes ; 49 proposé |
| Sapin | 61 | Sapin pectiné | couvert : `Abies_alba` (l. 47) | S | |
| Épicéa | 62 | Épicéa commun | couvert : `Picea_abies` (l. 33) | S | |
| Autres résineux | 75 (ou 68) | Autre conifère indigène / exotique | non couvert | A | Les deux codes sont non couverts : aucun calcul NEMETON, repli actuel de Marculus (modèle gris résineux) |

### B. Noms courts rattachés à la main (`Cubage.kt` l. 53-91) : 36 noms nouveaux

(39 clés de `RATTACHEMENT_ESSENCES` entre l. 53 et 91, dont « chene », « sapin », « epicea » déjà en A ; la fiche 037 en annonçait 38, l'écart de un est signalé.)

| Nom saisi | Clé EMERGE | Code IFN | Libellé IFN | Paquet | Statut | Remarque |
|---|---|---|---|---|---|---|
| Mélèze | mélèze d'Europe | 63 | Mélèze d'Europe | couvert : `Larix_decidua` (l. 54) | S | |
| Érable | érable sycomore | 15 | Grand érable (A. pseudoplatanus ou platanoides) | couvert : `Acer_pseudoplatanus` (l. 14) | A | Le paquet signale le rattachement non univoque du code 15 ; érable champêtre, de Montpellier, à feuilles d'obier = code 21 « Petit érable » (hors paquet, repli genre → 15, à décider D5) |
| Pin | pin générique | (aucun) | pas de modalité « Pin » seule | repli genre Pinus → 52, ou aucun | X | Pas d'espèce dominante (voir la table EMERGE) ; code 52 en repli seulement si Pascal l'accepte |
| Pin noir | pin noir d'Autriche | 54 | Pin noir (P. nigra ssp. nigricans ou clusiana) | repli genre → 52 | S | |
| Pin laricio | pin laricio de Corse | 53 | Pin laricio | repli genre → 52 | S | |
| Laricio | pin laricio de Corse | 53 | Pin laricio | repli genre → 52 | S | |
| Alep | pin d'Alep | 57 | Pin d'Alep | repli genre → 52 | S | |
| Frêne | frêne commun | 17 | Frêne (F. excelsior, oxyphylla ou ornus) | couvert : `Fraxinus_excelsior` (l. 22) | A | Rattachement non univoque signalé par le paquet ; le frêne commun saisi est F. excelsior, donc le tarif du paquet convient, le frêne à fleurs (ornus) y est aussi |
| Tilleul | tilleul à petites feuilles | 20 | Tilleul | repli classe → 49 | S | |
| Bouleau | bouleau verruqueux | 12 | Bouleau | couvert : `Betula_spec` (l. 26) | S | |
| Peuplier | peuplier autre cultivé | 19 | Peuplier cultivé | repli classe → 49 | A | Le peuplier non cultivé (blanc, noir) est le code 33 ; « Peuplier » nu = cultivé par convention EMERGE, à confirmer |
| Peuplier tremble | tremble | 24 | Tremble | repli classe → 49 | S | |
| Aulne | aulne glutineux | 13 | Grand aulne | repli classe → 49 | S | Aulne vert = code 37, saisi en toutes lettres |
| Robinier | robinier faux acacia | 14 | Robinier faux acacia | repli classe → 49 | S | |
| Noyer | noyer commun | 27 | Noyer | repli classe → 49 | S | |
| Orme | orme champêtre | 18 | Orme | repli classe → 49 | S | |
| Cèdre | cèdre de l'Atlas | 65 | Cèdre de l'Atlas | non couvert (classe conifères) | S | Cèdre du Liban = 76 |
| Saule | saule générique | 25 | Saule | repli classe → 49 | S | |
| Platane | platane à feuilles d'érable | 26 | Platane | repli classe → 49 | S | |
| Sorbier | sorbier des oiseleurs | 23 | Autre fruitier (liste latine : sorbus aria ou aucuparia ou domestica…) | repli classe → 49 | A | Le sorbier des oiseleurs n'est pas un fruitier, mais son nom latin est dans la liste du code 23 (pas de code propre) ; sorbier torminal = 41 |
| Alisier | alisier torminal | 41 | Alisier torminal | repli classe → 49 | S | |
| Noisetier | noisetier coudrier | 31 | Noisetier | repli classe → 49 | S | |
| Sureau | sureau noir | 49 | Autre feuillu indigène | couvert : `Autres_feuillus` | C | Aucun Sambucus dans la liste IFN (relue p. 40) |
| Cyprès | cyprès de Provence | 66 | Cyprès (C. sempervirens) | non couvert | S | |
| Thuya | thuya géant | 68 | Autre conifère exotique | non couvert | C | Aucun Thuja dans la liste IFN |
| Genévrier | genévrier commun | 75 | Autre conifère indigène | non couvert | C | Le code 69 ne vaut que pour J. thurifera |
| Pommier | pommier sauvage | 23 | Autre fruitier (Pirus malus…) | repli classe → 49 | S | |
| Poirier | poirier commun | 23 | Autre fruitier (Pirus communis…) | repli classe → 49 | S | |
| Marronnier | marronnier d'Inde | 29 | Autre feuillu exotique | couvert : `Autres_feuillus` | C | Aucun Aesculus dans la liste IFN ; 29 (exotique) proposé, 49 équivalent pour le calcul |
| Tulipier | tulipier de Virginie | 42 | Tulipier de Virginie | repli classe → 49 | S | |
| Prunier | prunier domestique | 23 | Autre fruitier (prunus domestica) | repli classe → 49 | S | |
| Mûrier | mûrier blanc | 29 | Autre feuillu exotique | couvert : `Autres_feuillus` | C | Aucun Morus dans la liste IFN ; 29 proposé |
| Olivier | olivier d'Europe | 28 | Olivier | repli classe → 49 | S | |
| Fruitier | fruitiers divers | 23 | Autre fruitier | repli classe → 49 | S | |
| Fruitiers | fruitiers divers | 23 | Autre fruitier | repli classe → 49 | S | Pluriel |
| Charme commun | charme | 11 | Charme | couvert : `Carpinus_betulus` (l. 18) | S | |

### C. Noms latins courts (`Cubage.kt` l. 93-94) : 2 noms

| Nom saisi | Code IFN | Libellé IFN | Paquet | Statut | Remarque |
|---|---|---|---|---|---|
| Quercus petraea | 3 | Chêne rouvre (Quercus sessiliflora = petraea) | couvert : `Quercus_spec` (l. 7) | S | |
| Quercus robur | 2 | Chêne pédonculé (Quercus pedunculata = robur) | couvert : `Quercus_spec` (l. 6) | S | |

### D. Noms exacts de la table EMERGE, cités par la table de revue du 2026-11-27 : 15 noms

| Nom saisi | Code IFN | Libellé IFN | Paquet | Statut | Remarque |
|---|---|---|---|---|---|
| Douglas | 64 | Douglas | couvert : `Pseudotsuga_menziesii` (l. 61) | S | |
| Charme | 11 | Charme | couvert : `Carpinus_betulus` (l. 18) | S | |
| Tremble | 24 | Tremble | repli classe → 49 | S | |
| Merisier | 22 | Cerisier ou merisier | repli classe → 49 | S | |
| Cerisier | 22 | Cerisier ou merisier | repli classe → 49 | S | |
| Châtaignier | 10 | Châtaignier | repli classe → 49 | S | |
| Houx | 49 | Autre feuillu indigène | couvert : `Autres_feuillus` | C | Aucun Ilex dans la liste IFN |
| If | 67 | If | non couvert | S | |
| Buis | 49 | Autre feuillu indigène | couvert : `Autres_feuillus` | C | Aucun Buxus dans la liste IFN |
| Pin sylvestre | 52 | Pin sylvestre | couvert : `Pinus_sylvestris` (l. 40) | S | |
| Pin maritime | 51 | Pin maritime | repli genre → 52 | S | |
| Pin d'Alep | 57 | Pin d'Alep | repli genre → 52 | S | |
| Pin à crochets | 58 | Pin à crochets | repli genre → 52 | S | |
| Chêne rouge | 4 | Chêne rouge d'Amérique | repli genre Quercus → 2/3 (`Quercus_spec`) | S | Repli genre à décider (D5) |
| Chêne sessile | 3 | Chêne rouvre | couvert : `Quercus_spec` (l. 7) | S | |

### E. Noms sans code proposé (7), hors des 59

« Arbre », « Autre », « Inconnu » (aucun rattachement même côté EMERGE) ; « Hêtres », « Chênes » (pluriels, sans rattachement côté EMERGE) ; « Feuillus » et « Résineux » (groupes, modèle gris côté EMERGE : à décider si NEMETON les envoie vers 49 et vers aucun repli). Je n'invente aucun code pour eux.

## Décomptes (relancés avant le rendu)

- Noms avec ligne dans la table : 6 + 36 + 2 + 15 = **59** ; sans code proposé (E) : 7.
- Statuts sur les 59 : **S 44**, **A 7** (Chêne, Autres feuillus, Autres résineux, Érable, Frêne, Peuplier, Sorbier), **C 7** (Sureau, Thuya, Genévrier, Marronnier, Mûrier, Houx, Buis), **X 1** (Pin) ; 44 + 7 + 7 + 1 = 59.
- Colonne « Paquet » sur les 59 : couvert 21 ; repli genre 8 (Pin noir, Pin laricio, Laricio, Alep, Pin d'Alep, Pin maritime, Pin à crochets, Chêne rouge) ; repli classe → 49 : 23 ; non couvert 6 (Autres résineux, Cèdre, Cyprès, Thuya, Genévrier, If) ; sans code 1 (Pin) ; 21 + 8 + 23 + 6 + 1 = 59.
- Clés de `RATTACHEMENT_ESSENCES` : 41 (39 noms courts, dont 3 déjà en A, + 2 noms latins) ; 38 annoncés par la fiche : **écart d'un relevé** (comptage manuel des lignes 53 à 91).
- Codes IFN de la référence : 68 modalités (lignes 15 à 82 de la copie locale).

## Limites dites

- Les repli indigène/exotique (29 ou 49) et les codes de classe proposés pour Sureau, Thuya, Genévrier, Marronnier, Mûrier, Houx, Buis sont des propositions de l'agent (les genres sont absents de la liste IFN, relue p. 40-41), pas une règle IGN.
- Non fait : aucun branchement, aucun test, aucune modification de `Cubage.kt` ; le paquet n'est pas compilé dans Marculus, donc rien ici n'est vérifié par un vert.
- L'abondance relative des espèces (qui fait choisir « l'essence la plus courante ») vient de la table EMERGE du même jour, non rejouée.
