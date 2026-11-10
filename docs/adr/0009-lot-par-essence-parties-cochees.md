# ADR 0009 — Lot d'affouage : les parties cochées (tige, houppier) de chaque essence

- **Date :** 2026-11-10
- **Statut :** **décision acceptée (Pascal, 2026-11-10, `pilotage/journal.md` l. 220 et 235) ; texte proposé**, à valider par Pascal à la lecture de la PR ; livrée par la PR #58 (07b5870, mission 026) et la PR #60 (85030ff, mission 027)
- **Remplace :** [ADR 0004](0004-lot-affouage-houppier-seul.md) (houppier seul quand l'arbre en a un), conservé pour mémoire
- **Proposée par :** Pascal, après l'« Affouage » de Daniel (journal l. 220)
- **Tranchée par :** Pascal

## La décision en une phrase

Le volume d'un lot d'affouage compte, pour chaque tige, les parties cochées de **son essence** : la tige si « tige » est cochée, plus le houppier si « houppier » est coché ; les deux sont cochées par défaut.

## La règle (source unique)

Les autres documents renvoient ici. Pour une tige d'essence E, le volume compté au lot est :
`(tige cochée ? volumeTigeM3 : 0) + (houppier coché ? volumeHouppierM3 : 0)`
(`AffouageLots.volumes`, `core/src/main/kotlin/fr/marculus/core/AffouageLots.kt` l. 30-35).

- Cases par essence, **les deux cochées par défaut**, contextes existants compris ; une essence sans réglage compte tige et houppier (`Contexte.partsLotDe`, `core/.../model/Modeles.kt` l. 143 ; `PartsLot`, même fichier l. 55). Les deux décochées sont permises : l'essence compte 0 m³ (journal l. 220).
- Un houppier non calculable (part 0 % en Schaeffer, essence non couverte par EMERGE, tarif Aucun) vaut 0 : le cocher n'ajoute rien (`AffouageLots.kt` l. 24-28).
- Les cases ne changent ni la numérotation des lots, ni le fait que le lot soit **figé sur la tige** (`Tige.lot`).

## Contexte

Ancienne règle (2026-10-24, ADR 0004) : « houppier seul s'il y a une part de houppier, sinon le volume total ». Pourquoi le changement (journal l. 220) : dans l'« Affouage » de Daniel, le lot ne comptait que 2,36 m³ (le houppier) et pas 13,39 + 2,36 m³. Pascal a décidé : deux cases par essence, « tige » et « houppier » ; les deux cochées par défaut ; les deux décochées permises ; cases portées par le `.marsync` et le CSV (format 8, brief à nemetonshiny).

## Ce qui est livré, avec les preuves

| Où | Quoi | Preuve |
|---|---|---|
| Modèle | `PartsLot(tige, houppier)`, `Contexte.partsLot` | `Modeles.kt` l. 55, 127-143 |
| Calcul | `AffouageLots.volumes` ; bilan par lot et état courant inchangés de forme | `AffouageLots.kt` l. 30-35 ; tests `AffouageLotsPartsTest`, `AffouageLotsTest` |
| Base | Room **v18**, colonne `partsLot` (vide = les deux cochées, `ADD COLUMN` seul, aucune donnée réécrite) | `MarculusDatabase.kt` l. 57 et 63 ; `Entities.kt` l. 60 ; test `PartsLotDepotTest` |
| `.marsync` | clé `partsLot` par contexte, **seulement les essences réglées** (l'écran n'enregistre pas une essence aux deux cases cochées) ; une essence absente = cochée ; clé absente d'un vieux fichier = cases locales gardées pour un contexte connu | `SauvegardeRepository.kt` l. 185-193 et 247-256 ; `ReglagesTerrain.kt` l. 21 et 43 ; `CreationContexteScreen.kt` l. 533 ; journal l. 235 |
| CSV | **format 8** : ligne `PartsLot;Chêne=tige:1,houppier:1|…` (toutes les essences du contexte), après `MillesimeTarifs` ; `FORMAT = 8` | `ExportCsv.kt` l. 48-51, 57, 95, 169-172 ; test `ExportCsvPartsLotTest.kt` l. 54 et 66 |
| Écran (027) | cases **T** et **H** sur la ligne de l'essence, champ de part renommé « % houpp. », ligne « Cette essence ne compte pas dans le lot » aussi quand les cases cochées ne peuvent rien ajouter | `LotEssence.kt` (`compteAuLot`) ; `CreationContexteScreen.kt` l. 134, 377-378, 533-554 ; PR #60 (85030ff) ; journal l. 238-240 |

**Changement de sens sans changement de nom.** `AffouageLotTotalM3` et la colonne `AffouageLotM3` du CSV, `affouageLotTotalM3` et `affouageLotM3` du `.marsync` gardent leur nom et leur place, mais comptent désormais les parties cochées ; jusqu'au format 7, ils comptaient le houppier seul quand l'essence en avait un (`ExportCsv.kt` l. 48-51). Un fichier se lit donc avec son `FormatCsv`.

Brief au consommateur : `~/dev/briefs/vers-nemetonshiny/2026-11-10-marculus-csv-format-8-lot-par-essence.md` (hors dépôt).

## Alternatives

Conserver « houppier seul » (ADR 0004) : écartée par Pascal (journal l. 220). Un réglage unique pour tout le contexte plutôt que par essence : non écrit dans le journal.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | La plus simple qui tienne ? | Deux booléens par essence, défaut « cochés » : aucune migration de données | Base v18, format CSV 8, un brief | Oui (journal l. 231, 235) |
| **R** | Qui fait quoi ? | Testeur (21 tests, PR #58, 07b5870), dev (PR #58, 07b5870), relecteur (prêt), afficheur (027, PR #60, 85030ff) | | |
| **A** | Et si l'imprévu arrive ? | Vieux `.marsync` : cases locales gardées (mutation « vieux fichier écrasant » détectée par 2 échecs, journal l. 237) ; migration `ADD COLUMN` seule | Un ancien lecteur lit `AffouageLotM3` avec l'ancien sens | Non écrit |
| **N** | Idées étranges écoutées ? | Non écrit | | |
| **C** | Coût pour l'ensemble ? | 8 attentes de tests existants changées, toutes dérivées de « les deux cochées par défaut » (journal l. 236) | Les lots déjà écrits (`Tige.lot`) ne sont pas renumérotés ; l'état courant se recalcule avec la nouvelle règle | Non écrit |
| **É** | Que réutilisons-nous ? | `Cubage.volumesUnitaire`, `JournalNet`, la forme des formats 6 et 7 | | |

## Valeurs sacrifiées

Non écrit.

## Sources

- `pilotage/journal.md` du 2026-11-10 : l. 220 (cap de Pascal), 230-231 (plan du dev validé), 234-237 (testeur, Pascal sur la forme `.marsync` et CSV 8, dev, relecteur 026), 238-240 (afficheur, relecteur, Pascal sur 027).
- PR #58 (07b5870), PR #60 (85030ff).
- Spec : `docs/specs/2026-10-07-contexte-affouage.md` (correction du 2026-11-10), `docs/specs/2026-06-21-marculus-v1-spec.md` § 5 bis et § 7.
