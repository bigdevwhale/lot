package app.lot.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import app.lot.ui.theme.C

/** A pin on the atlas: where someone was born, coloured by era. */
class Pin(val lon: Float, val lat: Float, val color: Color, val infant: Boolean, val jitter: Int, val on: Boolean = false)

/** An old equirectangular world map, 360×180 units, the continents drawn from a few dozen points each. */
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

    private val paths = LAND.map { pts ->
        Path().apply {
            moveTo(pts[0] + 180f, 90f - pts[1])
            var i = 2
            while (i < pts.size) { lineTo(pts[i] + 180f, 90f - pts[i + 1]); i += 2 }
            close()
        }
    }

    fun xy(lon: Float, lat: Float) = Offset(lon + 180f, 90f - lat)

    /** Where a pin sits on the map, jitter included, in map units. */
    fun at(p: Pin) = xy(p.lon, p.lat) + Offset(((p.jitter * 37) % 11 - 5) * .8f, ((p.jitter * 53) % 9 - 4) * .8f)

    fun DrawScope.map(pins: List<Pin>) {
        val k = size.width / W
        scale(k, k, Offset.Zero) {
            clipRect(0f, 0f, W, H) {
                drawRect(C.sea)
                // Sea hatching, like an engraved chart.
                var y = 3f
                while (y < H) { drawLine(C.line.copy(alpha = .35f), Offset(0f, y), Offset(W, y), .3f); y += 4f }
                var g = 0f
                while (g <= W) { drawLine(C.line, Offset(g, 0f), Offset(g, H), .35f); g += 30f }
                g = 0f
                while (g <= H) { drawLine(C.line, Offset(0f, g), Offset(W, g), .35f); g += 30f }
                for (p in paths) {
                    drawPath(p, C.land)
                    drawPath(p, C.ink2, style = Stroke(.7f, join = StrokeJoin.Round))
                }
                for (p in pins.sortedBy { it.on }) {
                    val c = at(p)
                    if (p.on) drawCircle(C.ink, 5.2f, c, style = Stroke(1f))
                    drawCircle(C.page, 3.6f, c)
                    drawCircle(p.color, 2.8f, c)
                    if (p.infant) drawCircle(C.page, .9f, c)
                }
            }
            drawRect(C.ink2, Offset.Zero, androidx.compose.ui.geometry.Size(W, H), style = Stroke(1f))
        }
    }
}
