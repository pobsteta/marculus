# ADR 0011 — Le tarif NEMETON est embarqué dans `:core`, en version provisoire (paquet 0.2.2-provisoire)

- **Date :** 2026-11-27
- **Statut :** **décision acceptée par Pascal** (« b », journal l. 329 ; porte n° 2 « oui », l. 330) ; **texte de l'ADR proposé**, à valider par Pascal à la lecture de la PR ; moteur livré par les PR #85 et #87 (fusionnées, journal l. 366) ; **branchement dans l'application : non livré, questions de sens ouvertes** (voir « Ce qui reste ouvert »)
- **Proposée par :** Claude (orchestrateur), qui a présenté les options à Pascal (journal l. 329 ; texte de l'option « a » : non écrit)
- **Tranchée par :** Pascal

Règle du bilan n° 3 : chaque fait cité dit l'état de sa source. Journal lu dans `pilotage/journal.md` à `0c6b889` (`main`, 2026-11-27) ; clé d'une ligne = date · auteur · mission.

## La décision en une phrase

Marculus embarque dès maintenant le tarif NEMETON, tel que livré par le dépôt `tarif-nemeton` dans son paquet **0.2.2-provisoire** (constantes, manifeste, jeu de contrôle), codé en Kotlin dans `:core` et prouvé sur les 1 032 sorties du jeu de contrôle ; le résultat est **provisoire** tant que le paquet l'est, et le **branchement** (écran, exports, lots) vient au cycle suivant.

## Contexte

- Le tarif NEMETON est livré à Marculus sous la forme d'un paquet versionné (CSV de constantes autonome, manifeste avec empreintes, jeu d'arbres de contrôle) ; Marculus code les formules et refuse un paquet dont l'empreinte ne correspond pas (ADR 0001 de `tarif-nemeton`, 2026-11-26, acceptée par Pascal, `~/dev/tarif-nemeton/docs/adr/0001-livrable-marculus.md` l. 8-16 ; état de `~/dev/tarif-nemeton` vérifié par le relecteur le 2026-11-27 : HEAD `43ef6a4` ; `docs/adr/0001-livrable-marculus.md` modifié en dernier par `fd97e12` (2026-11-26) ; aucune modification locale de ce fichier ni de `livrable/datapackage.json`. Numéros de ligne relus par l'archiviste sur le disque, sans `git`, dans cet état déclaré).
- Le paquet est **provisoire** : `datapackage.json` porte `"version": "0.2.2-provisoire"` (l. 4) et ses `limites_connues` (l. 28-40) disent que l'ADR « D5 reste à rédiger » (l. 29), que 68 et 75 (autres conifères) n'ont aucune constante, et que le repli par le genre est « à appliquer par Marculus, sur décision de Pascal » (l. 33). Fichier embarqué : `core/src/main/resources/nemeton/datapackage.json` (sur `main` à `0c6b889`).
- Pascal a choisi d'intégrer sans attendre la version définitive : « `b` : intégrer NEMETON dès le paquet 0.2.2-provisoire » (journal 2026-11-27 · Claude (orchestrateur) · 037-040, l. 329). Pourquoi « b » plutôt que d'attendre : **non écrit** (la ligne rapporte le choix, pas ses raisons).
- Le tarif est désigné « Tarif NEMETON » dans les rôles de `docs/brief.md` (journal 2026-11-27 · Pascal · bilan n° 3, l. 401).

## Ce qui a été décidé, décision par décision

| Décision de Pascal | Preuve (journal, clé) | Livré par |
|---|---|---|
| Embarquer NEMETON dès le paquet provisoire (« b ») ; équipe validée à la porte n° 2 | l. 329 (2026-11-27 · Claude (orchestrateur) · 037-040) ; l. 330 (2026-11-27 · Pascal · 037-040) | missions 037, 038, 039 |
| **Codes IFN des essences de Marculus**, ligne à ligne (11 lignes) : Chêne 3, Autres feuillus 49, Autres résineux 75 (non couvert), Érable 15, Frêne 17, Peuplier 19, Sorbier 23, Pin nu sans code, noms sans code (Arbre, Autre, Inconnu, Hêtres, Chênes), Feuillus 49, Résineux 75 | l. 353-363 (2026-11-27 · Pascal · 037, « ligne N/11 ») ; texte repris dans `docs/specs/2026-11-27-essences-nemeton.md` § « Décisions de Pascal » (l. 137-155) | PR #85 (04c5bf2) |
| **Repli par le genre (D5 côté Marculus)** : pins 51, 53, 54, 57, 58 cubés comme `Pinus_sylvestris` (52) ; chêne rouge (4) cubé comme `Quercus_spec` ; l'essence réellement utilisée accompagne chaque résultat. Pins : Pascal a choisi « A » contre la recommandation « B » de l'orchestrateur (l. 361 seulement) ; chêne rouge : « A » (l. 362), sans recommandation contraire écrite | l. 361 et l. 362 (2026-11-27 · Pascal · 037) | PR #85 (table) ; affichage : non livré |
| **Tolérance des tests** : `max(1e-6 relatif, 5e-9 absolu)` au lieu de 1e-6 relatif seul ; valeur gelée à la fusion de la PR #87 | l. 364 (2026-11-27 · Pascal · 038) ; code : `core/src/test/kotlin/fr/marculus/core/nemeton/PaquetDeTest.kt` l. 10-11 (`TOL_RELATIVE = 1e-6`, `TOL_ABSOLUE = 5.0001e-9`, sur `main` à `0c6b889`) | PR #87 (79d2f22) |
| **Entrées impossibles, option A** : le moteur refuse D ≤ 0, H ≤ 0 et, pour un résineux, H ≤ 1,301 m (`IllegalArgumentException`) ; l'écran dira « hauteur impossible » au branchement ; écart au brief NEMETON (« pas de refus ») à signaler au dépôt `tarif-nemeton` | l. 365 (2026-11-27 · Pascal · 039) ; code : `TarifNemeton.kt` l. 57-59 (KDoc), 63-64 (D, H) et 178 (résineux) (sur `main` à `0c6b889`) | PR #87 (79d2f22) |
| Mentions à ajouter à l'écran « À propos » : la source d'EMERGE (Deleuze et al. 2014) et le tarif NEMETON (version du paquet, « provisoire ») ; absentes en v2026.11.5 (capture du 2026-11-27) | l. 368 (2026-11-27 · Pascal · branchement NEMETON) ; carte du bac à sable, `pilotage/kanban.md` l. 40 (sur `main` à `0c6b889`) | non livré |

Valeur de repli D5 : la table de repli est elle-même marquée « VALEUR PROVISOIRE » dans `docs/specs/2026-11-27-essences-nemeton.md` (l. 21-30) ; la règle de la spec NEMETON qu'elle applique n'a pas été lue par l'acquéreur (même fichier, l. 23). Le brief déposé chez `tarif-nemeton` demande de la confirmer (journal 2026-11-27 · Pascal · 043, l. 388). Pascal a retiré du brouillon la phrase « Marculus s'alignera sur votre réponse ; en attendant, il refuse » (journal 2026-11-27 · Pascal · 043 · « Retire la », l. 387) : **Marculus ne s'est pas engagé à s'aligner** sur la réponse de `tarif-nemeton`.

## Pourquoi « provisoire », et jusqu'à quand

- Le paquet est provisoire de l'aveu de son propre manifeste (`datapackage.json` l. 4 et l. 29). Marculus ne rend donc aucun résultat NEMETON comme définitif. Ce que Pascal a décidé : la mention « provisoire » dans l'écran « À propos », avec la version du paquet (journal l. 368). Que « provisoire » accompagne aussi le résultat à l'écran, et sous quelle forme : **question de sens ouverte** (`pilotage/kanban.md` l. 40), non tranchée.
- Fin du provisoire : à la livraison, par `tarif-nemeton`, d'un paquet qui n'est plus « provisoire » **et** à la rédaction de leur ADR D5 (à rédiger, `datapackage.json` l. 29). Date : **non écrite**.
- Quand le paquet change, Marculus refuse un paquet dont l'empreinte ne correspond pas (ADR 0001 de `tarif-nemeton`). Rejouer le jeu de contrôle et le sort de la tolérance gelée (journal l. 364) à ce moment : **ouvert**, non écrit.

## Ce qui reste ouvert (non tranché ici, cycle 11)

Ces questions ne sont **pas** tranchées par cet ADR. Elles sont nommées dans `pilotage/kanban.md` l. 40 (sur `main` à `0c6b889`) et dans le journal (2026-11-27 · Claude (orchestrateur) · 037-040, l. 329 : « questions de sens à Pascal, ADR 0011 » ; 2026-11-27 · Pascal · cycle 10, l. 369 : branchement au cycle 11) :

1. Volumes montrés quand une sortie manque (pas de bois fort tige des feuillus, pas de bois fort total des résineux).
2. Parts tige / houppier d'un lot d'affouage avec NEMETON.
3. Si « provisoire » accompagne le résultat à l'écran, et sous quelle forme (la mention de « À propos » est décidée, l. 368).
4. Texte et comportement de « hauteur impossible » (et « non calculé » pour EMERGE, voir ADR 0012).
5. Modèle de données et exports (CSV, `.marsync`) : quelle colonne porte le volume NEMETON, la version du paquet, l'essence réellement utilisée.
6. Réponse de `tarif-nemeton` au brief du 2026-11-27 (refus des entrées impossibles, repli D5) : attendue dans `~/dev/briefs/vers-marculus/` (journal l. 388) ; pas de réponse au `0c6b889`.

Un ADR ou une ligne datée ajoutée ici (règle du sommier) rendra chaque réponse.

## Alternatives

- Attendre un paquet non provisoire : écartée par Pascal (« b »), raison non écrite.
- Générer du Kotlin depuis `tarif-nemeton`, table pré-calculée, JSON, SQLite : examinées et écartées dans l'ADR 0001 de `tarif-nemeton` (l. 30-43) ; Marculus suit leur choix (CSV + formules codées + jeu de contrôle).
- Option « a » présentée à Pascal en face de « b » : **non écrite** dans le journal.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | La plus simple qui tienne ? | Moteur dans `:core`, paquet lu en ressource, aucune dépendance ajoutée (relecteur, journal l. 350) | Formules codées deux fois (Python chez NEMETON, Kotlin ici) : écart rendu visible par 1 032 cas | Non écrit ; déduit de ce que Pascal a accepté en choisissant « b » (l. 329) |
| **R** | Qui fait quoi ? | acquereur (037), testeur (038), dev opus (039), relecteur ; Pascal tranche | | |
| **A** | Et si l'imprévu arrive ? | Entrée impossible : refus déclaré (option A) ; code non couvert : `null`, repli actuel de Marculus ; paquet altéré : refusé par l'empreinte (relecteur l. 350) | Écart au brief NEMETON « pas de refus », signalé à `tarif-nemeton` | Non écrit ; déduit du choix de l'option A par Pascal (l. 365) |
| **N** | Idées étranges écoutées ? | Non écrit | | |
| **C** | Coût pour l'ensemble ? | Un résultat provisoire affiché à des marteleurs : mention « provisoire » de « À propos » décidée (l. 368) ; à l'écran du résultat : ouvert | Résultats susceptibles de changer au prochain paquet | Non écrit |
| **É** | Que réutilisons-nous ? | Le paquet de `tarif-nemeton` tel quel (octets identiques, SHA-256 égaux au manifeste, journal l. 350) ; la table de rattachement `RATTACHEMENT_ESSENCES` de `Cubage.kt` | | |

## Valeurs sacrifiées

- **Le définitif** : on livre un calcul provisoire plutôt que d'attendre. Contrepartie : mention « provisoire » dans « À propos » (l. 368) ; à l'écran du résultat : ouvert.
- **L'accord exact avec le brief NEMETON** (« hors domaine : calcul et alerte, pas de refus », journal l. 351) : écart accepté par Pascal (l. 365).

## Sources

- `pilotage/journal.md` à `0c6b889` : l. 329, 330, 348-351, 353-366, 368, 369, 387, 388, 401.
- PR #85 (04c5bf2, table des essences), PR #87 (79d2f22, moteur NEMETON) : fusionnées le 2026-11-27 (journal l. 366).
- `docs/specs/2026-11-27-essences-nemeton.md` (sur `main` à `0c6b889`).
- `core/src/main/kotlin/fr/marculus/core/nemeton/TarifNemeton.kt`, `core/src/main/resources/nemeton/` (sur `main` à `0c6b889`).
- `~/dev/tarif-nemeton/docs/adr/0001-livrable-marculus.md` et `livrable/datapackage.json` (état vérifié par le relecteur le 2026-11-27 : HEAD `43ef6a4`, ADR modifié en dernier par `fd97e12`, pas de modification locale).
- Fiches `pilotage/missions/037-essences-nemeton.md` à `039-moteur-nemeton.md`, `043-brief-tarif-nemeton.md`.

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ? Réponse de Pascal : non écrite.
