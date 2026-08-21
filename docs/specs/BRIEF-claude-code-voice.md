# Brief Claude Code — Marculus : saisie vocale des tiges (Vosk)

## Contexte

Tu travailles dans le dépôt `pobsteta/marculus` : app Android native Kotlin (Compose Material 3, minSdk 26) de saisie de tiges en martelage forestier. Architecture 3 modules : `:core` (domaine pur Kotlin/JVM, couvert par JaCoCo, **zéro dépendance Android**), `:data` (Room + DataStore), `:app` (Compose UI + services).

Fonctionnalités existantes sur lesquelles tu t'appuies SANS les modifier :
- journal de tiges **append-only** avec UUID (l'annulation est un événement, pas une suppression) ;
- rattachement GNSS point-dans-polygone (récepteur externe déjà connecté en Bluetooth) ;
- annonces vocales TTS (nombre, étiquette, alertes) ;
- comptage par **appui court sur les boutons de volume** ;
- contextes de martelage configurables : essences (codes ONF 3 lettres, table gftools), axe de classes de diamètre/circonférence, qualités.

## Objectif

Ajouter la dictée vocale des tiges : **Vosk + modèle `vosk-model-small-fr-0.22` + grammaire fermée + `[unk]` + confirmation TTS**, avec trois déclencheurs push-to-talk convergeant vers un contrôleur unique : FAB écran, appui **long** volume bas, bouton Flic 2 (BLE).

Énoncés cibles : `"hetre quarante cinq"` → tige HET/45 ; `"hetre quarante cinq bravo"` → HET/45/B ; `"quarante cinq"` seul → classe 45 sur l'essence courante (mode rafale) ; `"annule"`, `"repete"` → commandes.

## Matériaux fournis

Dans `_incoming/voice/` (à intégrer puis supprimer le dossier) :
- `VoiceGrammar.kt` → `:core` : grammaire JSON Vosk, lexique inversé, nombres français en jetons (logique vérifiée : 70→"soixante dix", 80→"quatre vingt", 95→"quatre vingt quinze", 71→"soixante et onze").
- `UtteranceParser.kt` → `:core` : jetons → `VoiceEvent` (Tige/Commande/Rejet), appariement au plus long, tout `[unk]` = Rejet.
- `VoskVoiceService.kt` → `:app` : cycle modèle/recognizer/micro.
- `INTEGRATION.md` : spécification détaillée (déclencheurs, code Activity, coexistence BT, matrice de validation). **Fais-en ta référence ; en cas de conflit avec ce brief, ce brief prime.**

Les packages `org.marculus.*` de ces fichiers sont des placeholders : adapte-les au namespace réel du dépôt. Les appels commentés (`journal.ajouterTige`, `annonceur.annoncer`...) sont des esquisses : **découvre les vrais noms dans le code existant** avant de câbler.

## Tâches, dans l'ordre

1. **Exploration** (aucune écriture) : lis la structure des modules, repère les composants réels — journal des tiges, annonceur TTS, gestion des touches volume du comptage, modèle de contexte de martelage (essences/classes/qualités), écran Paramètres, foreground service GNSS. Produis un court plan de câblage nommant les vraies classes, attends ma validation si un point est ambigu (notamment : où vit la capture des touches volume aujourd'hui).
2. **`:core`** : place `VoiceGrammar.kt` + `UtteranceParser.kt`, ajoute les tests JVM du plan de l'INTEGRATION.md §tests (nombres français, aller-retour classes, triplet complet, mode rafale, Rejets UNK/INCOMPLET/AMBIGU, grammaire sans doublons avec `[unk]`).
3. **Référentiel parlé** : génère `SpokenEssence` depuis la table gftools existante — libellé normalisé minuscules sans accents ; forme à deux mots imposée pour tout couple partageant le premier mot ou quasi-homophone (chêne/frêne, orme/charme, aulne/orme). Qualités en alphabet radio (alpha/bravo/charlie/delta). Ne dicte que les essences du contexte actif.
4. **`:app` — Vosk** : dépendances `com.alphacephei:vosk-android` + `net.java.dev.jna:jna@aar`, permission `RECORD_AUDIO` runtime (même mécanique que la permission GNSS). Modèle : choisis assets (~41 Mo APK) ou téléchargement au premier lancement selon la taille actuelle de l'APK — justifie le choix. Recognizer reconstruit à chaque changement de contexte.
5. **`PttController`** (`:app`) : point de convergence unique, garde anti-double-démarrage, bip à l'ouverture du micro. Seul habilité à appeler start/stopListening.
6. **Déclencheurs** :
   - FAB micro maintenu-appuyé dans la feuille de martelage ;
   - volume bas : appui court = comptage inchangé, appui **long** (~500 ms, `startTracking`/`onKeyLongPress`) = PTT — code de référence dans INTEGRATION.md ;
   - Flic 2 : `flic2lib-android` (JitPack), `onButtonUpOrDown` → PttController, manager hébergé dans le foreground service GNSS existant, permissions `BLUETOOTH_CONNECT`/`BLUETOOTH_SCAN` si absentes. **Derrière un réglage désactivé par défaut.**
   - Paramètres : trois cases indépendantes (écran / volume long / Flic) persistées DataStore.
7. **Câblage métier** : `VoiceEvent.Tige` → couper le micro (anti-larsen) → insertion journal via le mécanisme existant (UUID, rattachement GNSS) → annonce TTS → réouverture après `onDone`. `Commande(ANNULE)` → mécanisme d'annulation existant. `Rejet` → vibration double (pattern distinct du tick) + TTS "non compris", jamais d'insertion devinée.
8. **Finitions** : notice de licence Apache 2.0 (Vosk + modèle) dans l'écran À propos ; matrice de validation terrain d'INTEGRATION.md recopiée dans la doc du dépôt comme checklist de recette manuelle.

## Contraintes non négociables

- `:core` reste 100 % JVM : aucune dépendance Android, Vosk ou Flic n'y entre.
- Le comptage par appui court volume fonctionne **exactement comme avant** quand la case PTT-volume est décochée, et en appui court quand elle est cochée.
- Anti-larsen obligatoire : jamais micro ouvert pendant une annonce TTS.
- Aucun changement de schéma dans `:data` : la tige vocale est une tige normale.
- Tout `[unk]` dans un énoncé = Rejet complet, aucune récupération partielle.
- Respecte les conventions du dépôt (style, i18n, injection, nommage) que tu auras observées en tâche 1.

## Critères d'acceptation

- `./gradlew :core:test` vert, nouveaux tests inclus, couverture JaCoCo des deux fichiers `:core` ≥ le seuil du dépôt.
- Build APK vert, lint sans nouvelle erreur.
- Démo émulateur : contexte avec 3 essences → dictée simulée (injection du texte Vosk) → tige au journal + annonce.
- Les 7 cas de la matrice terrain sont documentés comme recette manuelle (leur exécution physique reste à ma charge : Flic 2, GNSS, gants).

Commence par la tâche 1 et montre-moi ton plan de câblage avant d'écrire du code.
