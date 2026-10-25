package app.lot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.lot.R

/** Old paper, iron-gall ink, a burnt wooden tag and a sack of hessian. */
@Immutable
object C {
    val paper = Color(0xFFEFE0BF)
    val paperHi = Color(0xFFF6EAD0)
    val paperLo = Color(0xFFDCC493)
    val page = Color(0xFFF7ECD2)
    val rule = Color(0xFFD9C49B)
    val light = Color(0xFFF8EFD9)
    val card = Color(0xFFFBF4E3)
    val line = Color(0xFFC8AE80)
    val ink = Color(0xFF2A1A10)
    val ink2 = Color(0xFF5B3D26)
    val muted = Color(0xFF8A6A4A)
    val red = Color(0xFF8F2F22)
    /** The registrar's ink in the book. */
    val blue = Color(0xFF2B2A5A)
    val wood = Color(0xFFC99A5E)
    val woodHi = Color(0xFFD9B077)
    val woodLo = Color(0xFFA87A44)
    val grain = Color(0xFF8A6034)
    val burn = Color(0xFF3A1D0C)
    val sack = Color(0xFFC9A774)
    val sackLo = Color(0xFFB08C58)
    val sackDark = Color(0xFF3A2414)
    val string = Color(0xFF7A5230)
    val night = Color(0xE01A120C)
    val bar = Color(0xFF24170E)
    val barText = Color(0xFFBFA57C)
    val barOn = Color(0xFFF3D796)
    val shut = Color(0xFFE6D8B8)
    val land = Color(0xFFD4BD8C)
    val sea = Color(0xFFE9D7AE)
}

val Serif = FontFamily(
    Font(R.font.pt_serif_regular, FontWeight.Normal),
    Font(R.font.pt_serif_bold, FontWeight.Bold),
    Font(R.font.pt_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

/** The registrar's hand in the book. */
val Hand = FontFamily(Font(R.font.marck_script, FontWeight.Normal))

object T {
    val Title = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Serif, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val BodyB = Body.copy(fontWeight = FontWeight.Bold)
    val Italic = Body.copy(fontStyle = FontStyle.Italic, color = C.muted)
    val Small = TextStyle(fontFamily = Serif, fontSize = 13.sp, lineHeight = 18.sp, color = C.muted)
    val Label = TextStyle(fontFamily = Serif, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.4.sp, color = C.muted)
    val Button = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp)
    val Big = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp, color = C.ink)
    val Write = TextStyle(fontFamily = Hand, fontSize = 21.sp, lineHeight = 26.sp, color = C.blue)
    val WriteAge = TextStyle(fontFamily = Hand, fontSize = 20.sp, lineHeight = 26.sp, color = C.red)
    val Epitaph = TextStyle(fontFamily = Hand, fontSize = 24.sp, lineHeight = 30.sp, color = C.ink)
}

@Composable
fun LotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.ink, onPrimary = C.light, background = C.paper, surface = C.card, onSurface = C.ink,
            onBackground = C.ink, outline = C.line, error = C.red,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
