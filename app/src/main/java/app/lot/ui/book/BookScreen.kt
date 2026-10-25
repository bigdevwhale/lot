package app.lot.ui.book

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.lot.BuildConfig
import app.lot.container
import app.lot.data.LifeRow
import app.lot.domain.Choice
import app.lot.domain.Entry
import app.lot.domain.Heir
import app.lot.domain.Mark
import app.lot.domain.Page
import app.lot.domain.Things
import app.lot.domain.Tx
import app.lot.ui.Home
import app.lot.ui.Words
import app.lot.ui.art.ThingArt.thing
import app.lot.ui.components.Btn
import app.lot.ui.components.Chip
import app.lot.ui.components.Field
import app.lot.ui.components.Gap
import app.lot.ui.components.Kind
import app.lot.ui.components.Label
import app.lot.ui.components.LocalToast
import app.lot.ui.components.Rule
import app.lot.ui.components.TopBar
import app.lot.ui.theme.C
import app.lot.ui.theme.T

/** The register of the life being lived: an entry per page turned, a fork with its doors, and at the end an epitaph. */
@Composable
fun BookScreen(h: Home, onDraw: (Long?) -> Unit, onArchive: () -> Unit) {
    val w = h.w
    val c = LocalContext.current.container
    val page = h.s.page
    val cur = h.s.cur
    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(w.t("Книга", "The book")) {
            if (h.s.lives.isNotEmpty()) Chip(w.t("Все книги", "All books"), false, onClick = onArchive)
        }
        if (page == null || cur == null) {
            Text(w.t("Книга пуста. Сначала вытяни жребий.", "The book is empty. Draw a lot first."), style = T.Italic, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 40.dp))
            Box(Modifier.padding(horizontal = 16.dp)) { Btn(w.t("К мешку", "To the sack")) { onDraw(null) } }
            return
        }
        val scroll = rememberScrollState()
        // Entries written since the screen opened run in ink; the rest are already dry.
        var dry by remember(cur.seed) { mutableIntStateOf(page.entries.size) }
        LaunchedEffect(page.entries.size, page.pending, page.end) { scroll.animateScrollTo(scroll.maxValue) }
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(bottom = 12.dp)) {
            Register(page, w, h.s.lives.firstOrNull { it.id == cur.lifeId }?.gen ?: ((page.birth.heir?.gen ?: 0) + 1), cur.witness, dry)
            page.pending?.let { ChoiceCard(it, w) { i -> dry = page.entries.size; c.write { repo.pick(i) } } }
            page.end?.let {
                val row = cur.lifeId?.let { id -> h.s.byId[id] }
                if (row != null) EndCard(h, row, onThing = null)
                Column(Modifier.padding(horizontal = 16.dp)) {
                    if (row != null && Heir.possible(row.year, row.kids)) { Gap(4); Btn(w.t("Тянуть жребий правнука", "Draw a great-grandchild's lot")) { onDraw(row.id) } }
                    Gap(8)
                    Btn(w.t("Новый жребий", "A new lot"), kind = if (row != null && Heir.possible(row.year, row.kids)) Kind.GHOST else Kind.INK) { onDraw(null) }
                }
            }
        }
        if (page.canTurn) Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Btn(w.t("Дальше", "Turn the page")) { dry = page.entries.size; c.write { repo.turn() } }
        }
    }
}

/** The parish-book page: a ruled sheet, the name at the top, ages in red, entries in the registrar's hand. */
@Composable
fun Register(page: Page, w: Words, gen: Int, witness: Boolean, dry: Int = Int.MAX_VALUE) {
    val b = page.birth
    Register(b.name, b.year, page.end?.age, b.region.name, gen, witness, page.entries, w, dry)
}

/** The register from what a finished life kept: its name, years and the entries as written. */
@Composable
fun Register(name: Tx, year: Int, endAge: Int?, region: Tx, gen: Int, witness: Boolean, entries: List<Entry>, w: Words, dry: Int = Int.MAX_VALUE) {
    Column(
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
            .drawBehind {
                drawRect(C.ink2.copy(alpha = .12f), Offset(0f, 3.dp.toPx()), size)
                drawRect(C.page)
                drawLine(C.red.copy(alpha = .3f), Offset(54.dp.toPx(), 0f), Offset(54.dp.toPx(), size.height), 1f)
            }
            .border(1.dp, C.line).padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(Modifier.fillMaxWidth().background(C.page)) {
            Column {
                Text(w.x(name), style = T.Heading)
                Text(listOfNotNull(w.year(year) + (endAge?.let { " – " + w.year(year + it) } ?: ""), w.x(region),
                    if (gen > 1) w.gen(gen) else null, if (witness) w.t("свидетель", "witness") else null).joinToString(" · "), style = T.Small)
            }
        }
        Rule(Modifier.padding(top = 6.dp, bottom = 4.dp), C.ink2)
        for ((i, e) in entries.withIndex()) EntryRow(e, w, fresh = i >= dry && !BuildConfig.SHOTS, delayIndex = (i - dry).coerceAtLeast(0))
    }
}

@Composable
private fun EntryRow(e: Entry, w: Words, fresh: Boolean, delayIndex: Int) {
    val ink = remember(e) { Animatable(if (fresh) 0f else 1f) }
    LaunchedEffect(e) { if (fresh) { kotlinx.coroutines.delay(delayIndex * 500L); ink.animateTo(1f, tween(900 + e.text.of(w.ru).length * 12, easing = LinearEasing)) } }
    Column(Modifier.fillMaxWidth()) {
        if (e.mark == Mark.DEATH) Rule(Modifier.padding(top = 6.dp, bottom = 2.dp), C.ink2)
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text("${e.age}", style = T.WriteAge, textAlign = TextAlign.End, modifier = Modifier.width(30.dp))
            Box(Modifier.width(14.dp))
            var lines by remember { mutableStateOf<TextLayoutResult?>(null) }
            Text(
                (if (e.mark == Mark.MEET) "✦ " else "") + w.x(e.text),
                onTextLayout = { lines = it },
                style = T.Write.copy(color = when (e.mark) { Mark.HIST, Mark.MEET -> C.red; Mark.DEATH -> C.ink; Mark.PLAIN -> C.blue }),
                modifier = Modifier.weight(1f).drawBehind {
                    // The ruled lines of the register, under each written line.
                    val l = lines ?: return@drawBehind
                    for (i in 0 until l.lineCount) { val y = l.getLineBottom(i) - 2.dp.toPx(); drawLine(C.rule, Offset(-44.dp.toPx(), y), Offset(size.width, y), 1f) }
                }.drawWithContent {
                    // The pen writes one line after another, left to right.
                    val l = lines
                    if (l == null || ink.value >= 1f) { drawContent(); return@drawWithContent }
                    val widths = (0 until l.lineCount).map { l.getLineRight(it) }
                    var left = widths.sum() * ink.value
                    for (i in widths.indices) {
                        if (left <= 0f) break
                        clipRect(0f, l.getLineTop(i), minOf(widths[i], left), l.getLineBottom(i)) { this@drawWithContent.drawContent() }
                        left -= widths[i]
                    }
                },
            )
        }
    }
}

/** A fork: the open doors to choose from, and the shut ones with the reason they are shut. */
@Composable
fun ChoiceCard(ch: Choice, w: Words, onPick: (Int) -> Unit) {
    var picked by remember(ch) { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(C.card).border(1.dp, C.ink2, RoundedCornerShape(4.dp)).padding(12.dp)) {
        Text(w.x(ch.q), style = T.Heading)
        Gap(4)
        for ((i, d) in ch.doors.withIndex()) {
            val shape = RoundedCornerShape(3.dp)
            Column(
                Modifier.padding(vertical = 4.dp).fillMaxWidth().clip(shape)
                    .then(if (d.open) Modifier.background(C.light).border(1.dp, C.line, shape).clickable(enabled = !picked) { picked = true; onPick(i) }
                    else Modifier.background(C.shut).drawBehind {
                        val step = 9.dp.toPx()
                        var x = -size.height
                        while (x < size.width) { drawLine(C.line.copy(alpha = .45f), Offset(x, size.height), Offset(x + size.height, 0f), 1.5f); x += step }
                    })
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(w.x(d.text), style = T.Body.copy(color = if (d.open) C.ink else C.muted, textDecoration = if (d.open) null else TextDecoration.LineThrough))
                d.why?.let { Text(w.x(it), style = T.Italic.copy(color = C.red, fontSize = T.Small.fontSize, lineHeight = T.Small.lineHeight), modifier = Modifier.padding(top = 2.dp)) }
            }
        }
    }
}

/** The end of a life: the epitaph, the thing that goes to the chest, and a question for the one reading. */
@Composable
fun EndCard(h: Home, row: LifeRow, onThing: ((Long) -> Unit)?) {
    val w = h.w
    val c = LocalContext.current.container
    val toast = LocalToast.current
    val thing = h.s.thing(row.thingId)
    val holders = h.s.holders(row.thingId)
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(C.card).border(1.dp, C.line, RoundedCornerShape(4.dp)).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Label(w.t("Эпитафия", "Epitaph"))
        Text(w.x(row.epitaph), style = T.Epitaph, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        if (thing != null) {
            Rule(Modifier.padding(vertical = 12.dp))
            val kind = Things.of(thing.key)
            Row(Modifier.fillMaxWidth().then(if (onThing != null) Modifier.clickable { onThing(thing.id) } else Modifier), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(56.dp)) { thing(kind.shape) }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Label(w.t("В сундук", "To the chest"))
                    Text(w.x(kind.name).replaceFirstChar { it.uppercase() }, style = T.BodyB)
                    val n = holders.indexOfFirst { it.id == row.id } + 1
                    Text(if (n <= 1) w.t("первый владелец", "first holder") else w.t("$n-й владелец: вещь перешла по наследству", "holder no. $n: handed down"), style = T.Small)
                }
            }
        }
        Rule(Modifier.padding(vertical = 12.dp))
        Column(Modifier.fillMaxWidth()) {
            Label(w.t("Вопрос тебе", "A question for you"))
            Text(w.x(row.bridge), style = T.Italic.copy(color = C.ink), modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
            var text by remember(row.id) { mutableStateOf(row.answer) }
            Field(text, { text = it }, w.t("Можно не отвечать…", "You don't have to answer…"))
            if (text.trim() != row.answer) {
                Gap(8)
                Btn(w.t("Записать в тетрадку", "Write it in the notebook"), kind = Kind.SOFT) {
                    c.write { repo.answer(row.id, text) }
                    toast.show(w.t("Записано. Тетрадка — в атласе.", "Written down. The notebook is in the atlas."))
                }
            }
        }
    }
}
