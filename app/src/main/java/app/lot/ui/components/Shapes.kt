package app.lot.ui.components

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * The shapes of the paper things on the screens, instead of one rounded rectangle for everything:
 * a plate with its corners cut in, a ticket with round bites at the corners, a leaf torn along its foot,
 * and a niche with an arched top for the chest.
 */
object Paper {
    /** Corners cut inwards in a quarter circle, like an old label or a bookplate. */
    fun plate(r: Dp = 9.dp): Shape = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val c = with(density) { r.toPx() }.coerceAtMost(minOf(size.width, size.height) / 3)
            val w = size.width; val h = size.height
            return Outline.Generic(Path().apply {
                moveTo(c, 0f); lineTo(w - c, 0f)
                arcTo(Rect(w - c, -c, w + c, c), 180f, -90f, false)
                lineTo(w, h - c)
                arcTo(Rect(w - c, h - c, w + c, h + c), 270f, -90f, false)
                lineTo(c, h)
                arcTo(Rect(-c, h - c, c, h + c), 0f, -90f, false)
                lineTo(0f, c)
                arcTo(Rect(-c, -c, c, c), 90f, -90f, false)
                close()
            })
        }
    }

    /** Small rounded corners with a round bite out of the middle of each side, like a ticket stub. */
    fun ticket(bite: Dp = 5.dp, corner: Dp = 4.dp): Shape = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val b = with(density) { bite.toPx() }; val r = with(density) { corner.toPx() }
            val w = size.width; val h = size.height; val m = h / 2
            return Outline.Generic(Path().apply {
                moveTo(r, 0f); lineTo(w - r, 0f); quadraticTo(w, 0f, w, r)
                lineTo(w, m - b); arcTo(Rect(w - b, m - b, w + b, m + b), 270f, -180f, false)
                lineTo(w, h - r); quadraticTo(w, h, w - r, h)
                lineTo(r, h); quadraticTo(0f, h, 0f, h - r)
                lineTo(0f, m + b); arcTo(Rect(-b, m - b, b, m + b), 90f, -180f, false)
                lineTo(0f, r); quadraticTo(0f, 0f, r, 0f)
                close()
            })
        }
    }

    /** A leaf with a straight head and a torn foot; [seed] makes every leaf tear its own way. */
    fun leaf(seed: Int = 0, depth: Dp = 3.dp): Shape = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val d = with(density) { depth.toPx() }; val step = with(density) { 7.dp.toPx() }
            val w = size.width; val h = size.height; val r = with(density) { 3.dp.toPx() }
            return Outline.Generic(Path().apply {
                moveTo(r, 0f); lineTo(w - r, 0f); quadraticTo(w, 0f, w, r)
                lineTo(w, h - d)
                var x = w
                var i = 0
                while (x > 0f) {
                    x = (x - step).coerceAtLeast(0f)
                    // A deterministic ragged line: two waves out of step with each other.
                    val n = sin((i + seed * 3.7f) * 1.9f) * .55f + sin((i + seed) * 4.3f) * .45f
                    lineTo(x, h - d / 2 + n * d / 2)
                    i++
                }
                lineTo(0f, r); quadraticTo(0f, 0f, r, 0f)
                close()
            })
        }
    }

    /** A museum niche: an arched head over straight sides. */
    val niche: Shape = GenericShape { size, _ ->
        val w = size.width; val h = size.height; val a = minOf(w / 2, h / 3)
        moveTo(0f, h)
        lineTo(0f, a)
        cubicTo(0f, a * .1f, w * .2f, 0f, w / 2, 0f)
        cubicTo(w * .8f, 0f, w, a * .1f, w, a)
        lineTo(w, h)
        close()
    }
}
