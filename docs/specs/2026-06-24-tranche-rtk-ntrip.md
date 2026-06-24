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

## 3. Décisions figées (2026-06-24)

1. **Matériel cible** : **u-blox ZED-F9P piloté par un ESP32** (rover Centipede DIY).
   - **Transport = Bluetooth Classic SPP** : la quasi-totalité des firmwares ESP32 Centipede
     exposent le F9P en série-sur-Bluetooth (comme attendu par SW Maps / Lefebure NTRIP Client).
     UUID SPP `00001101-0000-1000-8000-00805F9B34FB`. USB/BLE écartés pour l'instant.
   - **Conséquence majeure — qui fait le NTRIP ?** Deux topologies, l'app supporte les deux :
     - **(A) ESP32 corrigé en WiFi** : l'ESP32 fait lui-même le client NTRIP et alimente le F9P ;
       le téléphone **n'a qu'à lire le NMEA** (GGA donne déjà fix=4 RTK fixe). → **G2 devient
       optionnel**, juste de la config côté ESP32.
     - **(B) Téléphone = pont NTRIP** : l'ESP32 ne sert que le NMEA ; l'app tire le RTCM du
       caster et le **renvoie au F9P via le même lien Bluetooth**. → **G2 requis**.
     Le réglage « source des corrections » bascule entre *récepteur (ESP32)* et *application (pont)*.
2. **Tige enrichie = OUI** : `qualiteFix` + `precisionM` figées au martelage → **migration Room**
   + colonnes export CSV (certifie qu'un point a été pris en RTK FIXE).
3. **Caster par défaut = OUI, Centipede** : champs pré-remplis (`caster.centipede.fr:2101`),
   l'utilisateur n'ajuste que le mountpoint et ses identifiants.

## 4. Sous-tranches

### G1 — Liaison récepteur + NMEA (sans corrections) 🔜
*But : lire la position d'un récepteur externe et l'afficher avec sa qualité, en fix
autonome/DGPS (pas encore RTK).*

- **Transport Bluetooth Classic SPP** : `BluetoothSocket` SPP (UUID
  `00001101-0000-1000-8000-00805F9B34FB`), appareil choisi parmi `BluetoothAdapter.bondedDevices`
  (ESP32 déjà appairé au niveau système). Lecture du flux d'octets sur l'`InputStream`.
  - Permissions runtime : `BLUETOOTH_CONNECT` (API 31+) ; legacy `BLUETOOTH`/`BLUETOOTH_ADMIN`.
  - En topologie (A), G1 suffit déjà à afficher un fix **RTK fixe** (l'ESP32 corrige tout seul).
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

### G2 — Client NTRIP + corrections → RTK 🔜 *(requis seulement en topologie B)*
*But : quand l'ESP32 ne fait pas lui-même le NTRIP, l'app tire le RTCM du caster et le renvoie
au F9P pour obtenir un fix RTK FIXE. Inutile si l'ESP32 corrige déjà en WiFi (topologie A).*

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
  - bascule interne / externe ; choix du récepteur ESP32 (liste des appareils appairés) ;
  - **source des corrections** : *récepteur (ESP32 en WiFi)* — défaut — ou *application (pont NTRIP)* ;
  - **config caster** pré-remplie Centipede (`caster.centipede.fr:2101`), mountpoint
    (idéalement liste depuis la *sourcetable*), identifiant, mot de passe — visible seulement
    si « pont NTRIP » ;
  - bouton **Tester** (état de connexion + qualité de fix en direct).
  - Persistance dans `Reglages`/DataStore (adresse ESP32, source corrections, caster).
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

- **Validation = matériel réel obligatoire** (ESP32 + F9P) : impossible à tester sur émulateur.
  Prévoir un mode « rejeu » d'un fichier NMEA/RTCM enregistré pour les tests UI.
- **Firmware ESP32** : confirmer qu'il expose bien le F9P en **BT Classic SPP** et s'il fait
  son propre NTRIP (topologie A) ou non (B) ; format NMEA (talker GN/GP/GL), débit série.
- **VRS vs station unique** : la GGA renvoyée est obligatoire pour le VRS, inutile pour une base
  fixe — gérer les deux.
- **Batterie / chaleur** : BT + réseau + service continu ; bouton d'arrêt explicite.
- **Latence d'âge des corrections** : afficher l'âge ; dégrader proprement vers FLOAT/DGPS si le
  flux se coupe.
- **Découpe domaine pur** : NMEA et framing NTRIP en `:core` (TDD) ; I/O (BT, sockets, service)
  hors `:core`.

## 7. Ordre de livraison conseillé

`G1 (lire + afficher un fix externe)` → `G2 (pont NTRIP, topologie B seulement)` →
`G3 (réglages, trace, export)`. Chaque sous-tranche est démontrable seule. **En topologie A
(ESP32 corrigé en WiFi), G1 livre déjà le RTK centimétrique** — G2 peut être différé voire
sauté. Tester d'abord l'ESP32 en mode A pour décider si G2 est nécessaire.
