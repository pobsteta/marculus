# Mission 002 — README : qui utilise vraiment `terrain`

## Mission

**En une phrase (quoi, et pourquoi) :** corriger le README, qui dit `terrain` « partagée avec
Groma et Surculus » alors qu'aucun des deux ne la déclare dans son build, pour qu'un successeur
ne croie pas qu'une correction de `terrain` touche déjà trois applications.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | brief Surculus du 2026-10-22, § 1 |
| A-t-il les moyens ? | oui | vérifié le 2026-10-23 : ni `~/dev/groma` ni `~/dev/surculus` ne déclarent `com.github.pobsteta.terrain` |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `archiviste`
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** le § 2 du même brief (jetons `terrain-style`), bloqué tant
  que `terrain-style` n'est pas publié.

## Harnais

- **Peut :** modifier `README.md` (l. 106-115), dans sa branche ; ajouter en tête de la copie
  `~/dev/surculus/pilotage/briefs/vers-marculus/2026-10-22-readme-terrain-et-style-commun.md`
  la ligne « § 1 traité le … (PR #…) ; § 2 en attente de `terrain-style` ».
- **Ne doit pas :** commiter dans le dépôt Surculus, toucher au code, à `main`.
- **Sources autorisées :** `README.md`, le brief Surculus ci-dessus, les fichiers de build de
  `~/dev/groma` et `~/dev/surculus` (lecture seule).

## Tests

- **Comment saura-t-on que c'est juste :** le README ne nomme comme utilisateurs de `terrain` que
  les applications qui la déclarent dans leur build (critère d'acceptation du brief), avec la
  formule « utilisée par Marculus ; Groma et Surculus doivent l'adopter ».

## Terrain

- **Échantillon que Pascal vérifiera :** le paragraphe du README sur GitHub.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

La session Surculus lit la ligne de statut et classe le § 1.
