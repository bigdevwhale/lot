package app.lot.ui.draw

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lot.BuildConfig
import app.lot.container
import app.lot.domain.Birth
import app.lot.domain.Heir
import app.lot.domain.Sex
import app.lot.domain.Wish
import app.lot.domain.World
import app.lot.ui.Home
import app.lot.ui.Words
import app.lot.ui.art.Ico
import app.lot.ui.art.Pic
import app.lot.ui.art.Pics
import app.lot.ui.art.Sack.stage
import app.lot.ui.art.hasPic
import app.lot.ui.art.rememberSackArt
import app.lot.ui.components.Btn
import app.lot.ui.components.Card
import app.lot.ui.components.Chip
import app.lot.ui.components.EraCaption
import app.lot.ui.components.EraStrip
import app.lot.ui.components.Flow
import app.lot.ui.components.Gap
import app.lot.ui.components.Kind
import app.lot.ui.components.Label
import app.lot.ui.components.Meter
import app.lot.ui.components.Paper
import app.lot.ui.components.Rule
import app.lot.ui.components.Segmented
import app.lot.ui.components.Stat
import app.lot.ui.components.TopBar
import app.lot.ui.components.roman
import app.lot.ui.theme.C
import app.lot.ui.theme.T

/** The sack: pull a lot, watch the tag rise, read who you are. */
@Composable
fun DrawScreen(h: Home, onDraw: (Long?) -> Unit, onBook: () -> Unit, onSettings: () -> Unit) {
    val w = h.w
    val ctx = LocalContext.current
    val c = ctx.container
    val haptic = LocalHapticFeedback.current
    val cur = h.s.cur
    val page = h.s.page
    val tm = rememberTextMeasurer()
    val art = rememberSackArt()
    val shake = remember { Animatable(0f) }
    val rise = remember { Animatable(if (cur != null) 1f else 0f) }
    var shown by remember { mutableStateOf(cur != null) }

    LaunchedEffect(cur?.seed, cur?.drawnAt) {
        val fresh = cur != null && System.currentTimeMillis() - cur.drawnAt < 4_000 && !BuildConfig.SHOTS
        if (!fresh) { rise.snapTo(if (cur != null) 1f else 0f); shown = cur != null; return@LaunchedEffect }
        shown = false
        rise.snapTo(0f)
        if (cur.heirOf == null) for ((i, a) in listOf(-6f, 5f, -4f, 3f, -1.5f, 0f).withIndex()) shake.animateTo(a, tween(if (i == 0) 120 else 140))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        rise.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        shown = true
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(w.t("Жребий", "Lot"), w.t("Ты — один из ста семнадцати миллиардов людей, когда-либо живших на Земле. Кем именно — решит жребий.",
            "You are one of the 117 billion people who have ever lived. Which one, the lot decides.")) {
            Icon(Icons.Outlined.Settings, w.t("Настройки", "Settings"), tint = C.muted, modifier = Modifier.clip(CircleShape).clickable(onClick = onSettings).padding(10.dp))
        }
        val birth = page?.birth
        val tag = birth?.let { w.tag(it) }
        // The sack itself draws a lot when there is none being lived.
        val free = page == null || page.end != null
        Canvas(
            Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 24.dp)
                .then(if (free) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onClickLabel = w.t("Тянуть жребий", "Draw a lot")) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDraw(null)
                } else Modifier),
        ) { stage(shake.value, rise.value, tag, cur?.seed ?: 0L, tm, art) }

        Column(Modifier.padding(horizontal = 16.dp)) {
            val ended = page?.end != null
            if (page == null || ended) {
                val last = cur?.lifeId?.let { h.s.byId[it] }
                if (last != null && page != null) {
                    Reveal(page.birth, w, h, ended = true)
                    Gap(8)
                }
                if (last != null && Heir.possible(last.year, last.kids)) {
                    Btn(w.t("Тянуть жребий правнука", "Draw a great-grandchild's lot"), icon = "tab_chest") { onDraw(last.id) }
                    Gap(8)
                }
                Btn(w.t("Тянуть жребий", "Draw a lot"), kind = if (last != null && Heir.possible(last.year, last.kids)) Kind.GHOST else Kind.INK, icon = "tab_draw") {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDraw(null)
                }
                Gap(10)
                WishPanel(h)
                Gap(10)
                Segmented(
                    listOf(w.t("жить и выбирать", "live and choose") to "signpost", w.t("быть свидетелем", "be a witness") to "lantern"),
                    if (h.settings.witness) 1 else 0,
                ) { i -> c.write { settings.update { it.copy(witness = i == 1) } } }
                Text(
                    if (h.settings.witness) w.t("Свидетель не выбирает: за него решает судьба. Ты только читаешь.", "A witness does not choose: fate decides. You only read.")
                    else w.t("На развилках выбираешь ты — из тех дверей, что открыты для тебя.", "At each fork you choose, from the doors that are open to you."),
                    style = T.Small, modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            } else {
                AnimatedVisibility(shown, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 6 }) {
                    Column {
                        Reveal(page.birth, w, h, ended = false)
                        Gap(10)
                        Btn(if (page.entries.size <= 2) w.t("Открыть книгу", "Open the book") else w.t("Читать дальше", "Read on"), icon = "tab_book", onClick = onBook)
                        Gap(8)
                        var ask by remember { mutableStateOf(false) }
                        Btn(w.t("Тянуть другой жребий", "Draw another lot"), kind = Kind.GHOST) { ask = true }
                        if (ask) AlertDialog(
                            onDismissRequest = { ask = false },
                            containerColor = C.card,
                            title = { Text(w.t("Оставить эту жизнь?", "Leave this life?"), style = T.Heading) },
                            text = {
                                Text(w.t("${w.x(page.birth.name)} останется недочитанной книгой: в архив, атлас и сундук она не попадёт.",
                                    "${w.x(page.birth.name)} will stay an unfinished book: it won't go to the archive, the atlas or the chest."), style = T.Body)
                            },
                            confirmButton = { Text(w.t("Тянуть другой", "Draw another"), style = T.Button.copy(color = C.red), modifier = Modifier.clickable { ask = false; onDraw(null) }.padding(10.dp)) },
                            dismissButton = { Text(w.t("Читать дальше", "Read on"), style = T.Button.copy(color = C.ink), modifier = Modifier.clickable { ask = false }.padding(10.dp)) },
                        )
                        Gap(10)
                        WishPanel(h)
                    }
                }
            }
            Gap(18)
            val lives = h.s.lives
            if (lives.isNotEmpty()) {
                Card {
                    Row(Modifier.fillMaxWidth()) {
                        Stat("people", "${lives.size}", w.t("прожито жизней", "lives lived"), Modifier.weight(1f))
                        Stat("chronicle", "${lives.sumOf { it.death }}", w.t("лет на всех", "years in all"), Modifier.weight(1f))
                        Stat("tab_chest", "${h.s.things.count { h.s.holders(it.id).size > 1 }}", w.t("вещей в роду", "heirlooms"), Modifier.weight(1f))
                    }
                }
            }
            Gap(16)
        }
    }
}

/**
 * The optional wish before the draw: an era, a part of the world, a sex. On the screen it is one line that says what is
 * asked; the choice itself opens in a sheet from below, where all of it fits without scrolling. Combinations that never
 * existed (Russia in the Stone Age) are faded and shut.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishPanel(h: Home) {
    val w = h.w
    val c = LocalContext.current.container
    val wish = h.settings.wish
    var open by rememberSaveable { mutableStateOf(false) }
    fun set(f: (Wish) -> Wish) = c.write { settings.update { it.copy(wish = f(it.wish)) } }
    val asked = listOfNotNull(
        wish.eraI?.let { w.x(World.ERAS[it].name) },
        World.area(wish.area)?.let { w.x(it.name) },
        wish.sex?.let { if (it == Sex.M) w.t("мальчик", "a boy") else w.t("девочка", "a girl") },
    )
    Card(pad = false) {
        Row(Modifier.fillMaxWidth().clickable { open = true }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (hasPic("icon/globe")) { Ico("globe", Modifier.size(24.dp), tint = if (asked.isEmpty()) C.muted else C.red); Spacer(Modifier.width(10.dp)) }
            Column(Modifier.weight(1f)) {
                Text(if (asked.isEmpty()) w.t("Жребий из всех людей", "A lot from everyone") else w.t("Жребий из выбранного", "A lot from your choice"), style = T.BodyB)
                Text(if (asked.isEmpty()) w.t("Можно сузить: эпоха, место, пол", "You may narrow it: era, place, sex") else asked.joinToString(" · "),
                    style = T.Small.copy(color = if (asked.isEmpty()) C.muted else C.red))
            }
            Text(if (asked.isEmpty()) w.t("выбрать", "choose") else w.t("изменить", "change"), style = T.Small.copy(color = C.red))
        }
    }
    if (!open) return
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = { open = false }, sheetState = sheet, containerColor = C.paper, contentColor = C.ink) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 18.dp, end = 18.dp, bottom = 12.dp)) {
            Text(w.t("Сузить жребий", "Narrow the lot"), style = T.Heading)
            Text(w.t("Внутри выбранного жребий тянет по тем же честным весам.", "Within what you choose the lot keeps the same fair weights."), style = T.Small)

            Label(w.t("Эпоха", "Era"), Modifier.padding(top = 14.dp, bottom = 6.dp))
            EraStrip(w, wish.eraI, w.t("любая", "any"), "∗", main = { roman(it) }, open = { World.fits(it, wish.area) }) { i -> set { it.copy(eraI = i) } }
            EraCaption(w, wish.eraI, w.t("Любая эпоха, по честным весам", "Any age, by the fair weights"))

            Label(w.t("Где", "Where"), Modifier.padding(top = 14.dp, bottom = 6.dp))
            // Three to a row: the whole world in four short rows.
            val places = listOf<Pair<String?, String>>(null to w.t("где угодно", "anywhere")) + World.AREAS.map { it.key to w.x(it.name) }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                for (row in places.chunked(3)) Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for ((key, name) in row) Place(name, wish.area == key, World.fits(wish.eraI, key), Modifier.weight(1f)) { set { it.copy(area = key) } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Label(w.t("Кем родиться", "Born as"), Modifier.padding(top = 14.dp, bottom = 6.dp))
            Segmented(
                listOf(w.t("как выпадет", "as it falls") to null, w.t("мальчиком", "a boy") to null, w.t("девочкой", "a girl") to null),
                when (wish.sex) { null -> 0; Sex.M -> 1; Sex.F -> 2 },
            ) { i -> set { it.copy(sex = when (i) { 1 -> Sex.M; 2 -> Sex.F; else -> null }) } }

            Gap(18)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(w.t("Сбросить", "Clear"), Modifier.weight(1f), kind = Kind.GHOST, enabled = !wish.any) { set { Wish() } }
                Btn(w.t("Готово", "Done"), Modifier.weight(1f)) { open = false }
            }
        }
    }
}

/** A part of the world to be born in: a small tile, shut when that part had no one in the chosen age. */
@Composable
private fun Place(name: String, on: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    val bg by animateColorAsState(if (on) C.ink else C.card, label = "place")
    Box(
        modifier.height(40.dp).clip(shape).background(bg).border(1.dp, if (on) C.ink else C.line, shape)
            .selectable(on, enabled = enabled, role = Role.RadioButton, onClick = onClick).alpha(if (enabled) 1f else .35f).padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(name, style = T.Small.copy(color = if (on) C.light else C.ink, textAlign = TextAlign.Center, lineHeight = 14.sp), maxLines = 2)
    }
}

/** Who the lot made you, and how likely that was. */
@Composable
fun Reveal(b: Birth, w: Words, h: Home, ended: Boolean) {
    Card(pad = false) {
        // The era as a plate from an old history book, with its name on a coloured ribbon.
        val plate = Pics.era(b.era, b.region)
        val painted = hasPic(plate)
        Box(Modifier.fillMaxWidth().height(if (painted) 150.dp else 44.dp).background(Brush.verticalGradient(listOf(C.paperHi, C.card)))) {
            Pic(plate, Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp))
            Box(Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(12.dp)).background(Color(b.era.color)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                Text(w.x(b.era.name).uppercase(), style = T.Label.copy(color = C.light))
            }
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Label(w.x(b.era.span))
            Text(w.x(b.name), style = T.Heading.copy(fontSize = T.Title.fontSize, lineHeight = T.Title.lineHeight), modifier = Modifier.padding(top = 4.dp))
            Text(w.born(b), style = T.Body, modifier = Modifier.padding(top = 4.dp))
            h.s.cur?.wish?.takeIf { !it.any && b.heir == null }?.let { wish ->
                val asked = listOfNotNull(wish.eraI?.let { w.x(World.ERAS[it].name) }, World.area(wish.area)?.let { w.x(it.name) },
                    wish.sex?.let { if (it == Sex.M) w.t("мальчик", "a boy") else w.t("девочка", "a girl") })
                Text(w.t("Жребий из выбранного: ", "A lot from your choice: ") + asked.joinToString(" · "), style = T.Italic, modifier = Modifier.padding(top = 4.dp))
            }
            b.heir?.let { Text(w.t("Жребий правнука: ${it.name.ru}, ${w.gen(it.gen + 1)}.", "A great-grandchild's lot: ${it.name.en}, ${w.gen(it.gen + 1)}."), style = T.Italic, modifier = Modifier.padding(top = 4.dp)) }
            if (ended) {
                val end = h.s.page?.end
                if (end != null) Text(w.t("Прожито: ${w.lived(end.age)}.", "Lived: ${w.lived(end.age)}."), style = T.BodyB, modifier = Modifier.padding(top = 6.dp))
            } else {
                Gap(12)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Odd("share", w.pct(b.share), w.t("всех живших — такие, как ты", "of everyone who ever lived were like you"), null, Modifier.weight(1f))
                    Odd("people", w.people(b.share), w.t("родились в такой же доле", "were born to the same lot"), null, Modifier.weight(1f))
                }
                Gap(10)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Odd("cradle", w.pct(b.survive5), w.t("детей здесь доживают до пяти", "of children here live to five"), b.survive5.toFloat(), Modifier.weight(1f))
                    Odd("literacy", w.pct(b.literacy), w.t("вокруг умеют читать", "of the people around can read"), b.literacy.toFloat(), Modifier.weight(1f))
                }
                val before = h.s.lives.count { it.eraI == b.era.i }
                if (before > 0) Text(
                    w.t("В эту эпоху ты рождаешься уже не впервые: до этого ${w.n(before, "раз", "раза", "раз", "", "")}.",
                        "You have been born in this era ${if (before == 1) "once" else "$before times"} before."),
                    style = T.Small, modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** One of the odds: an icon, the number, a bar where it is a share of the people around, and what it means. */
@Composable
private fun Odd(icon: String, value: String, label: String, f: Float?, modifier: Modifier) {
    val shape = remember { Paper.ticket() }
    Column(modifier.clip(shape).background(C.paperHi.copy(alpha = .8f)).border(1.dp, C.line.copy(alpha = .7f), shape).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasPic("icon/$icon")) { Ico(icon, Modifier.size(20.dp), tint = C.muted); Spacer(Modifier.width(6.dp)) }
            Text(value, style = T.Big)
        }
        if (f != null) Meter(f, if (f < .5f) C.red else C.ink2, Modifier.padding(vertical = 5.dp), 5.dp)
        Text(label, style = T.Small)
    }
}
