package app.lot.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lot.ui.theme.C
import app.lot.ui.theme.T
import kotlinx.coroutines.delay

/** The paper of every screen: warm, a little lighter at the top and darker at the foot, with a few foxing spots. */
fun Modifier.paper() = this.background(C.paper).drawBehind {
    drawRect(Brush.radialGradient(listOf(C.paperHi, Color.Transparent), Offset(size.width * .3f, size.height * .15f), size.maxDimension * .6f))
    drawRect(Brush.radialGradient(listOf(C.paperLo.copy(alpha = .7f), Color.Transparent), Offset(size.width * .85f, size.height * .95f), size.maxDimension * .55f))
    val spots = listOf(.12f to .32f, .78f to .18f, .64f to .71f, .22f to .86f, .9f to .48f)
    for ((x, y) in spots) drawCircle(C.paperLo.copy(alpha = .18f), 3.dp.toPx() + (x * 9).dp.toPx() % 5, Offset(size.width * x, size.height * y))
}

enum class Kind { INK, GHOST, SOFT }

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, kind: Kind = Kind.INK, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    val (bg, fg) = when (kind) {
        Kind.INK -> C.ink to C.light
        Kind.GHOST -> Color.Transparent to C.ink
        Kind.SOFT -> C.card to C.ink
    }
    Box(
        modifier
            .fillMaxWidth()
            .then(if (kind == Kind.INK && enabled) Modifier.drawBehind { drawRoundRect(Color(0x66000000), Offset(0f, 3.dp.toPx()), size, CornerRadius(4.dp.toPx())) } else Modifier)
            .clip(shape)
            .background(if (enabled) bg else C.line)
            .then(if (kind != Kind.INK) Modifier.border(1.5.dp, if (enabled) C.ink else C.muted, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = T.Button.copy(color = if (enabled) fg else C.muted), textAlign = TextAlign.Center) }
}

@Composable
fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(modifier.clip(shape).background(if (on) C.ink else C.card).border(1.dp, if (on) C.ink else C.line, shape).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 7.dp)) {
        Text(text, style = T.Body.copy(color = if (on) C.light else C.ink))
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(C.card).border(1.dp, C.line, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 14.dp, vertical = 12.dp),
        content = content,
    )
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
    Box(modifier.fillMaxWidth().clip(shape).background(C.page).border(1.dp, C.line, shape).padding(horizontal = 10.dp, vertical = 8.dp)) {
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
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = T.Title, modifier = Modifier.weight(1f))
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
