# Mission 035 — la fiche de Marculus pour le dépôt IzzyOnDroid

## Mission

**En une phrase (quoi, et pourquoi) :** ajouter au dépôt les métadonnées Fastlane (titre, description
courte et longue, icône, captures, en français et en anglais) qu'IzzyOnDroid exige, pour que Pascal
puisse demander l'inclusion de Marculus et qu'un affouagiste l'installe et le mette à jour depuis F-Droid
sans manipuler d'APK. Décision de Pascal du 2026-11-11 : IzzyOnDroid plutôt que F-Droid officiel.

**Redite par l'agent :** _à compléter par l'agent avant d'agir._

## Ce qui existe (preuves, à c330d1c)

- Conditions d'IzzyOnDroid, lues le 2026-11-11 : https://izzyondroid.org/docs/general/AppInclusionPolicy/
  et https://izzyondroid.org/contributing/NewAppInclusions/ (licence libre, APK sur les releases GitHub,
  moins de 30 Mo, signé par la clé du développeur, ni `debuggable` ni `testOnly`, métadonnées Fastlane dans
  le dépôt, description courte de 80 caractères au plus, tags conformes à versionName).
- Déjà conformes : `LICENSE` (GPL v3) ; APK v0.73.2 de 20 602 379 octets sur la release GitHub, signé par
  `marculus-release.keystore` (`app/build.gradle.kts` l. 26-43) ; tag `v0.73.2` pour versionName `0.73.2`.
- Manque : aucun dossier `fastlane/` dans le dépôt (`git ls-tree -r origin/main | grep -i fastlane` vide).
- Réseau à décrire honnêtement dans la description : tuiles de carte (osmdroid) et modèle vocal Vosk
  téléchargé à la demande (`ModeleVosk.kt` l. 23) ; tout le reste est hors ligne.

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment faire ? | oui : structure Fastlane d'IzzyOnDroid (https://izzyondroid.org/docs/general/Fastlane/) | |
| A-t-il les moyens ? | oui : émulateur pour les captures, `docs/brief.md` et la spec pour les textes | |

**Temps de la main :** appuyer. Les textes disent ce qu'est le produit : Pascal les relit mot à mot.

## L'Homme

- **Agent :** `afficheur` (sonnet). Captures sur l'émulateur avec des données d'exemple, avant la relecture.

## Harnais

- **Peut :** créer `fastlane/metadata/android/fr-FR/` et `en-US/` (`title.txt`, `short_description.txt`,
  `full_description.txt`, `images/icon.png` 512 × 512 tiré de l'icône de l'appli,
  `images/phoneScreenshots/` : liste, feuille, Par lot, carte), un test qui vérifie longueurs et présence
  des fichiers, une ligne au README.
- **Ne doit pas :** toucher au code de l'appli, à la signature ni à `release.yml` ; mettre dans une capture
  un nom de personne ou une donnée réelle de Pascal ; dire « GPS ». Signaler (sans le corriger) si un
  commit qui ne touche que `fastlane/` déclencherait une release (règle de la 031).

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

Aucune. Ensuite, hors mission : Pascal ouvre la demande sur https://codeberg.org/IzzyOnDroid/repodata/issues
avec son compte Codeberg.

## Tests

- Fichiers présents dans les deux langues ; description courte ≤ 80 caractères ; titre « Marculus » ;
  icône 512 × 512 ; au moins deux captures par langue.

## Terrain

- **Échantillon :** Pascal lit les deux descriptions et regarde les captures avant la fusion.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Arrêt :** 3 allers-retours. **Qui tranche :** Pascal.
