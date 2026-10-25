package app.lot.ui.draw

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.lot.BuildConfig
import app.lot.container
import app.lot.domain.Birth
import app.lot.domain.Heir
import app.lot.ui.Home
import app.lot.ui.Words
import app.lot.ui.art.Sack.stage
import app.lot.ui.components.Btn
import app.lot.ui.components.Card
import app.lot.ui.components.Chip
import app.lot.ui.components.Flow
import app.lot.ui.components.Gap
import app.lot.ui.components.Kind
import app.lot.ui.components.Label
import app.lot.ui.components.Rule
import app.lot.ui.components.TopBar
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
        TopBar(w.t("Жребий", "Lot"), w.t("Ты — один из ста семнадцати миллиардов людей, когда-либо живших на Земле. Кем именно — решит мешок.",
            "You are one of the 117 billion people who have ever lived. Which one, the sack decides.")) {
            Icon(Icons.Outlined.Settings, w.t("Настройки", "Settings"), tint = C.muted, modifier = Modifier.clip(CircleShape).clickable(onClick = onSettings).padding(10.dp))
        }
        val birth = page?.birth
        val tag = birth?.let { w.tag(it) }
        Canvas(Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 24.dp)) { stage(shake.value, rise.value, tag, cur?.seed ?: 0L, tm) }

        Column(Modifier.padding(horizontal = 16.dp)) {
            val ended = page?.end != null
            if (page == null || ended) {
                val last = cur?.lifeId?.let { h.s.byId[it] }
                if (last != null && page != null) {
                    Reveal(page.birth, w, h, ended = true)
                    Gap(8)
                }
                if (last != null && Heir.possible(last.year, last.kids)) {
                    Btn(w.t("Тянуть жребий правнука", "Draw a great-grandchild's lot")) { onDraw(last.id) }
                    Gap(8)
                }
                Btn(w.t("Тянуть жребий", "Draw a lot"), kind = if (last != null && Heir.possible(last.year, last.kids)) Kind.GHOST else Kind.INK) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDraw(null)
                }
                Gap(10)
                Flow {
                    Chip(w.t("жить и выбирать", "live and choose"), !h.settings.witness) { c.write { settings.update { it.copy(witness = false) } } }
                    Chip(w.t("быть свидетелем", "be a witness"), h.settings.witness) { c.write { settings.update { it.copy(witness = true) } } }
                }
                Text(
                    if (h.settings.witness) w.t("Свидетель не выбирает: за него решает судьба. Ты только читаешь.", "A witness does not choose: fate decides. You only read.")
                    else w.t("На развилках выбираешь ты — из тех дверей, что открыты для тебя.", "At each fork you choose, from the doors that are open to you."),
                    style = T.Small, modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                AnimatedVisibility(shown, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 6 }) {
                    Column {
                        Reveal(page.birth, w, h, ended = false)
                        Gap(10)
                        Btn(if (page.entries.size <= 2) w.t("Открыть книгу", "Open the book") else w.t("Читать дальше", "Read on"), onClick = onBook)
                    }
                }
            }
            Gap(18)
            val lives = h.s.lives
            if (lives.isNotEmpty()) {
                Rule()
                Text(
                    w.t("Прожито жизней: ${lives.size} · лет на всех: ${lives.sumOf { it.death }}", "Lives lived: ${lives.size} · years in all: ${lives.sumOf { it.death }}"),
                    style = T.Small, modifier = Modifier.padding(vertical = 10.dp),
                )
            }
            Gap(10)
        }
    }
}

/** Who the lot made you, and how likely that was. */
@Composable
fun Reveal(b: Birth, w: Words, h: Home, ended: Boolean) {
    Card {
        Label("${w.x(b.era.name)} · ${w.x(b.era.span)}")
        Text(w.x(b.name), style = T.Heading.copy(fontSize = T.Title.fontSize, lineHeight = T.Title.lineHeight), modifier = Modifier.padding(top = 4.dp))
        Text(w.born(b), style = T.Body, modifier = Modifier.padding(top = 4.dp))
        b.heir?.let { Text(w.t("Жребий правнука: ${it.name.ru}, ${w.gen(it.gen + 1)}.", "A great-grandchild's lot: ${it.name.en}, ${w.gen(it.gen + 1)}."), style = T.Italic, modifier = Modifier.padding(top = 4.dp)) }
        if (ended) {
            val end = h.s.page?.end
            if (end != null) Text(w.t("Прожито: ${w.lived(end.age)}.", "Lived: ${w.lived(end.age)}."), style = T.BodyB, modifier = Modifier.padding(top = 6.dp))
        } else {
            Gap(10)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Odd(w.pct(b.share), w.t("всех живших — такие, как ты", "of everyone who ever lived were like you"), Modifier.weight(1f))
                Odd(w.people(b.share), w.t("родились в такой же доле", "were born to the same lot"), Modifier.weight(1f))
            }
            Gap(8)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Odd(w.pct(b.survive5), w.t("детей здесь доживают до пяти", "of children here live to five"), Modifier.weight(1f))
                Odd(w.pct(b.literacy), w.t("вокруг умеют читать", "of the people around can read"), Modifier.weight(1f))
            }
            val before = h.s.lives.count { it.eraI == b.era.i }
            if (before > 0) Text(
                w.t("В эту эпоху ты рождаешься уже не впервые: до этого ${w.n(before, "раз", "раза", "раз", "", "")}.",
                    "You have been born in this era ${if (before == 1) "once" else "$before times"} before."),
                style = T.Small, modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun Odd(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Rule()
        Text(value, style = T.Big, modifier = Modifier.padding(top = 6.dp))
        Text(label, style = T.Small)
    }
}
