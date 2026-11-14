package app.lot.ui.atlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lot.container
import app.lot.domain.Hist
import app.lot.domain.Sex
import app.lot.domain.World
import app.lot.ui.Home
import app.lot.ui.art.Ico
import app.lot.ui.art.MapArt
import app.lot.ui.art.MapArt.map
import app.lot.ui.art.Pin
import app.lot.ui.art.rememberMapPaint
import app.lot.ui.components.Card
import app.lot.ui.components.Frame
import app.lot.ui.components.Gap
import app.lot.ui.components.Meter
import app.lot.ui.components.Rule
import app.lot.ui.components.Section
import app.lot.ui.components.Stat
import app.lot.ui.components.TopBar
import app.lot.ui.theme.C
import app.lot.ui.theme.T

/** Where you were born across the ages, how your lots compare with everyone ever born, the people met, and your own answers. */
@Composable
fun AtlasScreen(h: Home, onLife: (Long) -> Unit, first: Long? = null) {
    val w = h.w
    val lives = h.s.lives
    var u by rememberSaveable { mutableFloatStateOf(1f) }
    val until = toYear(u)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(w.t("Атлас", "Atlas"), if (lives.isEmpty()) w.t("Здесь появятся места твоих рождений.", "Your birthplaces will appear here.")
            else w.t("Жизней: ${lives.size}. Каждая точка — место рождения.", "Lives: ${lives.size}. Each dot is a birthplace."))
        var picked by rememberSaveable { mutableStateOf(first) }
        val paint = rememberMapPaint()
        val shown = lives.filter { u >= 1f || it.year <= until }
        val pins = shown.map { l ->
            val r = World.region(l.region, l.eraI)
            Pin(r.lon, r.lat, Color(World.ERAS[l.eraI].color), l.death < 5, l.id.toInt(), l.id == picked)
        }
        Canvas(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().aspectRatio(MapArt.W / MapArt.H)
            .drawBehind { drawRect(C.ink2.copy(alpha = .12f), Offset(0f, 3.dp.toPx()), size) }
            .pointerInput(shown) {
                // The nearest dot within a finger's reach; a tap on open sea closes the note.
                detectTapGestures { tap ->
                    val k = size.width / MapArt.W
                    val near = pins.indices.minByOrNull { (MapArt.at(pins[it]) * k - tap).getDistance() }
                    picked = near?.takeIf { (MapArt.at(pins[it]) * k - tap).getDistance() < 24.dp.toPx() }?.let { shown[it].id }
                }
            }) { map(pins, paint) }
        shown.firstOrNull { it.id == picked }?.let { l ->
            val e = World.ERAS[l.eraI]
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(C.paperHi)
                .clickable { onLife(l.id) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(e.color)))
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(w.x(l.name), style = T.BodyB)
                    Text("${w.year(l.year)} – ${w.year(l.year + l.death)} · ${w.x(e.name)} · ${w.x(World.region(l.region, l.eraI).name)}", style = T.Small)
                }
                Text(w.t("Книга ›", "Book ›"), style = T.Small.copy(color = C.red), modifier = Modifier.padding(start = 8.dp))
            }
        }
        Card(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ico("chronicle", Modifier.padding(end = 8.dp).size(20.dp), tint = C.muted)
                Text(if (u >= 1f) w.t("все жизни · ${shown.size}", "all lives · ${shown.size}") else w.t("до ", "until ") + w.year(until) + " · ${shown.size}",
                    style = T.BodyB, modifier = Modifier.weight(1f))
                if (u < 1f) Text(w.t("сброс", "reset"), style = T.Small.copy(color = C.red), modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { u = 1f }.padding(4.dp))
            }
            TimeSlider(u, Modifier.fillMaxWidth()) { u = it }
            // Each turning point written under its tick on the slider.
            Layout({
                for (y in listOf(-8000, 1000, 1800)) Text(w.year(y), style = T.Small.copy(fontSize = T.Label.fontSize))
                Text(w.t("сейчас", "now"), style = T.Small.copy(fontSize = T.Label.fontSize))
            }, Modifier.fillMaxWidth()) { ms, c ->
                val ps = ms.map { it.measure(c.copy(minWidth = 0)) }
                val r = 12.dp.roundToPx()
                val at = listOf(.3f, .55f, .8f, 1f)
                layout(c.maxWidth, ps.maxOf { it.height }) {
                    for ((i, p) in ps.withIndex()) {
                        val x = (r + (c.maxWidth - 2 * r) * at[i] - p.width / 2).toInt().coerceIn(0, c.maxWidth - p.width)
                        p.place(x, 0)
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            Gap(14)
            Section("census", w.t("Перепись", "Census"), w.t("Твои жребии рядом с настоящим распределением всех, кто когда-либо родился.", "Your lots next to the real spread of everyone ever born."))
            Gap(8)
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp, 8.dp).background(C.paperLo)); Text(w.t("  все люди   ", "  everyone   "), style = T.Small)
                    Box(Modifier.size(12.dp, 5.dp).background(C.ink2)); Text(w.t("  твои жребии (цвет эпохи)", "  your lots (era colour)"), style = T.Small)
                }
                val n = lives.size.coerceAtLeast(1)
                val max = World.ERAS.maxOf { it.births / World.TOTAL }
                for (e in World.ERAS) {
                    val mine = lives.count { it.eraI == e.i }
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(e.color)))
                        Text(w.x(e.name), style = T.Small.copy(color = C.ink), modifier = Modifier.padding(start = 6.dp).width(120.dp))
                        Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(C.paperHi)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth((e.births / World.TOTAL / max).toFloat()).clip(RoundedCornerShape(8.dp)).background(C.paperLo))
                            Box(Modifier.align(Alignment.CenterStart).padding(start = 3.dp).height(6.dp).fillMaxWidth(((mine.toDouble() / n) / max).toFloat().coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(3.dp)).background(Color(e.color)))
                        }
                        Text("$mine", style = T.Small, modifier = Modifier.width(34.dp).padding(start = 8.dp))
                    }
                }
            }
            if (lives.isNotEmpty()) {
                val infants = lives.count { it.death < 5 }
                Gap(10)
                Card {
                    Row(Modifier.fillMaxWidth()) {
                        Stat("cradle", "$infants / ${lives.size}", w.t("не дожили до пяти", "died before five"), Modifier.weight(1f))
                        Stat("globe", "${lives.count { it.sawSea }}", w.t("видели море", "saw the sea"), Modifier.weight(1f))
                        Stat("literacy", "${lives.count { it.lit }}", w.t("умели читать", "could read"), Modifier.weight(1f))
                    }
                }
            }

            val hist = LocalContext.current.container.content.hist
            val seen = h.s.seen
            fun said(e: Hist) = w.x(e.text.g(seen[e.id]?.sexOf ?: Sex.M))

            // The encounters: people later written about, met in passing; the unmet ones wait as a count.
            val meets = hist.filter { it.meet }
            val met = meets.filter { it.id in seen }.sortedBy { it.mid }
            Gap(18)
            Section("encounters", w.t("Встречи", "Encounters"), w.t("Те, о ком потом писали книги, — увиденные мимоходом. Остальные ждут в других веках и странах.",
                "People later written about, seen in passing. The rest wait in other centuries and lands."))
            Progress(met.size, meets.size, C.red)
            for (e in met) {
                val l = seen.getValue(e.id)
                Note(w.x(l.name) + ", " + w.year(l.year), said(e), onClick = { onLife(l.id) })
            }

            // The chronicle: every real event any life has met, by era, with what is still unseen.
            Gap(18)
            Section("chronicle", w.t("Летопись", "Chronicle"), w.t("Настоящие события, которые застали твои жизни.", "Real events your lives have lived through."))
            Progress(seen.size, hist.size, C.ink2)
            Gap(6)
            Card {
                for (e in World.ERAS) {
                    val inEra = hist.filter { World.eraOf(it.mid).i == e.i }
                    if (inEra.isEmpty()) continue
                    val got = inEra.count { it.id in seen }
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(e.color)))
                        Text(w.x(e.name), style = T.Small.copy(color = C.ink), modifier = Modifier.padding(start = 6.dp).width(126.dp))
                        Meter(got.toFloat() / inEra.size, Color(e.color), Modifier.weight(1f))
                        Text("$got / ${inEra.size}", style = T.Small, textAlign = TextAlign.End, modifier = Modifier.width(58.dp))
                    }
                }
            }
            Gap(6)
            val lived = hist.filter { it.id in seen && !it.meet }.sortedBy { it.mid }
            var all by rememberSaveable { mutableStateOf(false) }
            for (e in if (all) lived else lived.take(6)) {
                val l = seen.getValue(e.id)
                Note(w.year(e.from) + (if (e.to != e.from) " – " + w.year(e.to) else "") + " · " + w.x(l.name), said(e), onClick = { onLife(l.id) })
            }
            if (lived.size > 6) Text(if (all) w.t("Свернуть", "Show less") else w.t("Вся летопись (${lived.size})", "The whole chronicle (${lived.size})"),
                style = T.Button.copy(color = C.red), modifier = Modifier.clickable { all = !all }.padding(vertical = 10.dp))

            Gap(18)
            Section("notebook", w.t("Тетрадка", "Notebook"), w.t("Ответы на вопросы, которые жизни задали тебе.", "Your answers to the questions the lives asked you."))
            Gap(4)
            val answered = lives.filter { it.answer.isNotBlank() }.reversed()
            if (answered.isEmpty()) Text(w.t("Пока пусто.", "Nothing yet."), style = T.Italic, modifier = Modifier.padding(vertical = 10.dp))
            for ((i, l) in answered.withIndex()) { Slip(w.x(l.bridge), w.x(l.name) + ", " + w.year(l.year), l.answer, l.id.toInt(), if (i % 2 == 0) -.6f else .5f, onClick = { onLife(l.id) }); Gap(10) }
            Gap(24)
        }
    }
}

/** How many of [all] are found, as a bar and a count. */
@Composable
private fun Progress(got: Int, all: Int, color: Color) {
    Row(Modifier.padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Meter(if (all == 0) 0f else got.toFloat() / all, color, Modifier.weight(1f), 8.dp)
        Text("$got / $all", style = T.BodyB, modifier = Modifier.padding(start = 10.dp))
    }
}

/** An answer written on a slip of paper, with the question above and whose question it was. */
@Composable
private fun Slip(q: String, who: String, answer: String, seed: Int, tilt: Float, onClick: () -> Unit) {
    Card(Modifier.graphicsLayer { rotationZ = tilt }, onClick = onClick, frame = Frame.LEAF, seed = seed) {
        Text(q, style = T.Italic.copy(fontSize = T.Small.fontSize, lineHeight = T.Small.lineHeight, color = C.ink2))
        Text(answer, style = T.Write, modifier = Modifier.padding(vertical = 4.dp))
        Text("— $who", style = T.Small, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
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
