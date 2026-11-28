# Mission 045 — `docs/spec.md` consolidée et archivage du bilan n° 3

## Mission

**En une phrase (quoi, et pourquoi) :** écrire `docs/spec.md`, la spec consolidée et à jour de Marculus (spec de juin
et specs de tranche, livraisons 029 à 043 comprises, chaque fait avec sa preuve), et solder les trous de mémoire du
bilan n° 3 (Redite, README, kanban, `CLAUDE.md`, briefs entrants marqués), pour qu'un successeur sache enfin où lire
(décision de Pascal du 2026-11-27, option A).

**Redite par l'agent :** Écrire `docs/spec.md`, la spec consolidée de Marculus (une ligne par fonction livrée, chacune avec sa preuve relue sur `a215c0a`, le détail restant dans les specs de tranche), et solder les trous de mémoire du bilan n° 3 (Redite vides, README, kanban, `CLAUDE.md`, briefs entrants marqués), pour qu'un successeur sache où lire ; plan d'abord, validé par Pascal le 2026-11-27. _Rédigée par l'archiviste le 2026-11-27, avant d'agir._

## Ce qui existe (preuves)

- Constat de l'archiviste au bilan n° 3 : ligne « 2026-11-27 · archiviste · — · bilan n° 3 » du journal (G1 à G9).
- Spec en vigueur : `docs/specs/2026-06-21-marculus-v1-spec.md` (contradiction base l. 306 v17 / l. 267 v18 ; code v18,
  `data/.../db/MarculusDatabase.kt` l. 63) ; specs de tranche : `docs/specs/*.md`.
- Méthode de reconstitution des Redite : mission 024 (`pilotage/missions/024-archivage-016-023.md`).
- Briefs entrants : `~/dev/briefs/vers-marculus/` (4 fichiers du 2026-11-20 et du 2026-11-24).
- Décision de Pascal (journal, « question 3/5 : A »).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | à moitié | plan de `docs/spec.md` (sommaire, ce qui reste dans les specs de tranche, ce qui devient historique) : plan d'abord, montré à Pascal |
| A-t-il les moyens ? | oui | specs, journal, code en lecture |

**Temps de la main :** donner, **plan d'abord, validé par Pascal** (la source de vérité change). **Degré de la main :**
le chef tranche.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `archiviste` · **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** changer une règle métier ; effacer une spec datée (elles restent, `docs/spec.md`
  y renvoie) ; écrire les ADR (046) ; traiter le fond des briefs `terrain` (bac à sable).

## Harnais

- **Peut (périmètre) :** `docs/spec.md` (nouveau) ; `docs/specs/2026-06-21-marculus-v1-spec.md` (ajout daté en tête :
  « remplacée par `docs/spec.md` » ; l. 306 par ajout daté) ; la ligne **Redite** des fiches `pilotage/missions/024` à
  `043` (« reconstituée d'après le journal » ou « non retrouvée ») ; `README.md` (lien vers le kanban, badge
  « méthode : Impératrice » du brief du 2026-11-24) ; `pilotage/kanban.md` (en-tête du bac à sable, ajout daté ; cartes
  des briefs `terrain`) ; `CLAUDE.md` du projet (table des sources : `docs/spec.md` existe ; « Règles métier » : specs
  du 2026-11-27) ; en tête de chaque brief de `~/dev/briefs/vers-marculus/`, une ligne « vu le 2026-11-27 par
  Marculus : … » ; le brief du badge déplacé dans `~/dev/briefs/traites/` une fois fait.
- **Câblé (ne peut pas) :** pas de `main` ; journal en ajout seul.
- **Ne doit pas (écrit) :** toucher `pilotage/pre-fusion.md` (044), `docs/adr/` (046), `.github/` (047) ; recopier une
  information qui a déjà sa source (un lien plutôt qu'une copie).
- **Sources autorisées :** tout le dépôt en lecture, `~/dev/briefs/`.

## Journal

- **Ce que l'agent inscrit :** une ligne pour le plan, puis une par rendu : sections écrites, Redite reconstituées
  (nombre), trous soldés (G1 à G9).

## Valeur provisoire (chiffre, réglage)

- aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 046 pour les liens vers les ADR 0011 à 0013 (noms fixés dans la fiche 046 : liens écrits d'avance).
- **Ordre de fusion :** après 046. **PR en brouillon jusqu'à :** la fusion de 046. **CI rouge attendue :** aucune.
- **Plafond humain (3 PR) :** la PR s'ouvre quand l'une des PR 044, 046 ou 047 est fusionnée.

## Tests

- **Comment saura-t-on que c'est juste :** chaque fonction livrée (029-043) a sa ligne dans `docs/spec.md` avec sa
  preuve ; le relecteur tire 10 faits au hasard et les recontrôle à la source ; `CLAUDE.md` ne cite plus de fichier absent.

## Terrain

- **Échantillon que Pascal vérifiera :** le plan, puis le sommaire de `docs/spec.md` et trois sections au choix.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Test du successeur « oui » au bilan n° 4 : la spec se lit dans un seul fichier.
