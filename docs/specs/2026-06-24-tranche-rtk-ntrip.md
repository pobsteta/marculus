# Tranche GNSS externe RTK/NTRIP — plan

> Objectif : ajouter à Marculus la **précision centimétrique** via un récepteur GNSS
> **externe** (type réseau **Centipede**), en plus du GNSS interne du téléphone (qui reste
> le mode par défaut, précision métrique). Préparé le 2026-06-24.
> Légende : ✅ fait · 🟡 partiel · 🔜 à faire.

## 1. Contexte technique actuel

- Position acquise via `android.location.LocationManager` (`GPS_PROVIDER`), pas de Play Services.
- Deux chemins dans `FeuilleMartelageScreen.kt` :
  - `positionActuelle(active)` — écoute continue (`requestLocationUpdates`, 2 s / 1 m) ;
  - `capturerPositionPonctuelle(context, onResult)` — one-shot (`getCurrentLocation` API 30+).
- `Position(latitude, longitude)` (dans `core/model/Modeles.kt`) — pas de qualité ni précision.
- La position (+ parcelle rattachée) est **figée sur la tige** au martelage
  (`Tige.position`, `Tige.parcelle`).
- Permissions présentes : `ACCESS_FINE_LOCATION`, `INTERNET`, `ACCESS_NETWORK_STATE`.

## 2. Principe de la chaîne RTK

```
  Caster NTRIP (Centipede)  --(RTCM3, Internet)-->  App Marculus
        ^                                                 |
        |  (GGA renvoyée, ~10 s, pour VRS)                |  (RTCM3 transféré)
        |                                                 v
        +-------------------------------------------  Récepteur GNSS externe
                                                      (rover : calcule la
                                                       solution RTK en interne)
                                                            |
                                                            v  (NMEA : GGA/GST/RMC)
                                                          App  →  Position + qualité de fix
```

- Le **récepteur (rover)** calcule lui-même la solution RTK ; l'app ne fait **pas** de calcul
  de positionnement. Elle joue le rôle de **pont** : relaie le flux RTCM du caster vers le
  récepteur, lit le NMEA en retour, renvoie périodiquement la trame GGA au caster (réseaux VRS).
- Les **données de martelage restent hors-ligne** ; seul le **flux de corrections** exige du
  réseau — inhérent au RTK, donc acceptable comme exception ciblée.

## 3. Décisions à figer (2026-06-24)

> Source : doc officielle Centipede `docs.centipede-rtk.org/rover/smartphone-rover-apps.html`.
> Elle confirme : **Bluetooth Classic** comme transport, **l'app mobile comme client NTRIP**
> (pont des corrections vers le récepteur), retour **NMEA**, et met en avant le **« mock
> location »** (Lefebure NTRIP Client / Bluetooth GNSS) pour partager la position RTK à toutes
> les apps Android.

### 3.0 — Stratégie d'intégration (LE choix structurant, à trancher en premier)

Deux voies pour amener le RTK dans Marculus, d'effort très différent :

- **Voie 1 — Déléguer via « mock location »** *(recommandée Centipede, effort minime)* :
  l'utilisateur lance une app tierce (**Lefebure NTRIP Client** ou **Bluetooth GNSS** open-source)
  qui connecte le récepteur (BT), fait le NTRIP et **injecte la position RTK dans le fournisseur
  de localisation Android**. Marculus la lit **par son `LocationManager` actuel, sans rien
  changer** — il suffit d'exploiter `Location.getAccuracy()` (précision en m, ~0,02 m en RTK
  fixe) pour afficher/figer la qualité. **Quasi zéro dev** ; contrepartie : une 2ᵉ app à
  installer/configurer (et l'option « application de position fictive » des options dév.).
- **Voie 2 — Intégrer dans Marculus** *(tout-en-un, effort important)* : Marculus fait lui-même
  BT SPP + parser NMEA + client NTRIP (sous-tranches G1→G3 ci-dessous). Expérience mono-app
  soignée, mais c'est le gros du travail et c'est dépendant du matériel pour la validation.

**Recommandation** : tester d'abord la **Voie 1** sur le terrain (Lefebure + mock location).
Si elle suffit, G1/G2 deviennent inutiles et il ne reste que la **trace de qualité sur la tige**
(une partie de G3). N'engager la Voie 2 que si l'expérience mono-app est jugée nécessaire.

> **Décision 2026-06-24 : VOIE 2 retenue** (intégration mono-app dans Marculus). On développe
> G1 → G2 → G3. G0 (mock location) abandonné. Ordre d'attaque : commencer par le **parser NMEA
> en `:core` (TDD, sans matériel)**, puis la liaison Bluetooth, puis le client NTRIP.

### 3.1 — Décisions techniques (ne valent que pour la Voie 2)

1. **Matériel cible** : récepteur Centipede actuel = **Septentrio mosaic-X5** ou **Unicore UM980**
   (multi-bandes, classe géodésique ; le F9P/ESP32 n'est plus la cible).
   - **Point clé pour l'app** : les deux modules **sortent du NMEA** (GGA/GST/RMC) et **acceptent
     du RTCM3** en entrée ; ils **calculent eux-mêmes la solution RTK**. L'app reste donc un
     **pont/lecteur**, indépendamment du module. (Formats binaires natifs — Septentrio **SBF**,
     **Unicore binary** — ignorés : on n'exploite que le NMEA.)
   - **Transport = Bluetooth Classic SPP** (norme confirmée par la doc Centipede ; USB série en
     option ultérieure). On isole tout de même un **`Transport`** abstrait (lecture/écriture
     d'octets) pour garder USB/TCP ouverts.
   - **NTRIP : l'app fait le pont (topologie B), c'est le défaut documenté Centipede** — l'app
     tire le RTCM du caster et le **renvoie au récepteur via le lien BT** ; le récepteur renvoie
     le NMEA corrigé. *(Cas A — récepteur/compagnon WiFi autonome qui corrige seul — reste
     supporté gratuitement : l'app lit alors juste le NMEA déjà en fix=4.)*
2. **Tige enrichie = OUI** : `qualiteFix` + `precisionM` figées au martelage → **migration Room**
   + colonnes export CSV (certifie qu'un point a été pris en RTK FIXE). **Vaut pour les deux
   voies** (en Voie 1, on figerait `Location.accuracy`).
3. **Caster par défaut = OUI, Centipede** : champs pré-remplis (`caster.centipede.fr:2101`),
   l'utilisateur n'ajuste que le mountpoint et ses identifiants.

## 4. Sous-tranches

### G0 — Voie 1 : qualité de fix via mock location 🔜 *(chemin court, à évaluer d'abord)*
*But : exploiter une position RTK déjà injectée par Lefebure/Bluetooth GNSS, sans coder BT/NTRIP.*

- Documenter le réglage terrain (app tierce + « application de position fictive » des options dév.).
- Lire `Location.getAccuracy()` (+ `hasAccuracy()`) sur le chemin `LocationManager` **existant** ;
  en dériver un `QualiteFix` approché (ex. < 0,05 m ⇒ RTK fixe, < 0,5 m ⇒ float/DGPS, sinon
  autonome) faute de champ NMEA brut.
- Afficher le badge de qualité + **figer `precisionM` sur la tige** (cf. G3, migration Room).
- **Si la Voie 1 suffit, G1/G2 ne sont pas développés.**

### G1 — Voie 2 : liaison récepteur + NMEA (sans corrections) 🔜
*But : lire la position d'un récepteur externe et l'afficher avec sa qualité, en fix
autonome/DGPS (pas encore RTK).*

- **`Transport` abstrait** (lecture/écriture d'octets) ; première implémentation selon le lien
  confirmé (§3.1) :
  - *Bluetooth Classic SPP* : `BluetoothSocket` (UUID `00001101-0000-1000-8000-00805F9B34FB`),
    appareil parmi `BluetoothAdapter.bondedDevices`. Permissions `BLUETOOTH_CONNECT` (API 31+) /
    legacy `BLUETOOTH`/`BLUETOOTH_ADMIN`.
  - *TCP/WiFi* : `Socket(host, port)` vers la carte porteuse (le plus simple à coder/tester).
  - *USB série* (CDC/FTDI) ou *BLE* : implémentations ultérieures si besoin.
  - En topologie (A), G1 suffit déjà à afficher un fix **RTK fixe** (le récepteur corrige seul).
- **Parser NMEA en domaine pur** (`:core`, TDD) :
  - `$--GGA` → lat/lon, **qualité de fix** (0 invalide · 1 autonome · 2 DGPS · 4 RTK fixe ·
    5 RTK float), altitude, HDOP, nb satellites ;
  - `$--GST` → écart-type lat/lon → **précision estimée (cm)** ;
  - `$--RMC` → cap/vitesse/horodatage (option).
  - Modèle `FixGnss(position, qualite: QualiteFix, precisionM: Double?, nbSats: Int, ageCorrM: Double?)`.
  - Validation checksum NMEA (`*hh`). Tests : trames réelles GGA/GST fixe/float/autonome.
- **Abstraction `SourcePosition`** : interface émettant un `Flow<FixGnss?>`, deux implémentations :
  - `SourceInterne` — encapsule le `LocationManager` existant (refactor des 2 fonctions actuelles) ;
  - `SourceExterne` — flux Bluetooth + parser NMEA.
  - Sélection selon réglage ; l'UI consomme `FixGnss`, agnostique de la source.
- **UI minimale** : badge de qualité de fix (couleur + libellé + précision + nb sats) sur la
  feuille/carte.

### G2 — Voie 2 : client NTRIP + corrections → RTK 🔜
*But : l'app tire le RTCM du caster Centipede et le renvoie au récepteur (pont, topologie B
par défaut) pour obtenir un fix RTK FIXE. Sautable si le récepteur corrige déjà seul (cas A).*

- **Client NTRIP** (réseau, `:data` ou module dédié) :
  - connexion TCP au caster, requête NTRIP (GET mountpoint, `Authorization: Basic`,
    en-têtes `Ntrip-Version: Ntrip/2.0`, `User-Agent`) ;
  - réception du flux **RTCM3** binaire ;
  - **renvoi de la GGA** du récepteur au caster toutes ~10 s (réseaux **VRS** : le caster a
    besoin de la position approchée du rover) ;
  - gestion reconnexion / timeout / 401 / sourcetable.
- **Pont RTCM** : écriture du flux RTCM reçu vers l'`OutputStream` du transport récepteur (G1).
- **Service de premier plan** (`foregroundServiceType="location"`, permissions
  `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION`) pour maintenir BT + NTRIP au-delà des
  rotations et écran éteint ; notification persistante avec état (caster / qualité de fix).
- Tests JVM : construction de la requête NTRIP, génération/cadence de la GGA.

### G3 — Réglages, intégration martelage & export 🔜
*But : configurable bout en bout et tracé dans les données.*

- **Paramètres** → section « GNSS externe (RTK) » :
  - bascule interne / externe ; choix du récepteur (appareil BT appairé ou host:port TCP) ;
  - **source des corrections** : *application (pont NTRIP)* — défaut — ou *récepteur autonome* ;
  - **config caster** pré-remplie Centipede (`caster.centipede.fr:2101`), mountpoint
    (idéalement liste depuis la *sourcetable*), identifiant, mot de passe — visible seulement
    si « pont NTRIP » ;
  - bouton **Tester** (état de connexion + qualité de fix en direct).
  - Persistance dans `Reglages`/DataStore (adresse récepteur BT, source corrections, caster).
    `ReglagesRepository`.
- **Feuille / carte** : l'indicateur de fix conditionne le martelage (option : avertir si le
  fix n'est pas RTK FIXE au moment du `+`).
- **Tige enrichie** (si décision §3.2) : `qualiteFix`, `precisionM` figées au martelage →
  **migration Room** ; colonnes ajoutées à l'export CSV (qualité + précision).
- i18n fr/en de toutes les nouvelles chaînes.

## 5. Permissions & manifeste (à ajouter)

- `BLUETOOTH_CONNECT` (+ `BLUETOOTH_SCAN` si découverte ; sinon appareils déjà appairés),
  `BLUETOOTH`/`BLUETOOTH_ADMIN` (maxSdk 30).
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`.
- `INTERNET` déjà présent (NTRIP).
- `<uses-feature android:name="android.hardware.bluetooth" android:required="false"/>`.

## 6. Risques / points de vigilance

- **Validation = matériel réel obligatoire** (mosaic-X5 / UM980) : impossible à tester sur
  émulateur. Prévoir un mode « rejeu » d'un fichier NMEA/RTCM enregistré pour les tests UI.
- **Lien à confirmer** : transport exact de la carte porteuse (BT SPP / BLE / USB / TCP-WiFi) et
  si elle fait son propre NTRIP (topologie A) ou non (B) ; format/cadence NMEA (talker GN/GP/GL),
  débit. mosaic-X5 et UM980 peuvent sortir leur binaire natif (SBF / Unicore) — forcer le **NMEA**.
- **VRS vs station unique** : la GGA renvoyée est obligatoire pour le VRS, inutile pour une base
  fixe — gérer les deux.
- **Batterie / chaleur** : BT + réseau + service continu ; bouton d'arrêt explicite.
- **Latence d'âge des corrections** : afficher l'âge ; dégrader proprement vers FLOAT/DGPS si le
  flux se coupe.
- **Découpe domaine pur** : NMEA et framing NTRIP en `:core` (TDD) ; I/O (BT, sockets, service)
  hors `:core`.

## 7. Ordre de livraison (Voie 2 retenue)

1. ✅ **Parser NMEA en `:core`** (TDD, sans matériel) — GGA/GST → `FixGnss` + checksum
   (`NmeaParser`, `QualiteFix`, `FixGnss` ; 12 tests JVM).
2. 🟡 **`G1`** — couche faite : `NmeaDecoupeur` (`:core`, testé), `Transport`/`TransportBluetoothSpp`,
   `SourcePosition` + `SourcePositionInterne`/`SourcePositionExterne`, `BadgeFix`, permissions BT
   au manifeste. **Reste (→ G3)** : sélection de l'appareil + bascule interne/externe + câblage
   dans la feuille/carte (dépend des réglages).
3. **`G2`** — client NTRIP (caster Centipede, GGA renvoyée pour VRS), pont RTCM, service avant-plan.
4. **`G3`** — réglages complets, trace `qualiteFix`/`precisionM` sur la tige (migration Room),
   export CSV, i18n.

G0 (mock location) abandonné. Chaque étape est démontrable seule ; le parser NMEA est livrable
et testable immédiatement, indépendamment du matériel.

## 8. Références

- **Doc apps rover Centipede** — `docs.centipede-rtk.org/rover/smartphone-rover-apps.html`
  (transport BT, app = client NTRIP, NMEA, mock location).
- **millipede-caster** — `github.com/CentipedeRTK/millipede-caster` : le **caster** (serveur C,
  côté réseau Centipede), *pas* réutilisable comme client mais **référence de protocole** :
  format de la **sourcetable**, auth `Basic` (`source.auth`/`host.auth`), mode **VRS/« NEAR »**
  confirmant le **renvoi périodique de la GGA** par le client (cf. G2).
- **Apps de référence** (comportement à imiter) : Lefebure NTRIP Client, **Bluetooth GNSS**
  (open-source — utile pour le parsing NMEA u-blox/NTRIP), SW Maps, QField.
