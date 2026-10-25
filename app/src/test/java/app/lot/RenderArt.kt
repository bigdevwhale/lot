package app.lot

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import app.lot.domain.Shape
import app.lot.ui.art.MapArt
import app.lot.ui.art.MapArt.map
import app.lot.ui.art.Pin
import app.lot.ui.art.Sack.stage
import app.lot.ui.art.TagText
import app.lot.ui.art.ThingArt.thing
import app.lot.ui.theme.C
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Draws the sack, the tag, the things and the map to PNGs (run with `-Prender`): a look at the art without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArt {
    private val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }
    private val tm by lazy { TextMeasurer(createFontFamilyResolver(ApplicationProvider.getApplicationContext()), Density(1f), LayoutDirection.Ltr) }

    private fun render(name: String, w: Int, h: Int, draw: DrawScope.() -> Unit) {
        val image = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat()), draw)
        File(out, "$name.png").outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun sack() = render("sack", 1500, 660) {
        drawRect(C.paper)
        val tag = TagText("1347", "Франция", "в семье крестьян", "девочка")
        for ((i, r) in listOf(0f, .45f, 1f).withIndex()) translate(i * 500f, 0f) {
            drawIntoSize(500f, 660f) { stage(if (i == 0) -5f else 0f, r, tag, 7L, tm) }
        }
    }

    @Test fun things() {
        val cell = 120f
        val n = Shape.entries.size
        render("things", (cell * 5).toInt(), (cell * ((n + 4) / 5)).toInt()) {
            drawRect(C.card)
            for ((i, s) in Shape.entries.withIndex()) translate((i % 5) * cell + 10f, (i / 5) * cell + 10f) { drawIntoSize(cell - 20f, cell - 20f) { thing(s) } }
        }
    }

    @Test fun map() = render("map", 1080, 540) {
        val pins = listOf(Pin(112f, 33f, Color(0xFF5C6E3A), false, 1), Pin(79f, 22f, Color(0xFF8F6A2F), true, 2), Pin(2f, 47f, Color(0xFF3F6A6A), false, 3), Pin(36f, 0f, Color(0xFF6B4A2B), false, 4))
        drawIntoSize(1080f, 540f) { map(pins) }
        check(MapArt.W == 360f)
    }

    /** Runs [block] with the draw scope's size set to [w]×[h] at the current origin. */
    private fun DrawScope.drawIntoSize(w: Float, h: Float, block: DrawScope.() -> Unit) {
        val img = ImageBitmap(w.toInt(), h.toInt())
        CanvasDrawScope().draw(this, layoutDirection, Canvas(img), Size(w, h), block)
        drawImage(img, Offset.Zero)
    }
}

/** The launcher icon drawn from its own vectors into docs/logo.png (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArtIcon {
    @Test fun logo() {
        val c = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        for (id in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
            ContextCompat.getDrawable(c, id)!!.apply { setBounds(0, 0, 512, 512); draw(canvas) }
        }
        File("../docs/logo.png").apply { parentFile?.mkdirs() }.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
