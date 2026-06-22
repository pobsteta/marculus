package io.github.pobsteta.marculus.ui.carte

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import io.github.pobsteta.marculus.data.OrthoSource
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex

/** Fournisseur osmdroid hors-ligne : sert les tuiles ortho (déjà reprojetées en Web Mercator) du GPKG. */
class GpkgTileModule(
    private val context: Context,
    private val ortho: OrthoSource,
) : MapTileModuleProviderBase(2, 16) {

    override fun getName(): String = "GPKG Ortho"

    override fun getThreadGroupName(): String = "gpkg-ortho"

    override fun getUsesDataConnection(): Boolean = false

    override fun getMinimumZoomLevel(): Int = ortho.zoomMin

    override fun getMaximumZoomLevel(): Int = ortho.zoomMax

    override fun setTileSource(tileSource: ITileSource?) { /* fixe : tuiles du GPKG */ }

    override fun getTileLoader(): TileLoader = object : TileLoader() {
        override fun loadTile(pMapTileIndex: Long): Drawable? {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            val bytes = ortho.tuile(z, x, y) ?: return null
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            return BitmapDrawable(context.resources, bmp)
        }
    }
}
