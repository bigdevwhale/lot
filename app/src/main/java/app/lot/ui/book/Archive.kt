package app.lot.ui.book

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lot.container
import app.lot.domain.World
import app.lot.ui.Home
import app.lot.ui.components.Card
import app.lot.ui.components.Gap
import app.lot.ui.components.Sheet
import app.lot.ui.theme.T

/** Every life lived, newest first. */
@Composable
fun ArchiveSheet(h: Home, onLife: (Long) -> Unit, onClose: () -> Unit) {
    val w = h.w
    Sheet(w.t("Все книги", "All books"), onClose, w.t("Закрыть", "Close")) {
        Text(w.t("Каждую можно перечитать: жизнь записана целиком.", "Each can be read again: the whole life is kept."), style = T.Small)
        Gap(8)
        for (l in h.s.lives.reversed()) {
            Card(onClick = { onLife(l.id) }) {
                Text(w.x(l.name), style = T.BodyB)
                Text("${w.year(l.year)} – ${w.year(l.year + l.death)} · ${w.x(World.region(l.region, l.eraI).name)}" +
                    (if (l.gen > 1) " · ${w.gen(l.gen)}" else ""), style = T.Small)
                Text(w.x(l.epitaph), style = T.Write.copy(fontSize = T.Body.fontSize), modifier = Modifier.padding(top = 4.dp))
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
    val page = remember(id, h.s.lives.size) { repo.book(row, h.s) }
    Sheet(w.t("Книга", "The book"), onClose, w.t("Закрыть", "Close")) {
        Column(Modifier.padding(horizontal = 0.dp)) {
            Register(page, w, row.gen, row.witness)
            EndCard(h, row, onThing = onThing)
            Gap(16)
        }
    }
}
