# Mission 046 — ADR 0011 à 0013 : NEMETON embarqué, plafond EMERGE, versions calendaires

## Mission

**En une phrase (quoi, et pourquoi) :** écrire, en texte proposé, les ADR 0011 (tarif NEMETON embarqué, provisoire),
0012 (plafond continu de la part de tige EMERGE) et 0013 (versions calendaires `vAAAA.MM.N`), parce que ces trois
décisions de Pascal ne vivent aujourd'hui que dans le journal et des specs datées (trou G1 du bilan n° 3).

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Ce qui existe (preuves)

- **0011 :** décision « b » du 2026-11-27 (intégrer NEMETON dès le paquet 0.2.2-provisoire), missions 037-039, PR
  #85 et #87, option A pour les entrées impossibles, tolérance des tests, repli D5 ; ADR 0001 de `tarif-nemeton`
  (`~/dev/tarif-nemeton/docs/adr/0001-livrable-marculus.md`) ; brief déposé le 2026-11-27 (043).
- **0012 :** missions 040-042, `docs/specs/2026-11-27-emerge-regles-sans-source.md` (§ 8 « Décisions de Pascal »), PR
  #86 et #90, cap du journal (« EMERGE règle 2 »).
- **0013 :** mission 036, PR #77, `.github/scripts/version-calendaire.sh`, cap l. 307 (« défait le SemVer ») ; ADR 0008
  (release conditionnelle) pour le lien.
- Forme : `docs/adr/README.md` et les ADR 0007 à 0010.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | gabarit des ADR 0007-0010 |
| A-t-il les moyens ? | oui | décisions déjà prises, preuves nommées |

**Temps de la main :** donner. **Degré de la main :** le chef tranche (le texte est proposé, Pascal valide à la PR).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `archiviste` · **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** prendre une décision nouvelle ; trancher les questions encore ouvertes du
  branchement NEMETON (cycle 11) : elles sont nommées « ouvertes » dans 0011.

## Harnais

- **Peut (périmètre) :** `docs/adr/0011-nemeton-embarque-provisoire.md`, `docs/adr/0012-plafond-part-de-tige-emerge.md`,
  `docs/adr/0013-versions-calendaires.md`, `docs/adr/README.md` (index).
- **Câblé (ne peut pas) :** pas de `main` ; journal en ajout seul.
- **Ne doit pas (écrit) :** modifier une ADR existante autrement que par un renvoi daté ; citer un fait sans sa preuve
  et l'état de sa source.
- **Sources autorisées :** journal, fiches 036-043, specs du 2026-11-27, `docs/adr/`, `~/dev/tarif-nemeton/docs/adr/`.

## Journal

- **Ce que l'agent inscrit :** une ligne par rendu : les trois ADR, les décisions et leurs lignes de journal.

## Valeur provisoire (chiffre, réglage)

- aucune nouvelle ; 0011 et 0012 nomment leurs valeurs provisoires (paquet 0.2.2-provisoire, plafond 0,95).

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune. **Ordre de fusion :** avant 045. **CI rouge attendue :** aucune.

## Tests

- **Comment saura-t-on que c'est juste :** chaque décision citée renvoie à sa ligne de journal (avec sa clé) et à sa
  PR ; le relecteur les recontrôle toutes.

## Terrain

- **Échantillon que Pascal vérifiera :** les trois ADR, à la PR.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

« Pourquoi NEMETON est-il provisoire, et pourquoi 0,95 ? » se lit en moins d'une minute dans `docs/adr/`.
