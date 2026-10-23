# Journal de Marculus

> **Règle du sommier : on n'efface pas, on ajoute.**
> Chaque inscription est datée et signée (agent ou personne). Une correction est une
> nouvelle ligne qui renvoie à l'ancienne. Un hook refuse toute modification qui
> supprimerait ou réécrirait une inscription existante.
>
> Format : `AAAA-MM-JJ · auteur · mission · ce qui a été fait / décidé / bloqué`
>
> **Le carnet des écarts** (fiche n° 3), en une ligne marquée `écart` :
> `AAAA-MM-JJ · auteur · mission · écart · attendu : … · vu : … · vérifié : comment (ou « non ») · décision : …`
> La décision est l'une des trois : **garder** (vérifié : on adopte), **creuser** (prochain
> essai, daté), **abandonner** (vrai hasard : on le note).
> Règle : on ne sanctionne pas un écart signalé ; on regarde d'abord, on corrige ensuite,
> s'il le faut.
>
> **Les changements de cap** (fiche n° 13), en une ligne marquée `cap`, signée par qui l'a
> décidé : `AAAA-MM-JJ · auteur · mission · cap · ancien → nouveau · pourquoi : … · fait : …`

2026-10-23 · Claude (orchestrateur) · — · méthode installée (CLAUDE.md, docs/brief.md, pilotage/, docs/adr/) sur un projet déjà avancé (v0.55, spec docs/specs/2026-06-21-marculus-v1-spec.md) · reste à remplir : docs/brief.md (rubriques en italique, par Pascal), puis /imperatrice:brief ; .claude/test-command à écrire à l'étape 7 de /brief
2026-10-23 · Claude (orchestrateur) · — · relevé des briefs : `~/dev/briefs/vers-marculus/` vide ; 13 briefs Marculus traités (INDEX, v0.37 → v0.62) ; deux briefs dans `~/dev/surculus/pilotage/briefs/vers-marculus/` : bibliothèques communes (déjà traité le 2026-10-17, copie non classée côté Surculus) et README `terrain` + style commun (2026-10-22, non traité). Rubriques factuelles de docs/brief.md remplies, bac à sable du kanban garni ; « Ma réponse d'abord », problème et coût restent à Pascal
2026-10-23 · Pascal · — · cap · `tarifsProposes` « prime en entier » (brief du 2026-10-16) → seul le niveau `foret` est lu ; `ser`, `greco` et `national` font double emploi avec les tables IFN embarquées en v0.62.0 · pourquoi : même règle et mêmes données des deux côtés, seul l'échelon forêt manque au téléphone · fait : brief `~/dev/briefs/vers-nemetonshiny/2026-10-23-marculus-tarifs-proposes-niveau-foret.md`, carte au bac à sable
2026-10-23 · Claude (orchestrateur) · — · brief Surculus du 2026-10-15 (bibliothèques communes) : statut « traité le 2026-10-17 » ajouté en tête de la copie de `~/dev/surculus/pilotage/briefs/vers-marculus/`, sans déplacement ni commit (dépôt Surculus) ; docs/brief.md : coût complet = inconnu par défaut
2026-10-23 · Claude (orchestrateur) · — · libellés du lot max. d'affouage : unité en tête (« (m³) lot max. », « (stères) lot max. »), PR #3 fusionnée par squash après accord de Pascal ; méthode installée versée par PR
2026-10-23 · contradicteur · — · critique du brief (porte n° 1) : problème = défaut d'écran déjà corrigé (v0.63.5), pas de problème ouvert ; borne du lot ambiguë (m³ tige ou houppier, stères à coefficient unique, portions communales) ; « hors ligne total » contredit par NTRIP, fonds en ligne et téléchargement Vosk ; bénéficiaires non sourcés ; brief factuel en retard (v0.62, formats 6-7 non signalés, docs/spec.md absent) · à trancher par Pascal
2026-10-23 · Pascal · — · arbitrage de la critique du brief : « oui pour tout » · reporté par l'orchestrateur dans docs/brief.md (symptôme distinct du problème, question ouverte sur l'unité du lot, rôles, critères de lisibilité, réseau, retards de la spec, formats 6-7 non signalés) et dans CLAUDE.md (docs/spec.md à venir) · restent à Pascal : problème du produit, problème du cycle, replis sans réseau, appareil perdu, unité du lot
