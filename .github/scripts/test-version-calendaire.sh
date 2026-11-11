#!/bin/sh
# Tests de version-calendaire.sh (mission 036) : dépôt temporaire, tags synthétiques.
D="$(cd "$(dirname "$0")" && pwd)"
S="$D/version-calendaire.sh"
R="$D/release-necessaire.sh"
ECHECS=0
verifie() { # nom attendu obtenu
  if [ "$2" = "$3" ]; then echo "ok   $1"; else echo "ECHEC $1 : attendu '$2', obtenu '$3'"; ECHECS=$((ECHECS+1)); fi
}
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
cd "$T" && git init -q . && git config user.email t@t && git config user.name t
commit() { git add -A && git commit -q -m "$1"; }
ecrit() { mkdir -p "$(dirname "$1")"; echo "$2" >> "$1"; }

ecrit app/A.kt 1; commit init
verifie "aucun tag" v2026.11.1 "$("$S" 2026.11)"
git tag v0.73.3; git tag v0.9.0
verifie "premier tag calendaire après des v0.*" v2026.11.1 "$("$S" 2026.11)"
git tag v2026.11.1
verifie "v2026.11.1 existe" v2026.11.2 "$("$S" 2026.11)"
for n in 2 3 4 5 6 7 8 9; do git tag "v2026.11.$n"; done
verifie "v2026.11.9 existe : tri numérique" v2026.11.10 "$("$S" 2026.11)"
git tag v2026.11.10
verifie "v2026.11.10 existe" v2026.11.11 "$("$S" 2026.11)"
verifie "mois suivant" v2026.12.1 "$("$S" 2026.12)"
git tag v2026.10.30
verifie "un tag d'un autre mois n'influe pas" v2026.11.11 "$("$S" 2026.11)"
verifie "mois précédent séparé" v2026.10.31 "$("$S" 2026.10)"
git tag v2026.11.5-rc1
verifie "tag non numérique ignoré" v2026.11.11 "$("$S" 2026.11)"
"$S" 2026-11 >/dev/null 2>&1
verifie "mois invalide : code 2" 2 "$?"
verifie "mois courant UTC par défaut" "v$(date -u +%Y.%m)." "$("$S" | sed 's/[0-9]*$//')"

# git describe --match 'v*' trouve le dernier tag, anciens et nouveaux mélangés (ordre de l'historique)
ecrit app/A.kt 2; commit code2; git tag v2026.12.1; ecrit app/A.kt 3; commit code3
verifie "describe : dernier tag calendaire" v2026.12.1 "$(git describe --tags --abbrev=0 --match 'v*')"
verifie "release-necessaire : base calendaire" "release: 1 fichier(s) de l'application changé(s) depuis v2026.12.1" \
  "$("$R" HEAD "" "$(git rev-parse HEAD)" | sed 's/ (ex\..*//')"

if [ "$ECHECS" -eq 0 ]; then echo "Tous les tests passent."; else echo "$ECHECS échec(s)."; exit 1; fi
