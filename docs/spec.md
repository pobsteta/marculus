# Marculus — spécification consolidée

## 0. En-tête : état, légende, lecture

> **État de ce document.** Écrit le 2026-11-27 par l'`archiviste` (mission 045, plan validé par Pascal le 2026-11-27).
> C'est **la** spec de Marculus : elle remplace `docs/specs/2026-06-21-marculus-v1-spec.md` (conservée, datée, en
> historique) comme point d'entrée. Elle dit **ce qui est livré**, en une ligne par fonction, avec sa preuve ; le
> **détail** reste dans les specs de tranche (`docs/specs/*.md`), qu'elle cite et ne recopie pas.
>
> **Source relue, état dit** (`pilotage/pre-fusion.md`, point 6) : toutes les preuves de fichier et de ligne sont
> relues sur le commit **`909b1dd`** (`origin/main`, 2026-11-27, PR #99), copie de travail propre (`git status` vide).
> Le **code n'a pas changé** entre `a215c0a` (tag `v2026.11.7`, état d'une première relecture) et `909b1dd` : `git diff
> --name-only a215c0a 909b1dd` ne liste que `docs/adr/`, `pilotage/journal.md`, `pilotage/pre-fusion.md` et la fiche 044.
> « journal l. N » = `pilotage/journal.md` (les lignes citées, jusqu'à la l. 405, n'ont pas bougé : ajout seul ; le
> fichier compte 427 lignes à `909b1dd`) ; les numéros de PR et de
> commit viennent de `git log` et `git tag` du même état. **Non vérifié** = dit tel quel, à l'endroit du fait. Les
> faits repris des specs antérieures sans nouvelle lecture portent la mention « repris » et renvoient à leur spec.
> Rien du code n'est modifié par cette mission. Pour les fichiers que la mission 045 modifie (`README.md`, `CLAUDE.md`,
> `pilotage/kanban.md`, les deux specs de `docs/specs/` annotées), les numéros de ligne cités sont ceux d'avant la mission.
>
> **Légende** : ✅ livré · 🟡 partiel · 🔜 à faire · ⏸ livré dans le moteur mais pas branché dans l'application.
>
> **Comment lire.** § 1 à 4 : le produit et ses données. § 5 : le cubage (le plus mouvant). § 6 à 12 : les fonctions.
> § 13 : la technique et la livraison. § 14 : où sont les décisions et les règles. § 15 : ce qui reste à faire.

## 1. Produit, public, limites

- Marculus est une application Android hors ligne de **martelage forestier** : une feuille **essences × classes**,
  traçable tige par tige, avec cubage, carte GeoPackage et GNSS (`CLAUDE.md`, première phrase ; `README.md` l. 13-15).
- **Pour qui** : tous les marteleurs, ONF, experts forestiers, gestionnaires privés, communes affouagistes
  (`CLAUDE.md`). Les rôles (utilisateur, bénéficiaires, consommateur des données `nemetonshiny`, tarif NEMETON) sont dans
  `docs/brief.md`, « Rôles ». **Le problème du produit en une phrase est toujours vide** dans `docs/brief.md`
  (rubrique « Problème du produit », journal l. 401, 405) : **ouvert, à Pascal**.
- **Contraintes** : hors ligne total pour le geste de martelage, le cubage et les lots ; trois fonctions demandent le
  réseau (NTRIP, fonds de carte IGN/OSM, téléchargement du modèle de voix) ; sans publicité ; partage **par fichier**
  (`.marsync`, CSV), sans serveur (`docs/brief.md`, « Contraintes connues » ; ADR 0001). Journal des tiges en ajout
  seul, UUID par tige (`CLAUDE.md`).
- **Hors périmètre** : synchro réseau entre appareils ; plantation et travaux (c'est Surculus) ; calcul de
  référentiel IFN (les tables viennent de `nemeton`, Marculus les embarque) (`docs/brief.md`, « Ce qu'il ne fait pas »).
- **Licence** : GPL-3.0 ou ultérieure (`README.md`, « Licence »).

## 2. Vocabulaire

- **Contexte** = une opération de martelage ; il peut couvrir une parcelle ou une emprise sur plusieurs parcelles
  (spec de juin § 2, repris). La notion de « groupe » de l'app de référence n'existe pas.
- **GNSS**, jamais « GPS », dans l'interface et les textes (`CLAUDE.md`, « Règles métier »).
- **Tige** = une ligne du journal (`PLUS` ou `ANNULATION`) ; **net** = PLUS − ANNULATION après la règle d'annulation
  unique (§ 8). **Lot** = groupe de tiges d'affouage de volume borné (§ 6). **Classe** = tranche de diamètre ou de
  circonférence d'un contexte. **SER** = sylvoécorégion (§ 5.6).
- **Quatre volumes** EMERGE, chacun sous son nom (§ 5.2) : tige, houppier, bois fort total, volume aérien total.

## 3. Modèle de données

- Un **contexte** porte : mode (diamètre ou circonférence), axe de classes (min, max, pas), essences, tarif par défaut
  et numéro, coefficient de forme, date de martelage, état Kanban, affouage, volume maximal d'un lot, part de houppier
  par essence, tarif par essence, parties comptées au lot par essence, stères par m³, commune INSEE, SER, millésime
  des tarifs, chemin du GeoPackage (`core/.../model/Modeles.kt` l. 83-138). ✅
- Une **tige** porte : UUID, contexte, essence, classe, action (`PLUS` / `ANNULATION`), horodatage, quantité,
  texte de hauteur, qualité de l'arbre, position, opérateur, parcelle, qualité du fix GNSS, précision, `modifie`, lot
  (`Modeles.kt` l. 156-177). ✅ Le journal est en ajout seul : une annulation est une tige, jamais un effacement.
- **Base Room version 18** (`data/.../db/MarculusDatabase.kt` l. 63, `version = 18`), migrations 12 → 18
  (l. 11-55 ; `MIGRATION_17_18` l. 55, colonne `partsLot` ajoutée, mission 026). ✅ *Cette ligne remplace « schéma v17 »
  de la spec de juin § 9 (l. 306), corrigée par erratum daté dans cette spec.*
- Catégories de grosseur PB / BM / GB / TGB, seuils 27,5 / 47,5 / 67,5 cm (`core/.../model/SeuilsCategories.kt`,
  `CategorieBois.kt` ; valeurs : spec de juin § 3, **repris, non relu**). État Kanban `PROPOSEE → VALIDEE → PLANIFIEE →
  REALISEE` + `ABANDONNEE` (`Modeles.kt` l. 33). ✅
- Essences par défaut : six noms (Chêne, Hêtre, Autres feuillus, Sapin, Épicéa, Autres résineux ;
  `core/.../Referentiels.kt` l. 5-7, cité par `docs/specs/2026-11-27-essences-nemeton.md` l. 14). Qualités arbre
  et qualités bois : listes modifiables (`Referentiels.kt`, **repris**). ✅

## 4. Hauteur

- Saisie en **texte libre** structuré : `27` ou `27-6AB4CD` (hauteur, `-`, découpe) ; analyse tolérante, texte brut
  toujours gardé (`core/.../HauteurParser.kt` ; spec de juin § 4, **repris**). ✅
- **Estimation par MNH** : une couche `houppier` du GeoPackage donne la hauteur par point-dans-polygone sur la
  position GNSS (`core/.../EstimationHauteur.kt` l. 17, 31 ; détail : `docs/specs/couche-houppier-mnh.md`). ✅
  Aucune colonne ne distingue une hauteur estimée d'une mesurée : 🔜 (§ 15).

## 5. Cubage

### 5.1 Tarifs Schaeffer ✅

- Quatre tarifs à une entrée (diamètre) : rapide `M/1400·(D−5)(D−10)`, intermédiaire `M/1600·(D−2,5)(D−7,5)`, lent
  `M/1800·D(D−5)`, très lent `M/2025·D²`, avec `M = 0,8 + 0,1 × numéro` ; résultat borné à 0 par le bas
  (`Cubage.kt` l. 121-131, `l. 122` pour M). Le numéro de tarif est un **entier** (décision de Pascal, 2026-10-16) ; 0
  hors Schaeffer (ADR 0006).
- Le tarif Schaeffer donne le bois fort **tige** (découpe 7 cm, `Cubage.kt` l. 32).

### 5.2 EMERGE ✅

- Modèle à deux entrées (circonférence à 1,30 m, hauteur totale) d'après Deleuze et al. 2014 ; coefficients de
  `gftools::ListTarEmerge` dans `core/.../EmergeCoefs.kt` (en-tête l. 3 : « Généré depuis gftools::ListTarEmerge […]
  Ne pas éditer à la main ») : **429 noms** (français et latin) dans `EMERGE_COEFS` (recompté le 2026-11-27 par
  `grep -c '" to CoefEmerge'`). Le chiffre « 226 » est celui des **entrées EMERGE** de `gftools::ListTarEmerge` (226 × 92, journal l. 344, relevé
  par le relecteur dans R ; la table vient de `data/ListTarEmerge.rda`, **non suivi par git** dans la copie de `gftools`
  (`git status` : `?? data/ListTarEmerge.rda` ; HEAD 187ad39 ne suit que `data/ListTarEmerge.R` ; vérifié par l'orchestrateur le 2026-11-27)) : **non recompté dans le code**, qui en tire 429 noms.
- **Quatre volumes** (mission 094, PR #82, `1e7c0a0`, tag `v2026.11.4`) : `tige`, `houppier`, `boisFortTotal` (= tige +
  houppier, découpe 7 cm ; c'est ce que Marculus appelait « total » avant le 2026-11-27) et `volumeAerienTotal`
  (= `formTot × volCyl`, sans découpe) (`Cubage.kt` l. 142-147 ; calcul l. 157-191). Hors EMERGE, `volumeAerienTotal` vaut 0
  et n'est ni affiché ni exporté (KDoc l. 139-140). Brief : `docs/specs/2026-11-27-correction-emerge-volumes.md`.
  Vérifié sur le téléphone de Pascal (v2026.11.4, journal l. 327).
- **Contexte mixte** : le volume aérien total est la somme des seules tiges EMERGE ; l'écran dit « aérien total sur N
  tige(s) EMERGE sur M » quand N < M (`strings.xml` l. 195 ; `StatutHistoriqueScreen.kt` l. 283-285 ; journal l. 324,
  326). Une tige EMERGE sans hauteur vaut 0 m³ sans que Statut dise combien : carte du bac à sable (§ 15).
- **Trois règles qui sont des choix de Marculus** (aucune n'est une règle publiée ; détail et sources :
  `docs/specs/2026-11-27-emerge-regles-sans-source.md`, mission 040, PR #86) :
  1. part de tige `fct` **sans borne basse** ; tige ou houppier négatif ramené à 0 (`Cubage.kt` l. 177 : `coerceAtLeast(0.0)` pour la tige ; l. 190 :
     `houppier < 0 → 0`) ; décision « A » de Pascal, journal l. 370 ;
  2. **plafond continu `fct = min(fct, 0,95)`**, **0,95 provisoire** (`Cubage.kt` l. 174 et 195, `FCT_PLAFOND`) ;
     remplace « fct > 0,95 → 0,8 » (copie de travail non commitée de gftools, sans source publiée) ; décision « B »,
     journal l. 371 ; missions 041 (tests, `EmergePlafondFctTest`, 14 cas) et 042 (code), PR #90 (`795079e`, tag
     `v2026.11.6`). Effet : hêtre C 25 cm (circonférence, D ≈ 8 cm) / H 20 m, tige 0,0177 → 0,0210 m³ (journal l. 384) ;
     **les classes de martelage courantes ne bougent pas** (hêtre D 27,5 cm : fct ≈ 0,8, journal l. 393) ;
  3. **hauteur ≤ 1,4 m ou C ≤ 0 : tout à 0** (`Cubage.kt` l. 158) ; décision « A », journal l. 372 ; l'écran
     « non calculé » est prévu au cycle 11 (§ 15).
  Décision de fond : ADR 0012 (`docs/adr/0012-plafond-part-de-tige-emerge.md`, PR #98 `178275d`, fusionnée sur `main`).
- EMERGE : la part de houppier saisie est **ignorée** (grisée), gardée pour un retour en Schaeffer (`Cubage.kt`
  l. 249-262 ; journal l. 394).
- Repli : essence non couverte → coefficient de forme `V = f·π/4·D²·H`, `f` réglable par contexte, 0,5 par défaut
  (`Cubage.kt` l. 30, 265-271) ; noms « résineux » / « conifère » et « feuillu » → modèle gris par groupe (l. 100-109).

### 5.3 Part de houppier en Schaeffer ✅

- Part `p` par essence (0 à 95 %, `PART_HOUPPIER_MAX`, `Cubage.kt` l. 198) : houppier = `V_tige·p/(1−p)` (l. 254),
  total = tige + houppier. Champ grisé en EMERGE. Clé `.marsync` `partHouppier` (`ReglagesTerrain.kt` l. 19).

### 5.4 Tarif par essence ✅

- Chaque essence cochée porte son tarif, son numéro et sa part de houppier (`Contexte.tarifsEssence`, `Modeles.kt`
  l. 125 ; `Cubage.tarifDe` l. 207) ; une essence absente prend le tarif par défaut du contexte. Base v16, CSV format 6,
  clé `.marsync` `tarifsEssence` (spec de juin § 5, **repris**).

### 5.5 Numéro proposé par sylvoécorégion ✅

- Commune INSEE → SER → numéro IFN de chaque essence en Schaeffer ; cascade **SER → GRECO → France**, seuil **30
  arbres** (`TarifsIfn.kt` l. 153-165, `SEUIL_ARBRES` l. 286) ; jamais réécrit après saisie manuelle ; sans les tables, la
  section n'apparaît pas. Détail : `docs/specs/2026-10-16-tarifs-ser.md`. Tables dans `data/src/main/assets/tarifs-ifn/`
  (`communes_ser.csv`, `ser.csv`, `tarifs_ser.csv`).
- **Table de nemeton 2026.11.31** (mission 018 de nemetonplateforme, PR #80, `8255b8f`, tag `v2026.11.3`) : 14 752
  lignes, 1 590 numéros vides, aucun ≤ 0 (recomptés le 2026-11-27 sur `tarifs_ser.csv`, séparateur `;`, colonne `numero`) ; une ligne au numéro vide ou ≤ 0 garde sa `part_houppier`, le tarif l'ignore
  (`TarifsIfn.kt` l. 305-315 ; journal l. 314). Un numéro ≤ 0 n'est **jamais** un tarif.
- **« Autres feuillus » / « Autres résineux »** (missions 029 et 030, PR #62, `609910f`, tag `v0.73.0`) : proposition
  **calculée**, moyenne des feuillus (ou résineux) de la SER **pondérée par le nombre d'arbres**, hors essences listées
  à part dans le contexte, même cascade et même seuil ; feuillu si ESS < 50, résineux sinon, if et genévriers
  résineux (`TarifsIfn.kt` l. 87-100, 219-231) ; recalculée au cochage et décochage (`CreationContexteScreen.kt` l. 186).
  Les codes `29AF` / `68CE` ne sont **pas** reliés (décision de Pascal, journal l. 228 ; note
  `docs/sources/2026-11-10-ifn-29AF-68CE.md`). Décision de fond : ADR 0010. Le brief à `nemetonshiny` sur l'origine
  des numéros ≤ 0 est hors dépôt (journal l. 270).
- Mission 032 (proposition au changement de tarif) : **close sans code**, défaut non reproduit (journal l. 279-280).
- Échelon **forêt** de `tarifsProposes` (lots Nemeton) : 🔜, ADR 0003 (§ 15).

### 5.6 NEMETON ⏸

- Le **moteur du tarif NEMETON** est dans `:core` (`core/.../nemeton/`, 5 fichiers) : paquet `0.2.2-provisoire`
  (`core/src/main/resources/nemeton/datapackage.json` l. 4), constantes et jeu de contrôle de 1 032 lignes
  (`jeu_controle.csv`, 1 033 lignes avec l'en-tête). Il **refuse** un paquet dont l'empreinte SHA-256 ne correspond pas
  (`PaquetNemeton.kt` l. 11, 31-33). Missions 038 (tests) et 039 (moteur), PR #87 (`79d2f22`, tag `v2026.11.5`) ;
  1 032 / 1 032 cas verts, tolérance `max(1e-6 relatif, 5,0001e-9 absolu)` (`PaquetDeTest.kt` l. 10-11 : `TOL_ABSOLUE = 5.0001e-9`, validée par
  Pascal, journal l. 364).
- **Entrées impossibles refusées** : D ≤ 0, H ≤ 0, résineux H ≤ 1,301 m → `IllegalArgumentException`
  (`TarifNemeton.kt` l. 57-64 et 179), option « A » de Pascal (journal l. 365). Écart connu au brief NEMETON (« pas de
  refus ») : dit à `tarif-nemeton` par le brief déposé le 2026-11-27 (mission 043, journal l. 388 ;
  `~/dev/briefs/vers-tarif-nemeton/2026-11-27-marculus-refus-et-repli-d5.md`) ; **pas de réponse reçue** au
  2026-11-27.
- **Pas branché** : aucun fichier de `:app` ni de `:data` n'appelle `TarifNemeton` ni `PaquetNemeton` (`grep -rl`, vide).
  Le résultat reste **provisoire** tant que le paquet l'est. Le branchement (écran, exports, lots, « À propos ») est le
  cycle 11 (§ 15). Décision : ADR 0011 (`docs/adr/0011-nemeton-embarque-provisoire.md`, PR #98 `178275d`, fusionnée sur `main`).
- **Codes IFN des essences** (mission 037, PR #85, `04c5bf2`) : table de revue de 59 noms et onze décisions de Pascal
  (Chêne 3, Autres feuillus 49, Autres résineux 75 non couvert, Érable 15, Frêne 17, Peuplier 19, Sorbier 23, Pin nu sans code ;
  pins 51/53/54/57/58 repliés sur 52 et chêne rouge sur `Quercus_spec`, « l'essence réellement utilisée accompagne
  chaque résultat »), dans `docs/specs/2026-11-27-essences-nemeton.md` § « Décisions de Pascal » ; journal l. 353-363.
  La règle de repli est une **valeur provisoire** (même spec l. 21-30). **Non branchée**.

### 5.7 Rattachement des noms d'essence ✅

- Un nom saisi est rattaché à une clé EMERGE **par une table écrite à la main**, sans préfixe ni sous-chaîne
  (`RATTACHEMENT_ESSENCES`, `Cubage.kt` l. 53-96, **41 clés** recomptées le 2026-11-27 : 39 noms courts et 2 noms
  latins) ; « Chêne » → chêne sessile (décision de Pascal, journal l. 324). Table de revue :
  `docs/specs/2026-11-27-rattachement-essences.md`. Test : `RattachementEssencesTest`. Sans rattachement : Arbre,
  Autre, Inconnu, pluriels (journal l. 325).
- **Code ONF à 3 caractères** de l'essence : `Cubage.codeEssence` (l. 114) depuis `EMERGE_CODES`
  (`EmergeCoefs.kt` l. 447) ; sert à l'affichage de la feuille (`FeuilleMartelageScreen.kt` l. 262), à la dictée et au
  numéro IFN (`TarifsIfn.kt` l. 155).

## 6. Affouage

- Case **Affouage** sur le contexte et **volume maximal d'un lot** (m³) ; lots numérotés à partir de 1 ; le lot est
  fermé par la tige qui atteint ou dépasse le maximum, **figé sur la tige** (`Tige.lot`), annoncé à chaque tige
  (« Chêne 35, lot numéro 3 », « lot 3 complet »). Exige au moins une essence cubée. ✅ Détail :
  `docs/specs/2026-10-07-contexte-affouage.md` ; code : `AffouageLots.kt` (`volumes` l. 30, `bilan` l. 56, `etat` l. 82).
- **Règle du lot : les parties cochées** (missions 026 et 027 : PR #58 `07b5870`, tag `v0.71.0`, et PR #60 `85030ff`, tag `v0.72.0`) : le lot
  d'une essence compte sa **tige**, son **houppier**, ou les deux, les deux cochées par défaut ; cases « T » et
  « H » sur la ligne de l'essence (`Modeles.kt` l. 55 `PartsLot` ; `AffouageLots.kt` l. 20, 32 ; `LotEssence.kt` l. 14).
  Remplace « houppier seul » (ADR 0004). Décision : ADR 0009. Un lot dont les deux cases sont décochées vaut 0 m³ ;
  aucun signal pour un lot qui dépasse sa borne après la mise à jour (journal l. 231). Essai sur le téléphone : conforme
  (journal l. 257).
- **Stères** : champ stères lié au m³ par `steresParM3` (1,5 par défaut) ; seuls les m³ bornent le lot ; Statut et
  CSV du bilan affichent « 12,34 m³ (18,51 st) » (`Steres.kt` l. 11-15 ; mission 017, PR #35 `0130bd6`). ✅
- **Annulation orpheline** : « Par lot » et le CSV du bilan l'ignorent (3e exception décidée, journal l. 210) ; Statut et
  « Par parcelle » la comptent en négatif.

## 7. Écrans

1. ✅ **Liste des contextes** : cartes (nom, nombre **net** de tiges, date, état Kanban) ; tri par date, recherche, vue
   Kanban à cinq colonnes ; partage `.marsync`. Nombre net : mission 018, PR #42. **« 1 tige » / « 1 essence » au
   singulier** (mission 033, PR #68, `b7aafc8`, tag `v0.73.1`) : pluriels `liste_carte_tiges` et `liste_carte_essences`
   (`strings.xml` l. 47-54), test de la phrase complète FR et EN.
2. ✅ **Créer / modifier un contexte** : nom, mode, axe, essences, tarifs, affouage, commune. **Virgule décimale** en
   français dans « % houpp. », stères et volume maximal (mission 034, PR #69, `ad3103d`, tag `v0.73.2` ;
   `ui/contextes/FormatNombre.kt`, langue de l'écran passée aux 8 appels, `locales[0]`). Décimales 2 contre 1 et
   coefficient de forme « 0.5 » : au bac à sable.
3. ✅ **Feuille de martelage** (cœur) : colonnes essences, lignes classes ; cellule `−` / `+`, boutons Hauteur et Qualité ;
   pas de remise à zéro ; boutons de volume optionnels ; code essence lisible en option.
4. ✅ **Statut / historique** : onglets Statut, **Par lot** (affouage seul), **Par parcelle**, Historique
   (`StatutHistoriqueScreen.kt`). « Par parcelle » au net (mission 019, PR #43) ; ligne **« sans position »** avec volumes
   (mission 021, PR #46 ; `ParParcelle.kt` l. 53) ; onglets lisibles, texte réduit jusqu'à 9 sp (mission 020, PR #44 ;
   `LibelleOnglet.kt`).
5. ✅ **Carte** : parcelles, desserte, ortho, position GNSS, rattachement (§ 11).
6. ✅ **Paramètres** : anti-veille, plein écran, vibration, son, annonce vocale, thème (Système / Clair / Sombre, ADR 0005),
   langue (FR / EN), GNSS, MNH, opérateur, export et import ZIP, export CSV, fusion `.marsync`.
7. ✅ **Référentiels** : édition des essences, qualités arbre, qualités bois.
8. ✅ **Dictée vocale** sur la feuille (§ 12).
- **Thème** : les 48 rôles de couleur viennent de `terrain-style` v0.7.1 (`Theme.kt`, `ThemeTest` ; mission 018 de
  nemetonplateforme, journal l. 315-316) ; Marculus garde `VertVif` en `primary`. La carte 006 reste suspendue (ADR 0005).
- **« À propos »** : ne cite ni la source d'EMERGE (Deleuze et al. 2014) ni le tarif NEMETON en v2026.11.5 ; décision de les
  ajouter au branchement (journal l. 368). 🔜

## 8. Comportement d'une cellule et règle d'annulation

- `+` ajoute une tige `PLUS` (hauteur et qualité demandées si obligatoires ; position GNSS si le ponctuel est actif).
  `−` enregistre une `ANNULATION` (spec de juin § 7, **repris**).
- **Une seule règle d'annulation** (mission 022, PR #49, `76bd939`) : une annulation retire la dernière tige de sa case,
  dans `JournalNet` seul (`core/.../JournalNet.kt` : règle dite l. 8-14, code l. 18-41) ; les comptes (Statut, liste, boîte « Ouvrir avec ») sont des
  sommes signées égales au net par test (mission 025, PR #55, `7f66180`, `EgaliteDesTotauxTest`, 18 tests). Décision :
  ADR 0007.

## 9. Exports et formats d'échange

- **CSV** : format courant **9** (`ExportCsv.kt` l. 57). Historique : 2 (réimportable par Nemeton) ; 3 volumes ; 4
  affouage ; 5 houppier ; 6 tarif par essence et stères ; 7 commune, SER, millésime ; 8 `PartsLot` (mission 026) ; **9** :
  `VolumeTotalM3` devient `VolumeBoisFortTotalM3`, ajouts `VolumeAerienTotalM3` et `NbTigesVolumeAerien` (vides hors
  EMERGE) (`ExportCsv.kt` l. 17-52, 74-101, 126-132 ; `CsvFoncier.kt` l. 38 : `BoisFortTotal_m3`, `AerienTotal_m3`).
  L'export offre « journal complet » ou « tiges à comptabiliser » (`Journal;NET`).
- **`.marsync`** (JSON) : clés `volumeBoisFortTotalM3`, `volumeAerienTotalM3`, `nbTigesVolumeAerien`
  (`SauvegardeRepository.kt` l. 195-199) ; `LectureVolumes` relit l'ancien `VolumeTotalM3` / `volumeTotalM3` comme
  **bois fort total**, jamais comme aérien total (`LectureVolumes.kt` l. 4-10).
- **Contrat avec `nemetonshiny`** : tout changement de format se signale par brief, avec un numéro de format
  (`docs/brief.md`, « Contraintes connues »). Le format 9 : **aucun brief trouvé** (`grep -rli 'format 9\|VolumeAerienTotal\|FormatCsv;9' ~/dev/briefs`, vide le
  2026-11-27) ; le journal ne dit pas qu'il a été écrit (l. 319, 322) : **trou de mémoire, à Pascal** (§ 15).
- Les exports sont écrits par `ExportFichier` dans `Download/Marculus`, par MediaStore (`app/.../ExportFichier.kt` l. 23, 31, 38) ; **Android 8-9** (pas d'`IS_PENDING`, `Build.VERSION_CODES.Q` l. 37) : repli vers le dossier de l'application, `Android/data/…/files/Download/Marculus/` (même fichier, l. 22-23, 40).

## 10. Synchronisation par fichier

- **Fusion `.marsync`** dans une transaction (`MergeDao`) : union par UUID, « dernière écriture gagne » ; ne touche
  jamais un contexte absent du fichier (spec de juin § 8, **repris**). ADR 0001.
- **Réglages du terrain préservés** : une clé absente du fichier garde la valeur locale ; 13 clés de contexte et le lot
  des tiges (`ReglagesTerrain.kt` l. 17-22 pour les clés), dont `partsLot` (mission 026).
- **« Ouvrir avec » / « Partager »** (mission 016, PR #36, `dbad43c`) : boîte « Fusionner ? » avec nombre **net** de tiges ;
  plafond de 50 Mo (`FichierMarsync.kt` l. 75) ; `launchMode="singleTask"` (`AndroidManifest.xml` l. 40). Essai A → B fait
  le 2026-11-10 sur le téléphone de Pascal (v0.70.5) et l'émulateur : « c'est pareil » (journal l. 217-218). ✅
- **Import d'un lot de chantiers (.zip)** : contextes fusionnés et GeoPackages rattachés (`ui/lot/ImportLot.kt` ; détail :
  `docs/specs/2026-08-23-import-lot-zip.md`). ✅
- Synchro temps réel / réseau : 🔜, **hors périmètre actuel**.

## 11. Carte et géographie

- **GeoPackage OGC** ; tout CRS déclaré est reprojeté en WGS84 / EPSG:4326 (`docs/specs/couches-gpkg.md`). Tables : parcelles
  (toute autre table), `houppier`, `desserte`, ortho tuilée. ✅
- **Fonds** : Plan IGN (par défaut), Ortho IGN, OpenStreetMap, ortho du chantier hors ligne (`CarteScreen.kt` l. 189-192) ;
  cadastre IGN en couche. La carte charge des tuiles en ligne dès l'ouverture (journal l. 302). ✅
- **Rattachement** point-dans-polygone, aires géodésiques, tige hors parcelle comptée « Hors parcelle » (`README.md`).
  Pas de badge GNSS sur la carte (refus de Pascal, 2026-09-23 ; source : mémoire de session, **non vérifié dans le dépôt**).
- **Foncier structuré** (Propriétaire → Forêt → Parcelle géré en propre) : 🔜.

## 12. GNSS et dictée vocale

- **GNSS ponctuel** (au clic) et continu ; permission demandée à l'exécution. ✅
- **GNSS externe RTK / NTRIP** : transport Bluetooth SPP ou TCP, client NTRIP, service de premier plan, qualité du fix et
  précision tracées sur la tige (`app/.../gnss/` : `ClientNtrip.kt`, `TransportBluetoothSpp.kt`, `TransportTcp.kt`,
  `ServiceGnssRtk.kt`) ; **validé sur récepteur u-blox F9P** le 2026-08-22 (`docs/specs/2026-06-24-tranche-rtk-ntrip.md`,
  en-tête ; **non rejoué** le 2026-11-27). ✅
- **Agent NTRIP** : `ClientNtrip.kt` l. 44 et 110 appellent `Ntrip.requete(...)` **sans** `agent`, ce que la prochaine
  version de `terrain` exige ; sans effet tant que `terrain` reste en `v0.5.0` (`gradle/libs.versions.toml` l. 18 ; brief
  `~/dev/briefs/vers-marculus/2026-11-20-terrain-agent-ntrip-obligatoire.md`). 🔜 (§ 15)
- **Dictée vocale** hors ligne (Vosk, grammaire fermée, push-to-talk par bouton ou volume bas) : « hêtre quarante cinq
  chablis » ; modèle français téléchargé à la demande (~42 Mo) (`app/.../voice/`, `core/.../voice/` ; détail :
  `docs/specs/2026-08-21-tranche-dictee-vocale.md`). ✅ Recette de terrain (7 cas, bruit, gants) : 🔜.
  `docs/specs/BRIEF-claude-code-voice.md` est le brief d'origine de cette tranche : **historique**.

## 13. Technique, livraison, outils

- **Architecture** : `:core` (domaine Kotlin pur, testé JVM), `:data` (Room, DataStore, GPKG, synchro), `:app` (Compose,
  Material 3) ; minSdk 26, targetSdk 36 (`app/build.gradle.kts` l. 14-15). Bibliothèque commune `terrain`
  (`terrain-gnss`, `-journal`, `-voix`, `-carte`, `-gpkg` en `v0.5.0` ; `-style` en `v0.7.1` : `libs.versions.toml`
  l. 18-19, 30-35) ; on corrige là-bas (ADR 0002). Seul `terrain-style` est monté : monter les autres casse
  `voice/ModeleVosk.kt` l. 29 (journal l. 316).
- **Tests** : commande `.claude/test-command` (`:core:test :data:testDebugUnitTest :app:testDebugUnitTest`). Au
  2026-11-27 : core 1 790, data 86, app 29 (journal l. 384, avant les missions 044-047 ; **non rejoué** pour ce
  document).
- **CI et releases** : au merge sur `main`, `ci.yml` teste, puis `release.yml` publie un APK signé (`workflow_run`) ;
  **pas de release** si seuls `pilotage/`, `docs/`, `*.md` ou badges changent, ni pour des tests seuls, ni pour un commit
  dépassé par la tête de `main` (`.github/scripts/release-necessaire.sh` ; ADR 0008 ; missions 023 et 031, PR #51 et #67
  `fe01907`). Détail opérationnel : `README.md`, « Déployer et publier ».
- **Versions calendaires** `vAAAA.MM.N` (année et mois UTC, compteur du mois repart à 1 ; `.github/scripts/version-calendaire.sh`
  l. 1-12 ; mission 036, PR #77 `a28dc2d`, tag `v2026.11.1`) ; les anciens `v0.*` restent. **Elles remplacent** les « releases
  SemVer automatiques » de la spec de juin § 9 (l. 314, erratum). Décision : ADR 0013 (`docs/adr/0013-versions-calendaires.md`, PR #98 `178275d`, fusionnée sur `main`).
  **Question ouverte** : le brief de `terrain` du 2026-11-20 annonce la notation `vAAAA.M.N` (mois sans zéro) de la
  plateforme ; Marculus écrit `vAAAA.MM.N` : à Pascal (§ 15).
- **Titre de la release** = la version seule, sans « Marculus » (`release.yml` l. 115 ; mission 047, PR #96, `a215c0a`,
  tag `v2026.11.7`). Les releases déjà publiées ne sont pas renommées sans l'accord de Pascal.
- **Distribution** : APK signé sur GitHub Releases ; fiche **IzzyOnDroid** (`fastlane/metadata/android/{fr-FR,en-US}/`, 16
  fichiers ; `FicheFastlaneTest` ; mission 035, PR #73 `2afeef7` et #76 `309b83e`, tags `v0.73.3` et `v2026.11.2`). La demande
  d'inclusion sur Codeberg revient à Pascal (journal l. 312) ; **sa suite est inconnue** (aucune ligne du journal après le
  2026-11-11). La clé de signature n'est pas sauvegardée hors de ce poste : décision de Pascal « sur clé USB », copie à faire
  (journal l. 294 ; `README.md`, « Clé de signature »).
- **Logiciels libres, formats ouverts** : CSV, JSON, GeoPackage ouverts, code sous GPL-3.0 (`README.md`, « Licence »).
  Le test « libre ne veut pas dire éternel » (fiche n° 11, outil par outil) **n'a pas été passé** dans cette mission :
  à faire au bilan n° 4. Deux briques à surveiller d'emblée : `terrain` (ADR 0002, une correction se fait là-bas) et
  `gftools`, dont la règle 2 d'EMERGE n'existe que dans une copie de travail non commitée (journal l. 340).

## 14. Décisions, règles, mémoire

- **Décisions d'architecture** : `docs/adr/` (index : `docs/adr/README.md`). ADR 0001 à 0013 sur `main` ; **0011** (NEMETON
  embarqué provisoire), **0012** (plafond de la part de tige EMERGE), **0013** (versions calendaires) : mission 046, PR #98
  (`178275d`), fusionnée.
- **Décisions de Pascal** : `pilotage/journal.md` (ajout seul) ; une décision de fond se dit « décision » ou « cap »
  (journal l. 398).
- **Sources de vérité** : table de `CLAUDE.md` ; avancement : `pilotage/kanban.md` ; missions : `pilotage/missions/` ;
  équipe : `pilotage/equipe.md` ; liste avant fusion : `pilotage/pre-fusion.md`.
- **Specs de tranche** (le détail, gardées) : `docs/specs/2026-06-24-tranche-rtk-ntrip.md`,
  `2026-08-21-tranche-dictee-vocale.md`, `2026-08-23-import-lot-zip.md`, `2026-10-07-contexte-affouage.md`,
  `2026-10-16-tarifs-ser.md`, `couches-gpkg.md`, `couche-houppier-mnh.md`, et les quatre du 2026-11-27
  (`correction-emerge-volumes`, `emerge-regles-sans-source`, `essences-nemeton`, `rattachement-essences`).
- **Specs historiques** (gardées, datées, non effacées) : `2026-06-21-marculus-v1-spec.md` (remplacée par ce document) ;
  `BRIEF-claude-code-voice.md` (brief d'origine de la dictée).
- **Briefs entrants** : `~/dev/briefs/vers-marculus/`, relevée à chaque `/boucle` (point 7 de `pre-fusion.md`, en vigueur
  sur `main` : mission 044, PR #97 `dc56377`).

## 15. Reste à faire

- 🔜 **Branchement de NEMETON** (cycle 11) : modèle, écran Statut, exports, lots, « À propos », volumes montrés quand une
  sortie manque, « hauteur impossible » / « non calculé » (kanban, bac à sable ; ADR 0011).
- 🔜 **Monter `terrain`** (épinglé `v0.5.0`) : agent NTRIP obligatoire, versions calendaires de `terrain`.
- 🔜 **Statut dit combien de tiges EMERGE n'ont pas de volume faute de hauteur** ; **aérien total négatif** des perches très
  élancées (hêtre C 10 cm, H 45 m : −0,00488 m³, journal l. 382-383).
- 🔜 Échelon **forêt** de `tarifsProposes` (ADR 0003, non livré).
- 🔜 Traçabilité des hauteurs estimées (MNH) contre mesurées ; foncier structuré ; synchro réseau ; bouton Flic 2.
- 🔜 Recettes terrain : affouage, dictée ; démo des tarifs SER en mode avion.
- 🔜 Captures de la fiche IzzyOnDroid à refaire ; demande d'inclusion à suivre.
- **Trous de mémoire ouverts** : brief du format CSV 9 vers `nemetonshiny` (§ 9) ; problème du produit en une phrase
  (§ 1) ; réponse de `tarif-nemeton` (§ 5.6) ; sauvegarde de la clé de signature (§ 13) ; notation `vAAAA.M.N` contre
  `vAAAA.MM.N` (§ 13).
