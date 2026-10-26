package app.lot.ui.chest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lot.domain.Things
import app.lot.domain.World
import app.lot.ui.Home
import app.lot.ui.art.ThingArt.thing
import app.lot.ui.components.Card
import app.lot.ui.components.Gap
import app.lot.ui.components.Sheet
import app.lot.ui.components.TopBar
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
            Text(w.t("Сундук пуст.", "The chest is empty."), style = T.Italic, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(40.dp))
            return
        }
        LazyVerticalGrid(
            GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val list = h.s.things.reversed()
            val handed = list.count { h.s.holders(it.id).size > 1 }
            if (handed > 0) item(span = { GridItemSpan(3) }) {
                Text(w.t("Переходили по наследству: $handed", "Handed down: $handed"), style = T.Small)
            }
            items(list, key = { it.id }) { t ->
                val k = Things.of(t.key)
                val holders = h.s.holders(t.id)
                val shape = RoundedCornerShape(4.dp)
                Column(
                    Modifier.clip(shape).background(C.card).border(1.dp, if (holders.size > 1) C.ink2 else C.line, shape).clickable { onThing(t.id) }.padding(horizontal = 6.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Canvas(Modifier.size(54.dp)) { thing(k.shape) }
                    Text(w.x(k.name).replaceFirstChar { it.uppercase() }, style = T.Small.copy(color = C.ink, textAlign = TextAlign.Center), minLines = 2, maxLines = 3)
                    Text(
                        if (holders.size > 1) w.n(holders.size, "поколение", "поколения", "поколений", "generation", "generations")
                        else holders.firstOrNull()?.let { w.year(it.year) } ?: "",
                        style = T.Small.copy(fontSize = T.Label.fontSize, color = if (holders.size > 1) C.red else C.muted),
                    )
                    if (holders.size > 1) Text(
                        "${w.year(holders.minOf { it.year })} – ${w.year(holders.maxOf { it.year + it.death })}",
                        style = T.Small.copy(fontSize = T.Label.fontSize, color = C.muted, textAlign = TextAlign.Center),
                    )
                }
            }
        }
    }
}

/** One thing and everyone who held it. */
@Composable
fun ThingSheet(h: Home, id: Long, onLife: (Long) -> Unit, onClose: () -> Unit) {
    val w = h.w
    val t = h.s.thing(id) ?: return onClose()
    val k = Things.of(t.key)
    val holders = h.s.holders(id)
    Sheet(w.x(k.name).replaceFirstChar { it.uppercase() }, onClose, w.t("Закрыть", "Close")) {
        Canvas(Modifier.size(110.dp).align(Alignment.CenterHorizontally)) { thing(k.shape) }
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
            Card(onClick = { onLife(l.id) }) {
                Row(verticalAlignment = Alignment.Top) {
                    Text("${i + 1}", style = T.WriteAge, modifier = Modifier.width(28.dp))
                    Column(Modifier.weight(1f)) {
                        Text(w.x(l.name), style = T.BodyB)
                        Text("${w.year(l.year)} – ${w.year(l.year + l.death)} · ${w.x(World.region(l.region, l.eraI).name)}", style = T.Small)
                        Text(w.x(l.epitaph), style = T.Write.copy(fontSize = T.Body.fontSize, lineHeight = T.Body.lineHeight), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            Gap(8)
        }
    }
}
