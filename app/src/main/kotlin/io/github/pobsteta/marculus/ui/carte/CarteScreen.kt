package io.github.pobsteta.marculus.ui.carte

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.Contexte
import fr.marculus.core.model.EssenceColonne
import fr.marculus.core.model.Position
import io.github.pobsteta.marculus.data.GpkgRepository
import io.github.pobsteta.marculus.data.MartelageRepository
import io.github.pobsteta.marculus.data.OrthoSource
import io.github.pobsteta.marculus.data.ReferentielsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
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
    referentielsRepository: ReferentielsRepository,
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

    val cheminGpkg by referentielsRepository.cheminGpkg.collectAsStateWithLifecycle(null)
    val parcelles by produceState(initialValue = emptyList<List<Position>>(), cheminGpkg) {
        value = cheminGpkg?.let { withContext(Dispatchers.IO) { gpkgRepository.parcelles(it) } } ?: emptyList()
    }
    val orthoSource by produceState<OrthoSource?>(initialValue = null, cheminGpkg) {
        val src = cheminGpkg?.let { withContext(Dispatchers.IO) { gpkgRepository.ouvrirOrtho(it) } }
        value = src
        awaitDispose { src?.fermer() }
    }
    val importGpkgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val chemin = withContext(Dispatchers.IO) { gpkgRepository.importer(uri) }
                referentielsRepository.enregistrerCheminGpkg(chemin)
            }
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            setUseDataConnection(true)
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

    // Fournisseur en ligne d'origine (OSM/Satellite) ; fournisseur ortho hors-ligne (GPKG).
    val providerBase = remember { mapView.tileProvider }
    val providerOrtho = remember(orthoSource) {
        orthoSource?.let { src ->
            MapTileProviderArray(
                XYTileSource("ortho", src.zoomMin, src.zoomMax, 256, ".png", emptyArray()),
                null,
                arrayOf(GpkgTileModule(context, src)),
            )
        }
    }
    // Bascule de fond : OSM / Satellite (en ligne) ou Ortho (GPKG hors-ligne, zoom jusqu'à sa résolution).
    LaunchedEffect(fond, providerOrtho) {
        when (fond) {
            Fond.OSM -> {
                mapView.tileProvider = providerBase
                mapView.setTileSource(TileSourceFactory.MAPNIK)
                mapView.maxZoomLevel = ZOOM_MAX
            }
            Fond.SATELLITE -> {
                mapView.tileProvider = providerBase
                mapView.setTileSource(SOURCE_SATELLITE)
                mapView.maxZoomLevel = ZOOM_MAX
            }
            Fond.ORTHO -> {
                val po = providerOrtho
                if (po != null) {
                    mapView.tileProvider = po
                    mapView.setTileSource(po.tileSource)
                    mapView.maxZoomLevel = orthoSource?.zoomMax?.toDouble() ?: ZOOM_MAX
                }
            }
        }
        mapView.invalidate()
    }

    val ctx = contexte
    // Surcouches reconstruites quand contexte, journal ou parcelles changent.
    LaunchedEffect(ctx, journal, parcelles) {
        if (ctx == null) return@LaunchedEffect
        val couleurs = ctx.essences.associate { it.nom to it.couleurFondArgb }
        mapView.overlays.clear()
        // Parcelles (dessous).
        parcelles.forEach { anneau ->
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
        // Tiges (dessus).
        val points = mutableListOf<GeoPoint>()
        journal.filter { it.action == ActionTige.PLUS && it.position != null }.forEach { t ->
            val gp = GeoPoint(t.position!!.latitude, t.position!!.longitude)
            points.add(gp)
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = gp
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = marqueur(context, couleurs[t.essence] ?: 0xFF888888.toInt(), tailleMarqueur(ctx.axe.min, ctx.axe.max, t.classe))
                    title = "${t.essence} ${t.classe}"
                },
            )
        }
        val cible = points.ifEmpty { parcelles.flatten().map { GeoPoint(it.latitude, it.longitude) } }
        if (!centre && cible.isNotEmpty()) {
            recadrerSur(mapView, cible)
            centre = true
        }
        mapView.invalidate()
    }

    fun recadrer() {
        val tiges = journal.filter { it.action == ActionTige.PLUS && it.position != null }
            .map { GeoPoint(it.position!!.latitude, it.position!!.longitude) }
        val cible = tiges.ifEmpty { parcelles.flatten().map { GeoPoint(it.latitude, it.longitude) } }
        recadrerSur(mapView, cible)
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { recadrer() }) { Text("Recentrer") }
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
                        Text("GPKG", color = MaterialTheme.colorScheme.onPrimary)
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
            LegendeEssences(
                essences = ctx.essences,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            )
        }
    }
}

@Composable
private fun LegendeEssences(essences: List<EssenceColonne>, modifier: Modifier = Modifier) {
    if (essences.isEmpty()) return
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ),
    ) {
        Column(Modifier.padding(8.dp).widthIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Essences", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            essences.forEach { e ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(12.dp).background(Color(e.couleurFondArgb)))
                    Text(e.nom, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
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
