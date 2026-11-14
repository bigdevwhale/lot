package app.lot.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lot.domain.World
import app.lot.ui.Words
import app.lot.ui.theme.C
import app.lot.ui.theme.T

private val ROMAN = listOf("I", "II", "III", "IV", "V", "VI", "VII")

/**
 * The seven ages in one row, oldest on the left, each a tile in its era's colour, after an "all" tile: every choice in
 * sight at once. [main] is the big line of a tile (a count, or the numeral); a tile is faded and shut when not [open].
 */
@Composable
fun EraStrip(
    w: Words, selected: Int?, allLabel: String, allMain: String,
    main: (Int) -> String, open: (Int) -> Boolean, onPick: (Int?) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        EraTile(selected == null, C.ink, true, allMain, allLabel, allLabel, 1.3f) { onPick(null) }
        for (e in World.ERAS) {
            // Tapping the chosen age again lets it go.
            EraTile(selected == e.i, Color(e.color), open(e.i), main(e.i), if (main(e.i) == ROMAN[e.i]) "" else ROMAN[e.i], w.x(e.name), 1f) {
                onPick(if (selected == e.i) null else e.i)
            }
        }
    }
}

/** The name and span of the chosen age, under the strip, in its colour. */
@Composable
fun EraCaption(w: Words, selected: Int?, none: String) {
    Text(
        selected?.let { "${w.x(World.ERAS[it].name)} · ${w.x(World.ERAS[it].span)}" } ?: none,
        style = T.Small.copy(color = selected?.let { Color(World.ERAS[it].color) } ?: C.muted),
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.EraTile(
    on: Boolean, color: Color, open: Boolean, main: String, sub: String, said: String, weight: Float, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    val bg by animateColorAsState(if (on) color else C.card, label = "era")
    Column(
        Modifier.weight(weight).clip(shape).background(bg).border(1.dp, if (on) color else C.line, shape)
            .selectable(on, enabled = open, role = Role.Tab, onClick = onClick).semantics { contentDescription = said }
            .alpha(if (open) 1f else .35f).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(width = 14.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(if (on) C.light else color))
        Text(main, style = T.BodyB.copy(color = if (on) C.light else C.ink), maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        if (sub.isNotEmpty()) Text(sub, style = T.Label.copy(color = if (on) C.light else C.muted, letterSpacing = 0.sp), maxLines = 1)
    }
}

/** The numeral of an age, for a strip without counts. */
fun roman(i: Int) = ROMAN[i]
