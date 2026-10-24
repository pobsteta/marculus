# Mission 004 — signaler à nemetonshiny les formats CSV 6 et 7

## Mission

**En une phrase (quoi, et pourquoi) :** écrire le brief qui décrit à `nemetonshiny` les formats
`FormatCsv;6` et `;7` de Marculus, parce qu'il ne lit que les formats 2 à 5 et que les
contextes exportés depuis la v0.62 portent des clés qu'il ne connaît pas.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | modèle : `~/dev/briefs/traites/2026-10-16-traite-marculus-csv-format-5-houppier.md` |
| A-t-il les moyens ? | oui | `core/.../export/ExportCsv.kt`, `ExportCsvTest.kt`, specs `2026-10-16-tarifs-ser.md` et v1 § 7 ; côté lecteur : `~/dev/nemetonshiny/R/service_marculus_import.R` (lecture seule) |

**Temps de la main :** appuyer — un format d'échange est un contrat : il rend le brief à
l'orchestrateur, qui le relit avec Pascal avant dépôt.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `archiviste`
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** modifier le format, le code de Marculus ou celui de
  `nemetonshiny`.

## Harnais

- **Peut :** écrire un brouillon de brief dans son dossier de travail, et le rendre.
- **Ne doit pas :** déposer lui-même dans `~/dev/briefs/vers-nemetonshiny/` ; c'est
  l'orchestrateur qui dépose, après accord de Pascal. Pas de PR dans Marculus.
- **Contenu attendu :** les lignes d'en-tête et colonnes ajoutées par le format 6 (tarif par
  essence, stères) et le format 7 (`CommuneInsee`, `Ser`, `MillesimeTarifs`), avec la version
  de Marculus qui les a introduites et un extrait réel issu de `ExportCsvTest.kt`.

## Tests

- **Comment saura-t-on que c'est juste :** chaque clé du brief existe dans `ExportCsv.kt`, et
  aucune clé émise par les formats 6 et 7 n'y manque (comparaison avec le format 5).

## Terrain

- **Échantillon que Pascal vérifiera :** un CSV exporté de son téléphone, mis en regard du brief.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (sur le brouillon), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** à la remise du brouillon.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

`nemetonshiny` importe un CSV au format 7 sans le refuser, et son aide dit « FormatCsv;2 à 7 ».

## Remarques du relecteur — aller-retour 1/3 (2026-10-24)

Brouillon de l'aller 1 : `/tmp/claude-1000/-home-pascal-dev-marculus/03f8eca2-caf3-4f30-ab79-0a18f611de32/scratchpad/brief-formats-6-7.md`
(à reprendre en place). Conforme : 10 clés et leur ordre, 22 colonnes de journal, `.marsync`,
versions **v0.60.0 (format 6) et v0.61.0 (format 7)** confirmées (la mention « v0.62 » de cette
fiche est fausse). À reprendre :

1. **Millésime** : l'exemple « `IFN 2005-2025` » est une valeur de test ; la forme réelle est
   `2011-2025` (`data/src/main/assets/tarifs-ifn/tarifs_ser.csv`, colonne `millesime`), celle
   que demande le brief du 2026-10-23 § 3.
2. **§ 5** : distinguer deux familles, comme le traité `traites/2026-10-16-traite-marculus-tarifs-ser.md`
   et le brief du 2026-10-23 : `communeInsee` et le tarif par essence ne sont jamais émis par
   Nemeton ; `ser` et `millesimeTarifs` le sont et doivent continuer à l'être.
3. **Extrait `TarifsEssence`** : reprendre la sortie réelle du test
   (`TarifsEssence;Chêne=SCHAEFFER_RAPIDE:9|Hêtre=SCHAEFFER_LENT:11`) et dire l'ordre
   (essences du contexte, puis essences réglées hors contexte, triées : `ExportCsv.tarifsEssence`).
4. **Valeurs de tarif** : nommer `SCHAEFFER_INTERMEDIAIRE` et `SCHAEFFER_TRES_LENT` (ajoutées en
   v0.61.0 à `TarifCubage`), possibles dans `Tarif`, `TarifsEssence` et le `tarif` du `.marsync`.
5. **Lecteur R** : `~/dev/nemetonshiny/R/service_marculus_import.R` l. 324-325 ne refuse que
   les formats < 2 ; un `FormatCsv;7` est déjà importé, ses nouvelles clés ignorées. Seul le
   texte d'aide `R/utils_i18n.R` l. 764-765 dit « 2 à 5 ». Remplacer le « non vérifié » par ce
   constat ; revoir § 6.1 en conséquence.
6. Mineurs : `tarifsEssence` du `.marsync` = seulement les essences à tarif propre (CSV : toutes) ;
   guillemets aussi si `"` ou saut de ligne ; § 4 : ligne facultative `Commentaire` et section
   `TOTAUX` ; « Fait suite à » : citer aussi le traité du 10-16 et le brief du 10-23.

**Questions de sens laissées à Pascal (ne pas trancher, les poser dans le rendu)** : urgence
(faible si le lecteur accepte déjà le format 7 ?) ; demander ou non de ranger `ser` et
`millesimeTarifs` dans `reglages_terrain` ; redéfinir le critère d'acceptation (déjà rempli ?).

## Remarques du relecteur — aller-retour 2/3 (2026-10-24)

Les six remarques de l'aller 1 sont traitées et vérifiées. À reprendre, dans le même brouillon :

1. **« Pour EMERGE le numéro est 0 » (§ 2) est faux en général** : `CreationContexteScreen.kt`
   l. 381 ne vide pas `numerosTarif` au passage à EMERGE, l. 506 l'enregistre ; l'import
   `.marsync` (`SauvegardeRepository.kt` l. 220) garde le `numero` reçu. On peut émettre
   `Hêtre=EMERGE:9`. Écrire : « Pour `AUCUN` et `EMERGE`, le numéro est sans objet : souvent 0,
   mais il peut garder un reste de saisie ; le lecteur doit l'ignorer. » Ne pas proposer de
   correctif de Marculus dans le brief (question posée à Pascal par l'orchestrateur).
2. **Phrase cassée au § 2** : « Les lignes Valeurs possibles de `TARIF` : … », à réécrire.
3. **§ 4 dans le désordre** : placer « Commentaire, TOTAUX, JOURNAL » après la liste
   « Puis : `Affouage` … `Journal` » ; lignes exactes : `Journal` l. 87, `Commentaire` l. 88,
   `TOTAUX` l. 91 de `ExportCsv.kt`.
4. **§ 5, nuance** : l'écran enregistre un tarif propre pour toutes les essences cochées
   (l. 505-506, 541) ; `tarifsEssence` du `.marsync` liste donc toutes les essences d'un contexte
   créé ou modifié depuis v0.60.0 ; seuls les contextes plus anciens et les plans venus de Nemeton
   en ont moins.
5. **§ 8** : y reporter aussi la question de la famille de `steresParM3`.
