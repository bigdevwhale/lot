package app.lot.domain

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/** What a great-grandchild inherits from the life before: the family, the place, a thing and maybe a story. */
data class Heir(
    val lifeId: Long,
    val name: Tx,
    val sex: Sex,
    val year: Int,
    val region: String,
    val cult: String,
    val rank: Int,
    val wealth: Int,
    val sur: Tx?,
    val thingId: Long,
    val thing: Tx,
    val story: Tx?,
    val dynasty: Long,
    val gen: Int,
) {
    companion object {
        /** A great-grandchild is born 70 to 94 years later, and must be born by now. */
        fun possible(year: Int, kids: Int) = kids > 0 && year + 94 < 2026
    }
}

enum class Mark { PLAIN, HIST, MEET, DEATH }

data class Entry(val age: Int, val text: Tx, val mark: Mark = Mark.PLAIN)

class Door(val text: Tx, val why: Tx?) {
    val open get() = why == null
}

class Choice(val age: Int, val q: Tx, val doors: List<Door>)

/** Who the lot made you. */
class Birth(
    val seed: Long,
    val era: Era,
    val region: Region,
    val cult: String,
    val estate: Estate,
    val sex: Sex,
    val year: Int,
    val first: Tx,
    val sur: Tx?,
    val name: Tx,
    /** The part of everyone ever born who were born like this: era, region, family, sex. */
    val share: Double,
    val heir: Heir?,
) {
    val survive5 get() = 1 - q0(era, region)
    val literacy get() = (era.lit * if (estate.rank >= 3) 4 else 1).coerceAtMost(1.0)
}

class End(
    val age: Int,
    val epitaph: Tx,
    val bridge: Tx,
    /** A new thing for the chest, or null when the heir's thing goes on to the next holder. */
    val thing: ThingKind?,
    val kidsAlive: Int,
    val wealth: Int,
    val rank: Int,
    val lit: Boolean,
    val sawSea: Boolean,
    val meet: Int?,
    val story: Tx?,
)

/** A life as far as it has been read: the entries so far, and either a choice to make, the end, or more to turn. */
class Page(val birth: Birth, val entries: List<Entry>, val pending: Choice?, val end: End?, val shown: Int) {
    val canTurn get() = pending == null && end == null
}

private fun q0(era: Era, r: Region) = if (era.i == 6 && "af" in r.tags) .12 else era.q0

/**
 * One life, replayed from its seed: the same seed, the same picks and the same number of turned pages always give the
 * same life, so only those three are stored.
 */
class Life private constructor(private val content: Content, seed: Long, heir: Heir?, private val witness: Boolean) {
    private val rnd = Random(seed)
    val birth: Birth
    private val era: Era
    private val region: Region
    private val sex: Sex
    private val year: Int
    private var rank: Int

    private var death: Int
    private var lit = false
    private val parentLit: Boolean
    private var firstLit = false
    private var wealth: Int
    private var married = false
    private var single = false
    private var love = false
    private var byParents = false
    private var monk = false
    private var sawSea: Boolean
    private var kind = false
    private var tree = false
    private var taught = false
    private var wishSea = false
    private var kids = 0
    private var kidsAlive = 0
    private var story: Tx? = null
    private var meet: Int? = null
    private var alive = false

    private val entries = mutableListOf<Entry>()
    private val used = mutableSetOf<Line>()
    private val usedHist = mutableSetOf<Int>()

    init {
        val y: Int
        val e: Era
        if (heir != null) {
            y = heir.year + 70 + rnd.nextInt(25)
            e = World.eraOf(y)
        } else {
            e = weighted(World.ERAS) { it.births }
            val u = rnd.nextDouble().pow(if (e.i == 0) .6 else .75)
            y = (e.to - (e.to - e.from) * (1 - u)).roundToInt().coerceIn(e.from, e.to - 1)
        }
        era = e; year = y
        region = heir?.let { h ->
            val was = World.ALL_REGIONS.firstOrNull { it.key == h.region }
            e.regions.firstOrNull { it.key == h.region } ?: was?.let { w -> e.regions.filter { r -> r.tags.any { it in w.tags } }.maxByOrNull { it.w } }
        } ?: weighted(e.regions) { it.w }
        val cult = World.culture(region, y)
        val pool = e.estates.filter { s -> s.only == null || s.only.any { it in region.tags } }
        val estate = heir?.let { h ->
            val want = (h.rank + when { h.wealth >= 4 -> 1; h.rank == 0 && h.wealth >= 1 -> 1; h.wealth <= 0 && rnd.nextDouble() < .3 -> -1; else -> 0 }).coerceIn(0, 3)
            pool.filter { it.rank == want }.takeIf { it.isNotEmpty() }?.let { c -> weighted(c) { it.w } }
        } ?: weighted(pool) { it.w }
        rank = estate.rank
        sex = if (rnd.nextBoolean()) Sex.M else Sex.F
        val p = Names.pool(cult)
        val first = (if (sex == Sex.M) p.male else p.female).random(rnd)
        val sur = if (Names.keepsSurnames(cult, e.i)) heir?.sur?.takeIf { heir.cult == cult } ?: p.sur.random(rnd) else null
        val name = Names.name(cult, e.i, sex, first, sur, rnd)
        death = if (rnd.nextDouble() < q0(e, region)) rnd.nextInt(5) else tri(e.die).roundToInt()
        // A life that would outrun today stops at today, unfinished.
        if (y + death > NOW) { death = NOW - y; alive = true }
        val share = e.births / World.TOTAL * region.w / e.regions.sumOf { it.w } * estate.w / pool.sumOf { it.w } * .5
        birth = Birth(seed, e, region, cult, estate, sex, y, first, sur, name, share, heir)
        parentLit = rnd.nextDouble() < if (rank >= 3) (e.lit * 6).coerceAtMost(.95) else e.lit * .6
        wealth = rank
        sawSea = region.sea && rnd.nextDouble() < .5
        lit = when (e.i) {
            6 -> rnd.nextDouble() < if (rank == 0) .6 else .92
            5 -> rnd.nextDouble() < e.lit * if (rank >= 2) 1.6 else .8
            else -> false
        }
    }

    private fun <T> weighted(list: List<T>, w: (T) -> Double): T {
        var r = rnd.nextDouble() * list.sumOf(w)
        for (x in list) { r -= w(x); if (r <= 0) return x }
        return list.last()
    }

    private fun tri(t: Triple<Int, Int, Int>): Double {
        val (a, m, b) = t
        val u = rnd.nextDouble()
        val f = (m - a).toDouble() / (b - a)
        return if (u < f) a + sqrt(u * (b - a) * (m - a)) else b - sqrt((1 - u) * (b - a) * (b - m))
    }

    private fun tx(ru: String, en: String) = Tx(ru, en).g(sex)
    private fun add(age: Int, t: Tx, mark: Mark = Mark.PLAIN) { entries += Entry(age, t.g(sex), mark) }
    private fun add(age: Int, ru: String, en: String, mark: Mark = Mark.PLAIN) = add(age, Tx(ru, en), mark)

    private fun line(age: Int, k: Kind, mark: Mark = Mark.PLAIN) {
        val all = content.lines(era.group, k, region)
        val fresh = all.filterNot { it in used }
        val l = (fresh.ifEmpty { all }).randomOrNull(rnd) ?: return
        used += l
        add(age, l.text, mark)
    }

    private class Act(val door: Door, val act: () -> Unit)
    private class Ask(val choice: Choice, val acts: List<() -> Unit>)

    private fun ask(age: Int, q: Tx, vararg doors: Act) = Ask(Choice(age, q.g(sex), doors.map { Door(it.door.text.g(sex), it.door.why?.g(sex)) }), doors.map { it.act })
    private fun door(ru: String, en: String, why: Tx?, act: () -> Unit) = Act(Door(Tx(ru, en), why), act)
    private fun why(ru: String, en: String) = Tx(ru, en)

    private enum class S { BIRTH, CHILD, WORK, YOUTH, MARRY17, MARRY24, JOY, CRISIS, ADULT, LEGACY, OLD, WISH }

    fun read(picks: List<Int>, shown: Int): Page {
        var revealed = 0
        var pi = 0
        var pending: Choice? = null
        var end: End? = null
        for ((age, s) in STOPS) {
            if (revealed >= shown) break
            revealed++
            if (s != S.BIRTH && age >= death) { end = die(); break }
            val a = stop(age, s) ?: continue
            val open = a.choice.doors.indices.filter { a.choice.doors[it].open }
            val pick = if (witness) open.random(rnd) else picks.getOrNull(pi++)?.takeIf { it in open } ?: if (pi > picks.size) null else open.first()
            if (pick == null) { pending = a.choice; break }
            a.acts[pick]()
        }
        return Page(birth, entries.toList(), pending, end, revealed)
    }

    private fun stop(age: Int, s: S): Ask? {
        var hist: Hist? = null
        if (age >= 4) {
            val h = content.hist.filter { it.id !in usedHist && it.fits(region, era.i, year + age) }.randomOrNull(rnd)
            if (h != null && rnd.nextDouble() < if (h.meet) .9 else .75) {
                hist = h; usedHist += h.id
                add(age, h.text, if (h.meet) Mark.MEET else Mark.HIST)
                if (h.meet) meet = h.id
            }
        }
        when (s) {
            S.BIRTH -> {
                val y = Years.inYear(year)
                add(0, Tx("{Родился|Родилась} ${Years.stop(y.ru)} ${region.name.ru}, в семье ${birth.estate.of.ru}.", "Born ${Years.stop(y.en)} ${region.name.en}, to ${birth.estate.of.en}."))
                birth.heir?.let { h ->
                    add(0, Tx("${if (h.sex == Sex.M) "Прадед" else "Прабабка"} — ${h.name.ru}. Семейная вещь: ${h.thing.ru}.",
                        "Your great-grand${if (h.sex == Sex.M) "father" else "mother"} was ${h.name.en}. The family keepsake: ${h.thing.en}."))
                }
            }
            S.CHILD -> if (hist == null || rnd.nextBoolean()) line(age, Kind.CHILD)
            S.WORK -> return work()
            S.YOUTH -> line(age, Kind.YOUTH)
            S.MARRY17 -> if (sex == Sex.F && era.i in 1..4 && !monk) return marry(age) else if (hist == null) line(age, Kind.YOUTH)
            S.MARRY24 -> if (!married && !monk && !single) return marry(age) else if (hist == null) line(age, Kind.ADULT)
            S.JOY -> line(age, Kind.JOY)
            S.CRISIS -> { if (hist == null) line(age, Kind.HARD, Mark.HIST); return crisis() }
            S.ADULT -> if (hist == null || rnd.nextBoolean()) line(age, Kind.ADULT)
            S.LEGACY -> return legacy()
            S.OLD -> if (hist == null) line(age, Kind.OLD)
            S.WISH -> return wish()
        }
        return null
    }

    private fun work(): Ask {
        val e = era.i
        val f = sex == Sex.F
        val learn = when {
            e == 0 -> why("Письма ещё не придумали.", "Writing has not been invented yet.")
            e < 6 && lit -> why("Ты уже умеешь читать.", "You can read already.")
            rank == 0 && e < 6 -> why("Несвободным учиться запрещено.", "The unfree are forbidden to learn.")
            e < 5 && f && rank < 3 -> why("Девочек грамоте не учат.", "Girls are not taught their letters.")
            e < 4 && rank < 3 -> why("Грамоте учат только детей писцов и знати.", "Only the children of scribes and nobles learn their letters.")
            e == 6 && rank == 0 -> why("Семье нужен твой заработок уже сейчас.", "Your family needs your wages now.")
            else -> null
        }
        val town = when {
            e == 0 -> why("Городов ещё нет.", "There are no towns yet.")
            rank == 0 && e < 6 -> why("Ты не {свободен|свободна} уйти.", "You are not free to leave.")
            f && e < 5 -> why("В ученики девочек не берут.", "Girls are not taken as apprentices.")
            else -> null
        }
        val doors = mutableListOf(
            door("Работать с родителями", "Work alongside your parents", null) { add(12, birth.estate.work) },
            door(if (e == 6) "Учиться дальше" else "Учиться грамоте", if (e == 6) "Stay on at school" else "Learn to read and write", learn) {
                if (e == 6) { wealth++; add(12, "Учишься дальше. Первым делом — в библиотеку.", "You stay on at school. First thing: the library.") }
                else {
                    lit = true; firstLit = !parentLit
                    if (firstLit) add(12, "{Выучился|Выучилась} читать — {первым|первой} в роду.", "You learn to read, the first in your family.")
                    else add(12, "{Выучился|Выучилась} читать и писать, как отец.", "You learn to read and write, like your father.")
                }
            },
            door(if (e == 6) "Уехать к родне в большой город" else "Уйти в город, в ученики к мастеру", if (e == 6) "Go to relatives in the big city" else "Go to town as an apprentice", town) {
                if (rnd.nextBoolean()) wealth++
                if (rnd.nextDouble() < .35) sawSea = true
                if (e == 6) add(12, "{Уехал|Уехала} в город. Спишь у дальней родни на полу.", "You move to the city. You sleep on a distant relative's floor.")
                else add(12, "{Ушёл|Ушла} в город, в ученики. Мастер бьёт, но кормит.", "You go to town as an apprentice. The master beats you, but feeds you.")
            },
        )
        if (e in 2..4) doors += door("Уйти в монастырь", "Enter a monastery", when {
            rank == 0 -> why("Несвободных не постригают.", "The unfree may not take vows.")
            region.tags.none { it in setOf("eu", "ru", "cn", "jp", "sea", "in") } -> why("Здесь нет монастырей.", "There are no monasteries here.")
            else -> null
        }) {
            monk = true
            if (rnd.nextDouble() < .7) { lit = true; firstLit = !parentLit }
            add(12, "{Ушёл|Ушла} в монастырь. Семьи не будет — будет тишина и молитва.", "You enter a monastery. There will be no family: there will be silence and prayer.")
        }
        return ask(12, Tx("Детство кончилось. Что дальше?", "Childhood is over. What next?"), *doors.toTypedArray())
    }

    private fun marry(age: Int): Ask {
        val e = era.i
        val f = sex == Sex.F
        val forced = if (e in 1..4 && (f || rank >= 3 || e < 3)) why("Выбирать тебе не дают: старшие уже сговорились.", "You are given no choice: the elders have already agreed.") else null
        return ask(age, if (age < 20) Tx("Тебя сватают.", "A match is being made for you.") else Tx("Пора заводить семью.", "Time to start a family."),
            door("Как решили родители", "As your parents decided", null) {
                byParents = true
                wed(age, "Свадьбу сыграли, как решили старшие. {Жену|Мужа} ты впервые {увидел|увидела} за неделю до неё.", "The wedding is held as the elders decided. You first saw your {wife|husband} a week before.")
            },
            door(if (e == 0) "Уйти к тому, кто нравится, в чужой род" else "По любви", if (e == 0) "Go to the one you like, in another band" else "For love", forced) {
                love = true
                if (e == 0) wed(age, "{Ушёл в чужой род за той, что нравилась.|Ушла в чужой род за тем, кто нравился.}", "You go to another band for the one you like.")
                else wed(age, "{Женился|Вышла замуж} по любви. Родня ворчала, потом привыкла.", "You marry for love. The family grumbles, then gets used to it.")
            },
            door("Не заводить семью", "Stay unmarried", when {
                e == 0 -> why("В одиночку не выжить.", "Nobody survives alone.")
                e < 5 && f -> why("Одинокой женщине не прожить — разве что в монастыре.", "A woman alone cannot get by, except in a convent.")
                e < 5 -> why("Без семьи тебе не дадут земли.", "Without a family you will get no land.")
                else -> null
            }) {
                single = true
                add(age, "{Решил|Решила} жить {один|одна}. Свободы больше, рук меньше.", "You decide to live alone. More freedom, fewer hands.")
            },
        )
    }

    private fun wed(age: Int, ru: String, en: String) {
        married = true
        add(age, ru, en)
        val n = (era.fert + (rnd.nextDouble() - .5) * 4).roundToInt().coerceAtLeast(0)
        kids = n
        kidsAlive = if (era.i >= 6) n else (n * (1 - era.q0) * (.8 + rnd.nextDouble() * .4)).roundToInt().coerceAtMost(n)
        val at = age + 2
        when {
            n == 0 -> add(at, "Детей не было.", "There are no children.")
            n == kidsAlive -> add(at, "Родилось детей: $n.", "Children born: $n.")
            else -> add(at, "Родилось детей: $n. Выросли ${kidsAlive}.", "Children born: $n. Grew up: $kidsAlive.")
        }
    }

    private fun crisis(): Ask {
        val unfree = rank == 0 && era.i < 6
        return ask(33, Tx("Беда. Что делать?", "Hard times. What now?"),
            door("Переждать дома", "Sit it out at home", null) {
                if (rnd.nextDouble() < .25) {
                    death = minOf(death, 33 + rnd.nextInt(3))
                    add(33, "Пережидали. Не все пережили.", "You wait it out. Not everyone lives through it.")
                } else {
                    wealth = (wealth - 1).coerceAtLeast(0)
                    add(33, "Пережидали. Выжили, но всё нажитое ушло.", "You wait it out. You live, but everything you had is gone.")
                }
            },
            door(when { era.i == 0 -> "Уйти всем родом за зверем"; unfree -> "Бежать"; else -> "Уйти всей семьёй в другие края" },
                when { era.i == 0 -> "Move the whole band after the game"; unfree -> "Run away"; else -> "Leave with the family for other lands" }, null) {
                if (rnd.nextDouble() < .4) sawSea = true
                if (unfree && rnd.nextDouble() < .4) {
                    death = minOf(death, 34)
                    add(33, "Беглых поймали.", "The runaways are caught.")
                } else if (unfree) {
                    rank = 1
                    add(33, "{Бежал|Бежала}. На новом месте никто не знает, кем ты {был|была}.", "You run. In the new place nobody knows what you were.")
                } else if (rnd.nextDouble() < .15) {
                    death = minOf(death, 35)
                    add(33, "Ушли. Дорога забрала многих.", "You leave. The road takes many.")
                } else {
                    add(33, "Ушли. Дорога забрала старших, но на новом месте есть земля.", "You leave. The road takes the old ones, but there is land in the new place.")
                }
            },
            door("Поделиться последним с соседями", "Share the last you have with the neighbours",
                if (wealth <= 0 && era.i > 0) why("Делиться нечем: у вас самих пусто.", "There is nothing to share: your own stores are empty.") else null) {
                kind = true
                wealth = (wealth - 1).coerceAtLeast(0)
                add(33, "{Отдал|Отдала} соседям половину запасов. Этого не забудут.", "You give the neighbours half of what you have. It will not be forgotten.")
            },
        )
    }

    private fun legacy(): Ask {
        val none = if (kidsAlive == 0) why("Детей у тебя нет.", "You have no children.") else null
        return ask(45, Tx("Что оставить детям?", "What will you leave the children?"),
            door("Научить своему делу", "Teach them your trade", none) { add(45, "Дети знают твоё дело лучше тебя.", "The children know your trade better than you do.") },
            door("Научить читать", "Teach them to read", none ?: if (!lit) why("Ты и сам{|а} не умеешь читать.", "You cannot read yourself.") else null) {
                taught = true
                add(45, "Учишь детей грамоте по единственной книге в доме.", "You teach the children their letters from the only book in the house.")
            },
            door("Копить на чёрный день", "Save for a rainy day", if (rank == 0 && era.i < 6) why("У несвободных своего нет.", "The unfree own nothing.") else null) {
                wealth++
                when (era.i) {
                    0 -> add(45, "Запас кремня и шкур — в сухой пещере.", "A store of flint and hides, in a dry cave.")
                    6 -> add(45, "Откладываешь понемногу каждый месяц.", "You put a little aside every month.")
                    else -> add(45, "Под половицей — горшок с монетами.", "Under the floorboard, a pot of coins.")
                }
            },
            door("Рассказывать истории", "Tell them stories", null) {
                val old = birth.heir?.story
                if (old != null && rnd.nextBoolean()) {
                    story = old
                    add(45, Tx("Рассказываешь ту же историю, что рассказывали тебе: ${old.ru}.", "You tell the same story you were told: ${old.en}."))
                } else {
                    val s = STORIES.random(rnd).g(sex)
                    story = s
                    add(45, Tx("Вечерами рассказываешь детям ${s.ru}. Они просят ещё.", "In the evenings you tell the children ${s.en}. They ask for more."))
                }
            },
        )
    }

    private fun wish(): Ask = ask(70, Tx("Последнее желание.", "One last wish."),
        door("Увидеть море", "See the sea", when {
            sawSea -> why("Ты его уже {видел|видела}.", "You have seen it already.")
            rank == 0 && era.i < 6 -> why("Тебя не отпустят.", "You will not be let go.")
            wealth < 1 && era.i < 5 -> why("Дорога к морю — месяц пути и все деньги дома.", "The road to the sea is a month's walk and all the money in the house.")
            else -> null
        }) {
            sawSea = true; wishSea = true
            add(70, "Море. Стоишь у воды до темноты и молчишь.", "The sea. You stand by the water until dark and say nothing.")
        },
        door("Помириться с братом", "Make peace with your brother", null) {
            add(70, "Помирились. Сидели рядом до ночи и не вспоминали, из-за чего.", "You make peace. You sit side by side till night and never mention what it was about.")
        },
        door("Посадить дерево у дома", "Plant a tree by the house", when {
            era.i == 0 -> why("Дома нет: род кочует.", "There is no house: the band moves on.")
            rank == 0 && era.i < 6 -> why("Земля не твоя.", "The land is not yours.")
            else -> null
        }) {
            tree = true
            add(70, "{Посадил|Посадила} грушу. Плодов ты не увидишь.", "You plant a pear tree. You will not see its fruit.")
        },
    )

    private fun die(): End {
        val a = death
        val how = when {
            a < 5 -> listOf(tx("Лихорадка.", "A fever."), tx("Не {пережил|пережила} зиму.", "The winter was too long."), tx("Жар и понос, три дня.", "Three days of fever and flux."))
            era.i == 6 && a < 60 -> listOf(tx("Сердце.", "The heart."), tx("Авария на дороге.", "A road accident."), tx("Болезнь, которую поздно заметили.", "An illness found too late."))
            a < 40 -> if (sex == Sex.F && married) listOf(tx("Горячка.", "A fever."), tx("Роды.", "Childbirth."), tx("Мор.", "Plague."))
                else listOf(tx("Горячка.", "A fever."), tx("Несчастье в поле.", "An accident in the field."), tx("Мор.", "Plague."))
            a < 60 -> listOf(tx("Горячка.", "A fever."), tx("Сердце.", "The heart."), tx("Чахотка.", "Consumption."))
            else -> listOf(tx("{Уснул|Уснула} и не {проснулся|проснулась}.", "Went to sleep and did not wake."), tx("Сердце.", "The heart."), tx("Старость.", "Old age."), tx("Тихо, при детях.", "Quietly, with the children near."))
        }.random(rnd)
        val when_ = when {
            alive -> tx("Сейчас тебе ${Years.yearsRu(a)}. Дальше книга ещё не написана.", "You are $a now. The rest of the book is not written yet.")
            a < 1 -> tx("{Умер|Умерла} младенцем.", "Died in infancy.")
            a < 5 -> tx("{Умер|Умерла} на ${a + 1}-м году.", "Died aged $a.")
            else -> tx("{Умер|Умерла} в $a ${Years.plural(a, "год", "года", "лет")}.", "Died aged $a.")
        }
        add(a, if (alive) when_ else when_ + Tx(" ", " ") + how, Mark.DEATH)

        val ep = mutableListOf<Tx>()
        if (a < 5 && !alive) ep += listOf(tx("Прожил{|а} мало, но {был|была} {любим|любима}.", "Lived little, and was loved."), tx("Мать помнила до конца своих дней.", "Mother remembered to the end of her days.")).random(rnd)
        else {
            if (firstLit) ep += tx("{Первый|Первая} в роду, кто {научился|научилась} читать.", "The first in the family to read.")
            if (kids > 0) ep += if (kidsAlive < kids) tx("{Пережил|Пережила} ${Years.childrenAcc(kids - kidsAlive)}.", "Outlived ${Years.childrenEn(kids - kidsAlive)}.")
                else tx("{Вырастил|Вырастила} ${Years.childrenAcc(kids)}.", "Raised ${Years.childrenEn(kids)}.")
            if (wishSea) ep += tx("{Увидел|Увидела} море на старости лет.", "Saw the sea in old age.")
            else if (!sawSea) ep += tx("Моря так и не {увидел|увидела}.", "Never saw the sea.")
            if (kind) ep += tx("В голодный год {делился|делилась} последним.", "Shared the last of it in a hungry year.")
            if (love) ep += tx("{Выбрал|Выбрала} сердцем.", "Chose with the heart.")
            if (monk) ep += tx("{Жил|Жила} в тишине.", "Lived in silence.")
            if (taught) ep += tx("Дети читали благодаря {ему|ей}.", "The children could read because of {him|her}.")
            if (tree) ep += tx("Груша у дома цветёт и сейчас.", "The pear tree by the house still blooms.")
            if (meet != null) ep += tx("Однажды {видел|видела} того, о ком потом писали книги.", "Once met someone later written about in books.")
            if (ep.isEmpty()) ep += listOf(tx("{Жил|Жила} как все. Это немало.", "Lived like everyone. That is no small thing."), tx("{Работал|Работала} и не {жаловался|жаловалась}.", "Worked and did not complain.")).random(rnd)
        }
        if (alive) ep.add(0, tx("{Жив|Жива} и сейчас.", "Still alive."))
        val epitaph = ep.take(3).reduce { x, y -> x + Tx(" ", " ") + y }

        val f = birth.first
        val qs = mutableListOf(
            Tx("Сколько людей сегодня помнят по имени твою прабабушку?", "How many people today remember your great-grandmother's name?"),
            Tx("Что в твоём сегодняшнем дне показалось бы этому человеку чудом?", "What in your day today would look like a miracle to this person?"),
        )
        if (a < 5 && !alive) qs += Tx("Эта жизнь длилась ${Years.yearsRu(a + 1)}. Чего ты ждёшь от своих ближайших четырёх?", "This life lasted ${Years.yearsEn(a + 1)}. What do you hope for from your next four?")
        else {
            if (!sawSea) qs += tx("${f.ru} так и не {увидел|увидела} моря. Когда море в последний раз было у тебя перед глазами?", "${f.en} never saw the sea. When was the sea last in front of your eyes?")
            if (!lit && a >= 12) qs += tx("${f.ru} так и не {научился|научилась} читать. Какую книгу стоило бы прочесть вслух — и кому?", "${f.en} never learned to read. Which book would be worth reading aloud, and to whom?")
            if (byParents) qs += tx("За {него|неё} всё решили старшие. Что в твоём последнем году — твой собственный выбор?", "The elders decided everything for {him|her}. What in your last year was your own choice?")
            if (kids > 0) qs += Tx("У этого человека ${if (kids == 1) "был" else "было"} ${Years.childrenNom(kids)}. Кому стоит позвонить сегодня?", "This person had ${Years.childrenEn(kids)}. Who should you call today?")
            if (kind) qs += Tx("В голодный год этот человек делился последним. С кем можешь поделиться ты?", "In a hungry year this person shared the last of it. Who could you share with?")
            if (monk) qs += Tx("Эта жизнь прошла в тишине. Где тишина у тебя?", "This life passed in silence. Where is your silence?")
            if (meet != null) qs += Tx("Этот человек видел того, о ком потом писали книги. Кого из встреченных тобой стоит запомнить?", "This person met someone later written about. Who among the people you have met is worth remembering?")
        }
        // The life's own questions first; the two that fit any life only when it has none.
        val bridge = (if (qs.size > 2) qs.drop(2) else qs).random(rnd)

        return End(a, epitaph, bridge, if (birth.heir != null) null else Things.group(era.group).random(rnd),
            kidsAlive, wealth, rank, lit, sawSea, meet, story)
    }

    companion object {
        /** The year the books are written up to. */
        const val NOW = 2026

        private val STOPS = listOf(
            0 to S.BIRTH, 4 to S.CHILD, 8 to S.CHILD, 12 to S.WORK, 15 to S.YOUTH, 17 to S.MARRY17, 24 to S.MARRY24, 28 to S.JOY,
            33 to S.CRISIS, 39 to S.ADULT, 45 to S.LEGACY, 51 to S.ADULT, 58 to S.OLD, 64 to S.JOY, 70 to S.WISH,
            76 to S.OLD, 82 to S.OLD, 88 to S.OLD, 94 to S.OLD, 100 to S.OLD,
        )

        private val STORIES = listOf(
            Tx("о том, как прадед перехитрил волка", "how great-grandfather outwitted a wolf"),
            Tx("о годе, когда река не пришла", "about the year the river did not come"),
            Tx("как бабка видела хвостатую звезду", "how grandmother saw a star with a tail"),
            Tx("о далёком городе с золотыми крышами", "about a far city with golden roofs"),
            Tx("как ты в детстве {потерялся|потерялась} и {нашёлся|нашлась}", "how you were lost as a child and found again"),
            Tx("о кладе под старой грушей, которого никто не нашёл", "about the treasure under the old pear tree that nobody found"),
        )

        fun read(content: Content, seed: Long, heir: Heir?, picks: List<Int>, shown: Int, witness: Boolean): Page =
            Life(content, seed, heir, witness).read(picks, shown)

        /** Reads a life to its end with [pick] choosing at every fork, for the demo and the tests. */
        fun live(content: Content, seed: Long, heir: Heir?, witness: Boolean = false, pick: (Choice, Int) -> Int): Pair<Page, List<Int>> {
            val picks = mutableListOf<Int>()
            var shown = 1
            while (true) {
                val p = read(content, seed, heir, picks, shown, witness)
                when {
                    p.end != null -> return p to picks
                    p.pending != null -> picks += pick(p.pending, picks.size)
                    else -> shown++
                }
            }
        }
    }
}

object Years {
    /** «1347», «1823 до н.э.», «ок. 23 000 до н.э.». */
    fun of(y: Int): Tx = when {
        y < -9000 -> { val k = abs(y / 1000) * 1000; Tx("ок. ${group(k, ' ')} до н.э.", "c. ${group(k, ',')} BC") }
        y < 0 -> Tx("${-y} до н.э.", "${-y} BC")
        else -> Tx("$y", "$y")
    }

    /** Ends a sentence after a year without doubling the dot of «до н.э.». */
    fun stop(s: String) = if (s.endsWith(".")) s else "$s."

    /** «в 1347 году», "in 1347". */
    fun inYear(y: Int): Tx = when {
        y < -9000 -> { val k = abs(y / 1000) * 1000; Tx("около ${group(k, ' ')} года до н.э.", "around ${group(k, ',')} BC") }
        y < 0 -> Tx("в ${-y} году до н.э.", "in ${-y} BC")
        else -> Tx("в $y году", "in $y")
    }

    private fun group(n: Int, sep: Char) = n.toString().reversed().chunked(3).joinToString(sep.toString()).reversed()

    fun plural(n: Int, one: String, few: String, many: String): String {
        val a = abs(n) % 100; val b = a % 10
        return when { a in 11..19 -> many; b == 1 -> one; b in 2..4 -> few; else -> many }
    }

    fun yearsRu(n: Int) = "$n ${plural(n, "год", "года", "лет")}"
    fun yearsEn(n: Int) = if (n == 1) "one year" else "$n years"
    /** «вырастил троих детей». */
    fun childrenAcc(n: Int) = when (n) {
        1 -> "одного ребёнка"; 2 -> "двоих детей"; 3 -> "троих детей"; 4 -> "четверых детей"; 5 -> "пятерых детей"; 6 -> "шестерых детей"; 7 -> "семерых детей"
        else -> "$n детей"
    }

    /** «было трое детей». */
    fun childrenNom(n: Int) = when (n) {
        1 -> "один ребёнок"; 2 -> "двое детей"; 3 -> "трое детей"; 4 -> "четверо детей"; 5 -> "пятеро детей"; 6 -> "шестеро детей"; 7 -> "семеро детей"
        else -> "$n детей"
    }
    fun childrenEn(n: Int) = when (n) {
        1 -> "one child"; 2 -> "two children"; 3 -> "three children"; 4 -> "four children"; 5 -> "five children"; 6 -> "six children"; 7 -> "seven children"
        else -> "$n children"
    }
}
