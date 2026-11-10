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

# mission 031 : tête de main et fichiers de test
CODE2=$(git rev-parse HEAD)
verifie "commit = tête de main" "release:" "$("$S" HEAD "" "$CODE2")"
ecrit pilotage/k.md x; commit pilotage3
verifie "commit dépassé par la tête" "pas de release: commit dépassé par $(printf '%s' "$(git rev-parse HEAD)" | cut -c1-7)" "$("$S" "$CODE2" "" "$(git rev-parse HEAD)")"
verifie "tête inconnue localement (SHA brut)" "pas de release: commit dépassé par 1234567" "$("$S" HEAD "" 1234567890abcdef1234567890abcdef12345678)"
verifie "tête illisible : on publie" "release:" "$("$S" HEAD)"
git tag v0.3.0
ecrit core/src/test/kotlin/XTest.kt x; ecrit app/src/androidTest/kotlin/YTest.kt x; ecrit data/src/test/resources/z.json x
ecrit .github/scripts/test-release-necessaire.sh x; commit tests
verifie "tests seuls" "pas de release:" "$("$S" HEAD "" "$(git rev-parse HEAD)")"
ecrit app/src/main/C.kt 1; commit "test + code"
verifie "tests + code" "release:" "$("$S" HEAD "" "$(git rev-parse HEAD)")"
git tag v0.4.0
ecrit app/src/test/Q.kt x; ecrit .github/scripts/release-necessaire.sh x; commit "test + script de release"
verifie "le script de release lui-même compte" "release:" "$("$S" HEAD "" "$(git rev-parse HEAD)")"

# historique réel des tags
cd "$REPO" || exit 1
git fetch -q --tags origin 2>/dev/null
check_tag() { # tag attendu
  PREV=$(git tag --list 'v*' --sort=v:refname | grep -B1 -x "$1" | head -n 1)
  if [ -z "$PREV" ] || [ "$PREV" = "$1" ]; then echo "SKIP $1 (tag ou précédent absent)"; return; fi
  verifie "$PREV..$1" "$2" "$("$S" "$1" "$PREV" "$1")"
}
for t in v0.68.1 v0.68.2 v0.68.3 v0.68.4 v0.69.1 v0.70.1; do check_tag $t "pas de release:"; done
for t in v0.68.0 v0.69.0 v0.70.0 v0.70.2; do check_tag $t "release:"; done

# mission 031, historique rejoué (tête de main passée explicitement)
rejoue() { # nom commit base tête attendu
  if git rev-parse --verify -q "$2^{commit}" >/dev/null && git rev-parse --verify -q "$3^{commit}" >/dev/null; then
    verifie "$1" "$5" "$("$S" "$2" "$3" "$4")"
  else echo "SKIP $1 (commit absent)"; fi
}
# CI annulées puis relancées : 07a2245, 02fc671, a800bb8 (pas de tag) dépassés par la tête de l'époque
rejoue "07a2245 relancé après 02fc671" 07a2245 v0.70.0 02fc671 "pas de release: commit dépassé par 02fc671"
rejoue "a800bb8 relancé après 76bd939" a800bb8 v0.70.0 76bd939 "pas de release: commit dépassé par 76bd939"
# #55 : tests seuls (+ pilotage) = même APK que v0.70.4, ne devait pas publier v0.70.5
rejoue "#55 tests seuls" 7f66180 v0.70.4 7f66180 "pas de release:"
# 026 seule (v0.71.0) publiée alors que la 027 allait être fusionnée : dépassée par la tête
rejoue "026 seule avant la 027" 07b5870 v0.70.5 85030ff "pas de release: commit dépassé par 85030ff"
rejoue "027 = tête, code changé" 85030ff 07b5870 85030ff "release:"

if [ "$ECHECS" -eq 0 ]; then echo "Tous les tests passent."; else echo "$ECHECS échec(s)."; exit 1; fi
