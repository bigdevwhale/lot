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
import app.lot.ui.components.CommunityLinks
import app.lot.ui.components.SupportCard
import app.lot.ui.components.Gap
import app.lot.ui.components.Section
import app.lot.ui.components.Segmented
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
            Section("globe", w.t("Язык", "Language"))
            Segmented(listOf("Русский" to null, "English" to null), if (h.ru) 0 else 1, Modifier.padding(top = 10.dp)) { i ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(if (i == 0) "ru" else "en"))
            }
        }
        Gap(10)
        Card {
            Section("signpost", w.t("Как жить", "How to live"))
            Text(w.t("Можно выбирать на развилках самому, а можно быть свидетелем: тогда за тебя решает судьба, и ты только читаешь книгу.",
                "You can choose at each fork yourself, or be a witness: then fate decides, and you only read the book."), style = T.Small, modifier = Modifier.padding(top = 8.dp))
            Segmented(listOf(w.t("жить и выбирать", "live and choose") to "signpost", w.t("быть свидетелем", "be a witness") to "lantern"),
                if (st.witness) 1 else 0, Modifier.padding(top = 10.dp)) { i -> c.write { settings.update { it.copy(witness = i == 1) } } }
        }
        Gap(10)
        Card {
            Section("quill", w.t("Почерк книги", "The book's hand"))
            Text(w.t("Родилась в семье пахаря. Видела море.", "Born to a ploughman's family. Saw the sea."),
                style = T.Write, modifier = Modifier.padding(top = 6.dp))
            Segmented(listOf(w.t("рукописный", "handwritten") to null, w.t("печатный", "printed") to null), if (st.hand) 0 else 1, Modifier.padding(top = 10.dp)) { i ->
                c.write { settings.update { it.copy(hand = i == 0) } }
            }
        }
        Gap(10)
        SupportCard(w)
        Gap(10)
        CommunityLinks(w)
        Gap(10)
        Card {
            Section("seal", w.t("О «Жребии»", "About Lot"))
            Gap(6)
            Text(w.t(
                "Мешок тянет по-честному: из ${World.TOTAL.toInt()} миллиардов когда-либо родившихся людей, по оценке Population Reference Bureau, с весами эпох, мест и сословий. Поэтому чаще всего ты — крестьянин до 1800 года. ${c.content.lines.size} строк быта и ${c.content.hist.size} настоящих событий. Без рекламы, без аккаунта, без интернета.",
                "The sack draws fairly: from the ${World.TOTAL.toInt()} billion people ever born, by the Population Reference Bureau's estimate, weighted by era, place and station. That is why you are most often a peasant before 1800. ${c.content.lines.size} everyday lines and ${c.content.hist.size} real events. No ads, no account, no internet."), style = T.Small)
            Text(w.t("Версия ${BuildConfig.VERSION_NAME} · шрифты Cormorant Garamond, PT Serif и Marck Script (OFL) · исходники на GitHub", "Version ${BuildConfig.VERSION_NAME} · Cormorant Garamond, PT Serif and Marck Script fonts (OFL) · source on GitHub"),
                style = T.Small.copy(color = C.red),
                modifier = Modifier.padding(top = 8.dp).clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bigdevwhale/lot"))) })
        }
        Gap(20)
    }
}
