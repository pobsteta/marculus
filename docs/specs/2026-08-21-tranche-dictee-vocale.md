# Tranche « dictée vocale des tiges » (Vosk)

Livré le 2026-08-21. Référence de conception : `BRIEF-claude-code-voice.md` (et l'INTEGRATION.md
fourni avec les sources, désormais résorbé dans ce document).

## Ce que ça fait

L'opérateur maintient un déclencheur, dit « hêtre quarante cinq chablis », relâche : une tige
Hêtre/45 de qualité Chablis entre au journal exactement comme une tige tapée (UUID, rattachement
GNSS point-dans-polygone, annulation par événement), et le téléphone confirme à voix haute.

Énoncés reconnus :

| Dit | Effet |
|---|---|
| « hêtre quarante cinq » | tige, essence + classe |
| « hêtre quarante cinq chablis » | tige, avec la qualité arbre du référentiel |
| « quarante cinq » | tige sur l'essence courante (**mode rafale**) |
| « annule » | annule la dernière tige (événement d'annulation, jamais d'effacement) |
| « hêtre quarante cinq hauteur vingt sept » | tige **et** sa hauteur, en une seule insertion |
| « chablis » | qualité arbre sur la dernière tige (équivalent vocal du bouton Q) |
| « hauteur vingt sept six alpha bravo » | hauteur + découpe sur la dernière tige (`27-6AB`) |
| « repete » | ré-annonce la dernière tige |
| tout le reste | **rejet** : vibration double + « non compris », aucune insertion devinée |

Le mode rafale repart de zéro quand la parcelle rattachée change.

### Ce qui se dit pour la qualité — et ce qui ne se dit pas

La qualité dictée est la **qualité arbre**, celle du bouton Q de la cellule, et elle se dit avec le
**libellé du référentiel normalisé** : avec le référentiel par défaut, « sec », « chablis »,
« malade ». L'**alphabet radio** ne s'applique qu'aux entrées qui sont des **codes en lettres** :
un référentiel A/B/C/D se dicte « alpha », « bravo »… Dire « bravo » sur un référentiel en toutes
lettres est donc un mot hors grammaire, et l'énoncé entier est rejeté — c'est le comportement
voulu, mais l'exemple « hêtre quarante cinq bravo » du brief supposait un référentiel en codes.
L'aide « Formes à dicter » de l'application construit désormais son exemple sur le contexte
réellement ouvert, pour qu'aucun exemple figé ne puisse mentir.

Une **qualité dite seule** annote la dernière tige, exactement comme la hauteur : ni essence ni
classe n'ayant été dites, il n'y a rien à créer. C'est ce qui permet de qualifier sans regarder
l'écran une tige comptée au doigt sur la grille. S'il n'y a pas de tige dans la session,
l'application répond « aucune tige » et n'écrit rien — même règle que la hauteur.

En revanche « hêtre chablis » (essence sans classe) reste un **rejet incomplet** : on ne devine
jamais la classe manquante. Les trois cas sont verrouillés par des tests.

### Hauteur et qualité bois de découpe

La qualité bois n'a pas de champ propre dans le schéma : elle vit dans le texte de hauteur
(`27-6AB4CD` = 27 m dont 6 m de AB et 4 m de CD), et une qualité de découpe sans longueur n'a pas
de sens. Elle se dicte donc **avec** la hauteur, derrière le mot-clé `hauteur` :

| Dit | Texte produit |
|---|---|
| « hauteur vingt sept » | `27` |
| « hauteur vingt sept six alpha bravo » | `27-6AB` |
| « hauteur vingt sept six alpha bravo quatre charlie delta » | `27-6AB4CD` |

Le mot-clé bascule **la suite** de l'énoncé en mètres : sans lui « quarante cinq » est une classe de
diamètre, avec lui c'est une longueur. Il peut donc ouvrir l'énoncé — la hauteur annote alors la
dernière tige — ou le **couper en deux** :

> « hêtre quarante cinq chablis hauteur vingt sept six alpha bravo »

Une seule insertion porte alors essence, classe, qualité et hauteur ; pas d'annotation après coup,
une seule annonce. Devant le mot-clé on exige une **tige complète** : « chablis hauteur vingt sept »
est rejeté plutôt que d'annoter deux choses à la fois — et surtout, rendre ici l'événement de tête
reviendrait à jeter la hauteur en silence, ce qu'un test interdit désormais explicitement (le défaut
existait dans la première version de ce code).

Le compromis reste ouvert à l'opérateur : la forme longue économise un appui, la forme courte limite
la casse, puisqu'un seul mot mal décodé rejette **tout** l'énoncé. Le texte produit est **exactement celui de la saisie manuelle** — il
repasse par `annoterHauteur` et se relit avec `HauteurParser`, sans rien de nouveau dans le schéma.
Les lettres dictables sont celles du **référentiel de qualité bois** (A/B/C/D par défaut), épelées
en alphabet radio ; les hauteurs vont jusqu'à 50 m.

**Priorité sur l'estimation MNH** : une hauteur dictée est une hauteur *mesurée*, elle prime sur
toute estimation. Le réglage « Estimer la hauteur (MNH) » existe dans les Paramètres mais n'est
aujourd'hui branché nulle part (seul l'interrupteur est câblé) ; quand il le sera, il ne devra
compléter que les tiges **sans** hauteur — l'insertion passe déjà `hauteurTexte` explicitement pour
que la règle soit tenable. Une longueur de billon sans qualité derrière est
un énoncé incomplet, donc rejeté.

## Architecture

| Fichier | Module | Rôle |
|---|---|---|
| `voice/VoiceGrammar.kt` | `:core` | Grammaire JSON fermée, lexique inversé, nombres français en jetons |
| `voice/UtteranceParser.kt` | `:core` | Jetons Vosk → `VoiceEvent` (Tige / Commande / Rejet) |
| `voice/ReferentielParle.kt` | `:core` | Formes à dicter générées depuis les essences du contexte et les qualités du référentiel |
| `voice/NoyauPtt.kt` | `:core` | Machine à états du push-to-talk (garde anti-double-démarrage, anti-larsen) |
| `voice/VoskVoiceService.kt` | `:app` | Modèle → recognizer → micro |
| `voice/PttController.kt` | `:app` | Point de convergence unique des déclencheurs, bip d'ouverture |
| `voice/ModeleVosk.kt` | `:app` | Téléchargement / installation / suppression du modèle |
| `ui/feuille/DicteeVocale.kt` | `:app` | Colle Compose : grammaire du contexte, permission micro, bouton micro, aide-mémoire |

`:core` reste 100 % JVM : ni Vosk, ni Android n'y entrent. 44 tests JVM couvrent la grammaire, le
parseur, le référentiel parlé et le noyau PTT (93 % d'instructions couvertes sur le paquet `voice`).

### Référentiel parlé

Les formes à dicter sont **générées** depuis les libellés réels du contexte, pas saisies à la main :

- forme parlée = libellé normalisé (minuscules, sans accents), 3 mots au plus ;
- **forme à deux mots imposée** dès que deux essences partagent leur premier mot
  (« chêne sessile » / « chêne pédonculé ») ou que leurs premiers mots sont quasi-homophones
  (chêne/frêne, orme/charme, aulne/orme, pin/sapin) ;
- si le libellé n'a qu'un mot et entre en conflit, le second mot est **la lettre discriminante du
  code ONF en alphabet radio** : « Chêne » (CHR) et « Frêne » (FRC) se disent « chene charlie » et
  « frene foxtrot ». Aucune espèce n'est inventée : le libellé enregistré reste celui du contexte ;
- qualités : un code en lettres est épelé en radio (« A » → « alpha », « AB » → « alpha bravo »),
  un libellé en toutes lettres se dicte tel quel (« Chablis » → « chablis »).

Le menu ⋮ de la feuille de martelage affiche « Formes à dicter » : la liste exacte pour le contexte
ouvert. C'est l'aide de terrain à montrer à un nouvel opérateur.

### Rien n'est promis que le modèle ne sache décoder

`vosk-model-small-fr-0.22` a un vocabulaire fini (135 774 mots) et **ignore silencieusement** les
mots d'une grammaire qui n'y figurent pas : un simple `Ignoring word missing in vocabulary: 'volis'`
dans le journal, et la forme devient indictable sans que rien ne le signale à l'opérateur.

L'application lit donc ce vocabulaire avant de construire les formes parlées. Il est embarqué dans
la table de symboles OpenFst de `graph/Gr.fst` ; `VocabulaireVosk.motsConnus` parcourt cette table
en flux et ne teste que les mots candidats — le lexique complet n'est jamais chargé en mémoire.
Deux conséquences :

- un **libellé inconnu du modèle** (« Volis ») bascule sur l'épellation radio de ses initiales,
  allongée juste assez pour rester unique : **Volis → « victor »** ;
- les **mots de l'alphabet radio** ont eux-mêmes des replis, car `foxtrot`, `juliett`, `uniform` et
  `xray` **ne sont pas dans le modèle** — ils deviennent `fox`, `juliette`, `uniforme`, `xavier`.
  Sans ce garde-fou, la désambiguïsation « frêne » (FRC) aurait produit « frene foxtrot », que le
  décodeur n'aurait jamais reconnu.

La liste affichée dans « Formes à dicter » est donc, par construction, celle que le modèle sait
décoder.

### Déclencheurs

Deux sources, activables indépendamment dans Paramètres → « Dictée vocale », toutes deux passant par
`PttController` — seul habilité à ouvrir et fermer le micro :

1. **Bouton micro à l'écran** : maintenu appuyé dans la feuille de martelage.
2. **Volume bas maintenu** : appui court = comptage (inchangé), appui long ≈ 0,5 s = micro
   (`startTracking` / `onKeyLongPress`). Ne fonctionne qu'écran allumé, application au premier plan.

Quand la case « volume bas maintenu » est **décochée**, le comptage par boutons de volume se
comporte exactement comme avant. Quand elle est **cochée**, le volume bas est entièrement consommé
par la feuille : le tick de comptage se produit au relâchement, et le volume système ne bouge plus
tant que la feuille est ouverte.

### Bouton Flic 2 — reporté

Le troisième déclencheur prévu (Flic 2 en BLE, « téléphone en poche ») n'est **pas** dans ce lot : sa
bibliothèque n'est distribuée que par JitPack, et l'ajout de ce dépôt au projet a été écarté pour
l'instant. `PttController` est prêt à recevoir une troisième source : il suffira de lui envoyer
`demarrer()` sur `isDown` et `arreter()` sur `isUp`, avec le manager hébergé par le service de
premier plan `ServiceGnssRtk` (qui survit au Doze). Le garde du `NoyauPtt` gère déjà le cas « Flic et
volume tenus en même temps » — c'est le test `deux sources tenues simultanement`.

Coexistence Bluetooth vérifiée sur le papier : le récepteur GNSS (SPP ou BLE) et un bouton BLE sont
deux liens indépendants, multiplexés par la puce dual-mode ; aucun n'est un périphérique audio, donc
la capture reste sur le micro interne. À éviter : les télécommandes selfie type AB Shutter, qui
émulent volume+ et percuteraient le comptage.

### Anti-larsen

Le micro n'est **jamais** ouvert pendant une annonce : à la réception d'un événement, l'écoute est
suspendue avant l'insertion, et ne reprend qu'à la fin de la **dernière** annonce de la file
(`UtteranceProgressListener`, compteur d'annonces en cours — une alerte « avis » enchaînée derrière
la confirmation ne rouvre donc pas le micro trop tôt). Si l'opérateur a relâché le déclencheur
entre-temps, le micro ne se rouvre pas.

### Modèle acoustique

`vosk-model-small-fr-0.22` (~42 Mo, Apache 2.0), **téléchargé au premier usage** depuis les
Paramètres, décompressé dans le stockage interne — jamais embarqué dans les assets : l'APK release
pesait 11 Mo, l'embarquer l'aurait porté à ~53 Mo pour une fonction optionnelle et désactivée par
défaut. À télécharger en Wi-Fi avant la sortie ; ensuite tout est hors ligne.

Les bibliothèques natives de Vosk (4 ABI) pèsent tout de même ~42 Mo non compressées : le
`packaging { jniLibs { useLegacyPackaging = true } }` de `:app` les compresse dans l'APK (au prix
d'une extraction à l'installation), ce qui limite la croissance de l'APK release à ~+10 Mo.

## Recette manuelle (hors CI)

Ces sept cas demandent du matériel réel — ils ne sont pas automatisables et restent à exécuter sur
le terrain, gants compris.

| # | Cas | Attendu |
|---|---|---|
| 1 | Appui **court** sur volume bas | Tick de comptage, aucun micro |
| 2 | Appui **long** sur volume bas, dictée, relâchement | Bip → tige créée → annonce TTS |
| 3 | Déclencheur externe maintenu, dictée, relâchement | Idem, téléphone en poche, écran éteint (**reporté avec le Flic 2**) |
| 4 | Deux déclencheurs tenus en même temps | Une seule session d'écoute (garde du `NoyauPtt`) |
| 5 | Dictée pendant une annonce TTS | Impossible : micro coupé (anti-larsen) |
| 6 | Récepteur GNSS BT connecté + bouton BLE connecté | Fix conservé, aucune déconnexion croisée (**reporté avec le Flic 2**) |
| 7 | Écran éteint + touche volume | Rien (limite assumée : `onKeyDown` n'arrive qu'à l'Activity au premier plan) |

À vérifier en plus lors de la première sortie :

- **Bruit réel** : dictée sous vent/pluie, à 3 m d'une tronçonneuse à l'arrêt puis en marche.
- **Rejets** : dire volontairement une essence hors contexte → vibration double + « non compris »,
  et **aucune** tige au journal.
- **Rafale** : « hêtre quarante », puis « quarante cinq », « cinquante » sans répéter l'essence.
- **Changement de parcelle** : après avoir franchi une limite, une classe seule doit être rejetée
  (l'essence courante est remise à zéro) tant que l'essence n'a pas été redite.
- **Oreillette Bluetooth** (optionnel) : si une oreillette SCO est appairée, vérifier si la capture
  bascule sur son micro — potentiellement une amélioration (micro près de la bouche).

## Démo sans micro (émulateur)

En build **debug** uniquement, le dialogue « Formes à dicter » propose un champ « Énoncé simulé » :
le texte injecté suit exactement le même chemin que la sortie de Vosk (parseur → événement →
journal → annonce). C'est ce qui permet de démontrer la chaîne complète sur émulateur.

Démo passée le 2026-08-21 sur l'AVD `Medium_Phone_API_36.1` : modèle téléchargé depuis les
Paramètres, contexte « Demo » à 3 essences (Chêne / Hêtre / Sapin, axe 20–90 pas 5), injection de
« hetre quarante cinq » → **Hêtre 45 = 1**, puis « cinquante » seul → **Hêtre 50 = 1**
(mode rafale), la cellule active suivant la dernière tige.

## Étape suivante (hors lot)

- Bouton Flic 2 (cf. ci-dessus).
- Note libre dictée (whisper.cpp) — hors grammaire fermée par construction.
