# Mission 038 — Tests d'acceptation du moteur NEMETON

## Mission

**En une phrase (quoi, et pourquoi) :** écrire, sans lire le code qui l'implémente, les tests qui prouvent que le
moteur NEMETON de Marculus recalcule les 1 032 sorties du jeu de contrôle du paquet, refuse un paquet altéré et
signale un arbre hors domaine sans refuser de le cuber, pour que l'intégration du tarif provisoire (décision de
Pascal du 2026-11-27, option b) ne s'auto-évalue pas.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Ce qui existe (preuves)

- **Le paquet :** `~/dev/tarif-nemeton/livrable/` v0.2.2-provisoire : `constantes.csv` (247 lignes),
  `jeu_controle.csv` (1 032 lignes : essence, code_ifn, D_cm, H_m, sortie, valeur, unite), `datapackage.json`
  (version, SHA-256 de chaque fichier, `limites_connues`).
- **Le contrat (ADR 0001 de NEMETON, acceptée le 2026-11-26) :** « Marculus code les formules en Kotlin, refuse un
  paquet dont l'empreinte ne correspond pas, et vérifie son implémentation sur le jeu de contrôle »
  (`~/dev/tarif-nemeton/docs/adr/0001-livrable-marculus.md`).
- **Les exigences du brief NEMETON** (`~/dev/tarif-nemeton/docs/brief.md` l. 59-67) : hors domaine → calcul et
  alerte, pas de refus ; chaque calcul porte la version du tarif et l'essence réellement utilisée.
- **Les sorties absentes sont absentes**, jamais 0 (ex. pas de bois fort tige pour les feuillus,
  `limites_connues`).

## L'interface attendue (fixée ici, commune au testeur et au dev)

Paquet `fr.marculus.core.nemeton`, dans `:core` :

- `PaquetNemeton.charger(constantes: String, manifeste: String): TarifNemeton` — lève une exception si le SHA-256 de
  `constantes.csv` ne correspond pas au manifeste ;
- `TarifNemeton.version: String` (« 0.2.2-provisoire ») ;
- `TarifNemeton.sorties(codeIfn: String, dCm: Double, hM: Double): SortiesNemeton?` — `null` si le code n'est pas
  couvert ;
- `SortiesNemeton` : `valeurs: Map<String, Double>` (clés = noms de sortie du jeu de contrôle), `horsDomaine: Boolean`,
  `essence: String` (libellé de la ligne utilisée), `version: String`.

Le paquet est embarqué tel quel, octet pour octet, dans `core/src/main/resources/nemeton/`.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | interface fixée ci-dessus, données de contrôle fournies |
| A-t-il les moyens ? | oui | le jeu de contrôle est l'oracle |

**Temps de la main :** donner. **Degré de la main :** le chef tranche.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `testeur` · **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** implémenter le moteur ; changer le paquet.

## Harnais

- **Peut (périmètre) :** `core/src/test/kotlin/fr/marculus/core/nemeton/`, `core/src/main/resources/nemeton/` (copie
  du paquet, trois fichiers, sans modification).
- **Câblé (ne peut pas) :** pas de `main` ; journal en ajout seul.
- **Ne doit pas (écrit) :** lire `core/src/main/kotlin/fr/marculus/core/nemeton/` ; lire
  `~/dev/tarif-nemeton/code/paquet_livrable.py` (implémentation de référence réservée au dev).
- **Sources autorisées :** le paquet, ADR 0001 et brief de NEMETON, cette fiche.

## Journal

- **Ce que l'agent inscrit :** une ligne par rendu : tests écrits, tolérance retenue, rouge attendu.

## Valeur provisoire (chiffre, réglage)

- **Valeur provisoire :** tolérance relative de 1e-6 sur chaque sortie du jeu de contrôle (valeurs écrites à 8 chiffres
  significatifs).
- **Mesurée par :** le testeur, en première tâche (nombre de chiffres réellement écrits dans `jeu_controle.csv`).
- **Gel :** à la fusion de 038 + 039.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune ; branche `test/038-nemeton` partie de `main`.
- **Ordre de fusion :** groupée avec 039 (une PR).
- **PR en brouillon jusqu'à :** la fin de 039.
- **CI rouge attendue :** les tests de `fr.marculus.core.nemeton` ne compilent pas tant que 039 n'a pas écrit le moteur.

## Tests

- **Comment saura-t-on que c'est juste :**
  1. les 1 032 lignes du jeu de contrôle, chacune un cas nommé (essence, D, H, sortie) ;
  2. une sortie absente du jeu pour une essence est absente de `valeurs` ;
  3. un octet changé dans `constantes.csv` → refus au chargement ;
  4. un arbre hors domaine (D ou H) → `horsDomaine = true` et des valeurs calculées ;
  5. un code non couvert (68, 75) → `null` ;
  6. `version` = celle du manifeste.

## Terrain

- **Échantillon que Pascal vérifiera :** aucun écran ; le rapport liste le nombre de cas et la tolérance.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Avec 039, les 1 032 cas passent ; sans 039, ils ne compilent pas.
