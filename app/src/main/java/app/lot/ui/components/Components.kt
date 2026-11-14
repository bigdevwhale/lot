package app.lot.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lot.ui.art.Ico
import app.lot.ui.art.hasPic
import app.lot.ui.theme.C
import app.lot.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The paper of every screen: warm, a little lighter at the top and darker at the foot, with a few foxing spots. */
fun Modifier.paper() = this.background(C.paper).drawBehind {
    drawRect(Brush.radialGradient(listOf(C.paperHi, Color.Transparent), Offset(size.width * .3f, size.height * .15f), size.maxDimension * .6f))
    drawRect(Brush.radialGradient(listOf(C.paperLo.copy(alpha = .7f), Color.Transparent), Offset(size.width * .85f, size.height * .95f), size.maxDimension * .55f))
    val spots = listOf(.12f to .32f, .78f to .18f, .64f to .71f, .22f to .86f, .9f to .48f)
    for ((x, y) in spots) drawCircle(C.paperLo.copy(alpha = .18f), 3.dp.toPx() + (x * 9).dp.toPx() % 5, Offset(size.width * x, size.height * y))
}

enum class Kind { INK, GHOST, SOFT }

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, kind: Kind = Kind.INK, enabled: Boolean = true, icon: String? = null, onClick: () -> Unit) {
    val shape = remember { Paper.plate(7.dp) }
    val (bg, fg) = when (kind) {
        Kind.INK -> C.ink to C.light
        Kind.GHOST -> Color.Transparent to C.ink
        Kind.SOFT -> C.card to C.ink
    }
    val press = remember { MutableInteractionSource() }
    val down by press.collectIsPressedAsState()
    val k by animateFloatAsState(if (down) .97f else 1f, label = "press")
    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = k; scaleY = k }
            .then(if (kind == Kind.INK && enabled) Modifier.drawBehind {
                translate(0f, (if (down) 1 else 3).dp.toPx()) { drawOutline(shape.createOutline(size, layoutDirection, this), Color(0x55000000)) }
            } else Modifier)
            .clip(shape)
            .background(if (enabled) bg else C.line)
            .then(if (kind == Kind.INK && enabled) Modifier.drawBehind {
                // A thin gilt line inside the edge, like the tooling on a book's spine.
                inner(shape, 3.dp.toPx(), C.barOn.copy(alpha = .35f))
            } else Modifier)
            .then(if (kind != Kind.INK) Modifier.border(1.5.dp, if (enabled) C.ink else C.muted, shape) else Modifier)
            .clickable(press, LocalIndication.current, enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null && hasPic("icon/$icon")) {
                Ico(icon, Modifier.size(20.dp), tint = if (!enabled) C.muted else if (kind == Kind.INK) C.barOn else C.ink2)
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = T.Button.copy(color = if (enabled) fg else C.muted), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, dot: Color? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val bg by animateColorAsState(if (on) C.ink else C.card, label = "chip")
    Row(
        modifier.clip(shape).background(bg).border(1.dp, if (on) C.ink else C.line, shape)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) { Box(Modifier.size(8.dp).clip(CircleShape).background(if (enabled) dot else C.line)); Spacer(Modifier.width(6.dp)) }
        Text(text, style = T.Body.copy(color = if (on) C.light else if (enabled) C.ink else C.line))
    }
}

/** What a card is made of: a [PLATE] with cut corners and an inner rule, or a [LEAF] torn along its foot. */
enum class Frame { PLATE, LEAF }

@Composable
fun Card(
    modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, pad: Boolean = true, frame: Frame = Frame.PLATE, seed: Int = 0,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = remember(frame, seed) { if (frame == Frame.PLATE) Paper.plate() else Paper.leaf(seed) }
    Column(
        modifier.fillMaxWidth()
            .drawBehind { translate(0f, 2.dp.toPx()) { drawOutline(shape.createOutline(size, layoutDirection, this), C.ink2.copy(alpha = .12f)) } }
            .clip(shape).background(C.card)
            .then(if (frame == Frame.PLATE) Modifier.drawBehind { inner(shape, 4.dp.toPx(), C.line.copy(alpha = .6f)) } else Modifier)
            .border(1.dp, C.line, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(if (pad) Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = if (frame == Frame.LEAF) 16.dp else 14.dp) else Modifier),
        content = content,
    )
}

/** A thin rule [inset] inside [shape], following its corners. */
fun DrawScope.inner(shape: Shape, inset: Float, color: Color, width: Float = 1f) {
    val o = shape.createOutline(Size(size.width - 2 * inset, size.height - 2 * inset), layoutDirection, this)
    translate(inset, inset) { drawOutline(o, color, style = Stroke(width)) }
}

/** A section's heading: an engraved icon in a ring, the title, and a line on what it holds. */
@Composable
fun Section(icon: String, title: String, sub: String? = null, modifier: Modifier = Modifier, end: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (hasPic("icon/$icon")) Box(Modifier.padding(end = 10.dp).size(38.dp).clip(CircleShape).background(C.paperHi).border(1.dp, C.line, CircleShape), contentAlignment = Alignment.Center) {
            Ico(icon, Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Heading)
            if (sub != null) Text(sub, style = T.Small)
        }
        end()
    }
}

/** Two or three ways, side by side, one of them taken. */
@Composable
fun Segmented(options: List<Pair<String, String?>>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(modifier.fillMaxWidth().clip(shape).background(C.shut.copy(alpha = .6f)).border(1.dp, C.line, shape).padding(3.dp)) {
        for ((i, o) in options.withIndex()) {
            val on = i == selected
            val bg by animateColorAsState(if (on) C.ink else Color.Transparent, label = "seg")
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(bg).selectable(on, role = Role.RadioButton) { onSelect(i) }
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                val icon = o.second
                if (icon != null && hasPic("icon/$icon")) { Ico(icon, Modifier.size(18.dp), tint = if (on) C.barOn else C.muted); Spacer(Modifier.width(6.dp)) }
                Text(o.first, style = T.Small.copy(color = if (on) C.light else C.ink2, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), maxLines = 2)
            }
        }
    }
}

/** A thin bar filled to [f], in [color]. */
@Composable
fun Meter(f: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val v by animateFloatAsState(f.coerceIn(0f, 1f), tween(700), label = "meter")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(C.paperLo.copy(alpha = .45f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(v).clip(RoundedCornerShape(height / 2)).background(color))
    }
}

/** A number with its icon and what it counts. */
@Composable
fun Stat(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Ico(icon, Modifier.size(22.dp), tint = C.muted)
        Text(value, style = T.Big, modifier = Modifier.padding(top = 2.dp))
        Text(label, style = T.Small.copy(fontSize = T.Label.fontSize, lineHeight = T.Label.lineHeight, textAlign = TextAlign.Center))
    }
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier) = Text(text.uppercase(), style = T.Label, modifier = modifier)

@Composable
fun Gap(h: Int = 10) = Spacer(Modifier.height(h.dp))

@Composable
fun Flow(modifier: Modifier = Modifier, gap: Dp = 6.dp, content: @Composable () -> Unit) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) { content() }
}

/** A field written in the registrar's hand. */
@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, minLines: Int = 2, max: Int = 400) {
    val shape = RoundedCornerShape(3.dp)
    // When the keyboard comes up, the field scrolls itself into view above it.
    val seen = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Box(
        modifier.fillMaxWidth().bringIntoViewRequester(seen)
            .onFocusEvent { if (it.isFocused) scope.launch { delay(350); seen.bringIntoView() } }
            .clip(shape).background(C.page).border(1.dp, C.line, shape).padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        if (value.isEmpty()) Text(hint, style = T.Write.copy(color = C.muted.copy(alpha = .6f)))
        BasicTextField(
            value, { onChange(it.take(max)) }, Modifier.fillMaxWidth(), textStyle = T.Write, minLines = minLines, cursorBrush = SolidColor(C.blue),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
    }
}

/** The title of a tab. */
@Composable
fun TopBar(title: String, sub: String? = null, end: @Composable RowScope.() -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The first letter in red, as a rubricated initial in an old book.
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = C.red, fontSize = T.Title.fontSize * 1.18f)) { append(title.take(1)) }
                append(title.drop(1))
            }, style = T.Title, modifier = Modifier.weight(1f))
            end()
        }
        if (sub != null) Text(sub, style = T.Italic.copy(fontSize = T.Small.fontSize, lineHeight = T.Small.lineHeight), modifier = Modifier.padding(top = 2.dp, end = 8.dp))
    }
}

/** A full-screen sheet over the tabs, with a title and a way back. */
@Composable
fun Sheet(title: String, onClose: () -> Unit, closeLabel: String, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(C.night).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(10.dp).clip(RoundedCornerShape(10.dp)).paper()) {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = T.Heading, modifier = Modifier.weight(1f))
                Text(closeLabel, style = T.Button.copy(color = C.muted), modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClose).padding(10.dp))
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp), content = content)
        }
    }
}

class Toaster {
    var text by mutableStateOf<String?>(null)
    var seq by mutableIntStateOf(0)
    fun show(s: String) { text = s; seq++ }
}

val LocalToast = compositionLocalOf { Toaster() }

@Composable
fun BoxScope.ToastHost(t: Toaster) {
    LaunchedEffect(t.seq) { if (t.text != null) { delay(3000); t.text = null } }
    val s = t.text ?: return
    Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 18.dp, end = 18.dp, bottom = 84.dp)) {
        Text(s, style = T.Body.copy(color = C.light, textAlign = TextAlign.Center), modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(C.ink).clickable { t.text = null }.padding(horizontal = 16.dp, vertical = 11.dp))
    }
}

/** A thin rule like the ones in a parish book. */
@Composable
fun Rule(modifier: Modifier = Modifier, color: Color = C.line) = Box(modifier.fillMaxWidth().height(1.dp).background(color))

/** Plain text style for numbers in a grid of odds. */
val NumStyle: TextStyle get() = T.Big
