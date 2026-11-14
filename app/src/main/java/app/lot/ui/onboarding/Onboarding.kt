package app.lot.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.lot.container
import app.lot.ui.Words
import app.lot.ui.art.Ico
import app.lot.ui.art.Pic
import app.lot.ui.art.Sack.stage
import app.lot.ui.art.rememberSackArt
import app.lot.ui.art.TagText
import app.lot.ui.art.hasPic
import app.lot.ui.components.Btn
import app.lot.ui.components.Gap
import app.lot.ui.components.paper
import app.lot.ui.theme.C
import app.lot.ui.theme.T

/** Two pages: the sack and the 117 billion, then how a life is read. */
@Composable
fun Onboarding(w: Words) {
    val c = LocalContext.current.container
    var page by rememberSaveable { mutableIntStateOf(0) }
    val tm = rememberTextMeasurer()
    val art = rememberSackArt()
    Column(Modifier.fillMaxSize().paper().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp)) {
        if (page == 0) {
            val tag = TagText(w.t("ок. 1350", "c. 1350"), w.t("Китай", "China"), w.t("в семье крестьян", "born to peasants"), w.t("девочка", "a girl"))
            Canvas(Modifier.fillMaxWidth().height(280.dp)) { stage(0f, 1f, tag, 3L, tm, art) }
            Gap(10)
            Text(w.t("Один из ста семнадцати миллиардов", "One of 117 billion"), style = T.Title)
            Gap(8)
            Text(w.t(
                "Столько людей, по оценкам, родилось на Земле за всю её историю. Мешок вытягивает тебе одного из них — честно, с весами эпох, мест и сословий.",
                "That is roughly how many people have ever been born. The sack draws you one of them, fairly, weighted by era, place and station."), style = T.Body)
            Gap(8)
            Text(w.t(
                "Поэтому чаще всего ты родишься крестьянином задолго до 1800 года. Почти половина не доживёт до пяти. Дожить до старости — уже удача.",
                "So most often you are born a peasant long before 1800. Nearly half do not live to five. Reaching old age is luck already."), style = T.Body)
            Gap(20)
            Dots(0)
            Gap(12)
            Btn(w.t("Дальше", "Next")) { page = 1 }
        } else {
            Pic("pic/register", Modifier.fillMaxWidth().height(170.dp))
            Text(w.t("Как читается жизнь", "How a life is read"), style = T.Title)
            Gap(14)
            for ((n, pair) in listOf(
                w.t("Книга", "The book") to w.t("Жизнь записывается в метрическую книгу. Каждое «дальше» — несколько лет: детство, работа, свадьба, беды, старость. Настоящая история приходит сбоку — слухами, ценой хлеба, войском у околицы.",
                    "A life is written into a parish register. Each page turned is a few years: childhood, work, marriage, hard times, old age. Real history arrives from the side: as rumours, the price of bread, an army at the edge of the village."),
                w.t("Двери", "Doors") to w.t("На развилках видны все двери, но часть закрыта — и написано почему. «Девочек грамоте не учат». «Ты не свободен уйти». Неравенство без нотаций.",
                    "At each fork every door is shown, but some are shut, and it says why. \"Girls are not taught their letters.\" \"You are not free to leave.\" Inequality, without a lecture."),
                w.t("Сундук и правнуки", "The chest and the great-grandchildren") to w.t("Каждая жизнь оставляет вещь. Можно вытянуть жребий правнука: он унаследует фамилию, место, достаток — и ту же вещь, на которой копятся поколения.",
                    "Each life leaves a thing. You can draw a great-grandchild's lot: they inherit the name, the place, the family's means, and the same thing, gathering generations."),
                w.t("Вопрос тебе", "A question for you") to w.t("В конце жизнь задаёт один тихий вопрос — про тебя сегодняшнего. Ответы складываются в тетрадку в атласе.",
                    "At the end a life asks one quiet question, about you today. The answers gather in a notebook in the atlas."),
            ).withIndex()) {
                val (head, body) = pair
                val icon = listOf("tab_book", "door_shut", "tab_chest", "lantern")[n]
                Row(Modifier.padding(bottom = 14.dp)) {
                    if (hasPic("icon/$icon")) Box(
                        Modifier.padding(end = 12.dp).size(40.dp).clip(CircleShape).background(C.paperHi).border(1.dp, C.line, CircleShape), contentAlignment = Alignment.Center,
                    ) { Ico(icon, Modifier.size(24.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text(head, style = T.Heading)
                        Text(body, style = T.Body, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            Gap(10)
            Dots(1)
            Gap(12)
            Btn(w.t("К мешку", "To the sack"), icon = "tab_draw") { c.write { settings.update { it.copy(onboarded = true) } } }
        }
    }
}

/** Which of the two pages this is. */
@Composable
private fun Dots(at: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        for (i in 0..1) Box(Modifier.padding(horizontal = 4.dp).size(width = if (i == at) 22.dp else 8.dp, height = 8.dp).clip(CircleShape).background(if (i == at) C.ink2 else C.line))
    }
}
