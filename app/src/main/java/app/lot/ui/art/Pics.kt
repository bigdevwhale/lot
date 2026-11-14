package app.lot.ui.art

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.lot.domain.Era
import app.lot.domain.Region
import app.lot.domain.ThingKind
import app.lot.ui.art.ThingArt.thing
import app.lot.ui.theme.C

/**
 * The painted pictures in `assets/art` (made by tools/art): a thing for each kind in the chest, a plate for each era and part
 * of the world, a few larger pictures and the icons, which are white masks tinted where they are shown.
 *
 * Any of them may be missing from a build; then nothing is shown in its place (a thing falls back to its engraving in code).
 */
object Pics {
    private val cache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    private val missing = HashSet<String>()

    /** Decoded on first use and kept; called from composition, on the main thread: the files are small. */
    fun get(ctx: Context, path: String): ImageBitmap? {
        cache.get(path)?.let { return it }
        if (path in missing) return null
        val bmp = runCatching { ctx.assets.open("art/$path.webp").use { BitmapFactory.decodeStream(it) } }.getOrNull()
        if (bmp == null) { missing += path; return null }
        return bmp.asImageBitmap().also { cache.put(path, it) }
    }

    fun thing(k: ThingKind) = "thing/${k.key}"

    /** The plate of an era as it looked in [r]'s part of the world; a life never gets another culture's plate. */
    fun era(e: Era, r: Region) = "era/${e.i}.${world(r.cult)}"

    /** The regions' cultures folded into the worlds the plates are painted for (the keys of ERAS in tools/art/manifest.py). */
    private fun world(cult: String) = when (cult) {
        "stone" -> "any"
        "gr", "fr", "it", "de", "en" -> "eu"
        "mes", "eg" -> "me"
        "la" -> "am"
        else -> cult
    }
}

@Composable
private fun rememberPic(path: String): ImageBitmap? {
    val ctx = LocalContext.current
    return remember(path) { Pics.get(ctx, path) }
}

/** The painted sack and tag for the draw, when both are shipped. */
@Composable
fun rememberSackArt(): SackArt? {
    val sack = rememberPic("stage/sack") ?: return null
    val tag = rememberPic("stage/tag") ?: return null
    return remember(sack, tag) { SackArt(sack, tag) }
}

/** The atlas's painted sea, land and ornaments, as far as they are shipped. */
@Composable
fun rememberMapPaint(): MapPaint {
    val sea = rememberPic("map/sea"); val land = rememberPic("map/land")
    val compass = rememberPic("map/compass"); val ship = rememberPic("map/ship"); val serpent = rememberPic("map/serpent")
    return remember(sea, land, compass, ship, serpent) { MapPaint(sea, land, compass, ship, serpent) }
}

/** Whether the picture at [path] was painted and shipped. */
@Composable
fun hasPic(path: String): Boolean = rememberPic(path) != null

/** A painted picture; takes no room at all if the build has none. */
@Composable
fun Pic(path: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit) {
    val img = rememberPic(path) ?: return
    Image(img, null, modifier, contentScale = scale)
}

/** One of the engraved icons, in [tint]; nothing at all if it is missing. */
@Composable
fun Ico(name: String, modifier: Modifier = Modifier, tint: Color = C.ink2) {
    val img = rememberPic("icon/$name") ?: return
    Image(img, null, modifier, colorFilter = ColorFilter.tint(tint))
}

/** A thing from the chest: its painting, or the engraving drawn in code. */
@Composable
fun ThingPic(k: ThingKind, modifier: Modifier = Modifier) {
    val img = rememberPic(Pics.thing(k))
    if (img != null) Image(img, null, modifier) else Canvas(modifier) { thing(k.shape) }
}
