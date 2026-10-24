# Mission 012 — ADR des décisions déjà prises

## Mission

**En une phrase (quoi, et pourquoi) :** écrire dans `docs/adr/` les ADR 0001 à 0006 des décisions
déjà prises et éparses dans le journal et les specs, sans en prendre aucune nouvelle, pour qu'un
successeur retrouve chaque décision, ses alternatives et sa raison en moins d'une minute (bilan
n° 1 du 2026-10-24, validé par Pascal).

**Redite par l'agent :** écrire les ADR 0001 à 0006 de six décisions déjà prises, sans en ajouter aucune, chaque fait cité avec sa preuve, pour qu'un successeur retrouve décision, alternatives et raison en moins d'une minute. _(reportée par l'orchestrateur, 2026-10-24)_

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | gabarit `adr-france.md` du plugin `imperatrice` (skill `brief`) ; sources ci-dessous |
| A-t-il les moyens ? | oui | `pilotage/journal.md`, specs, code |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## Les six décisions (liste du bilan, à ne pas étendre)

1. Partage par fichier (`.marsync`, CSV), sans serveur ; synchro réseau hors périmètre.
2. Briques communes dans la bibliothèque `terrain` (JitPack) : on corrige là-bas.
3. `tarifsProposes` : seul l'échelon `foret` est lu (cap du 2026-10-23).
4. Lot d'affouage : houppier seul quand l'arbre en a un, EMERGE compris, sinon tige entière (cap du 2026-10-24).
5. Thème : choix Système / Clair / Sombre ; jetons `terrain-style` v0.6.0 annulés (v0.64.2), réadoption suspendue à une palette qui reste verte et à l'essai au soleil.
6. Numéro de tarif à 0 hors Schaeffer, perdu au retour en Schaeffer (2026-10-24).

## L'Homme

- **Agent :** `archiviste` · **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** décider, reformuler une décision, en ajouter une. Une ADR dont
  la raison ou une alternative manque au journal le dit (« non écrit ») et pose la question.

## Harnais

- **Peut :** créer `docs/adr/0001-….md` à `0006-….md` et `docs/adr/README.md` (index d'une ligne
  par ADR) ; ajouter dans les specs un renvoi d'une ligne vers l'ADR ; dans sa branche.
- **Ne doit pas :** modifier une règle métier ni le code ; toucher à `main`.
- **Règle de preuve (bilan n° 1) :** chaque fait cité porte sa preuve (ligne du journal, fichier et
  ligne, commit ou tag).

## Tests

- **Comment saura-t-on que c'est juste :** chaque ADR renvoie à sa ligne `cap` ou à sa source ;
  le relecteur recontrôle chaque preuve.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

« Pourquoi seul l'échelon forêt ? » trouve sa réponse dans `docs/adr/` en moins d'une minute.

## Remarques du relecteur — aller-retour 1/3 (2026-10-24)

Rendu : commit daff503 (worktree `.claude/worktrees/agent-a1aa9611df38f2bcb`). Les 6 ADR sont
fidèles aux caps, aucune décision ajoutée ; presque toutes les preuves recontrôlées exactes. À reprendre :

1. **ADR 0003, provenance fausse** : le brief du 2026-10-16 est un brief **Marculus**
   (`~/dev/briefs/traites/2026-10-16-traite-marculus-tarifs-ser.md` § 2), cité par l'en-tête de
   `~/dev/briefs/vers-nemetonshiny/2026-10-23-marculus-tarifs-proposes-niveau-foret.md` § 1 (qui
   existe : retirer « non revérifié ici »).
2. **ADR 0005, « Valeurs sacrifiées »** : journal l. 63, 65 ne parlent que du vert disparu ; la
   lisibilité au soleil (l. 92, 99) n'est pas la raison du revert. Ne garder que le vert, ou « non écrit ».
3. **ADR 0006** : « numéro Schaeffer entier » est écrit dans `CLAUDE.md` l. 51 : le citer au lieu de « non écrit ».
4. **Déductions à remplacer par « non écrit » ou par une citation** : 0001 crible C (« aucun serveur
   à héberger ») et F (« pas d'infrastructure à réparer ») ; 0005 crible F (« prêts à payer : oui,
   par le cap ») ; 0003 crible A (« fonctionne sans réseau », sans source).
5. **Mineurs** : 0004 « Proposée par » → « question soulevée par l'archiviste (journal l. 37) » ;
   0005 « Proposée par : orchestrateur » non écrit → « non écrit » ; 0002 : citer `docs/brief.md`
   l. 85 (brief validé, journal l. 27) plutôt que la mémoire de l'orchestrateur ; 0001 : ne pas
   ajouter « ZIP » à l'énoncé (format d'entrée, brief l. 38, le dire à part) ; colonnes du crible
   FRANCE avec le texte du gabarit.
6. **Statut** (consigne de l'orchestrateur, pas une décision nouvelle) : écrire « **décision
   acceptée par Pascal** (ligne `cap` …) ; **texte de l'ADR proposé**, à valider par Pascal à la
   lecture de la PR ».

## Remarques du relecteur — aller-retour 2/3 (2026-10-24)

Les 6 remarques de l'aller 1 sont traitées, preuves recontrôlées. À reprendre (dernier aller-retour) :

1. **0006 l. 33** (« sacrifiée contre la cohérence des données ») : déduction, contredite par la l. 18 →
   « raison : non écrit au-delà de l'exemple du hêtre (journal l. 89) ».
2. **0002 l. 31** (« l'autonomie d'une application est cédée… ») : déduction → « non écrit ».
3. **0002 crible F (l. 22)** : citer `README.md` l. 116 (« Publiée par tag… Une correction de ces
   briques se fait là-bas »).
4. **0004 l. 14** : « spec v1 l. 150-153 » est la numérotation d'avant la PR #17 → citer l. 141-147.
5. **0006, statut** : ajouter journal l. 72 (Pascal décide la mission correctif).
