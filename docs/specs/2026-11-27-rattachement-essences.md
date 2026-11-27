# Rattachement des noms d'essence courts à une clé EMERGE (mission 094)

> **À valider par Pascal avant la fusion.** Chaque ligne de la table `RATTACHEMENT_ESSENCES` (`core/src/main/kotlin/fr/marculus/core/Cubage.kt`) est ici, avec la clé que le nom prenait **avant** la correction (premier nom de la table commençant par le nom saisi, ou le contenant : arbitraire, car dépendant de l'ordre de la table `EmergeCoefs.kt` générée depuis `gftools`) et la clé proposée. Un test (`RattachementEssencesTest`) vérifie chaque ligne : la clé existe, et le nom saisi donne exactement les coefficients de cette clé.
>
> Règle suivie : l'essence la plus courante en France (IFN, BD Forêt V2, `docs/essences-bdforet-v2.html`) ; quand tous les représentants du genre ont les mêmes coefficients, le choix est sans effet sur le volume ; quand il n'y a pas d'espèce dominante, la ligne générique de la table elle-même (« pin generique », « saule generique »). Les chiffres d'abondance (IFN) sont de mémoire de l'agent, non rejoués dans cette session : **à confirmer**.
>
> Noms **sans rattachement** (volet « repli par coefficient de forme », visible dans la colonne `cubage`) : « Arbre » (avant : `arbre de judee`), « Autre » (avant : `autre conifere exotique`), « Inconnu », les pluriels (« Hêtres » : avant `hetre`), « Chênes ». « Feuillus », « Autres feuillus », « Résineux », « Autres résineux » ne passent pas par cette table : modèle gris feuillus/résineux, déclaré (avant, « Résineux » tombait sur `inconnu resineux sp`).
>
> Les noms déjà exacts dans la table (Hêtre, Douglas, Charme, Tremble, Merisier, Châtaignier, Houx, If, Buis, Pin sylvestre, Pin maritime, Pin d'Alep, Pin à crochets, Cerisier, Chêne rouge, Chêne sessile…) ne sont pas listés : ils ne changent pas.

| Nom saisi | Clé avant (coefficients) | Code avant | Clé proposée | Code | Raison / source |
|---|---|---|---|---|---|
| Chêne | `chene rouge` | `chene rouge` → CHR | `chene sessile` | CHS | Décision de Pascal (2026-11-27), comme NEMETON : Quercus robur_petraea / chêne sessile (Deleuze 2014, Tab. 1) ; BD Forêt « chênes décidus » |
| Sapin | `sapin pectine` | `sapin pectine` → S.P | `sapin pectine` | S.P | Seul sapin indigène ; BD Forêt « sapin ou épicéa » ; essence par défaut de l'application |
| Épicéa | `epicea commun` | `epicea commun` → EPC | `epicea commun` | EPC | Épicéa commun (Picea abies), seul épicéa forestier courant ; essence par défaut de l'application |
| Mélèze | `meleze d europe` | `meleze d europe` → MEE | `meleze d europe` | MEE | Seul mélèze indigène (Larix decidua) ; BD Forêt « mélèze pur » ; les mélèzes hybride et du Japon ne sont pris que saisis en toutes lettres |
| Érable | `erable plane` | `erable plane` → ERP | `erable sycomore` | ERS | Érable le plus abondant en forêt (IFN) ; à valider |
| Pin | `pin weymouth` | `pin weymouth` → P.W | `pin generique` | PEX | Pas d'espèce dominante (sylvestre, maritime, laricio, d'Alep se partagent la forêt) : ligne générique de la table gftools (code PEX), ou pas de rattachement si Pascal préfère |
| Pin noir | `pin noir d autriche` | `pin noir d autriche` → P.O | `pin noir d autriche` | P.O | Pinus nigra subsp. nigra, sous-espèce nominale |
| Pin laricio | `pin laricio de calabre` | `pin laricio de calabre` → PCA | `pin laricio de corse` | PCO | Calabre et Corse ont les mêmes coefficients ; Corse = provenance la plus plantée en France (BD Forêt « pin laricio ou pin noir ») |
| Laricio | `pin laricio de calabre` | `pin laricio de calabre` → PCA | `pin laricio de corse` | PCO | Nom court usuel, comme « Pin laricio » |
| Alep | `pin d alep` | `pin d alep` → P.A | `pin d alep` | P.A | BD Forêt « Alep pur » |
| Frêne | `frene commun` | `frene commun` → FRC | `frene commun` | FRC | Fraxinus excelsior, seul frêne forestier courant |
| Tilleul | `tilleul a petites feuilles` | `tilleul a petites feuilles` → TIP | `tilleul a petites feuilles` | TIP | Tous les tilleuls de la table ont les mêmes coefficients ; petites feuilles = tilleul forestier indigène courant |
| Bouleau | `bouleau pubescent` | `bouleau pubescent` → BOP | `bouleau verruqueux` | BOV | Les trois bouleaux ont les mêmes coefficients ; verruqueux (Betula pendula) = le plus répandu (IFN) |
| Peuplier | `peuplier autre cultive` | `peuplier autre cultive` → PEI | `peuplier autre cultive` | PEI | Peuplier de culture (cultivars) = l'essentiel du volume (peupleraies, BD Forêt « peupleraie »)  ; tremble et peuplier noir à saisir en toutes lettres |
| Peuplier tremble | `tremble` | `tremble` → TRE | `tremble` | TRE | Nom composé usuel : le tremble est une ligne de la table |
| Aulne | `aulne vert` | `aulne vert` → AUV | `aulne glutineux` | AUG | Aulne forestier de plaine (Alnus glutinosa), seul à fort volume |
| Robinier | `robinier faux acacia` | `robinier faux acacia` → ROB | `robinier faux acacia` | ROB | Seul robinier de la table ; BD Forêt « robinier pur » |
| Noyer | `noyer commun` | `noyer commun` → NOC | `noyer commun` | NOC | Juglans regia |
| Orme | `orme champetre` | `orme champetre` → ORC | `orme champetre` | ORC | Les ormes ont les mêmes coefficients ; champêtre = plus courant |
| Cèdre | `cedre de l atlas` | `cedre de l atlas` → CEA | `cedre de l atlas` | CEA | Les cèdres ont les mêmes coefficients ; cèdre de l'Atlas = le plus planté en France |
| Saule | `saule a trois etamines` | `saule a trois etamines` → SA3 | `saule generique` | SAU | Les saules ont les mêmes coefficients, pas d'espèce dominante : ligne générique de la table (code SAU) |
| Platane | `platane a feuilles d erable` | `platane a feuilles d erable` → PLA | `platane a feuilles d erable` | PLA | Platane commun (hybride) |
| Sorbier | `sorbier de finlande` | `sorbier des oiseleurs` → SOR | `sorbier des oiseleurs` | SOR | Les sorbiers ont les mêmes coefficients ; sorbier des oiseleurs = le plus courant et seul à code |
| Alisier | `alisier torminal` | `alisier torminal` → ALT | `alisier torminal` | ALT | Les alisiers ont les mêmes coefficients |
| Noisetier | `noisetier coudrier` | `noisetier coudrier` → NOI | `noisetier coudrier` | NOI | Seul noisetier de la table |
| Sureau | `sureau noir` | `sureau noir` → SUN | `sureau noir` | SUN | Les deux sureaux ont les mêmes coefficients |
| Cyprès | `cypres de l arizona` | `cypres de l arizona` → CAZ | `cypres de provence` | CTV | Cyprès commun en France (Cupressus sempervirens) |
| Thuya | `thuya du canada` | `thuya geant` → THU | `thuya geant` | THU | Thuja plicata, thuya forestier |
| Genévrier | `genevrier thurifere` | `genevrier thurifere` → GET | `genevrier commun` | GEC | Juniperus communis, seul courant |
| Pommier | `pommier sauvage` | `pommier sauvage` → POM | `pommier sauvage` | POM | Seul pommier de la table |
| Poirier | `poirier a feuilles d amandier` | `poirier a feuilles d amandier` → POA | `poirier commun` | POI | Pyrus communis ; mêmes coefficients |
| Marronnier | `marronnier d inde` | `marronnier d inde` → MAR | `marronnier d inde` | MAR | Seul marronnier de la table |
| Tulipier | `tulipier de virginie` | `tulipier de virginie` → TUL | `tulipier de virginie` | TUL | Seul tulipier de la table |
| Prunier | `prunier domestique` | `prunier domestique` → PRU | `prunier domestique` | PRU | Prunier cultivé ; mêmes coefficients |
| Mûrier | `murier blanc` | `murier blanc` → MUB | `murier blanc` | MUB | Mûrier blanc (Morus alba), le plus planté |
| Olivier | `olivier d europe` | `olivier d europe` → OLI | `olivier d europe` | OLI | Seul olivier indigène |
| Fruitier | `fruitiers divers` | `fruitiers divers` → FRU | `fruitiers divers` | FRU | Ligne « fruitiers divers » de la table (code FRU) |
| Fruitiers | `fruitiers divers` | `fruitiers divers` → FRU | `fruitiers divers` | FRU | Pluriel, même ligne |
| Charme commun | `charme` | `charme` → CHA | `charme` | CHA | Nom composé usuel : une seule ligne « charme » |
