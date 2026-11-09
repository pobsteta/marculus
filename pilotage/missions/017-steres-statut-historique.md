# Mission 017 — stères à côté des m³ dans Statut / historique (affouage)

## Mission

**En une phrase (quoi, et pourquoi) :** pour un contexte d'affouage dont le volume maximal d'un
lot est renseigné, afficher chaque volume de Statut / historique **en m³ et en stères (st)**, dans
les quatre onglets, et ajouter les stères au CSV du bilan par lot. L'affouagiste pense en stères,
mais l'écran ne donne que des m³ (`StatutHistoriqueScreen.kt` l. 228-260, 645, 674, 817, 342,
379-394) ; le coefficient existe déjà sur le contexte (`Contexte.steresParM3`,
`core/src/main/kotlin/fr/marculus/core/model/Modeles.kt` l. 117, défaut 1,5 l. 129).
Demande et décisions de Pascal du 2026-11-09.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Décisions de Pascal (2026-11-09)

1. **Les quatre onglets** : Statut, Par parcelle, Historique, Par lot. Stères **arrondis à
   2 décimales** à l'affichage (d'abord 1, porté à 2 le 2026-11-09 : une petite tige ne doit
   pas s'afficher « 0,0 st » dans l'Historique).
2. **Tous les volumes** : tige, houppier, total, volume du lot, volume maximal d'un lot.
3. **Le CSV du bilan par lot** gagne aussi les stères (seul Pascal le lit aujourd'hui).

## Règles

- **Condition :** `contexte.affouage && contexte.volumeMaxLotM3 > 0`. Le maximum saisi en stères
  est enregistré en m³ (spec § 5 bis, `docs/specs/2026-06-21-marculus-v1-spec.md` l. 148-150) :
  une seule condition couvre « max en stères ou en m³ ». Hors condition, l'écran ne change pas.
- **Calcul :** stères = m³ × `contexte.steresParM3`, dans une fonction pure de `:core`, une seule,
  appelée par l'écran et par le CSV. Aucune donnée nouvelle en base, dans le `.marsync` ni dans
  le CSV d'échange (`ExportCsv`) ; la borne du lot reste en m³.
- **Affichage :** les m³ gardent leur précision actuelle (2 décimales ; 3 dans l'Historique), les
  stères suivent sur la même ligne avec 2 décimales, dans la langue du téléphone :
  « 12,34 m³ · 18,51 st ». Le libellé du volume maximal devient
  « Volume maximal d'un lot : 10,00 m³ · 15,00 st ». Les en-têtes du tableau « Par lot » se
  dédoublent ou portent les deux unités : l'`afficheur` choisit ce qui tient en portrait (cf.
  bilan du 2026-10-24 : « unité du lot lisible en portrait ») et le montre en capture.
- **CSV du bilan par lot (`ExportBilanLots`) :** sous la même condition, ligne d'en-tête
  `SteresParM3` après `VolumeMaxLot_m3`, ligne `VolumeMaxLot_st`, et colonnes
  `VolumeTige_st`, `VolumeHouppier_st`, `VolumeLot_st` après `VolumeLot_m3` ; total compris.
  Précision 2 décimales, comme l'écran (c'est un rapport à lire, `ExportBilanLots.kt` l. 8-13). _Précision et
  ordre des colonnes proposés par l'orchestrateur, à confirmer à la relecture._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | multiplication par un coefficient existant ; tout l'affichage est dans un seul fichier |
| A-t-il les moyens ? | oui | `StatutHistoriqueScreen.kt`, `ExportBilanLots.kt`, `strings.xml` FR et EN, tests `:core` existants (`ExportBilanLotsTest.kt`) |

**Temps de la main :** appuyer — mission d'écran (`pilotage/equipe.md` l. 22).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **`testeur` (sonnet)**, d'abord : tests d'acceptation de la fonction de conversion, de la
  condition et du CSV, depuis cette fiche, sans lire le code.
- **`afficheur` (sonnet)** : réalise l'écran et le CSV, captures avant / après des quatre onglets
  en portrait, clair et sombre, sur un contexte d'affouage d'essai.

## Harnais

- **Peut :** ajouter la fonction de conversion et la condition dans `:core` ; modifier
  `StatutHistoriqueScreen.kt`, `ExportBilanLots.kt`, les chaînes FR **et** EN ; mettre à jour la
  spec § 5 bis (l. 147-170) d'une ligne datée ; dans sa branche.
- **Ne doit pas :** changer le calcul des volumes ni des lots (`AffouageLots`,
  `VolumesMartelage`), le `.marsync`, le CSV d'échange, la base ; toucher à `main`.
- **Attention :** les stères se calculent sur la valeur non arrondie (pas 2 décimales de m³ × 1,5) ;
  un `steresParM3` ≤ 0 n'affiche pas de stères ; le symbole est « st », pas « stère(s) ».

## Tests

- **Unitaires (`.claude/test-command`) :**
  - conversion : 10 m³ à 1,5 → 15,00 st ; coefficient réglé (ex. 1,4) respecté ;
  - condition : affouage sans maximum, maximum sans affouage, contexte ordinaire → pas de stères ;
  - CSV du bilan : colonnes et ligne `SteresParM3` présentes sous condition, absentes sinon ;
    totaux en stères = somme des m³ × coefficient (à l'arrondi près) ; décimales dans la langue.
- **Comment saura-t-on que c'est juste :** CI de la PR verte ; captures des quatre onglets ;
  le relecteur vérifie qu'une seule fonction convertit.

## Terrain

- **Échantillon que Pascal vérifiera :** sur un contexte d'affouage à 10 m³ par lot (15 st),
  marteler quelques tiges ; chaque onglet montre m³ et st, 2 décimales en stères ; un contexte
  ordinaire n'a pas changé ; le CSV du bilan s'ouvre au tableur avec les colonnes `_st`.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

L'affouagiste lit ses lots en stères sans calcul de tête, et les m³ restent la borne.
