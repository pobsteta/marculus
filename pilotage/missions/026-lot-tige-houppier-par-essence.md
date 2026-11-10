# Mission 026 — le lot d'affouage compte les parties cochées, essence par essence

## Mission

**En une phrase (quoi, et pourquoi) :** en affouage, chaque essence porte deux cases, « tige » et
« houppier », et le volume qu'une tige ajoute à son lot est la somme des parties cochées de son
essence, pour que la commune choisisse ce qu'elle donne aux affouagistes au lieu d'une règle fixe.
Décision de Pascal du 2026-11-10 (journal, ligne `Pascal · 026`) ; elle remplace la règle du
2026-10-24 (journal l. 73 : « houppier seul s'il y a une part de houppier, sinon le volume total »).

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Ce qui existe (preuves, à 4dd7526)

- La règle actuelle : `AffouageLots.volumes` (`core/src/main/kotlin/fr/marculus/core/AffouageLots.kt`
  l. 20-45) : `lotM3 = houppier` si l'essence a un coefficient de houppier (EMERGE couvert, ou part
  de houppier > 0 en Schaeffer), sinon `tige`.
- La borne du lot : `Contexte.volumeMaxLotM3`, la tige qui atteint ou dépasse la borne ferme son
  lot (`AffouageLots.kt` l. 10-11, 76, 101-113).
- Les réglages par essence voisins : `Contexte.partHouppier` et `Contexte.tarifsEssence`
  (`core/src/main/kotlin/fr/marculus/core/model/Modeles.kt` l. 113-115) ; base Room version 17
  (`data/.../db/MarculusDatabase.kt` l. 53) ; CSV `FormatCsv;7` (`ExportCsv.kt` l. 43-51).
- Le volume du lot voyage aujourd'hui dans le `.marsync` par tige (`affouageLotM3`).

## Règles (décisions de Pascal du 2026-11-10)

1. Deux réglages par essence du contexte, **tige** et **houppier**, utiles seulement quand
   « Affouage » est coché. Volume de lot d'une tige = (tige si coché) + (houppier si coché).
2. **Défaut : les deux cochés**, pour les nouveaux contextes **et pour les contextes existants**
   (migration). Les lots existants grossissent donc : « Affouage » de Daniel passe de 2,36 à
   15,75 m³ pour 14 m³ de borne. Le numéro de lot déjà porté par chaque tige ne change pas.
3. **Les deux décochées : permis**, l'essence compte 0 m³ (ses tiges restent comptées en nombre).
4. Les cases voyagent dans le `.marsync` (au contexte, comme `partHouppier`) et dans l'en-tête du
   CSV : **format 8**, signalé à nemetonshiny (mission 028). Un `.marsync` ancien, sans cases :
   défaut « les deux cochées ».
5. Une essence sans houppier calculable (part 0 en Schaeffer, hors EMERGE) a un houppier de 0 :
   cocher « houppier » n'ajoute rien.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : `partHouppier` est le modèle (contexte, base, `.marsync`, CSV) | |
| A-t-il les moyens ? | oui, après les tests du testeur (journaux d'essai, `EgaliteDesTotauxTest`) | testeur d'abord |

**Temps de la main :** **tenir** : modèle de données, migration de base, formats `.marsync` et CSV.
Le dev rend un plan (champs, migration 17 → 18, format 8, ordre des commits) que Pascal valide.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` (sonnet) écrit d'abord les tests d'acceptation, sans lire l'implémentation ;
  puis `dev` (**opus** : modèle de données et justesse des volumes).
- **Ce qui ne leur est pas demandé :** l'écran des cases (mission 027), la doc et le brief (028).

## Harnais

- **Peut :** `:core` (modèle, `AffouageLots`, `ExportCsv`, `.marsync`), `:data` (entité, migration
  17 → 18, `SauvegardeRepository`), leurs tests.
- **Ne doit pas :** toucher aux écrans, changer la règle d'annulation (`JournalNet`, ADR 0007),
  renuméroter les lots déjà portés par les tiges, toucher à `main`.
- **Sources :** cette fiche, `docs/specs/2026-10-07-contexte-affouage.md`, ADR 0007, journal l. 73.

## Valeur provisoire (chiffre, réglage)

Aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune ; part de `origin/main`.
- **Ordre de fusion :** 026 → 027 → 028.
- **CI rouge attendue :** les tests du testeur sont rouges jusqu'au commit du dev, sur la même branche ;
  la PR ne s'ouvre que verte.

## Tests

- `AffouageLots.volumes` : 4 combinaisons de cases × Schaeffer avec part / sans part / EMERGE.
- Cumul et borne : un lot qui passe la borne avec les deux cases ; numéros de lot inchangés.
- Migration 17 → 18 : un contexte existant reçoit « les deux cochées ».
- `.marsync` aller-retour avec cases ; fichier ancien sans cases → défaut.
- CSV `FormatCsv;8` : en-tête porteur des cases ; `EgaliteDesTotauxTest` reste vert.

## Terrain

- **Échantillon :** « Affouage » de Daniel : Par lot affiche 15,75 m³ au lot 1 avec les deux cases ;
  houppier seul → 2,36 m³ ; tige seule → 13,39 m³.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal (porte n° 3).
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** au plan du dev, puis à la PR.
- **Qui tranche :** Pascal.
