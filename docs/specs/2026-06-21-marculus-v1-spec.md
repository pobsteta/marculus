# Marculus — Spécification

> Application Android (smartphones + tablettes) de **martelage forestier**, inspirée de
> l'app de référence « Compteur Intelligent » (multicounter), transposée en
> **feuille de martelage**. Écrans de référence : `docs/ecran/`.
>
> Spec figée le 2026-06-21 après cadrage. On **repart de zéro** (aucun code hérité).
> **Mise à jour 2026-06-24** : l'avancée a largement dépassé le périmètre v1 « comptage seul ».
> La v1 est livrée et l'essentiel de la v2 (cubage EMERGE, géo/GPKG, GNSS, synchro par fichier)
> l'est aussi.
> **Mise à jour 2026-08-22** : le **GNSS externe RTK/NTRIP** est livré **et validé sur récepteur
> u-blox F9P** (la tranche complète est détaillée dans `2026-06-24-tranche-rtk-ntrip.md`) ; la
> tranche **dictée vocale hors ligne** (Vosk, grammaire fermée) est livrée
> (`2026-08-21-tranche-dictee-vocale.md`).
> **Mise à jour 2026-10-07** : **contexte d'affouage** livré (v0.52.0 → v0.55.1) : lots de volume
> borné annoncés à chaque tige, bilan par lot, CSV `FormatCsv;4`, fusion qui préserve les
> réglages du terrain (`2026-10-07-contexte-affouage.md`).
> Légende d'état : ✅ fait · 🟡 partiel · 🔜 à faire.

## 1. Périmètre

- ✅ **v1** : comptage traçable en **feuille de martelage** (matrice essences × classes),
  journal d'événements append-only, statut/historique, export.
- 🟡 **v2** : import **GPKG** des parcelles, carte (tuiles GPKG), **rattachement spatial**
  des tiges aux parcelles, **GNSS** ponctuel, **synchro multi-opérateurs**.
  - ✅ Import/affichage GPKG + carte, rattachement point-dans-polygone, aires géodésiques.
  - ✅ GNSS ponctuel (acquisition à la demande, permission, terminologie « GNSS »).
  - ✅ Fonds de carte **OSM + satellite ESRI** en ligne + **ortho GPKG** hors-ligne (bascule).
  - ✅ Synchro multi-opérateurs **par fichier** (`.marsync`, fusion « dernière écriture gagne »).
  - ✅ GNSS externe **RTK/NTRIP** (type projet Centipede) : transport BT SPP / TCP, client
    NTRIP, service de premier plan, qualité de fix et précision tracées sur la tige —
    **validé sur récepteur u-blox F9P**.
  - ✅ **Dictée vocale hors ligne** des tiges (Vosk, grammaire fermée, push-to-talk).
  - ✅ **Couches typées du GPKG** (`couches-gpkg.md`) : parcelles, `houppier` (estimation de la
    hauteur par MNH), `desserte` (routes/pistes/chemins sur la carte), ortho.
  - ✅ **Import d'un lot de chantiers** (.zip Nemeton) : fusion des contextes et rattachement
    automatique des GeoPackages (`2026-08-23-import-lot-zip.md`).
  - ✅ **Affouage** : contexte dont les tiges sont rangées en lots de volume borné
    (`2026-10-07-contexte-affouage.md`, §5 bis).
  - 🔜 Synchro **temps réel / réseau** entre appareils.
- Contraintes : **hors-ligne total**, **sans publicité**. Le partage reste **par fichier**
  (pas de serveur).

## 2. Vocabulaire métier

- La notion de « groupe » de l'app de référence **disparaît**.
- **Contexte** = une **opération de martelage**. Peut couvrir une parcelle dans une forêt
  (cas par défaut) ou une **emprise** traversant plusieurs propriétaires / parcelles.
- Foncier (référence) : `Propriétaire → Forêt → Parcelle`, contours importés depuis un
  **GPKG**. ✅ Rattachement d'une tige **déduit de la position GNSS** (polygone contenant le
  point) **si la position est activée**.

## 3. Modèle de données

```
Contexte        : id, nom, mode (DIAMETRE | CIRCONFERENCE),
                  classeMin, classeMax, classePas, essencesActives[],
                  tarifCubage, coefForme, état Kanban, dateMartelage,
                  affouage, volumeMaxLotM3,
                  dateCréation, opérateur, modifie (synchro)
Référentiels    : Essence       — Chêne / Hêtre / Autres feuillus / Sapin / Épicéa /
                                   Autres résineux            (prédéfini, modifiable)
                  QualitéArbre   — Sec / Chablis / Volis / Malade   (prédéfini, modifiable)
                  QualitéBois    — libellés A / B / C / D et combinaisons AB, BC, CD…
                                   (prédéfini, modifiable ; utilisés dans le texte hauteur)
Compteur        : clé = (contexteId, essence, classe)
                  hauteurObligatoire?, qualitéObligatoire?
                  (réglable par compteur, avec action « appliquer à tous »)
Tige (journal)  : uuid, contexteId, essence, classe,
                  action (PLUS | ANNULATION), quantite,
                  hauteurTexte?   (texte libre, ex. « 27-6AB4CD »),
                  qualitéArbre?   (mono-choix),
                  position?       (lat/lon brut, nullable),
                  lot?            (lot d'affouage, figé au martelage),
                  horodatage, opérateur, modifie (synchro)
```

- **Journal append-only** : un `+` crée une tige `PLUS` ; un `−` crée une tige
  `ANNULATION` (jamais d'effacement). **Totaux dérivés** : cellule = nb `PLUS` − nb `ANNULATION`.
- L'**axe des classes** (min / max / pas) est fixé **par contexte** et **commun à toutes les
  essences** → grille rectangulaire. Défauts proposés : diamètre 20→90 pas 5 ;
  circonférence pas 5 ou 10 (modifiable).
- ✅ **Catégories de grosseur** (PB / BM / GB / TGB) dérivées de la classe via des seuils
  de diamètre paramétrables (27,5 / 47,5 / 67,5 cm par défaut ; conversion ÷π en circonférence).
- ✅ **Statut Kanban** par contexte : `PROPOSEE → VALIDEE → PLANIFIEE → REALISEE` (+ `ABANDONNEE`),
  avec règle de plancher (présence de tiges → au moins Planifiée ; export → Réalisée).

## 4. Format de saisie de la hauteur

- Texte **libre, sans contrôle de cohérence**, mais structuré : **hauteur d'abord**, puis
  le séparateur **`-`**, puis **texte libre** de découpe.
  - `27` → arbre de 27 m, sans détail.
  - `27-6AB4CD` → arbre de 27 m, dont 6 m de qualité bois **AB** + 4 m de qualité bois **CD**
    (le reste non détaillé).
- ✅ Parsing tolérant en domaine pur (`HauteurParser`) : hauteur totale + segments de découpe
  (longueur + qualité bois), texte brut toujours conservé.

## 5. Cubage (v2, livré)

- ✅ Tarif réglable **par contexte** (`TarifCubage`) :
  - `AUCUN`
  - `SCHAEFFER_RAPIDE` — V = (M/1400)(D−5)(D−10)
  - `SCHAEFFER_LENT`   — V = (M/1800)·D·(D−5)
  - `EMERGE` — **vrai volume bois fort tige** (modèle + coefficients gftools/EMERGE),
    avec **part de tige** et **houppier** → **volume total aérien**.
- ✅ Table EMERGE **complète (226 essences)** générée depuis `gftools::ListTarEmerge`
  (`EmergeCoefs.kt`, ne pas éditer à la main) ; replis feuillus/résineux.
- ✅ **Coefficient de forme** réglable par contexte comme repli EMERGE pour une essence
  non couverte : V = f·π/4·D²·H.

## 5 bis. Affouage (livré 2026-10-07)

- ✅ Case **Affouage** sur le contexte + **volume maximal d'un lot** (m³, bois fort tige).
  Exige un tarif de cubage (≠ `AUCUN`) : sans volume par tige, aucun lot ne se ferme.
- ✅ Lots numérotés **à partir de 1** : le volume de chaque tige est cumulé dans le lot ouvert ;
  dès que le cumul est **≥ au maximum**, la tige qui l'atteint **ferme** son lot et la suivante
  ouvre le lot d'après. Un `−` juste après la tige qui a fermé un lot le rouvre.
- ✅ Le lot est **figé sur la tige** (`Tige.lot`), comme la parcelle : il a été dit et marqué sur
  l'arbre, rien ne le renumérote. Seul l'état courant (lot ouvert, cumul) est dérivé du journal
  (`AffouageLots`, domaine pur).
- ✅ **Annonce** à chaque tige, quels que soient les réglages d'annonce : « Chêne 35, lot
  numéro 3 » ; la tige qui ferme le lot ajoute « lot 3 complet ».
- ✅ Onglet **« Par lot »** de Statut / historique (tiges et m³ nets par lot, complet / en cours,
  total) et **export CSV du bilan par lot** (rapport, décimaux de la langue).
- En EMERGE, une tige sans hauteur vaut 0 m³ : Schaeffer est le choix naturel pour l'affouage.

## 6. Écrans

1. ✅ **Liste des contextes** — cartes (nom, nb tiges, date, état Kanban) + bouton créer ;
   tri par date de martelage, filtre de recherche, **vue Kanban 5 colonnes** (glisser-déposer),
   partage `.marsync`.
2. ✅ **Créer / éditer un contexte** — nom · diam/circ · min/max/pas · essences actives ·
   obligation hauteur/qualité · tarif de cubage · coefficient de forme · date de martelage ·
   **affouage** (volume maximal d'un lot).
3. ✅ **Feuille de martelage** *(cœur)* — matrice **colonnes = essences**, **lignes = classes** ;
   cellule = **valeur**, **`−` / `+`**, **bouton Hauteur**, **bouton Qualité arbre** ;
   **pas de reset** ; défilement vertical (classes) + horizontal (essences) ;
   boutons de volume optionnels ; code essence lisible en option.
4. ✅ **Statut + Historique** — restitution par essence/classe et journal détaillé des tiges
   (horodatage, action, valeur cumulée) ; onglets Statut · **Par lot** (affouage seulement) ·
   Par parcelle · Historique. *(écran `StatutHistoriqueScreen`)*
5. ✅ **Carte** — affichage des parcelles importées (tuiles GPKG), position GNSS,
   rattachement spatial. *(écran `CarteScreen`, `GpkgTileModule`)*
6. ✅ **Paramètres** — anti-veille, plein écran, vibration, son de clic, **annonce vocale**
   (nombre, étiquette, avis limite inf./sup. ; le lot d'affouage est toujours annoncé), boutons de volume, thème sombre, langue,
   GNSS ponctuel, opérateur, **Export/Import ZIP**, **Export CSV**, **fusion `.marsync`**.
7. ✅ **Référentiels** — édition des listes Essences / Qualité arbre / Qualité bois.
8. ✅ **Dictée vocale** *(sur la feuille de martelage)* — push-to-talk (volume bas maintenu ou
   bouton micro), grammaire fermée générée depuis le contexte, annonce TTS de confirmation,
   aide « Formes à dicter » au menu ⋮. Modèle Vosk téléchargé depuis les Paramètres.

## 7. Comportement d'une cellule (feuille de martelage)

- **`+`** : ajoute une tige `PLUS`. Si hauteur/qualité **obligatoires** pour ce compteur →
  saisie demandée immédiatement. Si **GNSS ponctuel** actif → acquisition de la position.
- **`−`** : enregistre une **annulation** (conservée au journal), décrémente le total.
- **Bouton Hauteur / Qualité arbre** : annote la **dernière tige** ajoutée de la cellule
  (saisie hauteur en texte libre / sélection mono-choix de la qualité arbre).
- **Export CSV** (`FormatCsv;4`) : feuille (totaux par essence × classe) **+** journal des
  tiges (+ volumes cubés selon le tarif du contexte, + colonne `Lot`). À l'export, choix entre
  **journal complet** et **tiges à comptabiliser** (`Journal;NET` : chaque annulation disparaît
  avec la tige qu'elle retire ; totaux identiques).

## 8. Synchro multi-opérateurs (v2, partiel)

- ✅ Chaque entité porte un horodatage `modifie` ; chaque tige porte l'`opérateur`.
- ✅ **Fusion par fichier** (`.marsync`, JSON) dans **une transaction atomique** (`MergeDao`) :
  union par UUID, **« dernière écriture gagne »** (l'entrant remplace le local s'il est plus
  récent), insertion des nouveautés, ajout des avis absents. Une fusion ne touche **jamais**
  les contextes absents du fichier.
- ✅ **Réglages du terrain préservés** (v0.55.1, `ReglagesTerrain`) : une clé **absente** du
  fichier garde la valeur locale (`affouage`, `volumeMaxLotM3`, `tarif`, `tarifNumero`,
  `coefficientForme`, `cheminGpkg` ; `lot` des tiges) ; une clé présente l'emporte. Un réexport
  du plan Nemeton ne décoche donc plus l'affouage ni ne remet le tarif à `AUCUN`.
- ✅ Partage via le **partage système** (pas de serveur).
- 🔜 Synchro **temps réel / réseau** entre appareils (hors périmètre actuel).

## 9. Technique

- **Kotlin + Jetpack Compose + Material 3**, MVVM + repository.
- **Couche domaine Kotlin pur** (`:core`) : totaux dérivés, parsing hauteur, génération de
  l'axe de classes, référentiels, catégories de grosseur, **cubage (Schaeffer + EMERGE)**,
  **géodésie (aires)**, **rattachement spatial (point-dans-polygone)**, **lots d'affouage**,
  **journal net** → **testée en JVM, TDD**.
- **Room** (journal + config + synchro ; schéma **v14**, migrations depuis la v12),
  **DataStore** (réglages).
- `:data` : `MartelageRepository`, `ReglagesRepository`, `ReferentielsRepository`,
  `SauvegardeRepository` (export/import/fusion), `GpkgRepository`.
- **minSdk 26 / targetSdk 35** ; smartphone **et** tablette ; portrait + paysage.
- Modules : `:core` (domaine pur) · `:data` (Room/DataStore/GPKG) · `:app` (Compose UI).
- ✅ **CI/CD** : couverture de tests (badge), **releases SemVer automatiques**, APK release
  horodaté (`marculus-<version>-<UTC>.apk`), **site GitHub Pages** (accueil + galerie + rapport
  de couverture).

## 10. Reste à faire

- 🔜 **Traçabilité des hauteurs estimées** : rien ne distingue au schéma une hauteur estimée
  (MNH) d'une hauteur mesurée. Une colonne dédiée + migration Room si la distinction devient
  nécessaire au cubage ou au contrôle.
- 🔜 Synchro **temps réel / réseau** multi-opérateurs (au-delà du fichier `.marsync`).
- 🔜 Foncier structuré complet (`Propriétaire → Forêt → Parcelle` géré en propre) au-delà
  de l'attribution déduite du GPKG.
- 🔜 **Bouton Flic 2** (BLE) comme troisième déclencheur de la dictée — reporté (dépendance
  JitPack écartée) ; `PttController` est prêt à le recevoir.
- 🔜 **Recette terrain de l'affouage** (annonces, bascule de lot, bilan) et réponse de
  nemetonshiny au brief `CSV format 4` (lecture du `lot`, règle « clé absente »).
- 🔜 **Recette terrain de la dictée** (7 cas, bruit réel, gants) — cf.
  `2026-08-21-tranche-dictee-vocale.md`.
