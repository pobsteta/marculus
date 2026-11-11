#!/bin/sh
# Mission 036 : prochain tag de release calendaire « vAAAA.MM.N » (N = compteur du mois, repart à 1 chaque mois).
# Usage : version-calendaire.sh [AAAA.MM]   (défaut : mois courant, UTC)
# Lit les tags du dépôt courant ; seuls comptent « vAAAA.MM.<entier> » du mois demandé (les anciens v0.* et
# les autres mois n'influent pas). Tri numérique : .9 -> .10. Sortie standard : le tag, ex. v2026.11.1.
MOIS="${1:-$(date -u +%Y.%m)}"
case "$MOIS" in
  [0-9][0-9][0-9][0-9].[0-1][0-9]) ;;
  *) echo "mois invalide : '$MOIS' (attendu AAAA.MM)" >&2; exit 2 ;;
esac
DERNIER=$(git tag --list "v$MOIS.*" | sed -n "s/^v$MOIS\.\([0-9][0-9]*\)$/\1/p" | sort -n | tail -n 1)
echo "v$MOIS.$(( ${DERNIER:-0} + 1 ))"
