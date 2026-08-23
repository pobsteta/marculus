# Tranche « import d'un lot de chantiers (.zip) »

Livré le 2026-08-23. Demande : `briefs/vers-marculus/2026-08-23-import-lot-zip.md` (Nemeton
v0.136.1). Aucun changement de modèle, aucune migration Room.

## Ce que ça fait

**Liste des contextes → menu ⋮ → « Importer un lot (.zip) »** — c'est là qu'on reçoit un lot, sur
l'écran où les chantiers apparaissent. Le même geste existe dans **Paramètres → Synchro**, à côté
de la fusion : un opérateur qui vient de recevoir un `.marsync` y cherchera naturellement l'import
du lot. Un seul composant sert les deux écrans (`ui/lot/ImportLot.kt`), pour que le geste soit
identique et qu'aucun des deux ne puisse dériver vers la restauration destructive.

En un geste, ce que l'opérateur faisait treize fois : les contextes du lot sont fusionnés, et **chaque GeoPackage est
rattaché à son contexte** sans passer par un sélecteur de fichiers.

```
lot.zip
├── ForetAccess.marsync                        ← les 13 contextes, dont « gpkgNom »
├── ForetAccess_-_ug_1_-_eclaircie.gpkg
└── …
```

L'appariement se fait sur le champ **`gpkgNom`** que Nemeton émet dans chaque contexte. Ce champ
était jusqu'ici ignoré par `versContexte()` ; il est maintenant lu — et lui seul : `cheminGpkg`
reste absent du lot, c'est un chemin de notre stockage privé que l'émetteur ne peut pas connaître.

## Fusion, jamais restauration

Le point dur du brief. Deux imports de ZIP cohabitent désormais dans le même écran :

| Bouton | Effet | Chemin |
|---|---|---|
| *Restaurer* (section Sauvegarde) | **efface** contextes, tiges, configs, puis insère | `importerJson()` |
| *Importer un lot* (section Synchro) | **union par UUID**, rien n'est effacé | `fusionnerJson()` |

Le lot vit donc dans un dépôt séparé, `LotRepository`, dont **aucun chemin ne mène à
`importerJson()`** — et le bouton est posé sous *Fusionner*, pas sous *Restaurer*. Le lecteur ZIP
existant (`lireJsonDepuisZip`, qui alimente la restauration) n'est pas réutilisé : son nom ne dit
pas qu'il ouvre la porte destructive.

## Décisions

**Zip-slip : refus de l'archive entière.** Une entrée dont le nom n'est pas un nom de fichier nu
(`/`, `\`, `..`, nom vide) arrête l'import et est **nommée** dans le message. Rien n'est assaini :
un chemin réécrit en silence est la définition de la faille, et ce qui resterait ne serait de toute
façon pas le lot attendu. Conséquence à connaître : une archive **re-zippée** avec un dossier
racine (ou les entrées `__MACOSX/` d'un zip macOS) est refusée. Le lot de Nemeton est plat.

**Lot amputé : on importe quand même.** Un `gpkgNom` absent de l'archive n'annule rien — douze
chantiers valent mieux que zéro. Les contextes restés sans carte sont **nommés** dans le message
final : un contexte muet dont on ignore pourquoi il n'a pas de carte est pire qu'un message.

**Idempotence.** Les identifiants de contexte sont stables, donc la fusion par UUID ne duplique
rien. Côté fichier, le GeoPackage est rangé sous un nom **déterministe** (`lot-<gpkgNom>`,
neutralisé en ASCII) : ré-importer écrase au lieu d'accumuler des `fichier(1).gpkg`.

**Un rattachement manuel survit à un ré-import.** Le lot n'émet pas `cheminGpkg` : si Nemeton
ré-émet un contexte avec un `modifie` plus récent, la fusion le remplacerait par un contexte sans
carte. Les rattachements existants sont donc relevés **avant** la fusion et rendus **après** à
ceux que le lot ne rattache pas lui-même.

**Un seul `.marsync`.** Zéro → refus explicite (« ce n'est pas un lot »). Plusieurs → refus aussi :
on ne devine pas lequel fait foi.

## Mémoire et disque

L'archive est parcourue **une fois** : le `.marsync` (quelques dizaines de Ko) est gardé en
mémoire, les GeoPackages (plusieurs Mo chacun) sont écrits au fil du flux dans le cache, puis
**déplacés** (renommés) dans le stockage privé — même système de fichiers, donc pas de seconde
copie de 12 Mo. Le répertoire temporaire est effacé dans un `finally`, y compris en cas d'échec.

## Architecture

| Fichier | Module | Rôle |
|---|---|---|
| `LotMartelage.kt` | `:core` | Ce qu'on refuse (zip-slip), extensions, nom local déterministe, appariement |
| `LotRepository.kt` | `:data` | Lecture de l'archive, copies, fusion, rattachement |
| `SauvegardeRepository.contextesDuLot` | `:data` | Lecture seule du `gpkgNom` (n'écrit rien) |
| `ui/lot/ImportLot.kt` | `:app` | Sélecteur, appel au dépôt, compte rendu — partagé par les deux écrans |
| `ListeContextesScreen.kt` · `ParametresScreen.kt` | `:app` | Les deux entrées vers ce composant |

`:core` reste pur : 8 tests JVM couvrent les refus et l'appariement, sans archive ni Android.

## Ce que contient un GeoPackage du lot

Tables `parcelle` (périmètre de l'UGF) et `desserte` (routes, pistes, chemins). Conforme à
`couches-gpkg.md` : `parcelle` n'est pas un nom réservé, il tombe donc dans la règle de repli
« toute table inconnue est une couche de parcelles » — ce qui est le bon comportement.

**Pas de table de tuiles** (l'ortho du projet pèse des gigaoctets) : la carte se rabat sur les
fonds en ligne. **Pas encore de couche `houppier`** : la segmentation MNH est en cours côté
Nemeton, elle arrivera dans le même GeoPackage sans changer le format du lot.

## Recette

| Contrôle | Attendu |
|---|---|
| Lot de 13 chantiers (12 Mo) | 13 contextes, 13 cartes rattachées, aucun geste manuel |
| ✅ Lot de 3 chantiers, 2 GeoPackages (2026-08-23, SM-S918B) | contextes créés et cartes rattachées — cas nominal vérifié sur matériel |
| Ré-import du même lot | aucun doublon, aucun `fichier(1).gpkg` |
| Lot amputé d'un GeoPackage | 12 rattachés, le 13ᵉ créé sans carte, **et nommé** dans le message |
| ZIP sans `.marsync` | refus explicite, rien créé |
| Entrée contenant `..` | refus, entrée nommée, rien créé |
| Tiges déjà saisies dans un contexte ré-importé | intactes (union par UUID) |

Les quatre derniers cas sont vérifiables sans matériel ; le premier demande un vrai lot Nemeton.

## Non fait, volontairement

- **Aucun intent-filter** : on ne peut toujours pas « ouvrir avec Marculus » depuis un
  gestionnaire de fichiers. Le brief le pose comme à arbitrer séparément.
- **Aucune récupération par URL** : le lot se transfère à la main (Quick Share, câble, ou
  navigateur du téléphone). À rouvrir si le transfert s'avère être le point de friction.
