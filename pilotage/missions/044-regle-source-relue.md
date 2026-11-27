# Mission 044 — Règle « source relue » et « briefs relevés » avant de dire prêt

## Mission

**En une phrase (quoi, et pourquoi) :** ajouter à `pilotage/pre-fusion.md` deux points, « toute preuve dit l'état de
sa source (commit ou tag, `git status` propre) et tout contrôle terrain est rejoué dans l'unité de l'écran » et « boîte
`~/dev/briefs/vers-marculus/` relevée, briefs sortants déposés », et préparer le brief qui les propose au gabarit de
fiche du plugin `imperatrice`, parce que la cause n° 1 du bilan n° 3 (faits sans preuve relue à la source, 10 incidents
sur 26) est revenue six fois avec des règles seulement locales.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Ce qui existe (preuves)

- Pareto du bilan n° 3 : `pilotage/journal.md`, ligne « 2026-11-27 · analyste · — · bilan n° 3 » ; incidents de la cause
  n° 1 : journal l. 205, 226, 341 (335), 351, 386, 393 (orchestrateur), 201, 266, 299, 377 (documents d'agents).
- Règles locales déjà écrites : « vérifier l'état git d'une source locale avant de la donner comme publiée » (l. 341),
  mémoire « contrôle terrain en unités de l'écran » (écart l. 393).
- Écart des briefs non relevés : ligne « 2026-11-27 · Claude (orchestrateur) · — · écart · attendu :
  `~/dev/briefs/vers-marculus/` vide ».
- `pilotage/pre-fusion.md` (5 points, l. 1-17) ; règle de preuve : `CLAUDE.md` du projet, « Règles techniques communes ».
- Modèle de brief vers la méthode : `~/dev/briefs/vers-methode-imperatrice/2026-11-24-tests-orphelins-hook-tests-obligatoires.md`.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | à moitié | la formulation d'une règle qui sert à la fois les agents et l'orchestrateur : plan d'abord |
| A-t-il les moyens ? | oui | incidents nommés, fichier cible court |

**Temps de la main :** donner, plan d'abord (l'orchestrateur le valide). **Degré de la main :** le chef tranche.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `archiviste` · **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** modifier le plugin `imperatrice` ; modifier `CLAUDE.md` (mission 045).

## Harnais

- **Peut (périmètre) :** `pilotage/pre-fusion.md` ; un brouillon de brief vers la méthode dans le scratchpad de la
  session (`/tmp/claude-1000/-home-pascal-dev-marculus/9a1cc8a0-71ea-40c1-af0c-c9793232660c/scratchpad/`).
- **Câblé (ne peut pas) :** pas de `main` ; journal en ajout seul.
- **Ne doit pas (écrit) :** déposer le brief (l'orchestrateur dépose après l'accord de Pascal) ; écrire une règle sans
  citer les incidents qui la justifient.
- **Sources autorisées :** journal, `pre-fusion.md`, `CLAUDE.md`, le skill `methode` (liste de contrôle, fournie par
  l'orchestrateur), le modèle de brief.

## Journal

- **Ce que l'agent inscrit :** une ligne par rendu : points ajoutés, incidents qu'ils auraient arrêtés.

## Valeur provisoire (chiffre, réglage)

- aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune. **Ordre de fusion :** libre. **CI rouge attendue :** aucune.

## Tests

- **Comment saura-t-on que c'est juste :** chacun des 10 incidents de la cause n° 1 est arrêté par l'un des points
  (tableau incident → point dans le rapport) ; le relecteur rejoue le tableau.
  _Ligne du 2026-11-27 (aller-retour 2/3) : 9 sur 10 sont arrêtés ; l. 299 (oubli de contenu, mission 035) n'est pas
  une preuve fausse et reste non couverte, dit tel quel plutôt que forcé._

## Terrain

- **Échantillon que Pascal vérifiera :** les deux points de `pre-fusion.md` et le brief vers la méthode, avant dépôt.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal (une règle de travail change). · **Critère d'arrêt :** 3 allers-retours.
  · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Au bilan n° 4, la cause « fait sans preuve relue à la source » recule sous 3 incidents.
