# Liste de pré-fusion

> Bilan n° 1 (2026-10-24), cause n° 2 du Pareto : la forme non contrôlée avant fusion. Tenue par
> l'orchestrateur, cochée **avant chaque merge** vers `main`, y compris les PR lâchées.

1. **Titre de la PR en commit conventionnel** (`feat:`, `fix:`, `docs:`, `test:`, `ci:`…) : il
   devient le message du squash et fixe la version de la release. Sinon `gh pr edit --title`.
2. **Rapport complet de l'agent** : redite, fait / pas fait, tests, échantillon terrain, ligne de
   journal. Un rapport vide ou tronqué = écart au journal, relecture sur pièces.
3. **Relecteur « prêt »** sur la tête de la PR, et **CI verte** (`gh pr checks`).
4. **Changement visible ?** Capture ou nuancier montré à Pascal **avant** le merge, ou vérification
   sur le téléphone annoncée et faite après l'installation.
5. **Dépendances** : une seule version par bibliothèque ; le `testeur` n'a pas lu le code qu'il teste.
