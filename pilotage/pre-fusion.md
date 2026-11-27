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

> **Bilan n° 3 (2026-11-27), cause n° 1 du Pareto : les points 6 et 7 se cochent aussi avant de soumettre
> un fait à Pascal (question, plan, fiche) et avant de dire « prêt », pas seulement avant le merge.**

6. **Source relue, et son état dit.** Toute preuve citée (fichier et ligne, commit, tag, ligne du
   journal) a été relue à la source **le jour même**, et la fiche dit l'état de cette source : commit ou
   tag, et `git status` propre (sinon : « copie de travail non commitée »). Tout nombre est recompté, tout
   renvoi de ligne rouvert. Tout contrôle terrain est rejoué **dans l'unité de l'écran** que Pascal verra
   (classe de diamètre ou circonférence, hauteur, tarif affiché), avec un cas qui fait réellement jouer la
   règle. Incidents que ce point aurait arrêtés (journal) : l. 201, 266, 299, 377 (preuves ou renvois faux
   dans des documents d'agents) ; l. 205 (« sans risque » affirmé sans relire le script) ; l. 226 (nombres
   additionnés au lieu d'être recomptés) ; l. 335 et 341 (source lue = copie de gftools non commitée) ;
   l. 351 (règle attribuée au mauvais outil) ; l. 386 et 393 (contrôle terrain jamais rejoué en unités de
   l'écran).
7. **Boîte `~/dev/briefs/vers-marculus/` relevée, briefs sortants déposés.** La boîte est lue à chaque
   `/boucle` (étape Observer) et avant de dire « prêt » ; chaque brief en attente est traité ou daté dans
   le journal ; les briefs sortants approuvés par Pascal sont déposés dans la boîte du destinataire.
   Incident (journal) : l. 399 (4 briefs en attente depuis le 2026-11-20 et le 2026-11-24, jamais relevés).
