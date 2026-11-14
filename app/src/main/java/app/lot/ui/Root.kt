package app.lot.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lot.R
import app.lot.container
import app.lot.data.Settings
import app.lot.data.State
import app.lot.ru
import app.lot.ui.art.Ico
import app.lot.ui.art.hasPic
import app.lot.ui.atlas.AtlasScreen
import app.lot.ui.book.ArchiveSheet
import app.lot.ui.book.BookScreen
import app.lot.ui.book.ReadSheet
import app.lot.ui.chest.ChestScreen
import app.lot.ui.chest.ThingSheet
import app.lot.ui.components.LocalToast
import app.lot.ui.components.ToastHost
import app.lot.ui.components.Toaster
import app.lot.ui.components.paper
import app.lot.ui.draw.DrawScreen
import app.lot.ui.onboarding.Onboarding
import app.lot.ui.settings.SettingsSheet
import app.lot.ui.theme.C
import app.lot.ui.theme.Ink
import app.lot.ui.theme.T

/** Everything a screen reads, at one moment. */
class Home(val s: State, val settings: Settings, val w: Words, val now: Long) {
    val ru get() = w.ru
}

enum class Tab { DRAW, BOOK, CHEST, ATLAS }

sealed interface Overlay {
    data object Settings : Overlay
    data object Archive : Overlay
    data class Read(val id: Long) : Overlay
    data class Thing(val id: Long) : Overlay
}

@Composable
fun LotRoot(settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val state by c.repo.state.collectAsStateWithLifecycle()
    val words = remember(ctx.ru) { Words(ctx.ru) }
    val toaster = remember { Toaster() }
    SideEffect { Ink.hand = settings.hand }
    LaunchedEffect(Unit) { c.repo.refresh() }
    if (!settings.onboarded) {
        CompositionLocalProvider(LocalToast provides toaster) { Box(Modifier.fillMaxSize()) { Onboarding(words); ToastHost(toaster) } }
        return
    }
    val ready by c.ready.collectAsStateWithLifecycle()
    val s = state?.takeIf { ready } ?: run { Box(Modifier.fillMaxSize().paper()); return }
    val home = Home(s, settings, words, System.currentTimeMillis())

    var tab by rememberSaveable { mutableStateOf(if (s.page?.end == null && s.page != null) Tab.BOOK else Tab.DRAW) }
    val overlays = remember { mutableStateListOf<Overlay>() }
    fun push(o: Overlay) = overlays.add(o)
    fun pop() { if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex) }
    BackHandler(enabled = overlays.isEmpty() && tab != Tab.DRAW) { tab = Tab.DRAW }
    /** A new lot, or a great-grandchild's: watched being drawn on the first tab. */
    fun draw(heirOf: Long?) {
        tab = Tab.DRAW
        c.write { repo.draw(settings.witness, heirOf, wish = settings.wish) }
    }

    CompositionLocalProvider(LocalToast provides toaster) {
        Box(Modifier.fillMaxSize().paper()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                    Crossfade(tab, animationSpec = tween(220), label = "tab") { t -> when (t) {
                        Tab.DRAW -> DrawScreen(home, onDraw = ::draw, onBook = { tab = Tab.BOOK }, onSettings = { push(Overlay.Settings) })
                        Tab.BOOK -> BookScreen(home, onDraw = ::draw, onArchive = { push(Overlay.Archive) })
                        Tab.CHEST -> ChestScreen(home, onThing = { push(Overlay.Thing(it)) })
                        Tab.ATLAS -> AtlasScreen(home, onLife = { push(Overlay.Read(it)) })
                    } }
                }
                TabBar(tab, waiting = s.page?.end == null && s.page != null) { tab = it }
            }
            when (val o = overlays.lastOrNull()) {
                Overlay.Settings -> SettingsSheet(home, onClose = { pop() })
                Overlay.Archive -> ArchiveSheet(home, onLife = { push(Overlay.Read(it)) }, onClose = { pop() })
                is Overlay.Read -> ReadSheet(home, o.id, onThing = { push(Overlay.Thing(it)) }, onClose = { pop() })
                is Overlay.Thing -> ThingSheet(home, o.id, onLife = { push(Overlay.Read(it)) }, onClose = { pop() })
                null -> Unit
            }
            ToastHost(toaster)
        }
    }
}

@Composable
private fun TabBar(tab: Tab, waiting: Boolean, onTab: (Tab) -> Unit) {
    val items = listOf(
        Triple(Tab.DRAW, R.string.tab_draw, "tab_draw"), Triple(Tab.BOOK, R.string.tab_book, "tab_book"),
        Triple(Tab.CHEST, R.string.tab_chest, "tab_chest"), Triple(Tab.ATLAS, R.string.tab_atlas, "tab_atlas"),
    )
    Row(
        Modifier.fillMaxWidth().background(C.bar).drawBehind { drawLine(C.barOn.copy(alpha = .25f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding().padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        // The icons only when all four are painted: a bar of mixed tabs looks broken.
        val painted = items.map { hasPic("icon/${it.third}") }.all { it }
        for ((t, label, icon) in items) {
            val sel = t == tab
            val fg by animateColorAsState(if (sel) C.barOn else C.barText, label = "tab")
            val glow by animateFloatAsState(if (sel) 1f else 0f, label = "tabGlow")
            Column(
                Modifier.weight(1f).padding(horizontal = 2.dp).clip(RoundedCornerShape(10.dp))
                    .then(if (painted) Modifier else Modifier.background(C.ink2.copy(alpha = .5f * glow)))
                    .selectable(sel, role = Role.Tab) { onTab(t) }.padding(vertical = if (painted) 6.dp else 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (painted) Box(
                    Modifier.size(width = 56.dp, height = 30.dp).clip(RoundedCornerShape(15.dp)).background(C.ink2.copy(alpha = .55f * glow)),
                    contentAlignment = Alignment.Center,
                ) {
                    Ico(icon, Modifier.size(22.dp), tint = fg)
                    // A life is open and waits in the book.
                    if (t == Tab.BOOK && waiting && !sel) Box(Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 12.dp).size(7.dp).clip(CircleShape).background(C.barOn))
                }
                Text(
                    stringResource(label),
                    style = if (painted) T.Small.copy(color = fg, fontSize = T.Label.fontSize, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    else T.Button.copy(color = fg, fontSize = T.Small.fontSize),
                    modifier = Modifier.padding(top = if (painted) 2.dp else 0.dp),
                )
            }
        }
    }
}
