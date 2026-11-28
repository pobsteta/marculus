# Mission 043 — Brief vers tarif-nemeton : refus des entrées impossibles et repli par le genre

## Mission

**En une phrase (quoi, et pourquoi) :** écrire le brief qui dit au dépôt `tarif-nemeton` deux écarts de Marculus à
son brief, le refus de D ≤ 0, H ≤ 0 et des résineux à H ≤ 1,301 m, et le repli par le genre (D5) des pins hors
paquet et du chêne rouge, pour que le propriétaire du tarif les connaisse et les accepte ou les conteste avant le
branchement de NEMETON à l'écran.

**Redite par l'agent :** _reconstituée le 2026-11-27 d'après le journal (lignes 373, 375-381, 387-388), l'agent n'ayant pas consigné sa redite à l'époque._ Écrire le brief à `tarif-nemeton` sur deux écarts de Marculus (refus des entrées impossibles, repli D5). Livrée **sans PR** : brief déposé hors dépôt dans `~/dev/briefs/vers-tarif-nemeton/2026-11-27-marculus-refus-et-repli-d5.md` (ligne 388), après deux relectures ; Pascal a fait retirer la phrase « Marculus s'alignera sur votre réponse » (ligne 387). Redite de l'agent d'origine : non retrouvée.

## Ce qui existe (preuves)

- **Le brief NEMETON :** `~/dev/tarif-nemeton/docs/brief.md` l. 59-67 (« hors domaine : Marculus calcule et affiche
  une alerte (pas de refus) » ; « chaque calcul porte la version du tarif et l'essence réellement utilisée »).
- **Le refus dans Marculus :** `core/src/main/kotlin/fr/marculus/core/nemeton/TarifNemeton.kt` l. 57-58 ; décision de
  Pascal du 2026-11-27 (« option A », `pilotage/journal.md`) ; écart relevé par le relecteur de la PR #87.
- **Le repli par le genre :** `docs/specs/2026-11-27-essences-nemeton.md` (règle de repli et « Décisions de Pascal »,
  lignes 9 et 10 sur 11) ; journal du 2026-11-27 : pins 51, 53, 54, 57, 58 → 52 (cubés comme `Pinus_sylvestris`),
  chêne rouge (4) → `Quercus_spec`, essence utilisée affichée.
- **Le modèle de brief :** `~/dev/briefs/traites/2026-10-16-traite-marculus-csv-format-5-houppier.md` et
  `~/dev/briefs/README.md` (convention `AAAA-MM-JJ-sujet.md`).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | modèle et convention nommés ci-dessus |
| A-t-il les moyens ? | oui | toutes les preuves sont dans les deux dépôts, en lecture |

**Temps de la main :** donner ; le brief revient à l'orchestrateur, qui le fait relire, puis le montre à Pascal
avant dépôt (il touche un autre projet). **Degré de la main :** le chef tranche.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `archiviste` · **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** modifier le code, une spec ou un fichier de `tarif-nemeton` ; trancher à la
  place de son propriétaire ; parler des règles EMERGE (missions 041-042 : autre brief, vers `nemeton`, après leur
  fusion).

## Harnais

- **Peut (périmètre) :** un brouillon dans `/tmp/claude-1000/-home-pascal-dev-marculus/9a1cc8a0-71ea-40c1-af0c-c9793232660c/scratchpad/`
  (dossier de travail de la session), rendu à l'orchestrateur. Aucun fichier du dépôt Marculus.
- **Câblé (ne peut pas) :** pas de `main` ; journal en ajout seul.
- **Ne doit pas (écrit) :** déposer lui-même dans `~/dev/briefs/vers-tarif-nemeton/` (l'orchestrateur dépose après
  l'accord de Pascal) ; citer un fait sans son fichier et sa ligne.
- **Sources autorisées :** les fichiers nommés ci-dessus ; `~/dev/tarif-nemeton/livrable/datapackage.json`
  (`limites_connues`) ; `~/dev/tarif-nemeton/code/paquet_livrable.py` (lecture, pour citer ce que fait la référence).

## Journal

- **Ce que l'agent inscrit :** une ligne par rendu : les deux points, leurs preuves, la question posée au destinataire.

## Valeur provisoire (chiffre, réglage)

- **Valeur provisoire :** aucune nouvelle ; le brief rappelle que le seuil 1,301 m vient du paquet 0.2.2-provisoire
  (`h_ref + marge_sommet`).

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune (PR #87 fusionnée). **Ordre de fusion :** pas de PR. **CI rouge attendue :** aucune.

## Tests

- **Comment saura-t-on que c'est juste :** chaque fait cité porte son fichier et sa ligne, recontrôlés par le
  relecteur ; le brief finit par une question par point au propriétaire de `tarif-nemeton`.

## Terrain

- **Échantillon que Pascal vérifiera :** le brief lui-même, avant dépôt.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal,
  qui est aussi le propriétaire de `tarif-nemeton` : il voit le brief avant qu'il soit déposé.

## Comment saura-t-on que ça marche ?

La session `tarif-nemeton` traite le brief (déplacé dans `~/dev/briefs/traites/`) et répond sur les deux points.
