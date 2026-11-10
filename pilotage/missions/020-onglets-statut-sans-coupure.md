# Mission 020 — les onglets de Statut / historique ne coupent plus un mot

## Mission

**En une phrase (quoi, et pourquoi) :** faire tenir les libellés des onglets de Statut / historique
sans couper un mot au milieu, sur le téléphone de Pascal comme sur un petit écran. Aujourd'hui,
sur son téléphone (1080 × 2316 px, densité 450, soit environ 384 dp de large), l'onglet
« Historique » s'affiche « Historiqu / e » quand les quatre onglets sont présents (contexte
d'affouage) : capture `tel-0670/2-par-lot.png`, v0.67.0, journal du 2026-11-09. Sur l'émulateur
(1080 × 2400), le mot tient, mais « Par parcelle » passe déjà sur deux lignes. Demande de Pascal
du 2026-11-09 : « écris aussi la fiche pour l'onglet Historique coupé ».

**Redite par l'agent :** Faire que les quatre ou trois onglets de Statut / historique se lisent d'un coup d'œil sans qu'un mot soit coupé au milieu (« Historique » d'un seul tenant), jusqu'à 360 dp et police 1,3, sans changer les libellés ni cacher un onglet derrière un défilement. (`afficheur`, 2026-11-10)

## Ce qui existe (preuves)

- **La rangée d'onglets :** un `TabRow` à largeurs égales, dont chaque `Tab` reçoit un
  `Text(stringResource(libelle))` sans réglage
  (`app/src/main/kotlin/io/github/pobsteta/marculus/ui/statut/StatutHistoriqueScreen.kt`
  l. 159-171).
- **Trois ou quatre onglets :** « Par lot » n'existe que pour un contexte d'affouage (l. 159-165).
- **Les libellés :** FR « Statut », « Par lot », « Par parcelle », « Historique »
  (`app/src/main/res/values/strings.xml` l. 164-166 et l. 490) ; EN « Status », « By lot »,
  « By parcel », « History » (`values-en/strings.xml` l. 162-164 et l. 488).
- **Densité du téléphone de Pascal :** `adb shell wm density` → 450 (relevé le 2026-11-09).

## Règles

- **Aucun mot coupé au milieu**, en FR et en EN, avec trois et avec quatre onglets, en portrait,
  jusqu'à **360 dp** de large et avec une taille de police système de **1,0 et 1,3**.
- **Les onglets restent visibles d'un coup d'œil.** Un onglet caché derrière un défilement
  horizontal est un recul : si l'`afficheur` le juge inévitable, il le dit et montre la capture
  avant de le faire.
- **Les libellés ne changent pas** sans l'accord de Pascal. Si un libellé plus court est la
  meilleure solution (par exemple « Lots », « Parcelles »), l'`afficheur` le propose en capture
  et n'en décide pas seul.
- Un passage sur deux lignes **entre deux mots** (« Par / parcelle ») reste acceptable.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | à moitié | plusieurs voies (taille du texte, marges des `Tab`, coupure entre mots seulement, défilement) : l'`afficheur` choisit la plus simple qui tient les règles et la montre |
| A-t-il les moyens ? | oui | émulateur `Medium_Phone_API_36.1`, réglable à la densité du téléphone (`adb -s emulator-5554 shell wm density 450`, `wm size`) et à la police (`settings put system font_scale`) |

**Temps de la main :** appuyer. Mission d'écran : capture avant la PR (`pilotage/equipe.md` l. 22),
puis capture sur le téléphone après la release.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **`afficheur` (sonnet)** seul. Pas de `testeur` : la mission ne change aucun calcul, et `:app`
  n'a pas de test d'interface ; la preuve est la capture. Si l'`afficheur` sort une règle
  testable (par exemple une fonction qui choisit la taille du texte), il en écrit le test.

## Harnais

- **Peut :** modifier la rangée d'onglets de `StatutHistoriqueScreen.kt` ; régler l'émulateur
  (densité, taille, police) et le remettre à son état d'origine à la fin ; dans sa branche.
- **Ne doit pas :** changer les libellés sans l'accord de Pascal, ni le contenu des onglets ;
  toucher à `main` ; utiliser le téléphone de Pascal (`R5CW51BZA2W`).
- **Attention :** les missions 018 et 019 travaillent en parallèle ; la 019 modifie le même
  fichier (calcul de « Par parcelle » et `csvFoncier`, l. 425 et suivantes) : ne toucher qu'à la
  rangée d'onglets (l. 159-171) pour que les deux PR se fusionnent sans conflit.

## Tests

- **Captures, avant / après**, sur l'émulateur réglé à 450 dpi et 1080 px de large (comme le
  téléphone) puis à 360 dp : quatre onglets (affouage) et trois onglets (contexte ordinaire), FR
  et EN, police 1,0 et 1,3, clair et sombre pour au moins un cas.
- **Comment saura-t-on que c'est juste :** aucune capture ne montre un mot coupé ; le relecteur
  les regarde toutes.

## Terrain

- **Échantillon que Pascal vérifiera :** sur le téléphone, contexte « Affouage » : « Historique »
  s'écrit d'un seul tenant ; capture par l'orchestrateur après la release.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. · **Critère d'arrêt :** 3 allers-retours. · **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

On lit chaque onglet d'un coup d'œil, sans mot coupé, sur le téléphone de Pascal.
