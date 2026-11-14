package app.lot.ui.chest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lot.domain.Things
import app.lot.domain.World
import app.lot.ui.Home
import app.lot.ui.art.Pic
import app.lot.ui.art.ThingPic
import app.lot.ui.components.Card
import app.lot.ui.components.Frame
import app.lot.ui.components.Gap
import app.lot.ui.components.Paper
import app.lot.ui.components.Segmented
import app.lot.ui.components.Sheet
import app.lot.ui.components.Stat
import app.lot.ui.components.TopBar
import app.lot.ui.components.inner
import app.lot.ui.theme.C
import app.lot.ui.theme.T

/** Every life leaves one thing; a family line hands its thing down. */
@Composable
fun ChestScreen(h: Home, onThing: (Long) -> Unit) {
    val w = h.w
    Column(Modifier.fillMaxSize()) {
        TopBar(w.t("Сундук", "The chest"), w.t("Каждая жизнь оставляет одну вещь. Если тянуть правнуков, вещь переходит по наследству.",
            "Each life leaves one thing behind. Draw great-grandchildren and the thing is handed down."))
        if (h.s.things.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Pic("pic/empty_chest", Modifier.size(200.dp))
                Text(w.t("Сундук пуст", "The chest is empty"), style = T.Heading, modifier = Modifier.padding(top = 8.dp))
                Text(w.t("Проживи первую жизнь — и в сундук ляжет её вещь.", "Live a first life, and its thing will be laid in the chest."),
                    style = T.Italic, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
            return
        }
        var filter by rememberSaveable { mutableIntStateOf(0) }
        val all = h.s.things.reversed()
        val handed = all.filter { h.s.holders(it.id).size > 1 }
        val list = if (filter == 1) handed else all
        LazyVerticalGrid(
            GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(3) }) {
                Column {
                    Card {
                        Row(Modifier.fillMaxWidth()) {
                            Stat("tab_chest", "${all.size}", w.t("вещей", "things"), Modifier.weight(1f))
                            Stat("people", "${handed.size}", w.t("в роду", "heirlooms"), Modifier.weight(1f))
                            Stat("chronicle", "${handed.maxOfOrNull { h.s.holders(it.id).size } ?: 1}", w.t("поколений\nсамое долгое", "generations\nat most"), Modifier.weight(1f))
                        }
                    }
                    if (handed.isNotEmpty()) {
                        Gap(10)
                        Segmented(listOf(w.t("все вещи", "all things") to null, w.t("наследство · ${handed.size}", "handed down · ${handed.size}") to null), filter) { filter = it }
                    }
                }
            }
            items(list, key = { it.id }) { t ->
                val k = Things.of(t.key)
                val holders = h.s.holders(t.id)
                val first = holders.firstOrNull()
                val tint = first?.let { Color(World.ERAS[it.eraI].color) } ?: C.line
                val shape = Paper.niche
                Column(
                    Modifier
                        .drawBehind { translate(0f, 2.dp.toPx()) { drawOutline(shape.createOutline(size, layoutDirection, this), C.ink2.copy(alpha = .12f)) } }
                        .clip(shape).background(Brush.verticalGradient(listOf(C.paperHi, C.card)))
                        .drawBehind {
                            // The arch in the colour of the era the thing comes from.
                            clipRect(0f, 0f, size.width, size.height * .3f) { drawOutline(shape.createOutline(size, layoutDirection, this), tint, style = Stroke(5.dp.toPx())) }
                            inner(shape, 5.dp.toPx(), C.line.copy(alpha = .5f))
                        }
                        .border(if (holders.size > 1) 1.5.dp else 1.dp, if (holders.size > 1) C.ink2 else C.line, shape)
                        .clickable { onThing(t.id) }.padding(start = 6.dp, end = 6.dp, top = 16.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        ThingPic(k, Modifier.size(72.dp))
                        if (holders.size > 1) Box(
                            Modifier.align(Alignment.TopEnd).clip(CircleShape).background(C.red).border(1.5.dp, C.card, CircleShape).padding(horizontal = 6.dp, vertical = 1.dp),
                        ) { Text("×${holders.size}", style = T.Label.copy(color = C.light, letterSpacing = 0.sp)) }
                    }
                    Text(w.x(k.name).replaceFirstChar { it.uppercase() }, style = T.Small.copy(color = C.ink, textAlign = TextAlign.Center), minLines = 3, maxLines = 3, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp))
                    // Deep in the Stone Age both ends of a family's years round to the same words: said once.
                    val from = holders.minOfOrNull { it.year }?.let { w.year(it) } ?: ""
                    val to = holders.maxOfOrNull { it.year + it.death }?.let { w.year(it) } ?: ""
                    Text(
                        if (holders.size > 1 && from != to) "$from – $to" else from,
                        style = T.Small.copy(fontSize = T.Label.fontSize, color = if (holders.size > 1) C.red else C.muted, textAlign = TextAlign.Center),
                        // One line in every cell, so the rows stay even; the whole span is on the thing's own page.
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** One thing and everyone who held it, as a family line running down the page. */
@Composable
fun ThingSheet(h: Home, id: Long, onLife: (Long) -> Unit, onClose: () -> Unit) {
    val w = h.w
    val t = h.s.thing(id) ?: return onClose()
    val k = Things.of(t.key)
    val holders = h.s.holders(id)
    Sheet(w.x(k.name).replaceFirstChar { it.uppercase() }, onClose, w.t("Закрыть", "Close")) {
        Box(
            Modifier.fillMaxWidth().height(210.dp).drawBehind {
                drawCircle(Brush.radialGradient(listOf(C.paperHi, Color.Transparent), Offset(size.width / 2, size.height / 2), size.height * .55f))
            },
            contentAlignment = Alignment.Center,
        ) { ThingPic(k, Modifier.size(180.dp)) }
        Text(
            if (holders.size > 1) {
                // The thing has been in the family from the first holder's birth to the last holder's death (or today).
                val from = holders.minOf { it.year }
                val to = holders.maxOf { it.year + it.death }
                val y = to - from
                w.t("Переходила из рук в руки: ${w.n(holders.size, "поколение", "поколения", "поколений", "", "")}, ${w.n(y, "год", "года", "лет", "", "")} — с ${w.year(from)} по ${w.year(to)}.",
                    "Passed from hand to hand: ${holders.size} generations, $y years, from ${w.year(from)} to ${w.year(to)}.")
            }
            else w.t("Одна жизнь.", "One life."),
            style = T.Italic, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        for ((i, l) in holders.withIndex()) {
            val era = World.ERAS[l.eraI]
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // The line of descent: a bead per holder, joined by a thread.
                Box(Modifier.width(34.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    if (holders.size > 1) Box(
                        Modifier.padding(top = if (i == 0) 23.dp else 0.dp).width(2.dp)
                            .then(if (i == holders.lastIndex) Modifier.height(23.dp) else Modifier.fillMaxHeight()).background(C.line),
                    )
                    Box(Modifier.padding(top = 10.dp).size(26.dp).clip(CircleShape).background(Color(era.color)).border(2.dp, C.card, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = T.Small.copy(color = C.light))
                    }
                }
                Card(Modifier.padding(start = 6.dp, bottom = 8.dp), onClick = { onLife(l.id) }, frame = Frame.LEAF, seed = l.id.toInt()) {
                    Text(w.x(l.name), style = T.BodyB)
                    Text("${w.year(l.year)} – ${w.year(l.year + l.death)} · ${w.x(World.region(l.region, l.eraI).name)}", style = T.Small)
                    Text(w.x(l.epitaph), style = T.Write.copy(fontSize = T.Body.fontSize, lineHeight = T.Body.lineHeight), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
