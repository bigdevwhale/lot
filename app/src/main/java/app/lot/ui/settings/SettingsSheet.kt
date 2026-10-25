package app.lot.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.lot.BuildConfig
import app.lot.container
import app.lot.domain.World
import app.lot.ui.Home
import app.lot.ui.components.Card
import app.lot.ui.components.Chip
import app.lot.ui.components.CommunityLinks
import app.lot.ui.components.Flow
import app.lot.ui.components.Gap
import app.lot.ui.components.Sheet
import app.lot.ui.theme.C
import app.lot.ui.theme.T

@Composable
fun SettingsSheet(h: Home, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    val st = h.settings

    Sheet(w.t("Настройки", "Settings"), onClose, w.t("Закрыть", "Close")) {
        Card {
            Text(w.t("Язык", "Language"), style = T.Heading)
            Flow(Modifier.padding(top = 8.dp)) {
                Chip("Русский", h.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru")) }
                Chip("English", !h.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
            }
        }
        Gap(10)
        Card {
            Text(w.t("Как жить", "How to live"), style = T.Heading)
            Text(w.t("Можно выбирать на развилках самому, а можно быть свидетелем: тогда за тебя решает судьба, и ты только читаешь книгу.",
                "You can choose at each fork yourself, or be a witness: then fate decides, and you only read the book."), style = T.Small)
            Flow(Modifier.padding(top = 8.dp)) {
                Chip(w.t("жить и выбирать", "live and choose"), !st.witness) { c.write { settings.update { it.copy(witness = false) } } }
                Chip(w.t("быть свидетелем", "be a witness"), st.witness) { c.write { settings.update { it.copy(witness = true) } } }
            }
        }
        Gap(10)
        Card {
            Text(w.t("Почерк книги", "The book's hand"), style = T.Heading)
            Text(w.t("Родилась в семье пахаря. Видела море.", "Born to a ploughman's family. Saw the sea."),
                style = T.Write, modifier = Modifier.padding(top = 6.dp))
            Flow(Modifier.padding(top = 8.dp)) {
                Chip(w.t("рукописный", "handwritten"), st.hand) { c.write { settings.update { it.copy(hand = true) } } }
                Chip(w.t("печатный", "printed"), !st.hand) { c.write { settings.update { it.copy(hand = false) } } }
            }
        }
        Gap(10)
        CommunityLinks(w)
        Gap(10)
        Card {
            Text(w.t("О «Жребии»", "About Lot"), style = T.Heading)
            Text(w.t(
                "Мешок тянет по-честному: из ${World.TOTAL.toInt()} миллиардов когда-либо родившихся людей, по оценке Population Reference Bureau, с весами эпох, мест и сословий. Поэтому чаще всего ты — крестьянин до 1800 года. ${c.content.lines.size} строк быта и ${c.content.hist.size} настоящих событий. Без рекламы, без аккаунта, без интернета.",
                "The sack draws fairly: from the ${World.TOTAL.toInt()} billion people ever born, by the Population Reference Bureau's estimate, weighted by era, place and station. That is why you are most often a peasant before 1800. ${c.content.lines.size} everyday lines and ${c.content.hist.size} real events. No ads, no account, no internet."), style = T.Small)
            Text(w.t("Версия ${BuildConfig.VERSION_NAME} · шрифты PT Serif и Marck Script (OFL) · исходники на GitHub", "Version ${BuildConfig.VERSION_NAME} · PT Serif and Marck Script fonts (OFL) · source on GitHub"),
                style = T.Small.copy(color = C.red),
                modifier = Modifier.padding(top = 8.dp).clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bigdevwhale/lot"))) })
        }
        Gap(20)
    }
}
