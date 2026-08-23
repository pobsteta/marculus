package io.github.pobsteta.marculus.ui.carte

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import io.github.pobsteta.marculus.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.AttributionSpatiale
import fr.marculus.core.Houppier
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.Position
import io.github.pobsteta.marculus.data.GpkgRepository
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.data.OrthoSource
import io.github.pobsteta.marculus.data.DesserteGpkg
import io.github.pobsteta.marculus.data.ParcelleGpkg
import io.github.pobsteta.marculus.gnss.ServiceGnssRtk
import io.github.pobsteta.marculus.gnss.SourcePositionInterne
import io.github.pobsteta.marculus.ui.BandeauCompact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.MapTileApproximater
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private const val ZOOM_MAX = 19.0

/** Au-delà de cette vitesse (m/s ≈ 2,9 km/h), on oriente le cône par le cap GNSS plutôt que la boussole. */
private const val SEUIL_VITESSE_MS = 0.8

/** Une couche du GeoPackage dans le menu : sa case, son libellé, son nombre d'objets. */
@Composable
private fun LigneCouche(libelle: String, coche: Boolean, disponible: Boolean, onChange: (Boolean) -> Unit) {
    DropdownMenuItem(
        enabled = disponible,
        text = { Text(libelle) },
        leadingIcon = {
            Checkbox(checked = coche && disponible, enabled = disponible, onCheckedChange = { onChange(it) })
        },
        onClick = { if (disponible) onChange(!coche) },
    )
}

/** Vert des houppiers : contour seul, assez soutenu pour se voir sur une ortho de forêt. */
private const val COULEUR_HOUPPIER = 0xFF2E7D32.toInt()

/** Ocre de la desserte : lisible sur l'ortho comme sur le fond OSM, distinct des essences. */
private const val COULEUR_DESSERTE = 0xFFB35C00.toInt()

/** Palette de couleurs distinctes pour colorer les parcelles par propriétaire. */
private val PALETTE_FONCIER = listOf(
    0xFF1E88E5.toInt(), 0xFF43A047.toInt(), 0xFFF4511E.toInt(), 0xFF8E24AA.toInt(),
    0xFFFDD835.toInt(), 0xFF00ACC1.toInt(), 0xFF6D4C41.toInt(), 0xFFEC407A.toInt(),
)

private enum class Fond(val libelle: String) { OSM("OSM"), SATELLITE("Satellite"), ORTHO("Ortho") }

private val SOURCE_SATELLITE: OnlineTileSourceBase = object : OnlineTileSourceBase(
    "ESRI World Imagery", 0, 19, 256, "",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        getBaseUrl() + MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarteScreen(
    repository: MartelageRepository,
    contexteId: String,
    gpkgRepository: GpkgRepository,
    onRetour: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contexte by produceState<Contexte?>(initialValue = null, contexteId) {
        value = repository.contexte(contexteId)
    }
    val journal by repository.journal(contexteId).collectAsStateWithLifecycle(emptyList())
    var fond by remember { mutableStateOf(Fond.OSM) }
    var centre by remember { mutableStateOf(false) }
    var chargement by remember { mutableStateOf(false) }
    var legendeOuverte by remember { mutableStateOf(false) }
    // Résolution au sol (mètres/pixel) du centre de la carte, recalculée à chaque scroll/zoom :
    // sert à dessiner l'échelle (barre Compose en bas-centre).
    var metresParPixel by remember { mutableStateOf(0.0) }

    // Le GPKG est rattaché au contexte (modifiable via l'import depuis la carte).
    var cheminGpkg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(contexte) { cheminGpkg = contexte?.cheminGpkg }
    var parcelleCentre by remember { mutableStateOf<String?>(null) }
    val parcelles by produceState(initialValue = emptyList<ParcelleGpkg>(), cheminGpkg) {
        value = cheminGpkg?.let { withContext(Dispatchers.IO) { gpkgRepository.parcellesDetail(it) } } ?: emptyList()
    }
    // Desserte : routes, pistes et chemins. Purement informative — elle sert à trouver l'accès
    // au chantier et le point de dépôt, elle n'entre dans aucun calcul.
    val dessertes by produceState(initialValue = emptyList<DesserteGpkg>(), cheminGpkg) {
        value = cheminGpkg?.let { withContext(Dispatchers.IO) { gpkgRepository.dessertes(it) } } ?: emptyList()
    }
    // Houppiers : la même couche que celle qui estime la hauteur des tiges, ici seulement
    // regardée. Lue quel que soit le réglage MNH — voir une couronne n'engage rien.
    val houppiers by produceState(initialValue = emptyList<Houppier>(), cheminGpkg) {
        value = cheminGpkg?.let { withContext(Dispatchers.IO) { gpkgRepository.houppiers(it) } } ?: emptyList()
    }
    // Couches affichées. Les houppiers sont décochés au départ : ils se comptent par milliers là
    // où les parcelles se comptent sur les doigts, et masqueraient le reste sous un tapis vert.
    var voirParcelles by rememberSaveable { mutableStateOf(true) }
    var voirDesserte by rememberSaveable { mutableStateOf(true) }
    var voirHouppiers by rememberSaveable { mutableStateOf(false) }
    var menuCouches by remember { mutableStateOf(false) }
    val orthoSource by produceState<OrthoSource?>(initialValue = null, cheminGpkg) {
        value = null
        val chemin = cheminGpkg
        if (chemin != null) {
            chargement = true
            val src = withContext(Dispatchers.IO) { gpkgRepository.ouvrirOrtho(chemin) }
            value = src
            chargement = false
            awaitDispose { src?.fermer() }
        } else {
            awaitDispose { }
        }
    }
    val importGpkgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                chargement = true
                val chemin = withContext(Dispatchers.IO) { gpkgRepository.importer(uri) }
                repository.enregistrerCheminGpkg(contexteId, chemin)
                if (chemin == null) chargement = false
                cheminGpkg = chemin
            }
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            setUseDataConnection(true)
            // Masque les boutons +/- intégrés d'osmdroid (on a nos propres FAB Material).
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setTileSource(TileSourceFactory.MAPNIK)
            minZoomLevel = 4.0
            maxZoomLevel = ZOOM_MAX // au-delà, plus de tuiles → fond blanc
            controller.setZoom(6.0)
            controller.setCenter(GeoPoint(46.6, 2.5)) // France
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    // === Indicateur « ma position » : point bleu + cercle de précision + cône de direction ===
    val densite = context.resources.displayMetrics.density
    val positionOverlay = remember { PositionOverlay(densite) }
    // Résolution au sol du centre de la carte (m/pixel) pour l'échelle Compose (bas-centre).
    fun majEchelle() {
        metresParPixel = resolutionAuSol(mapView.mapCenter.latitude, mapView.zoomLevelDouble)
    }

    // Permission de localisation (le GNSS interne alimente le point quand le RTK n'est pas actif).
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Source de position : fix RTK du service s'il tourne, sinon GNSS interne du téléphone.
    val fixRtk by ServiceGnssRtk.fixCourant.collectAsStateWithLifecycle()
    val sourceInterne = remember { SourcePositionInterne(context) }
    val fixInterne by remember { sourceInterne.fixs() }.collectAsStateWithLifecycle(initialValue = null)
    val fix = fixRtk ?: fixInterne

    // Boussole (rotation vector) : oriente le cône à l'arrêt, avec un léger lissage anti-tremblement.
    var azimutBoussole by remember { mutableStateOf<Float?>(null) }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val capteur = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val matrice = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrice, event.values)
                SensorManager.getOrientation(matrice, orientation)
                val deg = ((Math.toDegrees(orientation[0].toDouble()).toFloat()) + 360f) % 360f
                azimutBoussole = azimutBoussole?.let { lisserAngle(it, deg, 0.15f) } ?: deg
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (capteur != null) sm.registerListener(listener, capteur, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm?.unregisterListener(listener) }
    }

    // Cap effectif : GNSS en mouvement (cap fiable), boussole à l'arrêt (comme les apps de navigation).
    val capEffectif: Float? = run {
        val vitesse = fix?.vitesseMs ?: 0.0
        val capGnss = fix?.capDeg
        if (vitesse >= SEUIL_VITESSE_MS && capGnss != null) capGnss.toFloat() else azimutBoussole
    }

    // Met à jour la surcouche et redessine quand la position ou le cap changent.
    LaunchedEffect(fix, capEffectif) {
        val pos = fix?.position
        positionOverlay.maj(
            point = pos?.let { GeoPoint(it.latitude, it.longitude) },
            precisionM = fix?.precisionHorizontaleM ?: 0.0,
            capDeg = capEffectif,
        )
        mapView.invalidate()
    }

    // Bascule de fond : un fournisseur NEUF à chaque changement (osmdroid détache l'ancien).
    LaunchedEffect(fond, orthoSource) {
        val provider = when (fond) {
            Fond.OSM -> MapTileProviderBasic(context.applicationContext, TileSourceFactory.MAPNIK)
            Fond.SATELLITE -> MapTileProviderBasic(context.applicationContext, SOURCE_SATELLITE)
            Fond.ORTHO -> orthoSource?.let { src ->
                // Tuiles natives jusqu'à src.zoomMax ; au-delà, l'approximateur les agrandit (overzoom).
                val gpkgModule = GpkgTileModule(context, src)
                val approximateur = MapTileApproximater().apply { addProvider(gpkgModule) }
                MapTileProviderArray(
                    XYTileSource("ortho", src.zoomMin, src.zoomMax, 256, ".png", emptyArray()),
                    null,
                    arrayOf(gpkgModule, approximateur),
                )
            } ?: MapTileProviderBasic(context.applicationContext, TileSourceFactory.MAPNIK)
        }
        mapView.tileProvider = provider
        // En ortho : overzoom autorisé au-delà de la résolution native (≈20 cm) jusqu'à +5 niveaux.
        mapView.maxZoomLevel = if (fond == Fond.ORTHO) ((orthoSource?.zoomMax ?: 19) + 5).toDouble() else ZOOM_MAX
        mapView.invalidate()
    }

    val ctx = contexte
    // Surcouches reconstruites quand contexte, journal, parcelles ou desserte changent.
    LaunchedEffect(ctx, journal, parcelles, dessertes, houppiers, voirParcelles, voirDesserte, voirHouppiers) {
        if (ctx == null) return@LaunchedEffect
        val couleurs = ctx.essences.associate { it.nom to it.couleurFondArgb }
        mapView.overlays.clear()
        // Parcelles (dessous), colorées par propriétaire, avec étiquette au centre.
        val proprios = parcelles.mapNotNull { it.proprietaire }.distinct()
        fun couleurFonciere(p: String?): Int =
            if (p == null) 0xFF374742.toInt()
            else PALETTE_FONCIER[(proprios.indexOf(p).coerceAtLeast(0)) % PALETTE_FONCIER.size]
        // Tiges par parcelle, en une passe : le rattachement figé dans la tige fait foi, sinon
        // on retombe sur le point-dans-polygone (tiges d'avant l'import du GeoPackage).
        val tigesParParcelle = mutableMapOf<Long, Int>()
        journal.filter { it.action == ActionTige.PLUS && it.position != null }.forEach { t ->
            val p = t.position ?: return@forEach
            val pcl = parcelles.firstOrNull { it.label == t.parcelle }
                ?: parcelles.firstOrNull { AttributionSpatiale.contient(it.anneaux, p) }
            if (pcl != null) tigesParParcelle[pcl.id] = (tigesParParcelle[pcl.id] ?: 0) + t.quantite
        }
        (if (voirParcelles) parcelles else emptyList()).forEach { pcl ->
            val c = couleurFonciere(pcl.proprietaire)
            // Sans titre ni description, osmdroid ouvre quand même sa bulle — vide. Ce qu'on sait
            // de la parcelle vient du GeoPackage : autant le montrer là où on la touche.
            val titre = pcl.parcelleNom?.let { context.getString(R.string.carte_parcelle_titre, it) } ?: pcl.label
            val foncier = listOfNotNull(
                pcl.proprietaires.takeIf { it.isNotEmpty() }?.joinToString(" & ") ?: pcl.proprietaire,
                pcl.foret,
                pcl.commune,
            ).joinToString(" · ")
            val chiffres = context.getString(
                R.string.carte_parcelle_chiffres,
                String.format(Locale.getDefault(), "%.2f", pcl.surfaceHa),
                tigesParParcelle[pcl.id] ?: 0,
            )
            val details = listOf(foncier, chiffres).filter { it.isNotBlank() }.joinToString("\n")
            pcl.anneaux.forEach { anneau ->
                if (anneau.size >= 2) {
                    mapView.overlays.add(
                        Polygon(mapView).apply {
                            points = anneau.map { GeoPoint(it.latitude, it.longitude) }
                            fillPaint.color = (0x33 shl 24) or (c and 0xFFFFFF)
                            outlinePaint.color = c
                            outlinePaint.strokeWidth = 4f
                            title = titre
                            snippet = details
                        },
                    )
                }
            }
            val ext = pcl.anneaux.firstOrNull()
            if (!ext.isNullOrEmpty()) {
                val lat = ext.sumOf { it.latitude } / ext.size
                val lon = ext.sumOf { it.longitude } / ext.size
                mapView.overlays.add(
                    Marker(mapView).apply {
                        position = GeoPoint(lat, lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setTextIcon(pcl.parcelleNom ?: pcl.id.toString())
                        title = titre
                        snippet = details
                        setOnMarkerClickListener { m, _ -> m.showInfoWindow(); true }
                    },
                )
            }
        }
        // Houppiers (juste au-dessus des parcelles) : contour seul, pour qu'on lise ce qu'il y a
        // dessous. La bulle donne la hauteur d'apex, celle qui pré-remplit H à la saisie.
        if (voirHouppiers) {
            val titreHouppier = context.getString(R.string.carte_houppier_titre)
            houppiers.forEach { h ->
                val hauteur = context.getString(
                    R.string.carte_houppier_hauteur,
                    String.format(Locale.getDefault(), "%.1f", h.hauteurM),
                )
                h.anneaux.forEach { anneau ->
                    if (anneau.size >= 2) {
                        mapView.overlays.add(
                            Polygon(mapView).apply {
                                points = anneau.map { GeoPoint(it.latitude, it.longitude) }
                                fillPaint.color = 0x00000000
                                outlinePaint.color = COULEUR_HOUPPIER
                                outlinePaint.strokeWidth = 2f
                                title = titreHouppier
                                snippet = hauteur
                            },
                        )
                    }
                }
            }
        }
        // Desserte (au-dessus des parcelles, sous les tiges) : un tracé continu par tronçon.
        (if (voirDesserte) dessertes else emptyList()).forEach { d ->
            d.lignes.forEach { ligne ->
                if (ligne.size >= 2) {
                    mapView.overlays.add(
                        Polyline(mapView).apply {
                            setPoints(ligne.map { GeoPoint(it.latitude, it.longitude) })
                            outlinePaint.color = COULEUR_DESSERTE
                            outlinePaint.strokeWidth = 6f
                            d.label?.let { title = it }
                            d.type?.let { snippet = it }
                            setOnClickListener { _, _, _ -> false }
                        },
                    )
                }
            }
        }
        // Tiges (dessus) : titre = essence/classe, sous-titre = parcelle (rattachement spatial).
        val points = mutableListOf<GeoPoint>()
        journal.filter { it.action == ActionTige.PLUS && it.position != null }.forEach { t ->
            val pos = t.position ?: return@forEach
            val gp = GeoPoint(pos.latitude, pos.longitude)
            points.add(gp)
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = gp
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = marqueur(context, couleurs[t.essence] ?: 0xFF888888.toInt(), tailleMarqueur(ctx.axe.min, ctx.axe.max, t.classe))
                    title = "${t.essence} ${t.classe}"
                    snippet = t.parcelle
                        ?: parcelles.firstOrNull { AttributionSpatiale.contient(it.anneaux, pos) }?.label
                        ?: context.getString(R.string.carte_hors_parcelle)
                    val hq = buildList {
                        t.hauteurTexte?.takeIf { it.isNotBlank() }?.let { add(context.getString(R.string.carte_hauteur_prefix, it)) }
                        t.qualiteArbre?.takeIf { it.isNotBlank() }?.let { add(context.getString(R.string.carte_qualite_prefix, it)) }
                    }
                    if (hq.isNotEmpty()) subDescription = hq.joinToString(" · ")
                },
            )
        }
        // Surcouche persistante (ma position), ré-ajoutée après le clear, au-dessus.
        mapView.overlays.add(positionOverlay)
        val cible = points.ifEmpty { parcelles.flatMap { it.anneaux }.flatten().map { GeoPoint(it.latitude, it.longitude) } }
        if (!centre && cible.isNotEmpty()) {
            recadrerSur(mapView, cible)
            centre = true
        }
        mapView.invalidate()
    }

    fun recadrer() {
        val tiges = journal.filter { it.action == ActionTige.PLUS }
            .mapNotNull { it.position?.let { p -> GeoPoint(p.latitude, p.longitude) } }
        val cible = tiges.ifEmpty { parcelles.flatMap { it.anneaux }.flatten().map { GeoPoint(it.latitude, it.longitude) } }
        recadrerSur(mapView, cible)
    }

    fun majParcelleCentre() {
        if (parcelles.isEmpty()) {
            parcelleCentre = null
            return
        }
        val c = mapView.mapCenter
        val p = Position(c.latitude, c.longitude)
        parcelleCentre = parcelles.firstOrNull { AttributionSpatiale.contient(it.anneaux, p) }?.label ?: context.getString(R.string.carte_hors_parcelle)
    }

    DisposableEffect(parcelles) {
        majParcelleCentre()
        majEchelle()
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean { majParcelleCentre(); majEchelle(); return false }
            override fun onZoom(event: ZoomEvent?): Boolean { majParcelleCentre(); majEchelle(); return false }
        }
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener) }
    }

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(onClick = { mapView.controller.zoomIn() }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.carte_zoom_avant))
                }
                SmallFloatingActionButton(onClick = { mapView.controller.zoomOut() }) {
                    Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.carte_zoom_arriere))
                }
                SmallFloatingActionButton(onClick = { recadrer() }) {
                    Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.carte_recentrer))
                }
            }
        },
        topBar = {
            BandeauCompact(
                titre = contexte?.nom ?: "Carte",
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.carte_retour))
                    }
                },
                actions = {
                    // Couches du GeoPackage : chacune se coche, et celles que le fichier ne
                    // contient pas restent grisées — mieux vaut voir qu'il n'y a rien à afficher
                    // que de cocher une case sans effet.
                    Box {
                        IconButton(onClick = { menuCouches = true }) {
                            Icon(
                                Icons.Filled.Layers,
                                contentDescription = stringResource(R.string.carte_couches),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        DropdownMenu(expanded = menuCouches, onDismissRequest = { menuCouches = false }) {
                            LigneCouche(
                                libelle = stringResource(R.string.carte_couche_parcelles, parcelles.size),
                                coche = voirParcelles,
                                disponible = parcelles.isNotEmpty(),
                            ) { voirParcelles = it }
                            LigneCouche(
                                libelle = stringResource(R.string.carte_couche_houppiers, houppiers.size),
                                coche = voirHouppiers,
                                disponible = houppiers.isNotEmpty(),
                            ) { voirHouppiers = it }
                            LigneCouche(
                                libelle = stringResource(R.string.carte_couche_desserte, dessertes.size),
                                coche = voirDesserte,
                                disponible = dessertes.isNotEmpty(),
                            ) { voirDesserte = it }
                        }
                    }
                    TextButton(onClick = { importGpkgLauncher.launch(arrayOf("*/*")) }) {
                        Text(stringResource(R.string.carte_charge_gpkg), color = MaterialTheme.colorScheme.onPrimary)
                    }
                    TextButton(onClick = {
                        fond = when (fond) {
                            Fond.OSM -> Fond.SATELLITE
                            Fond.SATELLITE -> if (orthoSource != null) Fond.ORTHO else Fond.OSM
                            Fond.ORTHO -> Fond.OSM
                        }
                    }) {
                        Text(stringResource(R.string.carte_fond_label, fond.libelle), color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
            )
        },
    ) { padding ->
        if (ctx == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            parcelleCentre?.let { libelle ->
                Card(
                    modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    ),
                ) {
                    Text(
                        "◎ $libelle",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            LegendeEssences(
                essences = ctx.essences,
                ouverte = legendeOuverte,
                onToggle = { legendeOuverte = !legendeOuverte },
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            )
            EchelleCarte(
                metresParPixel = metresParPixel,
                densite = densite,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            )
            if (chargement) {
                IndicateurImport(Modifier.align(Alignment.Center))
            }
        }
    }
}

/**
 * Échelle cartographique (barre + distance) en bas-centre, **fond transparent**. Choisit une
 * distance « ronde » (1/2/5 × 10ⁿ) proche d'une largeur cible, et dessine la barre à la largeur
 * correspondante au zoom courant. Masquée tant que la résolution n'est pas connue.
 */
@Composable
private fun EchelleCarte(metresParPixel: Double, densite: Float, modifier: Modifier = Modifier) {
    if (metresParPixel <= 0.0) return
    val largeurCiblePx = 90f * densite
    val metresJolis = distanceRonde(metresParPixel * largeurCiblePx)
    if (metresJolis <= 0.0) return
    val largeurDp = ((metresJolis / metresParPixel) / densite).toFloat()
    val libelle = if (metresJolis >= 1000) "${(metresJolis / 1000).let { if (it == it.toInt().toDouble()) it.toInt().toString() else String.format(Locale.ROOT, "%.1f", it) }} km"
    else "${metresJolis.toInt()} m"
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(libelle, style = MaterialTheme.typography.labelSmall)
        Box(
            Modifier.padding(top = 2.dp).width(largeurDp.dp).height(3.dp)
                .background(MaterialTheme.colorScheme.onSurface),
        )
    }
}

/** Plus grande distance « ronde » (1, 2 ou 5 × 10ⁿ) inférieure ou égale à [metres]. */
private fun distanceRonde(metres: Double): Double {
    if (metres <= 0.0) return 0.0
    val exposant = floor(log10(metres))
    val base = 10.0.pow(exposant)
    val facteur = metres / base
    val joli = when {
        facteur >= 5.0 -> 5.0
        facteur >= 2.0 -> 2.0
        else -> 1.0
    }
    return joli * base
}

/** Résolution au sol (m/pixel) en projection Web Mercator, pour [latitude] (°) et [zoom] (osmdroid). */
private fun resolutionAuSol(latitude: Double, zoom: Double): Double =
    156543.03392 * cos(Math.toRadians(latitude)) / 2.0.pow(zoom)

@Composable
private fun LegendeEssences(
    essences: List<EssenceColonne>,
    ouverte: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (essences.isEmpty()) return
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ),
    ) {
        Column(
            Modifier.padding(8.dp).width(IntrinsicSize.Max).widthIn(max = 180.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.carte_essences),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (ouverte) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (ouverte) stringResource(R.string.carte_replier_legende) else stringResource(R.string.carte_deplier_legende),
                    modifier = Modifier.size(18.dp),
                )
            }
            if (ouverte) {
                essences.forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(12.dp).background(Color(e.couleurFondArgb)))
                        Text(e.nom, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Roue dentée animée affichée pendant l'import / la préparation du GPKG. */
@Composable
private fun IndicateurImport(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "gpkg")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp).rotate(angle),
            )
            Text(stringResource(R.string.carte_import_gpkg), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Lissage exponentiel d'un cap (°), en empruntant le plus court arc (gère le passage 359°→0°). */
private fun lisserAngle(precedent: Float, nouveau: Float, alpha: Float): Float {
    val delta = ((nouveau - precedent + 540f) % 360f) - 180f
    return ((precedent + alpha * delta) % 360f + 360f) % 360f
}

private fun recadrerSur(map: MapView, points: List<GeoPoint>) {
    when {
        points.size == 1 -> {
            map.controller.setZoom(17.0)
            map.controller.animateTo(points.first())
        }
        points.size > 1 -> map.zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true, 100)
    }
}

/** Diamètre (px) de la pastille, proportionnel à la classe dans l'étendue de l'axe. */
private fun tailleMarqueur(min: Int, max: Int, classe: Int): Int {
    val etendue = (max - min).coerceAtLeast(1)
    val fraction = ((classe - min).toFloat() / etendue).coerceIn(0f, 1f)
    return (24 + 40 * fraction).toInt()
}

/** Pastille circulaire colorée (couleur de l'essence) servant d'icône de marqueur. */
private fun marqueur(context: Context, couleur: Int, taille: Int): Drawable {
    val bmp = Bitmap.createBitmap(taille, taille, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val rayon = taille / 2f - 3f
    canvas.drawCircle(
        taille / 2f, taille / 2f, rayon,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = couleur; style = Paint.Style.FILL },
    )
    canvas.drawCircle(
        taille / 2f, taille / 2f, rayon,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        },
    )
    return BitmapDrawable(context.resources, bmp)
}
