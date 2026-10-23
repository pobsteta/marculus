# Marculus — brief

> À remplir par Pascal ; critiqué par le `contradicteur` avec `/imperatrice:brief`.

## Ma réponse d'abord (fiche n° 15)

> _Ce que j'attends, et pourquoi, en quelques lignes._
>
> — _Pascal, le JJ mois AAAA, écrit avant de lire la critique du contradicteur._

## Le problème, et pour qui (Abstraction)

- **Problème** : _ce qui ne va pas aujourd'hui, constaté où et par qui._
- **Bénéficiaires** : _qui en profite d'abord, qui ensuite._
- **Coût complet aujourd'hui** : inconnu (non renseigné, valeur par défaut le 2026-10-23).

## Ce que fait Marculus

> Rubriques remplies par l'orchestrateur le 2026-10-23 à partir de ce qui est livré
> (v0.55 → v0.62, `docs/specs/2026-06-21-marculus-v1-spec.md`) et des briefs traités
> (`~/dev/briefs/traites/INDEX.md`, section « Traités par la session `marculus` »). À corriger
> par Pascal.

- **En une phrase** : application Android hors ligne de martelage forestier : une feuille
  essences × classes, traçable tige par tige, avec cubage, carte GPKG et GNSS.
- **Les gestes sur le terrain** : compter les tiges désignées (bouton, boutons de volume ou
  dictée hors ligne), noter hauteur et qualité, rattacher la tige à sa parcelle par la
  position GNSS (RTK/NTRIP possible), suivre les lots d'affouage annoncés à la voix.
- **Ce qu'il produit** : volumes (Schaeffer par SER ou EMERGE, houppier, stères), bilan par
  lot, CSV `FormatCsv;7` et `.marsync`, relus par Nemeton (`nemetonshiny`) ; reçoit en entrée
  les lots de chantiers `.zip` produits par Nemeton.

## Ce qu'il ne fait pas (hors périmètre)

- Pas de serveur ni de synchro réseau : le partage passe par fichier (`.marsync`, CSV).
- Pas de plantation ni de travaux : c'est Surculus (brief `traites/2026-10-17-traite-plan-action-vers-surculus.md`).
- Pas de calcul de référentiel IFN : les tables viennent de nemeton, Marculus les embarque.
- Pas de publicité.

## Contraintes connues

- Hors ligne total sur le terrain ; lisible au soleil, utilisable avec des gants.
- Le journal des tiges est en ajout seul ; une fusion ne réécrit jamais un réglage du terrain
  absent du fichier (v0.55.1).
- Les formats d'échange (CSV, `.marsync`) sont un contrat avec `nemetonshiny` : tout
  changement se signale par brief, avec un numéro de format.
- Les briques GNSS, GPKG, carte, voix et journal sont partagées par la bibliothèque `terrain`
  (github.com/pobsteta/terrain, JitPack) : on les corrige là-bas.
- GPL-3.0-or-later ; APK release signé par la CI ; le téléphone de Pascal ne reçoit que la
  release.
