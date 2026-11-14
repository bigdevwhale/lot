package app.lot.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.lot.ui.theme.C

/** A pin on the atlas: where someone was born, coloured by era. */
class Pin(val lon: Float, val lat: Float, val color: Color, val infant: Boolean, val jitter: Int, val on: Boolean = false)

/** The painted textures and ornaments of the atlas (assets/art/map); the map is still whole without any of them. */
class MapPaint(val sea: ImageBitmap?, val land: ImageBitmap?, val compass: ImageBitmap?, val ship: ImageBitmap?, val serpent: ImageBitmap?)

/**
 * An old equirectangular world map, 360×180 units. The continents are a few dozen points each, joined by smooth curves
 * that pass through every point, so a birthplace on land stays on land. The paint only fills what the geometry outlines.
 */
object MapArt {
    const val W = 360f
    const val H = 180f

    private val LAND = listOf(
        floatArrayOf(-168f, 66f, -140f, 70f, -95f, 72f, -80f, 63f, -60f, 55f, -66f, 45f, -81f, 30f, -80f, 25f, -97f, 26f, -97f, 18f, -87f, 15f, -80f, 8f, -90f, 14f, -105f, 20f, -117f, 32f, -124f, 40f, -124f, 48f, -135f, 58f, -152f, 60f, -165f, 62f),
        floatArrayOf(-80f, 9f, -62f, 10f, -50f, 0f, -35f, -6f, -40f, -22f, -58f, -38f, -65f, -55f, -73f, -50f, -71f, -20f, -81f, -5f),
        floatArrayOf(-10f, 36f, -9f, 43f, -2f, 44f, -5f, 48f, 2f, 51f, 8f, 54f, 10f, 58f, 5f, 62f, 15f, 69f, 28f, 71f, 42f, 67f, 60f, 69f, 80f, 73f, 105f, 77f, 140f, 72f, 170f, 70f, 180f, 66f, 160f, 60f, 142f, 52f, 135f, 43f, 127f, 38f, 122f, 30f, 120f, 22f, 108f, 18f, 108f, 11f, 104f, 1f, 98f, 8f, 98f, 16f, 92f, 22f, 80f, 15f, 77f, 8f, 72f, 20f, 66f, 25f, 57f, 25f, 56f, 27f, 50f, 30f, 48f, 29f, 52f, 24f, 56f, 26f, 59f, 22f, 52f, 15f, 43f, 12f, 35f, 28f, 34f, 31f, 36f, 36f, 27f, 37f, 26f, 40f, 23f, 36f, 20f, 40f, 14f, 42f, 16f, 38f, 12f, 42f, 8f, 44f, 3f, 43f, -5f, 36f),
        floatArrayOf(-17f, 21f, -10f, 33f, -5f, 36f, 10f, 37f, 20f, 32f, 32f, 31f, 34f, 28f, 43f, 12f, 51f, 12f, 40f, -5f, 40f, -15f, 35f, -25f, 20f, -35f, 15f, -28f, 12f, -15f, 14f, -5f, 9f, 4f, -8f, 5f, -17f, 14f),
        floatArrayOf(114f, -22f, 130f, -12f, 137f, -12f, 142f, -11f, 146f, -19f, 153f, -28f, 150f, -37f, 141f, -38f, 131f, -31f, 115f, -34f),
        floatArrayOf(-55f, 60f, -45f, 60f, -20f, 70f, -20f, 82f, -60f, 82f, -72f, 77f),
        floatArrayOf(-5f, 50f, 1f, 51f, -2f, 57f, -6f, 58f, -5f, 55f),
        floatArrayOf(130f, 31f, 140f, 35f, 142f, 40f, 141f, 45f, 140f, 41f, 135f, 34f),
        floatArrayOf(44f, -25f, 47f, -25f, 50f, -15f, 49f, -12f, 44f, -17f),
        floatArrayOf(95f, 5f, 106f, -6f, 102f, -4f),
        floatArrayOf(109f, 1f, 117f, 7f, 119f, 0f, 116f, -4f, 110f, -3f),
        floatArrayOf(166f, -46f, 174f, -41f, 178f, -38f, 172f, -35f, 168f, -44f),
    )

    /** A closed Catmull-Rom curve through the points: rounded like a hand-drawn coast, and through every point. */
    private val paths = LAND.map { raw ->
        val p = (raw.indices step 2).map { xy(raw[it], raw[it + 1]) }
        val n = p.size
        Path().apply {
            moveTo(p[0].x, p[0].y)
            for (i in 0 until n) {
                val p0 = p[(i - 1 + n) % n]; val p1 = p[i]; val p2 = p[(i + 1) % n]; val p3 = p[(i + 2) % n]
                val c1 = p1 + (p2 - p0) / 6f
                val c2 = p2 - (p3 - p1) / 6f
                cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
            }
            close()
        }
    }

    fun xy(lon: Float, lat: Float) = Offset(lon + 180f, 90f - lat)

    /** Where a pin sits on the map, jitter included, in map units. */
    fun at(p: Pin) = xy(p.lon, p.lat) + Offset(((p.jitter * 37) % 11 - 5) * .8f, ((p.jitter * 53) % 9 - 4) * .8f)

    private fun DrawScope.image(img: ImageBitmap, x: Float, y: Float, w: Float, h: Float = w * img.height / img.width, alpha: Float = 1f) =
        drawImage(img, dstOffset = IntOffset(x.toInt(), y.toInt()), dstSize = IntSize(w.toInt(), h.toInt()), alpha = alpha, filterQuality = FilterQuality.High)

    fun DrawScope.map(pins: List<Pin>, paint: MapPaint? = null) {
        val k = size.width / W
        // The textures are drawn at screen resolution: map units are a few pixels each.
        scale(k, k, Offset.Zero) {
            clipRect(0f, 0f, W, H) {
                // The sea.
                if (paint?.sea != null) image(paint.sea, 0f, 0f, W, H)
                else {
                    drawRect(C.sea)
                    var y = 3f
                    while (y < H) { drawLine(C.line.copy(alpha = .35f), Offset(0f, y), Offset(W, y), .3f); y += 4f }
                }
                var g = 0f
                while (g <= W) { drawLine(C.ink2.copy(alpha = .16f), Offset(g, 0f), Offset(g, H), .3f); g += 30f }
                g = 0f
                while (g <= H) { drawLine(C.ink2.copy(alpha = .16f), Offset(0f, g), Offset(W, g), .3f); g += 30f }
                // The equator and the tropics, as old charts mark them.
                drawLine(C.red.copy(alpha = .35f), Offset(0f, 90f), Offset(W, 90f), .45f)
                for (t in listOf(66.5f, 113.5f)) drawLine(C.red.copy(alpha = .22f), Offset(0f, t), Offset(W, t), .35f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 2f)))

                // Ornaments in the empty oceans, under the coasts.
                paint?.compass?.let { image(it, 22f, 112f, 40f, alpha = .85f) }
                paint?.ship?.let { image(it, 130f, 62f, 24f, alpha = .8f) }
                paint?.serpent?.let { image(it, 238f, 112f, 26f, alpha = .8f) }

                // The shallows: a soft shadow along every coast, darkest at the shore.
                for (p in paths) for ((w, a) in listOf(7f to .05f, 4.5f to .06f, 2.6f to .09f)) drawPath(p, C.ink2.copy(alpha = a), style = Stroke(w, join = StrokeJoin.Round))
                // The land.
                for (p in paths) {
                    if (paint?.land != null) clipPath(p) { image(paint.land, 0f, 0f, W, H) } else drawPath(p, C.land)
                    drawPath(p, C.paperHi.copy(alpha = .5f), style = Stroke(1.6f, join = StrokeJoin.Round))
                    drawPath(p, C.ink2, style = Stroke(.6f, join = StrokeJoin.Round))
                }

                // Old paper darkens towards its edges.
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, C.ink2.copy(alpha = .22f)), Offset(W / 2, H / 2), W * .62f))

                for (p in pins.sortedBy { it.on }) {
                    val c = at(p)
                    drawCircle(C.ink.copy(alpha = .25f), 3.8f, c + Offset(.4f, .7f))
                    if (p.on) drawCircle(C.ink, 5.4f, c, style = Stroke(1.1f))
                    drawCircle(C.page, 3.6f, c)
                    drawCircle(p.color, 2.8f, c)
                    if (p.infant) drawCircle(C.page, .9f, c)
                }
            }
            frame()
        }
    }

    /** A chart's border: two rules with degrees between them, every ten degrees filled in turn. */
    private fun DrawScope.frame() {
        val b = 2.4f
        drawRect(C.page.copy(alpha = .9f), Offset.Zero, Size(W, b))
        drawRect(C.page.copy(alpha = .9f), Offset(0f, H - b), Size(W, b))
        drawRect(C.page.copy(alpha = .9f), Offset.Zero, Size(b, H))
        drawRect(C.page.copy(alpha = .9f), Offset(W - b, 0f), Size(b, H))
        var x = 0f
        var on = true
        while (x < W) {
            if (on) {
                drawLine(C.ink2, Offset(x, b / 2), Offset(minOf(x + 10f, W), b / 2), b * .55f, StrokeCap.Butt)
                drawLine(C.ink2, Offset(x, H - b / 2), Offset(minOf(x + 10f, W), H - b / 2), b * .55f, StrokeCap.Butt)
            }
            x += 10f; on = !on
        }
        var y = 0f
        on = true
        while (y < H) {
            if (on) {
                drawLine(C.ink2, Offset(b / 2, y), Offset(b / 2, minOf(y + 10f, H)), b * .55f, StrokeCap.Butt)
                drawLine(C.ink2, Offset(W - b / 2, y), Offset(W - b / 2, minOf(y + 10f, H)), b * .55f, StrokeCap.Butt)
            }
            y += 10f; on = !on
        }
        drawRect(C.ink2, Offset.Zero, Size(W, H), style = Stroke(.9f))
        drawRect(C.ink2, Offset(b, b), Size(W - 2 * b, H - 2 * b), style = Stroke(.45f))
    }
}
