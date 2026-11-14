package app.lot.ui.book

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lot.container
import app.lot.domain.Things
import app.lot.domain.World
import app.lot.ui.Home
import app.lot.ui.art.ThingPic
import app.lot.ui.components.Card
import app.lot.ui.components.EraCaption
import app.lot.ui.components.EraStrip
import app.lot.ui.components.Frame
import app.lot.ui.components.Gap
import app.lot.ui.components.Label
import app.lot.ui.components.Sheet
import app.lot.ui.theme.C
import app.lot.ui.theme.T

/** Every life lived, newest first. */
@Composable
fun ArchiveSheet(h: Home, onLife: (Long) -> Unit, onClose: () -> Unit) {
    val w = h.w
    Sheet(w.t("Все книги", "All books"), onClose, w.t("Закрыть", "Close")) {
        Text(w.t("Каждую можно перечитать: жизнь записана целиком.", "Each can be read again: the whole life is kept."), style = T.Small)
        Gap(8)
        // With many lives, the archive can be read by era: a strip of the ages, oldest on the left, every one in sight.
        var era by rememberSaveable { mutableStateOf<Int?>(null) }
        if (h.s.lives.map { it.eraI }.distinct().size > 1) {
            EraStrip(w, era, w.t("все", "all"), "${h.s.lives.size}", main = { i -> "${h.s.lives.count { it.eraI == i }}" },
                open = { i -> h.s.lives.any { it.eraI == i } }) { era = it }
            EraCaption(w, era, w.t("Все эпохи, новые книги сверху", "Every age, the newest books first"))
            Gap(10)
        }
        for (l in h.s.lives.reversed().filter { era == null || it.eraI == era }) {
            val era = World.ERAS[l.eraI]
            Card(onClick = { onLife(l.id) }, pad = false, frame = Frame.LEAF, seed = l.id.toInt()) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(Modifier.width(6.dp).fillMaxHeight().background(Color(era.color)))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Label(w.x(era.name))
                        Text(w.x(l.name), style = T.BodyB)
                        Text("${w.year(l.year)} – ${w.year(l.year + l.death)} · ${w.x(World.region(l.region, l.eraI).name)}" +
                            (if (l.gen > 1) " · ${w.gen(l.gen)}" else ""), style = T.Small)
                        Text(w.x(l.epitaph), style = T.Write.copy(fontSize = T.Body.fontSize), modifier = Modifier.padding(top = 4.dp))
                    }
                    h.s.thing(l.thingId)?.let { ThingPic(Things.of(it.key), Modifier.align(Alignment.CenterVertically).padding(end = 10.dp).size(54.dp)) }
                }
            }
            Gap(8)
        }
    }
}

/** One archived life, read again from its seed. */
@Composable
fun ReadSheet(h: Home, id: Long, onThing: (Long) -> Unit, onClose: () -> Unit) {
    val w = h.w
    val repo = LocalContext.current.container.repo
    val row = h.s.byId[id] ?: return onClose()
    val entries = remember(id, h.s.lives.size) { repo.book(row, h.s) }
    Sheet(w.t("Книга", "The book"), onClose, w.t("Закрыть", "Close")) {
        Column(Modifier.padding(horizontal = 0.dp)) {
            Register(row.name, row.year, row.death, World.region(row.region, row.eraI).name, row.gen, row.witness, entries, w)
            EndCard(h, row, onThing = onThing)
            Gap(16)
        }
    }
}
