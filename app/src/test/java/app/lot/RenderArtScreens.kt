package app.lot

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.lot.data.DemoSeed
import app.lot.ui.Home
import app.lot.ui.Words
import app.lot.ui.atlas.AtlasScreen
import app.lot.ui.book.BookScreen
import app.lot.ui.book.ReadSheet
import app.lot.ui.chest.ChestScreen
import app.lot.ui.chest.ThingSheet
import app.lot.ui.components.LocalToast
import app.lot.ui.components.Toaster
import app.lot.ui.components.paper
import app.lot.ui.draw.DrawScreen
import app.lot.ui.onboarding.Onboarding
import app.lot.ui.settings.SettingsSheet
import app.lot.ui.theme.LotTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo lives, drawn to PNGs (run with `-Prender`), English and Russian. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    private fun home(): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<LotApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(c, lang == "ru")
        Home(c.repo.refresh(), c.settings.now(), Words(lang == "ru"), System.currentTimeMillis())
    }

    private fun shot(name: String, content: @Composable (Home) -> Unit) {
        val h = home()
        rule.mainClock.autoAdvance = false
        rule.setContent { LotTheme { CompositionLocalProvider(LocalToast provides Toaster()) { Box(Modifier.fillMaxSize().paper()) { content(h) } } } }
        rule.mainClock.advanceTimeBy(4_000)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    /** A long life from a family line, answered, for the end of a book. */
    private fun Home.story() = s.lives.filter { it.death > 40 }.maxByOrNull { (if (it.answer.isNotBlank()) 100 else 0) + it.gen * 10 + s.holders(it.thingId).size }!!.id

    @Test fun welcome() = shot("0-welcome") { Onboarding(it.w) }
    @Test fun draw() = shot("1-draw") { DrawScreen(it, {}, {}, {}) }
    @Test fun book() = shot("2-book") { BookScreen(it, {}, {}) }
    @Test fun end() = shot("3-end") { h -> ReadSheet(h, h.story(), {}, {}) }
    @Test fun chest() = shot("4-chest") { ChestScreen(it) {} }
    @Test fun thing() = shot("5-thing") { h -> ThingSheet(h, h.s.things.maxBy { h.s.holders(it.id).size }.id, {}, {}) }
    @Test fun atlas() = shot("6-atlas") { AtlasScreen(it) {} }
    @Test fun settings() = shot("7-settings") { SettingsSheet(it) {} }
}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
