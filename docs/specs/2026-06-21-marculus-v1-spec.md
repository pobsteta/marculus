# Marculus — Spécification

> Application Android (smartphones + tablettes) de **martelage forestier**, inspirée de
> l'app de référence « Compteur Intelligent » (multicounter), transposée en
> **feuille de martelage**. Écrans de référence : `docs/ecran/`.
>
> Spec figée le 2026-06-21 après cadrage. On **repart de zéro** (aucun code hérité).
> **Mise à jour 2026-06-24** : l'avancée a largement dépassé le périmètre v1 « comptage seul ».
> La v1 est livrée et l'essentiel de la v2 (cubage EMERGE, géo/GPKG, GNSS, synchro par fichier)
> l'est aussi. Légende d'état : ✅ fait · 🟡 partiel · 🔜 à faire.

## 1. Périmètre

- ✅ **v1** : comptage traçable en **feuille de martelage** (matrice essences × classes),
  journal d'événements append-only, statut/historique, export.
- 🟡 **v2** : import **GPKG** des parcelles, carte (tuiles GPKG), **rattachement spatial**
  des tiges aux parcelles, **GNSS** ponctuel, **synchro multi-opérateurs**.
  - ✅ Import/affichage GPKG + carte, rattachement point-dans-polygone, aires géodésiques.
  - ✅ GNSS ponctuel (acquisition à la demande, permission, terminologie « GNSS »).
  - ✅ Synchro multi-opérateurs **par fichier** (`.marsync`, fusion « dernière écriture gagne »).
  - 🔜 GNSS externe **RTK/NTRIP** (type projet Centipede) ; fonds **OSM + satellite** en ligne ;
    synchro **temps réel / réseau** entre appareils.
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

## 6. Écrans

1. ✅ **Liste des contextes** — cartes (nom, nb tiges, date, état Kanban) + bouton créer ;
   tri par date de martelage, filtre de recherche, **vue Kanban 5 colonnes** (glisser-déposer),
   partage `.marsync`.
2. ✅ **Créer / éditer un contexte** — nom · diam/circ · min/max/pas · essences actives ·
   obligation hauteur/qualité · tarif de cubage · coefficient de forme · date de martelage.
3. ✅ **Feuille de martelage** *(cœur)* — matrice **colonnes = essences**, **lignes = classes** ;
   cellule = **valeur**, **`−` / `+`**, **bouton Hauteur**, **bouton Qualité arbre** ;
   **pas de reset** ; défilement vertical (classes) + horizontal (essences) ;
   boutons de volume optionnels ; code essence lisible en option.
4. ✅ **Statut + Historique** — restitution par essence/classe et journal détaillé des tiges
   (horodatage, action, valeur cumulée). *(écran `StatutHistoriqueScreen`)*
5. ✅ **Carte** — affichage des parcelles importées (tuiles GPKG), position GNSS,
   rattachement spatial. *(écran `CarteScreen`, `GpkgTileModule`)*
6. ✅ **Paramètres** — anti-veille, plein écran, vibration, son de clic, **annonce vocale**
   (nombre, étiquette, avis limite inf./sup.), boutons de volume, thème sombre, langue,
   GNSS ponctuel, opérateur, **Export/Import ZIP**, **Export CSV**, **fusion `.marsync`**.
7. ✅ **Référentiels** — édition des listes Essences / Qualité arbre / Qualité bois.

## 7. Comportement d'une cellule (feuille de martelage)

- **`+`** : ajoute une tige `PLUS`. Si hauteur/qualité **obligatoires** pour ce compteur →
  saisie demandée immédiatement. Si **GNSS ponctuel** actif → acquisition de la position.
- **`−`** : enregistre une **annulation** (conservée au journal), décrémente le total.
- **Bouton Hauteur / Qualité arbre** : annote la **dernière tige** ajoutée de la cellule
  (saisie hauteur en texte libre / sélection mono-choix de la qualité arbre).
- **Export CSV** : feuille (totaux par essence × classe) **+** journal des tiges
  (+ volumes cubés selon le tarif du contexte).

## 8. Synchro multi-opérateurs (v2, partiel)

- ✅ Chaque entité porte un horodatage `modifie` ; chaque tige porte l'`opérateur`.
- ✅ **Fusion par fichier** (`.marsync`, JSON) dans **une transaction atomique** (`MergeDao`) :
  union par UUID, **« dernière écriture gagne »** (l'entrant remplace le local s'il est plus
  récent), insertion des nouveautés, ajout des avis absents.
- ✅ Partage via le **partage système** (pas de serveur).
- 🔜 Synchro **temps réel / réseau** entre appareils (hors périmètre actuel).

## 9. Technique

- **Kotlin + Jetpack Compose + Material 3**, MVVM + repository.
- **Couche domaine Kotlin pur** (`:core`) : totaux dérivés, parsing hauteur, génération de
  l'axe de classes, référentiels, catégories de grosseur, **cubage (Schaeffer + EMERGE)**,
  **géodésie (aires)**, **rattachement spatial (point-dans-polygone)** → **testée en JVM, TDD**.
- **Room** (journal + config + synchro), **DataStore** (réglages).
- `:data` : `MartelageRepository`, `ReglagesRepository`, `ReferentielsRepository`,
  `SauvegardeRepository` (export/import/fusion), `GpkgRepository`.
- **minSdk 26 / targetSdk 35** ; smartphone **et** tablette ; portrait + paysage.
- Modules : `:core` (domaine pur) · `:data` (Room/DataStore/GPKG) · `:app` (Compose UI).
- ✅ **CI/CD** : couverture de tests (badge), **releases SemVer automatiques**, APK release
  horodaté (`marculus-<version>-<UTC>.apk`), **site GitHub Pages** (accueil + galerie + rapport
  de couverture).

## 10. Reste à faire

- 🔜 **GNSS externe RTK/NTRIP** (type projet Centipede) pour la précision centimétrique.
- 🔜 Fonds de carte **en ligne** (OSM + satellite) en complément des tuiles GPKG hors-ligne.
- 🔜 Synchro **temps réel / réseau** multi-opérateurs (au-delà du fichier `.marsync`).
- 🔜 Foncier structuré complet (`Propriétaire → Forêt → Parcelle`) au-delà de l'emprise.
