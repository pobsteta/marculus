# Liste de pré-fusion

> Bilan n° 1 (2026-10-24), cause n° 2 du Pareto : la forme non contrôlée avant fusion. Tenue par
> l'orchestrateur, cochée **avant chaque merge** vers `main`, y compris les PR lâchées.

1. **Titre de la PR en commit conventionnel** (`feat:`, `fix:`, `docs:`, `test:`, `ci:`…) : il
   devient le message du squash (la version, calendaire `vAAAA.MM.N`, ne dépend plus du titre). Sinon `gh pr edit --title`.
2. **Rapport complet de l'agent** : redite, fait / pas fait, tests, échantillon terrain, ligne de
   journal. Un rapport vide ou tronqué = écart au journal, relecture sur pièces.
3. **Relecteur « prêt »** sur la tête de la PR, et **CI verte** (`gh pr checks`).
4. **Changement visible ?** Capture ou nuancier montré à Pascal **avant** le merge, ou vérification
   sur le téléphone annoncée et faite après l'installation. **Bilan n° 2 (2026-11-10)** : pour une
   mission d'écran, les captures (clair et sombre, FR et EN, 384 et 360 dp) sont jointes au rapport
   de l'`afficheur` **avant** la relecture ; sans elles, pas de relecture.
5. **Dépendances** : une seule version par bibliothèque ; le `testeur` n'a pas lu le code qu'il teste.

> **Bilan n° 3 (2026-11-27), cause n° 1 du Pareto : le point 6 se coche aussi avant de soumettre un fait à
> Pascal (question, plan, fiche) et avant de dire « prêt », pas seulement avant le merge. Il est joint à la
> consigne de chaque agent qui rend un document (fiche, ADR, rapport, brouillon de brief), et l'orchestrateur
> se l'applique à lui-même. Le point 7 est celui de l'orchestrateur.** Numéros de ligne du journal : état du
> commit `0c6b889`.

6. **Source relue, et son état dit.** Trois sous-cases, à cocher une à une.
   - **6a. Preuve et état de la source.** Tout fait soumis porte sa preuve ; sans preuve, il est dit
     « non vérifié » (règle de `CLAUDE.md`, « Règles techniques communes » : renvoi, pas de copie). Toute
     preuve citée (fichier et ligne, commit, tag, ligne du journal) est relue sur l'état cité (commit ou
     tag, `git status`) au moment de rendre, et le message, la fiche ou le rapport dit cet état : commit ou
     tag, `git status` propre (sinon : « copie de travail non commitée »). Tout renvoi de ligne est rouvert.
     Incidents : l. 201 (preuves fausses), l. 266 (renvoi faux, affirmation sans preuve), l. 341 (source lue =
     copie de gftools non commitée ; l. 335 est la même affaire), l. 351 (règle attribuée au mauvais outil),
     l. 205 (« sans risque » affirmé sans relire le script : à dire « non vérifié »), l. 377 (source avancée
     non citée, deux engagements sans preuve).
   - **6b. Décompte.** Tout nombre est recompté en entier, pas additionné de mémoire. Incident : l. 226
     (deux codes additionnés au lieu d'être recomptés par niveau).
   - **6c. Contrôle terrain.** Il est rejoué **dans l'unité de l'écran** que Pascal verra (classe de
     diamètre ou circonférence, hauteur, tarif affiché), avec un cas qui fait réellement jouer la règle.
     Incidents : l. 386 (cas qui ne faisait pas jouer la règle : aucun saut à fct > 0,95), l. 393 (cas saisi
     en circonférence, écran en classe de diamètre).
   - **Non couverte** : l. 299 (section « Réseau » sans NTRIP ni cadastre) est un oubli de contenu (une
     section incomplète), pas une preuve fausse. Le point 6 ne l'aurait pas arrêtée.
7. **Boîte `~/dev/briefs/vers-marculus/` relevée, briefs sortants déposés.** La boîte est lue à chaque
   `/boucle` (étape Observer) et avant de dire « prêt » ; chaque brief en attente est traité ou daté dans
   le journal ; les briefs sortants approuvés par Pascal sont déposés dans la boîte du destinataire.
   Incident (journal, hors des 10 de la cause n° 1) : l. 399 (4 briefs en attente depuis le 2026-11-20 et
   le 2026-11-24, jamais relevés).
