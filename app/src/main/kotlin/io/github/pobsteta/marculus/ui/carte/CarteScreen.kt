package io.github.pobsteta.marculus.ui.carte

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.AttributionSpatiale
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.Position
import io.github.pobsteta.marculus.data.GpkgRepository
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.data.OrthoSource
import io.github.pobsteta.marculus.data.ParcelleGpkg
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

private const val ZOOM_MAX = 19.0

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

    // Le GPKG est rattaché au contexte (modifiable via l'import depuis la carte).
    var cheminGpkg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(contexte) { cheminGpkg = contexte?.cheminGpkg }
    var parcelleCentre by remember { mutableStateOf<String?>(null) }
    val parcelles by produceState(initialValue = emptyList<ParcelleGpkg>(), cheminGpkg) {
        value = cheminGpkg?.let { withContext(Dispatchers.IO) { gpkgRepository.parcellesDetail(it) } } ?: emptyList()
    }
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
    // Surcouches reconstruites quand contexte, journal ou parcelles changent.
    LaunchedEffect(ctx, journal, parcelles) {
        if (ctx == null) return@LaunchedEffect
        val couleurs = ctx.essences.associate { it.nom to it.couleurFondArgb }
        mapView.overlays.clear()
        // Parcelles (dessous).
        parcelles.forEach { pcl ->
            pcl.anneaux.forEach { anneau ->
                if (anneau.size >= 2) {
                    mapView.overlays.add(
                        Polygon(mapView).apply {
                            points = anneau.map { GeoPoint(it.latitude, it.longitude) }
                            fillPaint.color = 0x22374742
                            outlinePaint.color = 0xFF374742.toInt()
                            outlinePaint.strokeWidth = 4f
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
                    snippet = parcelles.firstOrNull { AttributionSpatiale.contient(it.anneaux, pos) }?.label
                        ?: "Hors parcelle"
                    val hq = buildList {
                        t.hauteurTexte?.takeIf { it.isNotBlank() }?.let { add("H : $it") }
                        t.qualiteArbre?.takeIf { it.isNotBlank() }?.let { add("Q : $it") }
                    }
                    if (hq.isNotEmpty()) subDescription = hq.joinToString(" · ")
                },
            )
        }
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
        parcelleCentre = parcelles.firstOrNull { AttributionSpatiale.contient(it.anneaux, p) }?.label ?: "Hors parcelle"
    }

    DisposableEffect(parcelles) {
        majParcelleCentre()
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean { majParcelleCentre(); return false }
            override fun onZoom(event: ZoomEvent?): Boolean { majParcelleCentre(); return false }
        }
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener) }
    }

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(onClick = { mapView.controller.zoomIn() }) {
                    Icon(Icons.Filled.Add, contentDescription = "Zoom avant")
                }
                SmallFloatingActionButton(onClick = { mapView.controller.zoomOut() }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Zoom arrière")
                }
                SmallFloatingActionButton(onClick = { recadrer() }) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Recentrer")
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(contexte?.nom ?: "Carte") },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(onClick = { importGpkgLauncher.launch(arrayOf("*/*")) }) {
                        Text("Charge : gpkg", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    TextButton(onClick = {
                        fond = when (fond) {
                            Fond.OSM -> Fond.SATELLITE
                            Fond.SATELLITE -> if (orthoSource != null) Fond.ORTHO else Fond.OSM
                            Fond.ORTHO -> Fond.OSM
                        }
                    }) {
                        Text("Fond : ${fond.libelle}", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
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
            if (chargement) {
                IndicateurImport(Modifier.align(Alignment.Center))
            }
        }
    }
}

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
                    "Essences",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (ouverte) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (ouverte) "Replier la légende" else "Déplier la légende",
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
            Text("Import du GPKG…", style = MaterialTheme.typography.bodyMedium)
        }
    }
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
