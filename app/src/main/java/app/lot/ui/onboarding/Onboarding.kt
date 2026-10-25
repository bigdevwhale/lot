package app.lot.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.lot.container
import app.lot.ui.Words
import app.lot.ui.art.Sack.stage
import app.lot.ui.art.TagText
import app.lot.ui.components.Btn
import app.lot.ui.components.Gap
import app.lot.ui.components.paper
import app.lot.ui.theme.T

/** Two pages: the sack and the 117 billion, then how a life is read. */
@Composable
fun Onboarding(w: Words) {
    val c = LocalContext.current.container
    var page by rememberSaveable { mutableIntStateOf(0) }
    val tm = rememberTextMeasurer()
    Column(Modifier.fillMaxSize().paper().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 18.dp)) {
        if (page == 0) {
            val tag = TagText(w.t("ок. 1350", "c. 1350"), w.t("Китай", "China"), w.t("в семье крестьян", "born to peasants"), w.t("девочка", "a girl"))
            Canvas(Modifier.fillMaxWidth().height(280.dp)) { stage(0f, 1f, tag, 3L, tm) }
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
            Btn(w.t("Дальше", "Next")) { page = 1 }
        } else {
            Text(w.t("Как читается жизнь", "How a life is read"), style = T.Title)
            Gap(14)
            for ((head, body) in listOf(
                w.t("Книга", "The book") to w.t("Жизнь записывается в метрическую книгу. Каждое «дальше» — несколько лет: детство, работа, свадьба, беды, старость. Настоящая история приходит сбоку — слухами, ценой хлеба, войском у околицы.",
                    "A life is written into a parish register. Each page turned is a few years: childhood, work, marriage, hard times, old age. Real history arrives from the side: as rumours, the price of bread, an army at the edge of the village."),
                w.t("Двери", "Doors") to w.t("На развилках видны все двери, но часть закрыта — и написано почему. «Девочек грамоте не учат». «Ты не свободен уйти». Неравенство без нотаций.",
                    "At each fork every door is shown, but some are shut, and it says why. \"Girls are not taught their letters.\" \"You are not free to leave.\" Inequality, without a lecture."),
                w.t("Сундук и правнуки", "The chest and the great-grandchildren") to w.t("Каждая жизнь оставляет вещь. Можно вытянуть жребий правнука: он унаследует фамилию, место, достаток — и ту же вещь, на которой копятся поколения.",
                    "Each life leaves a thing. You can draw a great-grandchild's lot: they inherit the name, the place, the family's means, and the same thing, gathering generations."),
                w.t("Вопрос тебе", "A question for you") to w.t("В конце жизнь задаёт один тихий вопрос — про тебя сегодняшнего. Ответы складываются в тетрадку в атласе.",
                    "At the end a life asks one quiet question, about you today. The answers gather in a notebook in the atlas."),
            )) {
                Text(head, style = T.Heading)
                Text(body, style = T.Body, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
            }
            Gap(10)
            Btn(w.t("К мешку", "To the sack")) { c.write { settings.update { it.copy(onboarded = true) } } }
        }
    }
}
