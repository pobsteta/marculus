package io.github.pobsteta.marculus.ui.carte

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.cos

/**
 * Surcouche « ma position » à la manière des apps de navigation : un **point bleu** à la position
 * courante, un **cercle de précision** translucide dont le rayon vaut la précision horizontale
 * (mètres) convertie en pixels au zoom courant (donc « à l'échelle »), et un **cône de direction**
 * orienté selon le cap fourni (boussole à l'arrêt, cap GNSS en mouvement — décidé par l'appelant).
 *
 * Les champs sont mis à jour via [maj] depuis l'UI, qui invalide ensuite la carte.
 */
class PositionOverlay(private val densite: Float) : Overlay() {

    private var point: GeoPoint? = null
    private var precisionM: Double = 0.0
    private var capDeg: Float? = null

    /** Met à jour la position, la précision (m) et le cap (° vrais) ; valeurs nulles ⇒ rien dessiné. */
    fun maj(point: GeoPoint?, precisionM: Double, capDeg: Float?) {
        this.point = point
        this.precisionM = precisionM
        this.capDeg = capDeg
    }

    private val peintreCercle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = (0x22 shl 24) or (COULEUR and 0xFFFFFF)
    }
    private val peintreBordCercle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * densite
        color = (0x55 shl 24) or (COULEUR and 0xFFFFFF)
    }
    private val peintreCone = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val peintreAnneau = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }
    private val peintrePoint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = COULEUR
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val p = point ?: return
        val ecran = mapView.projection.toPixels(p, null)
        val cx = ecran.x.toFloat()
        val cy = ecran.y.toFloat()

        // Cercle de précision : rayon (m) → pixels via la résolution au sol du zoom courant.
        val mppx = metresParPixel(p.latitude, mapView.zoomLevelDouble)
        if (mppx > 0) {
            val rayonPrecision = (precisionM / mppx).toFloat()
            if (rayonPrecision > RAYON_POINT * densite) {
                canvas.drawCircle(cx, cy, rayonPrecision, peintreCercle)
                canvas.drawCircle(cx, cy, rayonPrecision, peintreBordCercle)
            }
        }

        // Cône de direction (dégradé bleu → transparent), pointé vers le cap.
        capDeg?.let { cap ->
            val rayonCone = RAYON_CONE * densite
            peintreCone.shader = RadialGradient(
                cx, cy, rayonCone,
                intArrayOf((0x66 shl 24) or (COULEUR and 0xFFFFFF), COULEUR and 0xFFFFFF),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            // Canvas : 0° = est, sens horaire ; cap 0° = nord = -90°. Cône centré sur le cap.
            val angleDebut = cap - 90f - DEMI_ANGLE
            canvas.drawArc(
                cx - rayonCone, cy - rayonCone, cx + rayonCone, cy + rayonCone,
                angleDebut, DEMI_ANGLE * 2f, true, peintreCone,
            )
        }

        // Point : anneau blanc puis pastille bleue.
        val rayonPoint = RAYON_POINT * densite
        canvas.drawCircle(cx, cy, rayonPoint + 2.5f * densite, peintreAnneau)
        canvas.drawCircle(cx, cy, rayonPoint, peintrePoint)
    }

    /** Résolution au sol (m/pixel) d'une tuile Web-Mercator 256 px au zoom et à la latitude donnés. */
    private fun metresParPixel(latitude: Double, zoom: Double): Double =
        CIRCONFERENCE_TERRE * cos(Math.toRadians(latitude)) / (256.0 * Math.pow(2.0, zoom))

    companion object {
        private const val COULEUR = 0xFF1976D2.toInt() // bleu « position »
        private const val RAYON_POINT = 7f // dp
        private const val RAYON_CONE = 58f // dp
        private const val DEMI_ANGLE = 26f // demi-ouverture du cône (°)
        private const val CIRCONFERENCE_TERRE = 40075016.686 // m (équateur)
    }
}
