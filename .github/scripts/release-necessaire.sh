#!/bin/sh
# Mission 023 : faut-il publier une release ?
# Usage : release-necessaire.sh [COMMIT [TAG_DE_BASE]]   (défauts : HEAD, dernier tag v* atteignable)
# Sortie standard : une ligne « release: <raison> » ou « pas de release: <raison> ». Code 0 dans les deux cas.
# Ne comptent pas : pilotage/**, docs/**, *.md, .github/badges/**. Tout le reste compte ; en cas de doute, release.
HEAD_REF="${1:-HEAD}"
BASE="${2:-}"

if ! git rev-parse --verify -q "$HEAD_REF^{commit}" >/dev/null; then
  echo "release: commit $HEAD_REF introuvable (dans le doute, on publie)"; exit 0
fi
if [ -z "$BASE" ]; then
  BASE=$(git describe --tags --abbrev=0 --match 'v*' "$HEAD_REF" 2>/dev/null) || BASE=""
fi
if [ -z "$BASE" ]; then
  echo "release: aucun tag v* existant"; exit 0
fi
if ! CHANGED=$(git diff --name-only --no-renames "$BASE" "$HEAD_REF" 2>/dev/null); then
  echo "release: comparaison avec $BASE impossible (dans le doute, on publie)"; exit 0
fi

COMPTE=$(printf '%s\n' "$CHANGED" | grep -v -e '^$' -e '^pilotage/' -e '^docs/' -e '\.md$' -e '^\.github/badges/')
if [ -n "$COMPTE" ]; then
  PREMIER=$(printf '%s\n' "$COMPTE" | head -n 1)
  N=$(printf '%s\n' "$COMPTE" | wc -l | tr -d ' ')
  echo "release: $N fichier(s) de l'application changé(s) depuis $BASE (ex. $PREMIER)"
else
  echo "pas de release: depuis $BASE, seuls pilotage/, docs/, *.md ou le badge ont changé"
fi
