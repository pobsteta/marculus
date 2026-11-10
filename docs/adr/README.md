# Décisions d'architecture (ADR)

Une ligne par ADR. La décision est acceptée par Pascal ; le texte de chaque ADR est proposé, à valider par Pascal à la lecture de la PR. Gabarit : plugin `imperatrice`, skill `brief`, `adr-france.md`. Les ADR ne s'effacent pas : une correction est une nouvelle ADR ou une ligne datée.

| N° | Décision | Statut |
|---|---|---|
| [0001](0001-partage-par-fichier-sans-serveur.md) | Partage par fichier (`.marsync`, CSV), sans serveur | décision acceptée ; texte proposé |
| [0002](0002-briques-communes-bibliotheque-terrain.md) | Briques communes dans `terrain` (JitPack), on corrige là-bas | décision acceptée ; texte proposé |
| [0003](0003-tarifs-proposes-echelon-foret.md) | `tarifsProposes` : seul l'échelon `foret` est lu | décision acceptée, non livrée ; texte proposé |
| [0004](0004-lot-affouage-houppier-seul.md) | Lot d'affouage : houppier seul quand l'arbre en a un | décision acceptée, livrée, **remplacée par 0009** (2026-11-10) ; texte proposé |
| [0005](0005-theme-systeme-clair-sombre.md) | Thème Système / Clair / Sombre ; `terrain-style` suspendu | décision acceptée, réadoption suspendue ; texte proposé |
| [0006](0006-numero-tarif-zero-hors-schaeffer.md) | Numéro de tarif à 0 hors Schaeffer | décision acceptée, livrée ; texte proposé |
| [0007](0007-regle-annulation-unique-journalnet.md) | Une seule règle d'annulation, dans `JournalNet` | décision acceptée, livrée ; texte proposé |
| [0008](0008-release-conditionnelle.md) | Pas de release quand seuls pilotage, docs, `*.md` ou badge changent | décision acceptée, livrée ; texte proposé |
| [0009](0009-lot-par-essence-parties-cochees.md) | Lot d'affouage : les parties cochées (tige, houppier) de chaque essence ; remplace 0004 | décision acceptée, livrée ; texte proposé |
| [0010](0010-proposition-ifn-autres-feuillus-resineux.md) | Proposition IFN calculée (moyenne pondérée) pour « Autres feuillus » / « Autres résineux » ; lignes au numéro ≤ 0 écartées | décision acceptée, livrée ; texte proposé |
