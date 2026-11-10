#!/bin/sh
# Tests de release-necessaire.sh : cas synthétiques (dépôt temporaire) puis historique réel des tags.
S="$(cd "$(dirname "$0")" && pwd)/release-necessaire.sh"
REPO="$(git rev-parse --show-toplevel)"
ECHECS=0
verifie() { # nom attendu obtenu
  case "$3" in "$2"*) echo "ok   $1";; *) echo "ECHEC $1 : attendu '$2', obtenu '$3'"; ECHECS=$((ECHECS+1));; esac
}
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
cd "$T" && git init -q . && git config user.email t@t && git config user.name t
commit() { git add -A && git commit -q -m "$1"; }
ecrit() { mkdir -p "$(dirname "$1")"; echo "$2" >> "$1"; }

ecrit app/A.kt 1; commit init
verifie "aucun tag" "release:" "$("$S")"
git tag v0.1.0
verifie "rien changé" "pas de release:" "$("$S")"
ecrit pilotage/kanban.md x; ecrit docs/a.pdf x; ecrit README.md x; commit pilotage
verifie "seulement pilotage/docs/md" "pas de release:" "$("$S")"
ecrit app/A.kt 2; commit code
verifie "pilotage + code" "release:" "$("$S")"
git tag -f v0.1.0 HEAD >/dev/null
ecrit .github/badges/jacoco.svg x; commit badge
verifie "badge seul" "pas de release:" "$("$S")"
ecrit .github/workflows/release.yml x; commit workflow
verifie "workflow modifié" "release:" "$("$S")"
# code non publié, puis fusion de pilotage : on compare au tag, pas au commit précédent
git tag v0.2.0; ecrit app/B.kt 1; commit code2; ecrit pilotage/j.md x; commit pilotage2
verifie "code puis pilotage (CI annulée)" "release:" "$("$S")"
verifie "commit inconnu" "release:" "$("$S" deadbeef)"

# historique réel des tags
cd "$REPO" || exit 1
git fetch -q --tags origin 2>/dev/null
check_tag() { # tag attendu
  PREV=$(git tag --list 'v*' --sort=v:refname | grep -B1 -x "$1" | head -n 1)
  if [ -z "$PREV" ] || [ "$PREV" = "$1" ]; then echo "SKIP $1 (tag ou précédent absent)"; return; fi
  verifie "$PREV..$1" "$2" "$("$S" "$1" "$PREV")"
}
for t in v0.68.1 v0.68.2 v0.68.3 v0.68.4 v0.69.1 v0.70.1; do check_tag $t "pas de release:"; done
for t in v0.68.0 v0.69.0 v0.70.0 v0.70.2; do check_tag $t "release:"; done

if [ "$ECHECS" -eq 0 ]; then echo "Tous les tests passent."; else echo "$ECHECS échec(s)."; exit 1; fi
