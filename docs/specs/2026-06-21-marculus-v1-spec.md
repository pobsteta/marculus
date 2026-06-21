# Marculus — Spécification v1 (« comptage seul »)

> Application Android (smartphones + tablettes) de **martelage forestier**, inspirée de
> l'app de référence « Compteur Intelligent » (multicounter), transposée en
> **feuille de martelage**. Écrans de référence : `docs/ecran/`.
> Spec figée le 2026-06-21 après cadrage. On **repart de zéro** (aucun code hérité).

## 1. Périmètre

- ✅ **v1** : comptage traçable en **feuille de martelage** (matrice essences × classes),
  journal d'événements append-only, statut/historique, export.
- ⏭️ **v2** (repoussé) : import **GPKG** des parcelles fourni par le gestionnaire, carte
  (fonds OSM + satellite), **rattachement GPS** des tiges aux parcelles/propriétaires,
  **GNSS externe RTK/NTRIP** (type projet Centipede), **synchro multi-opérateurs**.
- Contraintes : **hors-ligne total**, **mono-appareil** en v1 (partage par fichier),
  **sans publicité**.

## 2. Vocabulaire métier

- La notion de « groupe » de l'app de référence **disparaît**.
- **Contexte** = une **opération de martelage**. Peut couvrir une parcelle dans une forêt
  (cas par défaut) ou une **emprise** traversant plusieurs propriétaires / parcelles
  (le foncier structuré et le rattachement sont en v2).
- Foncier (référence v2) : `Propriétaire → Forêt → Parcelle`, contours importés depuis un
  **GPKG**. Rattachement d'une tige **déduit de la position GPS** (polygone contenant le
  point) **si la position est activée**.

## 3. Modèle de données

```
Contexte        : id, nom, mode (DIAMETRE | CIRCONFERENCE),
                  classeMin, classeMax, classePas, essencesActives[],
                  dateCréation, opérateur
Référentiels    : Essence       — Chêne / Hêtre / Autres feuillus / Sapin / Épicéa /
                                   Autres résineux            (prédéfini, modifiable)
                  QualitéArbre   — Sec / Chablis / Volis / Malade   (prédéfini, modifiable)
                  QualitéBois    — libellés A / B / C / D et combinaisons AB, BC, CD…
                                   (prédéfini, modifiable ; utilisés dans le texte hauteur)
Compteur        : clé = (contexteId, essence, classe)
                  hauteurObligatoire?, qualitéObligatoire?
                  (réglable par compteur, avec action « appliquer à tous »)
Tige (journal)  : uuid, contexteId, essence, classe,
                  action (PLUS | ANNULATION),
                  hauteurTexte?   (texte libre, ex. « 27-6AB4CD »),
                  qualitéArbre?   (mono-choix),
                  position?       (lat/lon brut, nullable — pas de carte en v1),
                  horodatage, opérateur
```

- **Journal append-only** : un `+` crée une tige `PLUS` ; un `−` crée une tige
  `ANNULATION` (jamais d'effacement). **Totaux dérivés** : cellule = nb `PLUS` − nb `ANNULATION`.
- L'**axe des classes** (min / max / pas) est fixé **par contexte** et **commun à toutes les
  essences** → grille rectangulaire. Défauts proposés : diamètre 20→90 pas 5 ;
  circonférence pas 5 ou 10 (modifiable).

## 4. Format de saisie de la hauteur

- Texte **libre, sans contrôle de cohérence**, mais structuré : **hauteur d'abord**, puis
  le séparateur **`-`**, puis **texte libre** de découpe.
  - `27` → arbre de 27 m, sans détail.
  - `27-6AB4CD` → arbre de 27 m, dont 6 m de qualité bois **AB** + 4 m de qualité bois **CD**
    (le reste non détaillé).

## 5. Écrans

1. **Liste des contextes** — cartes (nom, nb tiges, date) + bouton créer. *(adapte 0b)*
2. **Créer / éditer un contexte** — nom · diam/circ · min/max/pas · essences actives ·
   obligation hauteur/qualité.
3. **Feuille de martelage** *(cœur)* — matrice **colonnes = essences**, **lignes = classes** ;
   cellule = **valeur**, **`−` / `+`**, **bouton Hauteur**, **bouton Qualité arbre** ;
   **pas de reset** ; défilement **vertical** (classes) + **horizontal** (essences) ;
   **boutons de volume** optionnels pour incrémenter.
4. **Statut** — donut + barres par essence/classe. *(adapte 3a)*
5. **Historique détaillé** — journal des tiges (essence + classe, horodatage, action,
   valeur cumulée). *(adapte 3b)*
6. **Paramètres** — plein écran, anti-veille, vibration, son de clic, annonce vocale,
   boutons de volume, thème sombre, langue, **Export/Import ZIP**, **Export CSV**.
   *(adapte 2a–2c, sans pubs ni achat in-app)*
7. **Référentiels** — édition des listes Essences / Qualité arbre / Qualité bois.

## 6. Comportement d'une cellule (feuille de martelage)

- **`+`** : ajoute une tige `PLUS`. Si hauteur/qualité **obligatoires** pour ce compteur →
  saisie demandée immédiatement.
- **`−`** : enregistre une **annulation** (conservée au journal), décrémente le total.
- **Bouton Hauteur / Qualité arbre** : annote la **dernière tige** ajoutée de la cellule
  (saisie hauteur en texte libre / sélection mono-choix de la qualité arbre).
- **Export CSV v1** : feuille (totaux par essence × classe) **+** journal des tiges.

## 7. Technique

- **Kotlin + Jetpack Compose + Material 3**, MVVM + repository.
- **Couche domaine Kotlin pur** (totaux dérivés, parsing hauteur, génération de l'axe de
  classes) → **testée en JVM, TDD**.
- **Room** (journal + config), **DataStore** (réglages).
- **minSdk 26 / targetSdk 35** ; smartphone **et** tablette ; portrait + paysage.
- Modules : `:core` (domaine pur) · `:data` (Room/DataStore) · `:app` (Compose UI).

## 8. Découpage en tranches verticales

- **Tranche 1 — squelette + domaine pur** : projet Gradle multi-modules, entités du domaine,
  totaux dérivés, parsing hauteur, génération de l'axe ; **tests JVM** (TDD).
- **Tranche 2 — persistance + UI comptage** : Room/DataStore, ViewModels, navigation,
  écrans Liste contextes / Création contexte / Feuille de martelage.
- **Tranche 3 — restitution** : Statut, Historique détaillé, Référentiels, Paramètres,
  Export/Import.
- **v2** : géo (GPKG, carte, rattachement GPS), GNSS RTK/NTRIP, synchro multi-opérateurs.
