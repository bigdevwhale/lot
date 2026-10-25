package app.lot.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import app.lot.ui.theme.C
import app.lot.ui.theme.Serif
import kotlin.random.Random

/** The four lines burnt into the wooden tag. */
class TagText(val year: String, val place: String, val family: String, val sex: String)

/**
 * The stage of the draw: a hessian sack and the wooden tag rising out of it, in 250×330 units.
 * [shake] tilts the sack in degrees; [rise] lifts the tag from inside the mouth (0) to the top (1); no tag when [tag] is null.
 */
object Sack {
    const val W = 250f
    const val H = 330f
    private const val TOP = 130f

    fun DrawScope.stage(shake: Float, rise: Float, tag: TagText?, seed: Long, tm: TextMeasurer) {
        val k = minOf(size.width / W, size.height / H)
        translate((size.width - W * k) / 2, (size.height - H * k) / 2) {
            scale(k, k, Offset.Zero) {
                translate(0f, TOP) { rotate(shake, Offset(125f, 180f)) { sack() } }
                if (tag != null && rise > 0f) {
                    val e = rise.coerceIn(0f, 1f)
                    val cy = 200f + (75f - 200f) * e
                    val s = .5f + .5f * e
                    val rot = -8f + 5f * e
                    // Inside the sack until it clears the mouth.
                    clipRect(0f, -200f, W, TOP + 78f) {
                        translate(125f - 110f, cy - 60f) {
                            scale(s, s, Offset(110f, 60f)) { rotate(rot, Offset(110f, 60f)) { tag(tag, seed, tm) } }
                        }
                    }
                }
            }
        }
    }

    private val body = Path().apply {
        moveTo(60f, 70f)
        cubicTo(30f, 110f, 30f, 170f, 70f, 188f)
        cubicTo(100f, 198f, 150f, 198f, 180f, 188f)
        cubicTo(220f, 170f, 220f, 110f, 190f, 70f)
        close()
    }
    private val mouth = Path().apply {
        moveTo(60f, 70f); cubicTo(90f, 58f, 160f, 58f, 190f, 70f); cubicTo(170f, 80f, 80f, 80f, 60f, 70f); close()
    }

    fun DrawScope.sack() {
        drawOval(C.ink.copy(alpha = .14f), Offset(30f, 184f), Size(190f, 16f))
        drawPath(body, C.sack)
        clipPath(body) {
            var x = 30f
            while (x < 220f) { drawLine(C.sackLo, Offset(x, 55f), Offset(x + 4f, 200f), 1.2f); x += 6f }
            var y = 60f
            while (y < 200f) { drawLine(C.sackLo.copy(alpha = .7f), Offset(25f, y), Offset(225f, y + 2f), 1f); y += 6f }
            drawRect(Brush.horizontalGradient(listOf(C.ink.copy(alpha = .18f), C.ink.copy(alpha = 0f), C.ink.copy(alpha = 0f), C.ink.copy(alpha = .22f)), 30f, 220f), Offset(25f, 55f), Size(200f, 150f))
        }
        drawPath(body, C.ink2, style = Stroke(2.5f))
        drawPath(mouth, C.sackDark)
        drawPath(Path().apply { moveTo(70f, 76f); cubicTo(100f, 86f, 150f, 86f, 180f, 76f) }, C.string, style = Stroke(5f, cap = StrokeCap.Round))
        val tie = Stroke(3f, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(128f, 84f); cubicTo(140f, 100f, 150f, 104f, 166f, 98f) }, C.string, style = tie)
        drawPath(Path().apply { moveTo(128f, 84f); cubicTo(132f, 104f, 118f, 116f, 104f, 116f) }, C.string, style = tie)
        drawCircle(C.string, 4.5f, Offset(128f, 84f))
        val fold = Stroke(2f, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(85f, 120f); cubicTo(95f, 150f, 90f, 170f, 100f, 182f) }, C.sackLo, style = fold)
        drawPath(Path().apply { moveTo(160f, 115f); cubicTo(150f, 140f, 162f, 165f, 155f, 184f) }, C.sackLo, style = fold)
    }

    private val tagShape = Path().apply {
        moveTo(30f, 4f); lineTo(204f, 4f); quadraticTo(216f, 4f, 216f, 16f); lineTo(216f, 104f)
        quadraticTo(216f, 116f, 204f, 116f); lineTo(30f, 116f); lineTo(4f, 60f); close()
    }

    /** The tag in its own 220×120 units: wood grain from [seed], a hole with a string, four burnt lines. */
    fun DrawScope.tag(t: TagText, seed: Long, tm: TextMeasurer) {
        drawPath(Path().apply { moveTo(22f, 60f); cubicTo(10f, 30f, -10f, 30f, -24f, 6f) }, C.string, style = Stroke(3f, cap = StrokeCap.Round))
        drawPath(tagShape, Brush.verticalGradient(listOf(C.woodHi, C.woodLo), 4f, 116f))
        clipPath(tagShape) {
            val r = Random(seed)
            for (i in 0 until 10) {
                val y = 10f + i * 11f + r.nextFloat() * 4f
                drawPath(Path().apply { moveTo(0f, y); cubicTo(60f, y + r.nextFloat() * 6 - 3, 140f, y + r.nextFloat() * 6 - 3, 220f, y) },
                    C.grain.copy(alpha = .35f), style = Stroke(.6f + r.nextFloat()))
            }
            val kx = 60f + r.nextFloat() * 120f
            drawOval(C.grain.copy(alpha = .3f), Offset(kx, 88f), Size(14f, 6f), style = Stroke(1f))
        }
        drawPath(tagShape, C.ink2, style = Stroke(2f))
        drawCircle(C.sackDark, 6f, Offset(22f, 60f))
        burn(tm, t.year, 32f, 21f, bold = true)
        burn(tm, t.place, 56f, 14f)
        burn(tm, t.family, 77f, 12.5f, italic = true)
        burn(tm, t.sex, 98f, 12.5f)
    }

    private fun DrawScope.burn(tm: TextMeasurer, s: String, baseline: Float, units: Float, bold: Boolean = false, italic: Boolean = false) {
        val style = TextStyle(
            fontFamily = Serif, fontSize = units.toSp(), color = C.burn, textAlign = TextAlign.Center,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        )
        var layout = tm.measure(s, style)
        // Long places shrink to fit the tag.
        if (layout.size.width > 176f) layout = tm.measure(s, style.copy(fontSize = (units * 176f / layout.size.width).toSp()))
        val x = 122f - layout.size.width / 2
        drawText(layout, topLeft = Offset(x, baseline - layout.firstBaseline))
    }

}
