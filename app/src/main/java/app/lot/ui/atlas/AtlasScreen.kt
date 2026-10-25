package app.lot.ui.atlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import app.lot.container
import app.lot.domain.World
import app.lot.ui.Home
import app.lot.ui.art.MapArt
import app.lot.ui.art.MapArt.map
import app.lot.ui.art.Pin
import app.lot.ui.components.Gap
import app.lot.ui.components.Rule
import app.lot.ui.components.TopBar
import app.lot.ui.theme.C
import app.lot.ui.theme.T

/** Where you were born across the ages, how your lots compare with everyone ever born, the people met, and your own answers. */
@Composable
fun AtlasScreen(h: Home, onLife: (Long) -> Unit) {
    val w = h.w
    val lives = h.s.lives
    var u by rememberSaveable { mutableFloatStateOf(1f) }
    val until = toYear(u)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(w.t("Атлас", "Atlas"), if (lives.isEmpty()) w.t("Здесь появятся места твоих рождений.", "Your birthplaces will appear here.")
            else w.t("Жизней: ${lives.size}. Каждая точка — место рождения.", "Lives: ${lives.size}. Each dot is a birthplace."))
        val pins = lives.filter { u >= 1f || it.year <= until }.map { l ->
            val r = World.region(l.region, l.eraI)
            Pin(r.lon, r.lat, Color(World.ERAS[l.eraI].color), l.death < 5, l.id.toInt())
        }
        Canvas(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().aspectRatio(MapArt.W / MapArt.H)) { map(pins) }
        Row(Modifier.padding(start = 16.dp, end = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (u >= 1f) w.t("все жизни", "all lives") else w.t("до ", "until ") + w.year(until), style = T.Small, modifier = Modifier.width(118.dp))
            TimeSlider(u, Modifier.weight(1f)) { u = it }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            Gap(14)
            Text(w.t("Перепись", "Census"), style = T.Heading)
            Text(w.t("Твои жребии рядом с настоящим распределением всех, кто когда-либо родился.", "Your lots next to the real spread of everyone ever born."), style = T.Small)
            Gap(8)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp, 8.dp).background(C.paperLo)); Text(w.t("  все люди   ", "  everyone   "), style = T.Small)
                Box(Modifier.size(12.dp, 5.dp).background(C.red)); Text(w.t("  твои жребии", "  your lots"), style = T.Small)
            }
            val n = lives.size.coerceAtLeast(1)
            val max = World.ERAS.maxOf { it.births / World.TOTAL }
            for (e in World.ERAS) {
                val mine = lives.count { it.eraI == e.i }
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(e.color)))
                    Text(w.x(e.name), style = T.Small.copy(color = C.ink), modifier = Modifier.padding(start = 6.dp).width(120.dp))
                    Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(2.dp)).background(C.paperHi)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth((e.births / World.TOTAL / max).toFloat()).background(C.paperLo))
                        Box(Modifier.align(Alignment.CenterStart).height(6.dp).fillMaxWidth(((mine.toDouble() / n) / max).toFloat().coerceIn(0f, 1f)).background(C.red))
                    }
                    Text("$mine", style = T.Small, modifier = Modifier.width(34.dp).padding(start = 8.dp))
                }
            }
            if (lives.isNotEmpty()) {
                val infants = lives.count { it.death < 5 }
                Text(w.t("Не дожили до пяти лет: $infants из ${lives.size}. Видели море: ${lives.count { it.sawSea }}. Умели читать: ${lives.count { it.lit }}.",
                    "Died before five: $infants of ${lives.size}. Saw the sea: ${lives.count { it.sawSea }}. Could read: ${lives.count { it.lit }}."), style = T.Small, modifier = Modifier.padding(top = 6.dp))
            }

            val met = lives.filter { it.meet != null }
            if (met.isNotEmpty()) {
                Gap(18)
                Text(w.t("Встречи", "Encounters"), style = T.Heading)
                Text(w.t("Те, о ком потом писали книги, — увиденные мимоходом.", "People later written about, seen in passing."), style = T.Small)
                val hist = LocalContext.current.container.content.hist
                for (l in met) Note(w.x(l.name) + ", " + w.year(l.year), hist.getOrNull(l.meet!!)?.text?.let(w::x) ?: "", onClick = { onLife(l.id) })
            }

            Gap(18)
            Text(w.t("Тетрадка", "Notebook"), style = T.Heading)
            Text(w.t("Ответы на вопросы, которые жизни задали тебе.", "Your answers to the questions the lives asked you."), style = T.Small)
            val answered = lives.filter { it.answer.isNotBlank() }.reversed()
            if (answered.isEmpty()) Text(w.t("Пока пусто.", "Nothing yet."), style = T.Italic, modifier = Modifier.padding(vertical = 10.dp))
            for (l in answered) Note(w.x(l.bridge) + " — " + w.x(l.name) + ", " + w.year(l.year), l.answer, onClick = { onLife(l.id) })
            Gap(24)
        }
    }
}

@Composable
private fun Note(head: String, body: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp)) {
        Text(head, style = T.Italic.copy(fontSize = T.Small.fontSize, lineHeight = T.Small.lineHeight))
        Text(body, style = T.Write)
        Rule(Modifier.padding(top = 6.dp))
    }
}

/** A thin engraved rule with a brass bead to drag along time. */
@Composable
private fun TimeSlider(u: Float, modifier: Modifier, onChange: (Float) -> Unit) {
    // The track is inset by the bead's radius, and the system back swipe is kept off it.
    fun at(x: Float, w: Float, r: Float) = ((x - r) / (w - 2 * r)).coerceIn(0f, 1f)
    Canvas(
        modifier.height(40.dp).systemGestureExclusion()
            .pointerInput(Unit) { detectTapGestures { onChange(at(it.x, size.width.toFloat(), 12.dp.toPx())) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { ch, _ -> onChange(at(ch.position.x, size.width.toFloat(), 12.dp.toPx())) } },
    ) {
        val y = size.height / 2
        val r = 12.dp.toPx()
        fun x(t: Float) = r + (size.width - 2 * r) * t
        drawLine(C.line, Offset(x(0f), y), Offset(x(1f), y), 2.dp.toPx())
        drawLine(C.ink2, Offset(x(0f), y), Offset(x(u), y), 2.dp.toPx())
        for (t in listOf(.3f, .55f, .8f)) drawLine(C.ink2, Offset(x(t), y - 5.dp.toPx()), Offset(x(t), y + 5.dp.toPx()), 1.dp.toPx())
        drawCircle(C.ink2, 9.dp.toPx(), Offset(x(u), y))
        drawCircle(C.woodHi, 6.dp.toPx(), Offset(x(u), y))
    }
}

/** The slider is slow through the Stone Age and fine-grained near now, where the years are crowded with people. */
private fun toYear(u: Float): Int = when {
    u < .3f -> (-50000 + (42000 * u / .3f)).toInt()
    u < .55f -> (-8000 + 9000 * (u - .3f) / .25f).toInt()
    u < .8f -> (1000 + 800 * (u - .55f) / .25f).toInt()
    else -> (1800 + 226 * (u - .8f) / .2f).toInt()
}
