# Mission 039 — Le moteur de calcul NEMETON dans `:core`

## Mission

**En une phrase (quoi, et pourquoi) :** coder en Kotlin, dans `:core`, le calcul des sorties NEMETON d'un arbre (code
IFN, D, H) à partir du seul paquet embarqué, pour que Marculus puisse proposer le tarif NEMETON provisoire (décision de
Pascal du 2026-11-27, option b) ; sans écran ni changement du modèle de données.

**Redite par l'agent :** _reconstituée le 2026-11-27 d'après le journal (lignes 343, 347-349, 365), l'agent n'ayant pas consigné sa redite à l'époque._ Coder dans `:core` le moteur NEMETON à partir du seul paquet embarqué, plan d'abord. Livrée par la PR #87 (79d2f22, v2026.11.5) : 1 032 sur 1 032 cas verts ; les entrées impossibles sont refusées (option « A » de Pascal, ligne 365). Pas d'écran : le branchement est au cycle 11. Redite de l'agent d'origine : non retrouvée.

## Ce qui existe (preuves)

- **Les tests de la 038** et l'interface qu'elle fixe (`pilotage/missions/038-tests-nemeton.md`, « L'interface
  attendue »).
- **Les formules :** source unique la méthodologie NEMETON (ADR 0001 de NEMETON, amendement 3) :
  `~/dev/tarif-nemeton/documents/methodologie/sources/NEMETON_v2.25.qmd` ; forme générale
  `ln V = ln(π/4·D²H) + a + b ln(D/30) + c ln(H/25)`, fractions de découpe `f(D) = fmax·σ((ln D − m)/s)`
  (`~/dev/tarif-nemeton/README.md`, « Principe ») ; intégration numérique de la tige des résineux.
- **L'implémentation de référence :** `~/dev/tarif-nemeton/code/paquet_livrable.py` l. 269-391 (classe qui calcule
  les sorties à partir des seules constantes : domaine, tige, expansion, infradensité).
- **Les modèles du paquet :** `volume_feuillu`, `fraction_decoupe`, `correction_menu_bois`, `profil_tige_resineux`,
  `integration_tige_resineux`, `expansion_resineux`, `infradensite`, `facteur_arbre_entier`, `carbone`,
  `domaine_feuillu`, `domaine_resineux`, `incertitude`… (colonne `modele` de `constantes.csv`).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | à moitié | formules dans une méthodologie longue et un script Python : **plan d'abord** (liste des sorties, formule et ligne de source de chacune), relu par l'orchestrateur avant le code |
| A-t-il les moyens ? | oui, après 038 | le jeu de contrôle |

**Temps de la main :** donner, plan d'abord. **Degré de la main :** le chef tranche.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `dev` · **Modèle :** opus (justesse des données)
- **Ce qui ne lui est pas demandé :** l'incertitude au lot (tirages, covariance) ; le choix NEMETON dans un contexte ;
  l'écran ; les exports.

## Harnais

- **Peut (périmètre) :** `core/src/main/kotlin/fr/marculus/core/nemeton/` ; `core/build.gradle.kts` seulement si la
  lecture d'une ressource l'exige.
- **Câblé (ne peut pas) :** pas de `main` ; pas de « terminé » sans tests verts ; journal en ajout seul.
- **Ne doit pas (écrit) :** modifier les tests de la 038 (un test faux remonte à l'orchestrateur) ; modifier le paquet ;
  ajouter une dépendance ; toucher à `Cubage.kt`, au modèle `TarifCubage` ou à la base.
- **Sources autorisées :** ci-dessus ; fermées : tout code de Marculus hors `:core`.

## Journal

- **Ce que l'agent inscrit :** le plan ; puis une ligne par rendu (cas verts, écarts, choix tranchés et réversibles).

## Valeur provisoire (chiffre, réglage)

- **Valeur provisoire :** tout le paquet 0.2.2-provisoire (coefficients non stabilisés, brief NEMETON hors périmètre
  n° 5).
- **Mesurée par :** le dépôt `tarif-nemeton` (critère A5 de sa spec, x à fixer par Pascal).
- **Gel :** aucun dans Marculus : le paquet se remplace par le suivant ; la version voyage avec chaque calcul.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 038 ; part de la branche `test/038-nemeton`.
- **Ordre de fusion :** groupée avec 038 (une PR).
- **PR en brouillon jusqu'à :** tests verts et relecture.
- **CI rouge attendue :** aucune à la fin de la mission.

## Tests

- **Comment saura-t-on que c'est juste :** les tests de la 038 passent sans modification ; `.claude/test-command` vert.

## Terrain

- **Échantillon que Pascal vérifiera :** aucun écran ; trois arbres du jeu de contrôle recopiés dans le rapport
  (un feuillu, un résineux, un hors domaine).

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Point de suivi :** à la
  remise du plan. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Le moteur recalcule le jeu de contrôle ; un paquet NEMETON suivant se dépose sans changer le code, sauf nouvelle formule.
