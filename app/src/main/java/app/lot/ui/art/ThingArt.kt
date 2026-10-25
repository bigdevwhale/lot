package app.lot.ui.art

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import app.lot.domain.Shape
import kotlin.math.PI
import kotlin.math.sin

/** The things in the chest, each drawn in 64×64 units like a small engraving. */
object ThingArt {
    private val edge = Color(0xFF5B3D26)
    private val bone = Color(0xFFE8DCC0)
    private val clay = Color(0xFFC49A68)
    private val copper = Color(0xFFB87333)
    private val brass = Color(0xFFB8A050)
    private val pewter = Color(0xFFA8A29A)
    private val paper = Color(0xFFF2E6C8)
    private val wood = Color(0xFFA87A44)

    fun DrawScope.thing(shape: Shape) {
        val k = minOf(size.width, size.height) / 64f
        translate((size.width - 64 * k) / 2, (size.height - 64 * k) / 2) { scale(k, k, Offset.Zero) { draw(shape) } }
    }

    private fun p(block: Path.() -> Unit) = Path().apply(block)
    private fun DrawScope.line(a: Offset, b: Offset, c: Color, w: Float) = drawLine(c, a, b, w, StrokeCap.Round)
    private val o = Stroke(2f)

    private fun DrawScope.draw(s: Shape) {
        drawOval(Color(0x22000000), Offset(12f, 54f), Size(40f, 6f))
        when (s) {
            Shape.NEEDLE -> { line(Offset(14f, 50f), Offset(48f, 12f), bone, 5f); line(Offset(14f, 50f), Offset(48f, 12f), Color(0xFF8A7458), 1.2f); drawCircle(edge, 2f, Offset(45f, 15f)) }
            Shape.FLINT -> {
                val f = p { moveTo(12f, 44f); lineTo(22f, 14f); lineTo(40f, 10f); lineTo(52f, 30f); lineTo(44f, 50f); close() }
                drawPath(f, Color(0xFF7D7A72)); drawPath(f, Color(0xFF3A3530), style = o)
                drawPath(p { moveTo(22f, 14f); lineTo(30f, 34f); lineTo(44f, 50f); moveTo(30f, 34f); lineTo(52f, 30f) }, Color(0xFF55524C), style = Stroke(1.4f))
            }
            Shape.BEADS -> {
                drawPath(p { moveTo(12f, 18f); cubicTo(20f, 50f, 44f, 50f, 52f, 18f) }, Color(0xFF7A5230), style = Stroke(1.4f))
                val cols = listOf(Color(0xFF3D6A8A), Color(0xFFD8C8A8), Color(0xFFA33A2C), Color(0xFFD8C8A8), Color(0xFF3D6A8A))
                for (i in 0..4) {
                    val t = i / 4f
                    val c = Offset(12f + 40f * t, 18f + 30f * sin(PI * t).toFloat())
                    drawCircle(cols[i], 5f, c); drawCircle(edge, 5f, c, style = Stroke(1f))
                }
            }
            Shape.POUCH -> {
                val b = p { moveTo(20f, 22f); cubicTo(10f, 34f, 12f, 52f, 32f, 54f); cubicTo(52f, 52f, 54f, 34f, 44f, 22f); close() }
                drawPath(b, Color(0xFF9A6A3A)); drawPath(b, Color(0xFF3A2414), style = o)
                line(Offset(20f, 22f), Offset(44f, 22f), Color(0xFF3A2414), 3f); drawCircle(Color(0xFFB5442A), 6f, Offset(32f, 40f))
            }
            Shape.FLUTE -> {
                rotate(-35f, Offset(32f, 32f)) {
                    drawRoundRect(bone, Offset(8f, 27f), Size(48f, 10f), CornerRadius(5f)); drawRoundRect(edge, Offset(8f, 27f), Size(48f, 10f), CornerRadius(5f), style = Stroke(1.5f))
                    for (i in 0..3) drawCircle(edge, 1.8f, Offset(22f + i * 8f, 32f))
                }
            }
            Shape.TABLET -> {
                drawRoundRect(clay, Offset(14f, 10f), Size(36f, 44f), CornerRadius(5f)); drawRoundRect(edge, Offset(14f, 10f), Size(36f, 44f), CornerRadius(5f), style = o)
                for (i in 0..4) drawLine(Color(0xFF7A5230), Offset(20f, 19f + i * 7), Offset(34f + (i * 7) % 10, 19f + i * 7), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 2f)))
            }
            Shape.MIRROR -> {
                drawRoundRect(Color(0xFF7A5230), Offset(29f, 40f), Size(6f, 16f), CornerRadius(2f))
                drawCircle(Color(0xFFB8893C), 16f, Offset(32f, 26f)); drawCircle(edge, 16f, Offset(32f, 26f), style = o); drawCircle(Color(0xFFD9B46A), 11f, Offset(32f, 26f))
            }
            Shape.AMULET -> {
                drawPath(p { moveTo(18f, 8f); cubicTo(20f, 26f, 44f, 26f, 46f, 8f) }, Color(0xFF7A5230), style = Stroke(1.4f))
                drawOval(Color(0xFF3D6A8A), Offset(22f, 22f), Size(20f, 28f)); drawOval(edge, Offset(22f, 22f), Size(20f, 28f), style = o)
                drawOval(Color(0xFF8FB4C9), Offset(27f, 27f), Size(6f, 9f))
            }
            Shape.COIN -> {
                drawCircle(copper, 18f, Offset(32f, 32f)); drawCircle(edge, 18f, Offset(32f, 32f), style = o)
                drawCircle(Color(0xFF8A5222), 13f, Offset(32f, 32f), style = Stroke(1f)); drawRect(edge, Offset(28f, 28f), Size(8f, 8f))
            }
            Shape.CUP -> {
                val c = p { moveTo(16f, 22f); lineTo(48f, 22f); cubicTo(48f, 42f, 42f, 50f, 32f, 50f); cubicTo(22f, 50f, 16f, 42f, 16f, 22f); close() }
                drawPath(c, clay); drawPath(c, edge, style = o); drawOval(Color(0xFF7A5230), Offset(16f, 18f), Size(32f, 8f))
                drawLine(Color(0xFF7A5230), Offset(20f, 32f), Offset(44f, 32f), 1.4f)
            }
            Shape.SPOON -> {
                line(Offset(28f, 27f), Offset(52f, 52f), wood, 5f)
                rotate(-40f, Offset(22f, 20f)) { drawOval(Color(0xFFC49A68), Offset(13f, 8f), Size(18f, 24f)); drawOval(edge, Offset(13f, 8f), Size(18f, 24f), style = o) }
            }
            Shape.CHARM -> {
                line(Offset(32f, 4f), Offset(32f, 10f), Color(0xFF4A4540), 2f)
                drawRoundRect(pewter, Offset(18f, 10f), Size(28f, 40f), CornerRadius(3f)); drawRoundRect(Color(0xFF4A4540), Offset(18f, 10f), Size(28f, 40f), CornerRadius(3f), style = o)
                drawCircle(Color(0xFFD9B46A), 6f, Offset(32f, 24f)); drawPath(p { moveTo(24f, 44f); cubicTo(24f, 32f, 40f, 32f, 40f, 44f); close() }, Color(0xFF7A5C4A))
            }
            Shape.COMB -> {
                drawRect(bone, Offset(10f, 20f), Size(44f, 10f)); drawRect(edge, Offset(10f, 20f), Size(44f, 10f), style = o)
                for (i in 0 until 10) drawLine(bone, Offset(13f + i * 4.3f, 30f), Offset(13f + i * 4.3f, 46f), 2.5f)
            }
            Shape.WHISTLE -> {
                drawRoundRect(Color(0xFFC9B07A), Offset(8f, 26f), Size(44f, 12f), CornerRadius(6f)); drawRoundRect(edge, Offset(8f, 26f), Size(44f, 12f), CornerRadius(6f), style = o)
                drawRect(edge, Offset(16f, 28f), Size(6f, 4f)); drawLine(Color(0xFF7A5230), Offset(30f, 26f), Offset(30f, 38f), 1f)
            }
            Shape.PIPE -> {
                line(Offset(10f, 22f), Offset(36f, 32f), bone, 5f)
                val b = p { moveTo(34f, 26f); lineTo(50f, 26f); lineTo(50f, 40f); cubicTo(50f, 48f, 34f, 48f, 34f, 40f); close() }
                drawPath(b, bone); drawPath(b, edge, style = o)
            }
            Shape.LETTER -> {
                drawRect(paper, Offset(12f, 16f), Size(40f, 32f)); drawRect(edge, Offset(12f, 16f), Size(40f, 32f), style = o)
                for (i in 0..3) drawPath(p { moveTo(17f, 24f + i * 6); cubicTo(25f, 22f + i * 6, 35f, 26f + i * 6, 47f, 24f + i * 6) }, Color(0xFF2B2A5A), style = Stroke(1.2f))
                drawCircle(Color(0xFF8F2F22), 5f, Offset(44f, 44f))
            }
            Shape.GLASSES -> {
                val wire = Stroke(2.2f)
                drawCircle(Color(0x33A8C8D8), 10f, Offset(20f, 34f)); drawCircle(Color(0xFF4A4540), 10f, Offset(20f, 34f), style = wire)
                drawCircle(Color(0x33A8C8D8), 10f, Offset(44f, 34f)); drawCircle(Color(0xFF4A4540), 10f, Offset(44f, 34f), style = wire)
                drawPath(p { moveTo(30f, 33f); quadraticTo(32f, 29f, 34f, 33f) }, Color(0xFF4A4540), style = wire)
                line(Offset(10f, 32f), Offset(4f, 26f), Color(0xFF4A4540), 2f); line(Offset(54f, 32f), Offset(60f, 26f), Color(0xFF4A4540), 2f)
            }
            Shape.BUTTON -> {
                drawCircle(brass, 16f, Offset(32f, 32f)); drawCircle(edge, 16f, Offset(32f, 32f), style = o)
                drawCircle(Color(0xFF8A7430), 12f, Offset(32f, 32f), style = Stroke(1f))
                drawPath(p { moveTo(24f, 32f); lineTo(32f, 24f); lineTo(40f, 32f); lineTo(32f, 40f); close() }, Color(0xFF8A7430))
            }
            Shape.PHOTO -> {
                drawRect(Color(0xFFD8C8A0), Offset(12f, 10f), Size(40f, 46f)); drawRect(edge, Offset(12f, 10f), Size(40f, 46f), style = o)
                drawRect(Color(0xFF8A7A62), Offset(17f, 15f), Size(30f, 32f))
                drawCircle(Color(0xFFC8B898), 4f, Offset(26f, 26f)); drawCircle(Color(0xFFC8B898), 4f, Offset(38f, 25f))
                drawPath(p { moveTo(20f, 47f); cubicTo(20f, 37f, 32f, 37f, 32f, 47f); moveTo(32f, 47f); cubicTo(32f, 37f, 44f, 37f, 44f, 47f) }, Color(0xFF5A4A3A))
            }
            Shape.TICKET -> {
                drawRect(Color(0xFFC7B37A), Offset(8f, 20f), Size(48f, 24f)); drawRect(edge, Offset(8f, 20f), Size(48f, 24f), style = o)
                drawLine(edge, Offset(40f, 20f), Offset(40f, 44f), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 2f)))
                drawLine(edge, Offset(14f, 28f), Offset(34f, 28f), 2f); drawLine(edge, Offset(14f, 34f), Offset(28f, 34f), 2f)
            }
            Shape.THIMBLE -> {
                val t = p { moveTo(20f, 50f); cubicTo(18f, 30f, 22f, 14f, 32f, 14f); cubicTo(42f, 14f, 46f, 30f, 44f, 50f); close() }
                drawPath(t, Color(0xFFB8B2A8)); drawPath(t, Color(0xFF4A4540), style = o)
                for (r in 0..2) for (c in 0..2) drawCircle(Color(0xFF6A655E), 1.4f, Offset(26f + c * 6, 26f + r * 7))
            }
            Shape.WATCH -> {
                drawLine(brass, Offset(32f, 6f), Offset(32f, 14f), 3f)
                drawCircle(brass, 18f, Offset(32f, 34f)); drawCircle(edge, 18f, Offset(32f, 34f), style = o); drawCircle(Color(0xFFF4ECD8), 14f, Offset(32f, 34f))
                for (i in 0 until 12) rotate(i * 30f, Offset(32f, 34f)) { drawLine(edge, Offset(32f, 22f), Offset(32f, 24.5f), 1f) }
                drawLine(edge, Offset(32f, 34f), Offset(32f, 25f), 1.6f); drawLine(edge, Offset(32f, 34f), Offset(39f, 37f), 1.6f)
            }
            Shape.PHONE -> {
                drawRoundRect(Color(0xFF3A3A40), Offset(20f, 8f), Size(24f, 48f), CornerRadius(4f)); drawRoundRect(Color(0xFF1A1A1E), Offset(20f, 8f), Size(24f, 48f), CornerRadius(4f), style = o)
                drawRect(Color(0xFF9AB88A), Offset(24f, 13f), Size(16f, 14f))
                for (r in 0..2) for (c in 0..2) drawRect(Color(0xFFAAAAAA), Offset(24f + c * 6, 31f + r * 7), Size(4f, 4f))
            }
            Shape.KEY -> {
                drawCircle(brass, 9f, Offset(20f, 32f), style = Stroke(4f))
                line(Offset(29f, 32f), Offset(54f, 32f), brass, 4f); line(Offset(46f, 32f), Offset(46f, 40f), brass, 4f); line(Offset(52f, 32f), Offset(52f, 38f), brass, 4f)
            }
            Shape.CASSETTE -> {
                drawRoundRect(Color(0xFF3A3A40), Offset(8f, 16f), Size(48f, 32f), CornerRadius(3f))
                drawRect(Color(0xFFE6D8B8), Offset(13f, 20f), Size(38f, 14f))
                drawLine(Color(0xFF8F2F22), Offset(15f, 24f), Offset(48f, 24f), 1.5f)
                drawCircle(Color(0xFF1A1A1E), 4f, Offset(24f, 39f)); drawCircle(Color(0xFF1A1A1E), 4f, Offset(40f, 39f))
                drawCircle(Color(0xFFE6D8B8), 2f, Offset(24f, 39f)); drawCircle(Color(0xFFE6D8B8), 2f, Offset(40f, 39f))
            }
        }
    }
}
