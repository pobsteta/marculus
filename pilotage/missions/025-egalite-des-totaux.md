# Mission 025 — un test qui voit deux écrans compter différemment

## Mission

**En une phrase (quoi, et pourquoi) :** écrire un test qui, pour un même journal de tiges, exige
le même nombre de tiges et les mêmes volumes partout où Marculus les affiche ou les exporte, pour
qu'une règle d'annulation recopiée ne revienne jamais en silence. Cause n° 2 du Pareto du bilan
n° 2 (journal du 2026-11-10) : 6 incidents, dont « Par parcelle » à 23 tiges contre 22 dans Statut
(journal du 2026-11-09) et quatre missions (018, 019, 021, 022) pour y remédier. Décision de
Pascal du 2026-11-10 : « je valide ».

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Ce qui existe (preuves)

- **Règle unique :** `JournalNet.tiges` (`core/src/main/kotlin/fr/marculus/core/JournalNet.kt`),
  utilisée depuis la 022 par `VolumesMartelage.totaux` et `AffouageLots` (PR #49, 76bd939).
- **Les endroits qui comptent :**
  - Statut : `TotauxMartelage.totalContexte()`, `VolumesMartelage.totaux` ;
  - Par lot : `AffouageLots.bilan` ;
  - Par parcelle : `ParParcelle.repartir` + `volumesSansPosition` (019, 021) ;
  - CSV foncier : `CsvFoncier.construire` (019) ;
  - liste des contextes : `ResumeContexte.nbTiges`, net en SQL (`data/.../Daos.kt`, 018) ;
  - boîte de fusion d'un `.marsync` : nombre net (016, 9099e3d) ;
  - CSV du journal net et du bilan par lot : `ExportCsv`, `ExportBilanLots`.

## Règles

- Un seul jeu de journaux d'essai (désordre, paquets, annulations partielles, orphelines,
  plusieurs cases et lots, tiges sans position), passé à **tous** les endroits ci-dessus ;
  égalité exigée sur le nombre net et sur les volumes tige, houppier, total (à l'arrondi machine).
- Exceptions **décidées** et écrites dans le test, avec leur source : l'annulation orpheline
  absente du CSV foncier (décision de Pascal du 2026-11-09) ; le lot courant qui ne redescend pas
  (`AffouageLots.etat`, 022).
- Les tests doivent être **verts** aujourd'hui ; s'ils trouvent un écart, le testeur s'arrête et
  le rapporte (c'est un défaut, à trancher), il ne l'efface pas.
- Ne touche pas au code de production.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | `:core:test` ; `:data` a des tests Room JVM (mission 001) |
| A-t-il les moyens ? | oui | |

**Temps de la main :** lâcher (tests seuls, sans changement d'écran) : fusion par l'orchestrateur
si relecteur « prêt », CI verte et `pilotage/pre-fusion.md` coché.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **`testeur` (sonnet)** seul.

## Harnais

- **Peut :** ajouter des tests dans `core/src/test/` et `data/src/test/`, dans sa branche.
- **Ne doit pas :** modifier le code de production ni un test existant ; toucher à `main`.

## Tests

- **Comment saura-t-on que c'est juste :** le relecteur réintroduit à la main, dans un worktree
  jetable, une pile d'annulation divergente dans un des endroits : le test doit échouer.

## Terrain

- Aucun changement visible.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Le jour où quelqu'un recopie une règle d'annulation, la CI devient rouge avant que Pascal ne voie
deux chiffres différents sur son téléphone.
