# Mission 029 — que valent les codes IFN `29AF` et `68CE` ?

## Mission

**En une phrase (quoi, et pourquoi) :** établir, sources à l'appui, ce que désignent les codes
d'espèce IFN `29AF` et `68CE` de la table embarquée et si leurs tarifs sont utilisables, pour
décider si « Autres feuillus » et « Autres résineux » peuvent recevoir une proposition de tarif
IFN comme les autres essences. Décision de Pascal du 2026-11-10 (« lance l'acquéreur pour la 1 »).

**Redite par l'agent :** _à compléter par l'agent avant d'agir._

## Ce qui existe (preuves, à 25b6314)

- La proposition passe par le code d'essence : `TarifsIfn.proposer`
  (`core/src/main/kotlin/fr/marculus/core/TarifsIfn.kt` l. 112-135), cascade SER → GRECO → France,
  seuil 30 arbres (l. 166).
- « Autres feuillus » et « Autres résineux » sont des regroupements de Marculus
  (`core/src/main/kotlin/fr/marculus/core/Referentiels.kt` l. 6), sans code IFN : rien n'est proposé.
- La table `data/src/main/assets/tarifs-ifn/tarifs_ser.csv`
  (`niveau;code;espar;essence_libelle;type;numero;n_arbres;cv;millesime;part_houppier`) contient
  `29AF` (24 lignes : SER 8, GRECO 8, FRANCE 8 ; libellé vide ; ex. `SER;B43;29AF;;SCHI;-2.7;3;47.7;2011-2025;17.8`)
  et `68CE` (20 lignes : SER 8, GRECO 12 ; libellé vide ; ex. `SER;B61;68CE;;SCHI;2.9;13;84;2011-2025;0.5`).
- La table vient de nemetonshiny (spec `docs/specs/2026-10-16-tarifs-ser.md`).

## Questions à éclairer (l'acquéreur ne tranche pas)

1. Que désignent `29AF` et `68CE` dans la nomenclature des espèces de l'Inventaire forestier
   (IGN) ? Libellé officiel, millésime de la nomenclature.
2. Ces codes regroupent-ils bien « autres feuillus » et « autres conifères », et lesquels ?
3. Pourquoi des numéros de tarif négatifs (−2,7 ; −3,1) ? Sont-ils possibles pour un tarif
   Schaeffer, ou signe d'un ajustement hors domaine (peu d'arbres) ?
4. Combien de SER, GRECO et niveau France atteignent le seuil de 30 arbres pour ces deux codes ?
5. Existe-t-il, côté IGN, un code « autres résineux » plus juste que `68CE` ?

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment faire ? | oui : documentation IGN de l'inventaire forestier, nemetonshiny | comparer au moins deux sources |
| A-t-il les moyens ? | oui : table embarquée, web | |

**Temps de la main :** appuyer.

## L'Homme

- **Agent :** `acquereur` (sonnet). **Ne lui est pas demandé :** changer le code ou la table.

## Harnais

- **Peut :** lire la table, la spec des tarifs SER, chercher sur le web (IGN, inventaire forestier,
  documentation de nemetonshiny) ; écrire une note de sources.
- **Ne doit pas :** décider la correspondance, modifier le code, la table ou la spec.

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

Aucune.

## Tests

- Chaque réponse porte sa source (URL consultée, date, version), deux sources comparées au moins ;
  les décomptes de la question 4 viennent d'une commande relancée.

## Terrain

- **Échantillon :** la ligne de la nomenclature IGN pour `29AF` et `68CE`, que Pascal peut ouvrir.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Arrêt :** 3 allers-retours.
- **Qui tranche :** Pascal : relier ou non « Autres feuillus » → `29AF` et « Autres résineux » →
  `68CE` (alors mission dev + testeur), ou passer au calcul d'une proposition (option 2, ADR).
