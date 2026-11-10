# Mission 022 — une seule règle d'annulation

## Mission

**En une phrase (quoi, et pourquoi) :** faire calculer à `VolumesMartelage.totaux` ses volumes à
partir de `JournalNet.tiges`, pour qu'il ne reste dans `:core` qu'une seule règle d'annulation,
sans changer un seul résultat. Aujourd'hui la règle existe en deux copies équivalentes
(`core/src/main/kotlin/fr/marculus/core/VolumesMartelage.kt` l. 84-102 et
`core/src/main/kotlin/fr/marculus/core/JournalNet.kt` l. 18-40, relecture de la 019 du
2026-11-10) ; une correction future dans l'une oublierait l'autre, et Statut divergerait de Par
lot, Par parcelle et des CSV. Décision de Pascal du 2026-11-10 : « Je valide les 4 ».

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Ce qui existe (preuves)

- **Copie 1 :** `VolumesMartelage.totaux` trie par `horodatage` puis `uuid`, tient une pile par
  case `CompteurCle(essence, classe)` ; une annulation orpheline retire le cubage de sa propre
  saisie (l. ~101).
- **Copie 2 :** `JournalNet.tiges` : même tri, même pile, décrément sur place ; une annulation
  orpheline est rendue avec `quantite = reste` (l. 37). Introduite avec l'affouage (59c6493).
- **Utilisateurs :** `JournalNet.tiges` — `AffouageLots.kt` l. 67, `ExportCsv.kt` l. 119, et
  `ParParcelle` / `CsvFoncier` (mission 019) ; `VolumesMartelage.totaux` — Statut
  (`StatutHistoriqueScreen.kt` l. 228), `ExportCsv.kt` l. 63, `SauvegardeRepository.kt` l. 142.
- **Équivalence vérifiée par le relecteur** (019, c4de624) et par `ParParcelleTest.kt` l. 154-168.

## Règles

- **Aucun résultat ne change.** Les tests existants de `VolumesMartelage`, `JournalNet`,
  `AffouageLots`, `ExportCsv`, `ParParcelle` et `CsvFoncier` restent verts **sans modification**.
- `VolumesMartelage.totaux` part de `JournalNet.tiges` ; la pile de `VolumesMartelage` disparaît.
- L'annulation orpheline garde son traitement actuel (cubage de sa saisie retiré).
- Aucun changement d'écran, de format de fichier, de base de données.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | refactorisation pure, filet de tests existant |
| A-t-il les moyens ? | oui | `:core:test` |

**Temps de la main :** appuyer (correctif sans changement d'écran : genre lâché si relecteur
« prêt », CI verte et pré-fusion cochée, `pilotage/equipe.md` l. 18-20).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **`testeur` puis `dev` (sonnet).** Le testeur écrit, sans lire la refactorisation, des tests
  d'égalité `VolumesMartelage.totaux` = somme des cubages de `JournalNet.tiges` sur des journaux
  variés (désordre, paquets, orphelines, plusieurs cases, EMERGE et Schaeffer) ; le dev
  refactorise.

## Harnais

- **Peut :** modifier `VolumesMartelage.kt` (et `JournalNet.kt` si besoin d'exposer ce qui
  manque), dans sa branche, partie de `main` après la fusion de la #43.
- **Ne doit pas :** modifier un test existant ; toucher à `app`, à `data`, à `main`.

## Tests

- Tests existants inchangés et verts ; tests d'égalité du testeur verts.
- **Comment saura-t-on que c'est juste :** `grep` ne trouve plus de pile d'annulation hors de
  `JournalNet.kt` ; le relecteur le vérifie.

## Terrain

- **Échantillon que Pascal vérifiera :** aucun changement visible ; contexte « Affouage » : Statut,
  Par lot et Par parcelle donnent toujours les mêmes nombres et volumes.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Une seule règle d'annulation dans le code, et aucun chiffre qui bouge.
