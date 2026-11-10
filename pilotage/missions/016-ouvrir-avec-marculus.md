# Mission 016 — « Ouvrir avec Marculus » : recevoir un `.marsync` sans passer par les Paramètres

## Mission

**En une phrase (quoi, et pourquoi) :** permettre de fusionner un fichier `.marsync` reçu
en l'ouvrant directement dans Marculus, depuis la notification Bluetooth, le gestionnaire de
fichiers ou la feuille de partage (« Ouvrir avec » / « Partager » → Marculus). Aujourd'hui, le
manifeste ne déclare que le lanceur (`app/src/main/AndroidManifest.xml` l. 43-46). La seule
voie de réception est donc Paramètres → « Fusionner », qui ouvre un sélecteur `*/*`
(`app/src/main/kotlin/io/github/pobsteta/marculus/ui/parametres/ParametresScreen.kt` l. 142-154
et l. 305). Le marteleur qui reçoit doit savoir où chercher, puis retrouver le bon fichier au
milieu de tous les autres (question de Pascal du 2026-11-09).

**Redite par l'agent :** _reconstituée le 2026-11-10 d'après le journal (2026-11-09, lignes 016), l'archiviste n'ayant pas rédigé de redite à l'époque._ Rendre possible la fusion d'un `.marsync` reçu en l'ouvrant dans Marculus (« Ouvrir avec » et « Partager »), avec une boîte de confirmation avant fusion, sans passer par Paramètres → Fusionner. Livrée par la PR #36 (dbad43c) après quatre allers-retours du dev (le quatrième, au-delà du critère d'arrêt, sur décision de Pascal : boîte au net). Redite de l'agent d'origine : non retrouvée.

## Ce qui existe (preuves)

- **Envoi :** `ListeContextesScreen.kt` l. 155-173. Le fichier
  `<contexte>-<opérateur>-<aaaa-MM-jj_HHmm>.marsync` est écrit dans le cache, puis exposé par le
  `FileProvider` et partagé en `ACTION_SEND` avec le type **`application/octet-stream`**.
- **Fusion :** `SauvegardeRepository.fusionnerJson` (`data/.../SauvegardeRepository.kt` l. 77-104)
  fait l'union par UUID en une seule transaction ; elle crée un contexte absent. C'est
  **la seule fonction de fusion** : la nouvelle voie l'appelle, sans en écrire une autre.
- **Messages :** `sync_msg_fusion` (« Synchronisation fusionnée. ») et `param_msg_illisible`
  existent déjà (`ParametresScreen.kt` l. 111-112).
- **Activité unique :** `MainActivity` (`exported="true"`) ne lit aujourd'hui aucun `Intent`
  entrant (`MainActivity.kt` l. 31-68).

## Décisions de Pascal (2026-11-09)

1. **Voies d'entrée : `ACTION_VIEW` et `ACTION_SEND`.** « Ouvrir avec » (notification
   Bluetooth, gestionnaire de fichiers) et « Partager » → Marculus (courriel, messagerie). Le
   `.marsync` n'a pas de type MIME propre : le filtre accepte `application/octet-stream`,
   `application/json` et `text/plain`. Prix accepté : Marculus apparaît dans « Ouvrir avec » de
   tout fichier binaire. L'appli refuse ensuite proprement ce qui n'est pas un `.marsync`.
2. **Confirmation avant fusion.** Boîte de dialogue « Fusionner *<nom du fichier>* ? »,
   Annuler / Fusionner. Annuler n'écrit rien. Le bouton Paramètres → « Fusionner » ne change pas.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | `intent-filter` sur `MainActivity`, lecture de `intent.data` / `EXTRA_STREAM` dans `onCreate` **et** `onNewIntent`, `contentResolver.openInputStream`, puis `fusionnerJson` |
| A-t-il les moyens ? | oui | manifeste, `MainActivity`, `SauvegardeRepository`, deux téléphones ou un émulateur et `adb` pour l'essai |

**Temps de la main :** appuyer. Le changement touche l'écran (la confirmation) et l'entrée de
l'appli (`pilotage/equipe.md` l. 22).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **`testeur` (sonnet), d'abord :** il écrit les tests d'acceptation à partir de cette fiche,
  sans lire le code.
- **`dev` (sonnet), ensuite :** il réalise la mission dans sa branche, tests compris.
- **`afficheur` :** il fournit la capture de la boîte de confirmation (clair et sombre) avant la PR.

## Harnais

- **Peut :** ajouter un ou deux `intent-filter` à `MainActivity`. Lire l'`Intent` entrant
  (`onCreate`, `onNewIntent`) et le transmettre à l'UI. Ajouter une fonction pure de
  reconnaissance du fichier (nom en `.marsync`, JSON avec `contextes` ou `tiges`). Ajouter des
  chaînes FR **et** EN. Mettre à jour `sync_desc` pour dire la nouvelle voie.
- **Ne doit pas :** modifier `fusionnerJson`, le format `.marsync` ou la voie Paramètres →
  « Fusionner » (elle reste) ; ajouter une permission de stockage (l'URI accordée par
  l'expéditeur suffit) ; toucher à `main`.
- **Attention :**
  - un `content://` n'a pas toujours de nom lisible : le nom s'obtient par `OpenableColumns.DISPLAY_NAME`, et à défaut c'est le contenu qui décide ;
  - les filtres `pathPattern` ne valent rien sur `content://` : la reconnaissance se fait dans le code, pas dans le manifeste ;
  - l'`Intent` déjà traité ne doit pas refusionner à la rotation de l'écran ni au retour dans l'appli ;
  - un fichier illisible ou étranger affiche `param_msg_illisible` et ne change rien ;
  - dire « GNSS », jamais « GPS », si un texte en parle.

## Tests

- **Unitaires (`.claude/test-command`) :**
  - la reconnaissance accepte un `.marsync` produit par `exporterContexteJson` ;
  - elle refuse un CSV, un fichier vide, un binaire et un JSON sans `contextes` ni `tiges` (une
    sauvegarde complète est acceptée : `fusionnerJson` la sait déjà fusionner, comme le bouton des Paramètres) ;
  - un contexte local absent du fichier survit à la fusion ; aucun test ne le prouvait (`MergeDao.kt` l. 39-56) ;
  - Annuler dans la confirmation n'écrit rien en base ;
  - recevoir deux fois le même fichier ne crée pas de doublon (l'union se fait par UUID).
- **Comment saura-t-on que c'est juste :** CI de la PR verte ; le relecteur vérifie que la seule
  écriture en base passe par `fusionnerJson`, et qu'un `Intent` est consommé une seule fois.

## Terrain

- **Échantillon que Pascal vérifiera :** sur le téléphone A, ⋮ → « Partager (.marsync) » d'un
  contexte, envoi en Bluetooth ou en Partage à proximité vers le téléphone B. Sur B, un appui
  sur le fichier reçu ouvre Marculus, qui demande confirmation puis fusionne ; le même essai passe par
  « Partager » → Marculus depuis une messagerie ; le
  contexte apparaît dans la liste avec le même nombre de tiges que sur A. Un second envoi du même
  fichier ne double rien.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Un marteleur fusionne le martelage d'un collègue d'un seul appui sur le fichier reçu, sans
connaître le chemin Paramètres → Fusionner.
