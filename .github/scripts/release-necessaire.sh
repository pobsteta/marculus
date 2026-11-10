#!/bin/sh
# Mission 023 : faut-il publier une release ?
# Usage : release-necessaire.sh [COMMIT [TAG_DE_BASE [TETE_DE_MAIN]]]
#   défauts : HEAD, dernier tag v* atteignable, tête de origin/main lue par « git ls-remote origin main ».
# Sortie standard : une ligne « release: <raison> » ou « pas de release: <raison> ». Code 0 dans les deux cas.
# Ne comptent pas : pilotage/**, docs/**, *.md, .github/badges/**, et (mission 031) les fichiers de test :
# **/src/test/**, **/src/androidTest/**, .github/scripts/test-*.sh. Tout le reste compte ; en cas de doute, release.
# Mission 031 : pas de release si le commit n'est plus la tête de main (une CI relancée sur un ancien commit
# ne doit pas publier une version plus haute contenant moins de code). Tête illisible : on publie.
HEAD_REF="${1:-HEAD}"
BASE="${2:-}"
TETE="${3:-}"

if ! git rev-parse --verify -q "$HEAD_REF^{commit}" >/dev/null; then
  echo "release: commit $HEAD_REF introuvable (dans le doute, on publie)"; exit 0
fi
COMMIT=$(git rev-parse "$HEAD_REF^{commit}")
if [ -z "$TETE" ]; then
  TETE=$(git ls-remote origin refs/heads/main 2>/dev/null | cut -f1 | head -n 1)
fi
if [ -n "$TETE" ]; then
  # tête inconnue localement (poussée après le checkout) : on garde le SHA brut, il diffère du commit
  TETE=$(git rev-parse --verify -q "$TETE^{commit}" 2>/dev/null || printf '%s' "$TETE")
  if [ "$TETE" != "$COMMIT" ]; then
    echo "pas de release: commit dépassé par $(printf '%s' "$TETE" | cut -c1-7)"; exit 0
  fi
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

COMPTE=$(printf '%s\n' "$CHANGED" | grep -v -e '^$' -e '^pilotage/' -e '^docs/' -e '\.md$' -e '^\.github/badges/' \
  -e '/src/test/' -e '/src/androidTest/' -e '^\.github/scripts/test-.*\.sh$')
if [ -n "$COMPTE" ]; then
  PREMIER=$(printf '%s\n' "$COMPTE" | head -n 1)
  N=$(printf '%s\n' "$COMPTE" | wc -l | tr -d ' ')
  echo "release: $N fichier(s) de l'application changé(s) depuis $BASE (ex. $PREMIER)"
else
  echo "pas de release: depuis $BASE, seuls pilotage/, docs/, *.md, le badge ou des fichiers de test ont changé"
fi
