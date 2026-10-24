# Marculus — brief

> À remplir par Pascal ; critiqué par le `contradicteur` avec `/imperatrice:brief`.

## Ma réponse d'abord (fiche n° 15)

> le problème c'est impossible de dire si c'est m3 ou stère en portrait
>
> — Pascal, le 23 octobre 2026, écrit avant de lire la critique du contradicteur (recopié
> mot pour mot de sa réponse en session, à sa demande).

## Le problème, et pour qui (Abstraction)

> Arbitrages de Pascal du 2026-10-23 sur la critique du `contradicteur` (journal, même date) :
> « oui pour tout ».

- **Symptôme qui a ouvert ce brief** : en portrait, impossible de dire si le lot max.
  d'affouage est en m³ ou en stères (Pascal, 2026-10-23, sur son téléphone, écran « Modifier
  le contexte »). Corrigé en v0.63.5 (PR #3) : l'unité est en tête du libellé.
- **Problème du produit** : _en une phrase, de la main de Pascal : ce que Marculus résout,
  pour qui._
- **Problème du cycle** : _choisi par Pascal dans le bac à sable de `pilotage/kanban.md`._
- **Question du lot : réponse de Pascal le 2026-10-24** (`pilotage/journal.md` l. 73, « règle du lot
  d'affouage, mot pour mot ») : le volume d'un lot est le volume total des tiges si le pourcentage de
  houppier est à 0, sinon le volume houppier ; spec v1 mise à jour (PR #17). Nuance EMERGE **confirmée
  par Pascal** (journal l. 78, « oui pour EMERGE ») : houppier seul dès qu'EMERGE couvre l'essence,
  sinon tige entière. L'ADR 0004 n'existe pas
  encore (`docs/adr/` vide au 2026-10-24) : renvoyer vers lui quand il sera écrit. La question
  ci-dessous reste telle qu'écrite le 2026-10-23 ; restent ouverts les portions sur plusieurs
  parcelles et le coefficient de stères du houppier.
- **Question ouverte (à trancher par Pascal)** : dans quelle unité la délibération communale
  fixe-t-elle un lot d'affouage (m³ de tige, m³ de houppier, stères) ? Un lot est-il une
  portion répartie sur plusieurs parcelles ? Aujourd'hui, la spec affouage cumule le m³ de bois
  fort tige (`docs/specs/2026-10-07-contexte-affouage.md`, l. 24-25), la spec générale ne
  compte que le houppier d'un arbre qui en a un (`docs/specs/2026-06-21-marculus-v1-spec.md`,
  l. 150-153), et seuls les m³ bornent le lot (l. 136-137).
- **Hypothèse cachée** : un seul coefficient stères/m³ (1,5 par défaut, « pour des bûches de
  1 m ») vaut pour la tige comme pour le houppier. Aucun coefficient publié pour le houppier
  n'a été trouvé.
- **Rôles** :
  - **Utilisateur** (tient le téléphone) : Pascal ; les autres marteleurs (ONF, experts
    forestiers, gestionnaires privés) sont une **hypothèse à vérifier**.
  - **Bénéficiaires** : les affouagistes, qui reçoivent des lots égaux, et les propriétaires,
    qui reçoivent un martelage tracé. En forêt communale, l'ONF martèle et la commune
    délibère ; ni la commune ni l'affouagiste ne tiennent le téléphone.
  - **Consommateur des données** : `nemetonshiny` (lots `.zip` en entrée, CSV et `.marsync`
    en retour).
- **Question de terrain** : faut-il signaler une tige qui dépasse le diamètre délivrable en
  affouage ? Le plafond ONF (instruction INS-17-T-90) varie selon les sources, 30 ou 45 cm :
  à vérifier sur le texte avant toute règle.
- **Coût complet aujourd'hui** : inconnu (valeur par défaut, 2026-10-23). Les arbitrages sont
  faits sans coût, tant qu'un ordre de grandeur (heures de Pascal par mois, services payants)
  n'est pas donné.

## Ce que fait Marculus

La description à jour est dans la spec, `docs/specs/2026-06-21-marculus-v1-spec.md` ; on ne la
recopie pas ici. Version installée : voir [les releases](https://github.com/pobsteta/marculus/releases)
(la dernière release est la version installée ; ne plus la recopier ici).

*Ligne du 2026-10-24 (remplace, sans l'effacer, la forme écrite le 2026-10-23 « Version installée :
**v0.63.6** (2026-10-23) », périmée depuis les v0.64.9 à v0.65.1) : les écarts de spec listés le 2026-10-23 sont corrigés (PR #17 ; base
Room v17 : `docs/specs/2026-06-21-marculus-v1-spec.md` l. 235 ; fonds Plan IGN, Ortho IGN et OSM : l. 32-34 ; tables SER livrées : l. 19-20 et 135-136 de la même spec et
`pilotage/journal.md` l. 36).
Ancien texte : « Écarts connus de la spec, à corriger : base Room « v14 » au § 9 contre v17 ; « tables en
attente » alors qu'elles sont livrées en v0.62.0 ; fonds « OSM + ESRI » alors qu'ils sont IGN depuis
v0.57.0. » Les trois points sont donc traités.*

**Échanges avec Nemeton** : Marculus écrit `FormatCsv;7`, mais `nemetonshiny` ne lit que les
formats 2 à 5 (`R/utils_i18n.R`, l. 764) : les formats 6 et 7 ne lui ont pas été signalés.

*Ligne du 2026-10-24 (corrige, sans l'effacer, la phrase ci-dessus écrite le 2026-10-23) : brief
déposé le 2026-10-24 (`~/dev/briefs/vers-nemetonshiny/2026-10-24-marculus-csv-formats-6-7.md` ;
`pilotage/journal.md` l. 57, « dépôt ordonné »). Le lecteur R ne refuse que les formats < 2
(`service_marculus_import.R` l. 324-325 ; constat du relecteur, journal l. 43) : il est tolérant ; seul
le texte d'aide (`R/utils_i18n.R` l. 764, « formats 2 à 5 ») est en retard. Urgence jugée faible par
Pascal (journal l. 72).*

## Ce qu'il ne fait pas (hors périmètre)

- Pas de serveur. La synchro réseau entre appareils est **hors périmètre actuel** : la spec la
  garde « à faire » (§ 10), sans cycle prévu ; le partage passe par fichier (`.marsync`, CSV).
- Pas de plantation ni de travaux : c'est Surculus
  (`~/dev/briefs/traites/2026-10-17-traite-plan-action-vers-surculus.md`).
- Pas de calcul de référentiel IFN : les tables viennent de nemeton, Marculus les embarque.
- Pas de publicité.

## Contraintes connues

- **Réseau** : le geste de martelage, le cubage et les lots marchent sans réseau. Trois
  fonctions le demandent : les corrections RTK par NTRIP, les fonds de carte IGN et OSM, et le
  téléchargement du modèle de voix Vosk. Repli sur le terrain : _à écrire (GNSS du téléphone
  sans RTK, ortho du chantier embarquée dans le GeoPackage, modèle téléchargé avant de partir ?)._
- **Appareil perdu** : sans export `.marsync` préalable, que reste-t-il ? _À trancher._
- **Lisibilité terrain**, critères vérifiables :
  - contraste du texte de 7:1, 4,5:1 pour le grand texte (WCAG 2.2, critère 1.4.6), comme
    les jetons de `terrain-style` acceptés pour Surculus ; le vert principal actuel donne
    5,17:1 ;
  - cibles tactiles d'au moins 48 dp, utilisables avec des gants ;
  - champs lisibles en portrait sur 360 dp avec la police système à 200 % ;
  - l'unité en suffixe du champ (convention SI : l'unité suit la valeur ; suffixe des champs
    Material 3). **Le libellé de la PR #3 est à rouvrir** au regard de ce critère.
- Le journal des tiges est en ajout seul ; une fusion ne réécrit jamais un réglage du terrain
  absent du fichier (v0.55.1).
- Les formats d'échange (CSV, `.marsync`) sont un contrat avec `nemetonshiny` : tout
  changement se signale par brief, avec un numéro de format.
- Les briques GNSS, GPKG, carte, voix et journal sont partagées par la bibliothèque `terrain`
  (github.com/pobsteta/terrain, JitPack) : on les corrige là-bas.
- GPL-3.0-or-later ; APK release signé par la CI ; le téléphone de Pascal ne reçoit que la
  release.
