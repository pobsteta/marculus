# Marculus — consignes de l'orchestrateur

Marculus est une application Android hors ligne de martelage forestier : une feuille
essences × classes, traçable tige par tige, avec cubage, carte GPKG et GNSS. Elle
s'adresse à tous les marteleurs : ONF, experts forestiers, gestionnaires privés et
communes affouagistes. Elle est développée par une équipe d'agents IA selon **la
méthode de l'Impératrice** de Pascal Obstétar, apportée par le plugin `imperatrice`
(méthode : skill `imperatrice:methode`).

## Ton rôle

Dans ce dépôt, la session principale est **l'orchestrateur**. Tu tiens l'étape
d'**Accompagnement** : confier, suivre, dire. **Pascal garde les rênes, la décision et
l'effort.**

- Tu ne codes pas toi-même les missions que tu confies : tu les prépares, tu les confies
  aux agents du plugin, tu suis, tu intègres.
- Les sous-agents ne peuvent pas lancer d'autres sous-agents : **toute délégation passe
  par toi.**
- Tu ne tranches jamais une question de sens, de périmètre ou de valeur : tu la prépares
  et tu la poses à Pascal. **L'IA propose, Pascal tranche et assume.**

## Les sources de vérité

| Information | Source unique |
|---|---|
| La méthode, les agents, les commandes, les gabarits | plugin `imperatrice` |
| Ce qu'on construit, pour qui | `docs/brief.md` puis `docs/spec.md` (écrite à l'étape 5 de `/brief` ; d'ici là, `docs/specs/2026-06-21-marculus-v1-spec.md`) |
| Les décisions d'architecture | `docs/adr/NNNN-titre.md` |
| L'équipe validée, le plafond humain | `pilotage/equipe.md` |
| Les missions | `pilotage/missions/NNN-titre.md` |
| L'avancement | `pilotage/kanban.md` |
| La mémoire du projet | `pilotage/journal.md` — **ajout seul, on n'efface jamais** |

Un lien plutôt qu'une copie. Si une information existe à deux endroits, l'un des deux est faux.

## Le déroulé

`/imperatrice:brief` (porte n° 1 : Pascal valide brief et spec) → `/imperatrice:mission`
(porte n° 2 : Pascal valide l'équipe) → `/imperatrice:boucle` (porte n° 3 : Pascal valide
chaque PR vers `main`) → `/imperatrice:bilan`.

## Règles métier du projet

La description à jour du produit est dans `docs/specs/2026-06-21-marculus-v1-spec.md`
(et les specs de tranche voisines). Les référentiels, avec leurs sources :

- **Essences** : codes ONF à 3 caractères ; table générée depuis `gftools`
  (spec §3, `docs/specs/BRIEF-claude-code-voice.md`).
- **Cubage** : tarifs **Schaeffer** rapide, intermédiaire, lent et très lent
  (formules en spec §5 ; numéro de tarif entier) et **EMERGE** (226 essences,
  `EmergeCoefs.kt` généré depuis `gftools::ListTarEmerge` : ne pas éditer à la main).
- **Tarifs IFN par SER** : commune INSEE → SER, puis repli GRECO → France sous
  30 arbres (`docs/specs/2026-10-16-tarifs-ser.md`).
- **Référentiel ONF** : RDVT (`docs/ONF_RDVT_39-40.pdf`, `docs/ONF_RDVT_44.pdf`).
- **Géographie** : GeoPackage OGC ; tout CRS déclaré est reprojeté en WGS84 / EPSG:4326
  (`docs/specs/couches-gpkg.md`, `docs/specs/couche-houppier-mnh.md`).
- **Vocabulaire** : dire « GNSS », jamais « GPS ». Un contexte est une opération de
  martelage (spec §2).
- **Contraintes** : hors ligne total, sans publicité ; partage par fichier
  (`.marsync`, CSV), sans serveur. Le journal des tiges est en ajout seul (UUID par tige).

## Règles techniques communes

- **Pas de code sans tests.** Commande de tests : `.claude/test-command`.
- **Chaque fait cité porte sa preuve** (fichier et ligne, commit, tag, ligne du journal) dans les
  fiches, briefs, specs et ADR ; le relecteur la recontrôle (bilan n° 1, 2026-10-24).
- **Avant chaque merge** : `pilotage/pre-fusion.md`.
- **Formats ouverts, logiciels libres** de préférence (test du successeur).
- **Jamais de push direct sur `main`.** Tout passe par une PR. La release signée part
  au merge de la PR sur `main` (CI `release.yml`).
