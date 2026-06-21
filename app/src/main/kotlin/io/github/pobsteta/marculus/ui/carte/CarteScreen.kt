package io.github.pobsteta.marculus.ui.carte

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.marculus.core.model.ActionTige
import fr.marculus.core.model.Contexte
import io.github.pobsteta.marculus.data.MartelageRepository
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

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
    onRetour: () -> Unit,
) {
    val context = LocalContext.current
    val contexte by produceState<Contexte?>(initialValue = null, contexteId) {
        value = repository.contexte(contexteId)
    }
    val journal by repository.journal(contexteId).collectAsStateWithLifecycle(emptyList())
    var satellite by remember { mutableStateOf(false) }
    var centre by remember { mutableStateOf(false) }
    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    fun recadrer() {
        val points = journal.filter { it.action == ActionTige.PLUS && it.position != null }
            .map { GeoPoint(it.position!!.latitude, it.position!!.longitude) }
        when {
            points.size == 1 -> {
                mapView.controller.setZoom(17.0)
                mapView.controller.animateTo(points.first())
            }
            points.size > 1 -> mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true, 80)
        }
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
                    TextButton(onClick = { satellite = !satellite }) {
                        Text(
                            if (satellite) "OSM" else "Satellite",
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        val ctx = contexte
        if (ctx == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val couleurs = ctx.essences.associate { it.nom to it.couleurFondArgb }
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = {
                mapView.apply {
                    setMultiTouchControls(true)
                    setTileSource(TileSourceFactory.MAPNIK)
                    controller.setZoom(6.0)
                    controller.setCenter(GeoPoint(46.6, 2.5)) // France
                }
            },
            update = { map ->
                map.setTileSource(if (satellite) SOURCE_SATELLITE else TileSourceFactory.MAPNIK)
                map.overlays.clear()
                val points = mutableListOf<GeoPoint>()
                journal.filter { it.action == ActionTige.PLUS && it.position != null }.forEach { t ->
                    val pos = t.position!!
                    val gp = GeoPoint(pos.latitude, pos.longitude)
                    points.add(gp)
                    map.overlays.add(
                        Marker(map).apply {
                            position = gp
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            icon = marqueur(
                                context,
                                couleurs[t.essence] ?: 0xFF888888.toInt(),
                                tailleMarqueur(ctx.axe.min, ctx.axe.max, t.classe),
                            )
                            title = "${t.essence} ${t.classe}"
                        },
                    )
                }
                if (!centre && points.isNotEmpty()) {
                    if (points.size == 1) {
                        map.controller.setZoom(17.0)
                        map.controller.setCenter(points.first())
                    } else {
                        map.zoomToBoundingBox(BoundingBox.fromGeoPoints(points), false, 80)
                    }
                    centre = true
                }
                map.invalidate()
            },
        )
    }
}

/** Diamètre (px) de la pastille, proportionnel à la classe dans l'étendue de l'axe. */
private fun tailleMarqueur(min: Int, max: Int, classe: Int): Int {
    val etendue = (max - min).coerceAtLeast(1)
    val fraction = ((classe - min).toFloat() / etendue).coerceIn(0f, 1f)
    return (24 + 40 * fraction).toInt() // 24 px (petite classe) → 64 px (grande classe)
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
