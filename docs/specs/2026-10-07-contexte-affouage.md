# Tranche « contexte Affouage » (lots de volume)

Demande de Pascal du 2026-10-07. Statut : **livré** (2026-10-07).

## Besoin

Pour un martelage d'affouage, les tiges sont réparties en **lots** de volume à peu près égal.
On ne fait pas les lots après coup sur un tableur : l'opérateur marque le numéro de lot sur
l'arbre au moment du martelage. L'application doit donc **dire** à chaque tige le lot où elle va.

## Ce que ça fait

**Création / modification d'un contexte** : une case **« Affouage »** dans la section du tarif
de cubage. Cochée, elle ouvre un champ **« Volume maximal d'un lot (m³) »** (décimal, > 0).

- Affouage exige un **tarif de cubage** (≠ Aucun) : sans volume par tige, aucun lot ne se ferme.
  Refus à l'enregistrement avec un message.
- En **EMERGE**, une tige sans hauteur vaut 0 m³ : le lot ne se remplit que par les tiges
  mesurées (MNH, dictée, bouton H). Le texte d'aide le dit — Schaeffer est le choix naturel.

**Au comptage** (feuille, carte, dictée vocale — tout passe par `sessionMartelage.ajouter`) :

1. Les lots démarrent à **1**.
2. Le volume de chaque tige (`VolumesMartelage.cubage(...).volumeTigeM3 × quantité`, bois fort
   tige) est cumulé dans le lot courant.
3. Dès que le cumul est **≥ volume maximal**, le lot est clos : la tige **suivante** ouvre le lot
   suivant (la tige qui fait déborder appartient au lot qu'elle ferme), et le cumul repart de 0.
4. L'annonce vocale reste celle des Paramètres (« Annonce de l'étiquette » → « Chêne 35 »,
   « Annonce du nombre » → total), et **dans tous les cas** on ajoute **« lot numéro N »** —
   même si les deux annonces sont décochées. Ex. : « Chêne 35, lot numéro 3, 12 ».
5. La tige qui ferme son lot ajoute **« lot N complet »** : « Chêne 35, lot numéro 3, lot 3 complet ».

## Décisions

### Le lot est figé sur la tige

Nouveau champ `Tige.lot: Int?` (null hors affouage), comme la parcelle : c'est un **instantané**.
Le numéro a été dit à voix haute et peint sur l'arbre ; il ne doit jamais changer après coup —
ni si une hauteur est complétée plus tard, ni si le tarif ou le volume maximal change, ni après
une fusion `.marsync` qui intercalerait les tiges d'un autre appareil.

### L'état courant est dérivé du journal

Pas de compteur stocké dans le contexte. `AffouageLots.prochainLot(contexte, journal)` (`:core`,
pur, testé) rejoue le journal :

- les annulations retirent, comme dans `VolumesMartelage`, les dernières tiges de leur case ;
- lot courant = plus grand lot porté par une tige PLUS (1 si aucune) ;
- cumul = volume des tiges **vivantes** de ce lot (volumes recalculés avec les hauteurs actuelles) ;
- prochain lot = lot courant + 1 si cumul ≥ max, sinon lot courant.

Conséquence voulue : un **−** juste après la tige qui a fermé un lot rouvre ce lot — la tige
suivante y retourne. Un lot vidé par annulation garde son numéro (on ne saute pas, on ne recule
pas au-delà du plus grand lot déjà annoncé).

Les tiges sans lot (affouage coché en cours de martelage) sont ignorées : on démarre au lot 1.

### Annonce avant l'écriture

Le lot est calculé sur le journal observé par la session, avant l'insertion, pour que l'annonce
parte sans latence (comme le total aujourd'hui). Deux tiges tapées plus vite que la remontée du
flux Room peuvent tomber dans un lot déjà plein : le lot déborde d'une tige, mais **ce qui est dit
est ce qui est enregistré** — c'est l'invariant qui compte sur le terrain.

## Modèle et persistance

| Où | Changement |
|---|---|
| `Contexte` | `affouage: Boolean = false`, `volumeMaxLotM3: Double = 0.0` |
| `Tige` | `lot: Int? = null` |
| Room | **v14**, `MIGRATION_13_14` : 2 colonnes `contexte`, 1 colonne `tige` (défauts 0 / NULL) |
| `.marsync` | `affouage`, `volumeMaxLotM3` (contexte), `lot` (tige) ; absents → défauts |
| CSV | **format 4** : `Affouage` / `VolumeMaxLotM3` en en-tête après `Increment` (15 premières lignes inchangées), colonne `Lot` en fin de journal (vide hors affouage) — à signaler à Nemeton |

## Fusion : les réglages du terrain survivent à un réexport (v0.56.0)

Nemeton réémet les contextes de son plan, plus récents, sans `tarif` ni `affouage`. Or la fusion
remplace la ligne entière : l'affouage se décochait. Désormais (`ReglagesTerrain`, appelé par
`fusionnerJson`, donc par l'import de lot et la fusion `.marsync`) une clé **absente** du fichier
garde la valeur locale : `affouage`, `volumeMaxLotM3`, `tarif`, `tarifNumero`,
`coefficientForme`, `cheminGpkg` pour un contexte, `lot` pour une tige. Une clé **présente**
l'emporte, même pour revenir au défaut. Contexte ou tige inconnus : défauts. Les contextes absents
du fichier n'ont jamais été touchés par une fusion (seule la restauration de sauvegarde efface).

## Hors périmètre (à proposer ensuite)


- Lot affiché sur la carte / dans la feuille.

## Saisie libre et « répète »

- Saisie libre (dialogue hors grille) : la tige reçoit le lot ouvert, sans annonce.
- Commande vocale « répète » : ré-annonce aussi le lot de la dernière tige.

## Export CSV « tiges à comptabiliser »

À l'export CSV (liste des contextes), une question : **journal complet** ou **uniquement les
tiges à comptabiliser**. En net (`JournalNet`), chaque annulation disparaît avec la tige qu'elle
retire (dernière tige de sa case, même règle que les volumes) ; un paquet (quantité > 1) annulé en
partie reste avec sa quantité restante ; une annulation sans tige à retirer est gardée. L'en-tête
porte `Journal;NET` ou `Journal;COMPLET` ; totaux et volumes d'en-tête sont identiques.

## Onglet « Par lot » (Statut / historique)

Visible seulement pour un contexte d'affouage, entre « Statut » et « Par parcelle ». Pour chaque
lot : nombre de tiges et volume bois fort tige (m³), **nets** (`AffouageLots.bilan`, sur
`JournalNet` : une tige annulée ne compte dans aucun lot), avec « complet » / « en cours », puis
une ligne Total. Les tiges comptées sans lot sont regroupées en dernier (« Sans lot »).

Bouton **« Exporter le bilan par lot (CSV) »** → `Download/Marculus/<contexte> - lots.csv`
(`ExportBilanLots`) : en-tête `Contexte`, `ContexteId`, `VolumeMaxLot_m3`, puis
`Lot;Tiges;Volume_m3;Etat` (`COMPLET` / `EN_COURS` / `SANS_LOT`) et une ligne `Total`. Rapport à
lire (comme le CSV foncier) : décimaux dans la langue du téléphone, 3 décimales.

## Tests

- `AffouageLotsTest` : départ à 1, cumul et bascule à l'égalité, débordement, annulation qui
  rouvre un lot, tiges sans lot ignorées, quantité > 1, tarif AUCUN.
- `AffouageLotsTest` : `fermeLot` (fermeture, égalité, hors affouage), `bilan` par lot.
- `JournalNetTest` : retrait de la paire, case, ordre, paquet partiel, annulation orpheline, totaux égaux.
- `ReglagesTerrainTest` (`:data`) : clé absente gardée, clé présente gagnante, contexte nouveau, lot de tige.
- `ExportBilanLotsTest` : lignes, état, total, décimaux.
- `ExportCsvTest` : format 4, en-tête affouage, colonne `Lot`, export net.
