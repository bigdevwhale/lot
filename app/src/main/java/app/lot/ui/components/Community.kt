package app.lot.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lot.ui.Words
import app.lot.ui.art.Ico
import app.lot.ui.theme.C
import app.lot.ui.theme.T

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "lot"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
    const val SUPPORT = "https://bigdevwhale.github.io/stillcraft/support/"
}

/** Support for the app, as in the other StillCraft apps: it is free, with no ads and no account. */
@Composable
fun SupportCard(w: Words) {
    val ctx = LocalContext.current
    Card {
        Section("heart", w.t("Поддержать «Жребий»", "Support Lot"), w.t("бесплатно, без рекламы и без аккаунта", "free, with no ads and no account"))
        Text(
            w.t("Приложение живёт на поддержке тех, кто в нём читает. Это помогает дописывать жизни, события и вещи.",
                "The app lives on the goodwill of the people who read in it. It helps write more lives, events and things."),
            style = T.Small, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
        )
        Btn(w.t("Поддержать", "Support"), icon = "heart") {
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Community.SUPPORT)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}

/** Report a bug, suggest an idea, follow updates, join the chat; each opens Telegram. */
@Composable
fun CommunityLinks(w: Words) {
    val ctx = LocalContext.current
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    Card {
        Section("chat", w.t("Сообщество", "Community"))
        Text(
            w.t("Приложение делается вместе с теми, кто им пользуется. Всё идёт через бота StillCraft в Telegram.", "The app is made together with the people who use it. It all goes through the StillCraft bot on Telegram."),
            style = T.Small, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        for ((icon, label, sub, url) in listOf(
            Link("bug", w.t("Сообщить о баге", "Report a bug"), w.t("что сломалось и как повторить", "what broke and how to repeat it"), Community.BUG),
            Link("idea", w.t("Предложить идею", "Suggest an idea"), w.t("функция, событие, вещь", "a feature, an event, a thing"), Community.IDEA),
            Link("bell", w.t("Подписаться на обновления", "Follow updates"), w.t("бот напишет, когда выйдет версия", "the bot writes when a version is out"), Community.FOLLOW),
            Link("chat", w.t("Чат StillCraft", "StillCraft chat"), w.t("все приложения и люди", "all the apps and people"), Community.CHAT),
        )) {
            Row(Modifier.fillMaxWidth().clickable { open(url) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(36.dp)) { Ico(icon, Modifier.size(24.dp), tint = C.ink2) }
                Column(Modifier.weight(1f)) {
                    Text(label, style = T.BodyB)
                    Text(sub, style = T.Small)
                }
                Text("›", style = T.Heading.copy(color = C.muted))
            }
        }
    }
}

private data class Link(val icon: String, val label: String, val sub: String, val url: String)
