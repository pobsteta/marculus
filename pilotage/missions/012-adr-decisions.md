# Mission 012 — ADR des décisions déjà prises

## Mission

**En une phrase (quoi, et pourquoi) :** écrire dans `docs/adr/` les ADR 0001 à 0006 des décisions
déjà prises et éparses dans le journal et les specs, sans en prendre aucune nouvelle, pour qu'un
successeur retrouve chaque décision, ses alternatives et sa raison en moins d'une minute (bilan
n° 1 du 2026-10-24, validé par Pascal).

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

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
