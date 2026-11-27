# Mission 047 — Titre des releases sans le nom du projet

## Mission

**En une phrase (quoi, et pourquoi) :** titrer les releases GitHub de Marculus par la version seule, identique au tag
(`v2026.11.N`), comme les autres projets de la plateforme, en réponse au brief
`~/dev/briefs/vers-marculus/2026-11-24-titre-release-sans-nom.md`.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Ce qui existe (preuves)

- `.github/workflows/release.yml` l. 115 : `name: Marculus ${{ … }}` (brief, lu sur `origin/main` le 2026-11-24 ; à
  relire sur `main` à jour).
- Le brief demande aussi : chercher le même préfixe ailleurs (scripts, `CLAUDE.md`, consignes) ; ne pas renommer les
  releases publiées sans l'accord de Pascal.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | une ligne de workflow |
| A-t-il les moyens ? | oui | le titre se vérifie à la release suivante |

**Temps de la main :** lâcher (genre CI réussi trois fois : 001, 015, 023 ; `pilotage/equipe.md`) ; fusion par
l'orchestrateur si relecteur « prêt » et CI verte. **Degré de la main :** le chef tranche.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `dev` · **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** renommer les releases déjà publiées ; changer le format des tags ; le badge du
  README (045).

## Harnais

- **Peut (périmètre) :** `.github/workflows/release.yml` ; un autre fichier de `.github/` qui porterait le même préfixe.
- **Câblé (ne peut pas) :** pas de `main` ; journal en ajout seul.
- **Ne doit pas (écrit) :** toucher `README.md`, `CLAUDE.md`, `docs/` (missions 045 et 046).
- **Sources autorisées :** `.github/`, le brief.

## Journal

- **Ce que l'agent inscrit :** une ligne : ligne changée, autres occurrences cherchées (commande).

## Valeur provisoire (chiffre, réglage)

- aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune. **CI rouge attendue :** aucune.

## Tests

- **Comment saura-t-on que c'est juste :** `.claude/test-command` vert ; `grep -rn "Marculus \${{" .github` vide ; la
  release suivante est titrée `v2026.11.N`.

## Terrain

- **Échantillon que Pascal vérifiera :** le titre de la prochaine release.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** l'orchestrateur (lâché) ;
  le brief est déplacé dans `~/dev/briefs/traites/` après la fusion.

## Comment saura-t-on que ça marche ?

La release qui suit la fusion s'appelle `v2026.11.N`, sans « Marculus ».
