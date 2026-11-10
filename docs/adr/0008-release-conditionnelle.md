# ADR 0008 — Pas de release quand seuls pilotage, docs, `*.md` ou badge changent

- **Date :** 2026-11-10
- **Statut :** **décision acceptée par Pascal** (« oui fais les 2 », journal du 2026-11-10) ; **texte de l'ADR proposé**, à valider par Pascal à la lecture de la PR ; livrée par la PR #51 (f440e2c)
- **Proposée par :** non écrit ; écart relevé au cycle 6 (journal du 2026-11-10, 2026-11-10 « oui fais les 2 »)
- **Tranchée par :** Pascal

## La décision en une phrase

Après des tests verts, la CI ne publie une release que si un fichier de l'application a changé depuis le dernier tag `v*` ; dans le doute, elle publie.

## Contexte

Relevé au journal du 2026-11-10 : 5 releases sur 10 sans changement de l'application (v0.68.1 à v0.68.4, v0.70.1), plus v0.69.1 (badge seul) (v0.68.1 à v0.68.3 : même APK que v0.68.0, journal du 2026-11-10). La CI de `main` annule ses runs en cours, d'où la comparaison au dernier tag plutôt qu'au commit précédent.

Règle : `.github/scripts/release-necessaire.sh` ; ne comptent pas `pilotage/**`, `docs/**`, `*.md`, `.github/badges/**`. Dite une fois dans `README.md` (« Déployer et publier »). Appelée par `release.yml` l. 38 ; testée par `test-release-necessaire.sh` (18 tests lancés par `ci.yml` l. 24). Historique rejoué : 6 « pas de release », 4 « release », conformes à la fiche (journal du 2026-11-10, dev 023). Essai en vrai : la fusion de la 023 crée v0.70.4 (workflows changés) ; les PR de pilotage #52 et #53 ne créent aucune version.

Alternative : ne rien changer et laisser 5 releases sur 10 vides ; écartée, sans autre raison écrite que ce relevé.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | La plus simple qui tienne ? | Un script shell, une liste d'exclusions, repli « on publie » | Un `.md` embarqué un jour dans les assets ne déclencherait pas de release (question du relecteur à Pascal) | Non écrit |
| **R** | Qui fait quoi ? | Dev 023 (7955374), relecteur (prêt) | | |
| **A** | Et si l'imprévu arrive ? | Comparaison impossible, commit introuvable, aucun tag : on publie. Réserve du relecteur : une CI d'un commit antérieur finissant après la fusion n'aurait pas le script (improbable, `cancel-in-progress`) | | |
| **N** | Idées étranges écoutées ? | Non écrit | | |
| **C** | Coût pour l'ensemble ? | Non écrit | | |
| **É** | Que réutilisons-nous ? | `git describe` et `git diff` ; troisième réussite du genre CI (001, 015, 023) | | |

## Valeurs sacrifiées

Non écrit.

## Sources

- `pilotage/journal.md` du 2026-11-10 (relevé des releases, dev 023, relecteur 023, essai en vrai) ; PR #51 (f440e2c).

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ? Réponse de Pascal : non écrite.
