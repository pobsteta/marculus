# Mission 013 — test de la reprise de l'ancien réglage « Thème sombre »

## Mission

**En une phrase (quoi, et pourquoi) :** écrire le test qui vérifie, dans `ReglagesRepository`, que
l'ancienne clé DataStore `theme_sombre` est lue (cochée → Sombre, sinon Système), que la clé `theme`
prime, et que `theme_sombre` n'est jamais réécrite, parce que cette reprise est livrée (v0.64.6)
sans test (journal, mission 008 ; bilan n° 1).

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | idiome des tests de `data/src/test/` (`ReglagesTerrainTest`) |
| A-t-il les moyens ? | à vérifier | DataStore en test JVM : s'il faut Robolectric ou un DataStore de test, le dire avant d'ajouter une dépendance |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `testeur` · **Modèle :** sonnet.
- **Exception à « sans lire le code » :** le comportement est déjà livré ; le testeur lit
  `ReglagesRepository.kt` pour l'appeler, mais tire ses cas de la fiche 008, pas du code.

## Harnais

- **Peut :** créer un test dans `data/src/test/` ; ajouter une dépendance de **test** seulement si
  elle est déjà utilisée dans `:data`, sinon s'arrêter et le dire ; dans sa branche.
- **Ne doit pas :** modifier le code de production ; toucher à `main`.

## Tests

- **Comment saura-t-on que c'est juste :** cas : `theme_sombre=true` seul → SOMBRE ;
  `theme_sombre=false` seul → SYSTEME ; aucune clé → SYSTEME ; `theme=CLAIR` + `theme_sombre=true`
  → CLAIR ; après une écriture, `theme_sombre` n'a pas changé de valeur ni été ajoutée. Une mutation
  (réécrire `theme_sombre`) fait échouer un test. `.claude/test-command` et la CI verts.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Une modification qui casse la reprise de l'ancien réglage échoue en CI.
