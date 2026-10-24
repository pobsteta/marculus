# ADR 0001 — Partage par fichier (`.marsync`, CSV), sans serveur

- **Date :** décision antérieure au 2026-10-23 (date exacte : non écrit) ; consignée le 2026-10-24
- **Statut :** **décision acceptée par Pascal** (contrainte posée dans `CLAUDE.md`, « Contraintes » ; périmètre rappelé dans `docs/brief.md` l. 59-60) ; **texte de l'ADR proposé**, à valider par Pascal à la lecture de la PR
- **Proposée par :** non écrit
- **Tranchée par :** Pascal

## La décision en une phrase

Les marteleurs échangent leurs données par fichier (`.marsync` JSON, CSV) via le partage système ; Marculus n'a pas de serveur et la synchro réseau entre appareils est hors périmètre.

## Contexte

L'application est hors ligne total et sans publicité (`CLAUDE.md`, « Contraintes »). La synchro multi-opérateurs est livrée par fichier : union par UUID, « dernière écriture gagne », dans une transaction atomique (`docs/specs/2026-06-21-marculus-v1-spec.md` §8, l. 214-225). La synchro temps réel / réseau reste « à faire » sans cycle prévu (spec l. 46, 226, 252 ; `docs/brief.md` l. 59-60). Les formats `.marsync` et CSV sont un contrat avec `nemetonshiny` (`docs/brief.md` l. 83) ; en entrée, `nemetonshiny` envoie des lots `.zip` (`docs/brief.md` l. 38), format d'entrée distinct du partage décrit ici.

Alternatives envisagées : non écrit (le journal et les specs ne les listent pas). Question à Pascal : quelles options (serveur propre, service tiers) ont été écartées, et pourquoi ?

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F** | Est-ce la solution la plus simple qui tienne ? Réparable avec les moyens du bord ? | Non écrit | Non écrit | Non écrit |
| **R** | Qui fait quoi, et chacun a-t-il les moyens de sa part ? | Non écrit | | Non écrit |
| **A** | Que se passe-t-il si ce qu'on n'a pas prévu arrive ? De qui ou de quoi dépendons-nous ? | Sans export `.marsync`, un appareil perdu perd ses données : « à trancher » (`docs/brief.md` l. 72) | Risque de perte ouvert | Non écrit |
| **N** | Avons-nous écouté toutes les idées, même les plus étranges ? | Non écrit | | |
| **C** | Que coûte cette décision à l'ensemble ? Y a-t-il une dépense inutile ? | Non écrit | Non écrit | Non écrit |
| **É** | Que réutilisons-nous, et qui pourra réutiliser ce que nous faisons ? | Formats ouverts (JSON, CSV) lisibles par `nemetonshiny` (`docs/brief.md` l. 38, 83) | | Non écrit |

Les cellules « non écrit » attendent la réponse de Pascal ; elles ne sont pas déduites ici.

## Valeurs sacrifiées

Le temps réel entre marteleurs d'une même coupe : accepté tant qu'aucun cycle n'est prévu (spec l. 252). La raison du sacrifice n'est pas écrite au-delà de « hors périmètre actuel ».

## Sources

- `CLAUDE.md`, « Contraintes » ; `docs/brief.md` l. 38, 59-60, 72, 83.
- `docs/specs/2026-06-21-marculus-v1-spec.md` l. 35, 46-48, 214-226, 252.

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ? Réponse de Pascal : non écrite.
