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
    /** Someone in the family line before could read, so this life is not the first. */
    val readers: Boolean = false,
) {
    companion object {
        /** A great-grandchild is born 70 to 94 years later, and must be born by now. */
        fun possible(year: Int, kids: Int) = kids > 0 && year + 94 < 2026
    }
}

enum class Mark { PLAIN, HIST, MEET, DEATH }

/**
 * What the reader has already met before a life was drawn: the Russian text of lines in recent books and every event
 * any life has seen. Built only from lives that ended before the draw, so replaying the life later gives the same book.
 */
class Past(val read: Set<String>, val seen: Set<Int>) {
    companion object {
        val NONE = Past(emptySet(), emptySet())
    }
}

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
    /** Every real event this life met, by id. */
    val seen: List<Int> = emptyList(),
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
class Life private constructor(
    private val content: Content,
    seed: Long,
    heir: Heir?,
    private val witness: Boolean,
    private val past: Past,
    private val wish: Wish,
) : Person {
    private val rnd = Random(seed)
    val birth: Birth
    private val era: Era
    private val region: Region
    private val sex: Sex
    private val year: Int
    override var rank: Int = 0
        private set

    private var death: Int
    private var lit = false
    private val parentLit: Boolean
    /** An ancestor in the family line could read. */
    private val kinLit: Boolean
    private var firstLit = false
    private var wealth: Int
    private var married = false
    private var single = false
    private var love = false
    private var byParents = false
    private var byMaster = false
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
    private var town = false
    private var age = 0
    /** The age the first child came at; lines about school-age, grown children and grandchildren wait for the years to pass. */
    private var firstChildAt = -1
    override val now get() = year + age
    /** Words a fork's outcome leaves behind (faith, brave, moved, ...), read by the epitaph and the question. */
    private val flags = mutableSetOf<String>()
    private val eps = mutableListOf<Tx>()
    private val asks = mutableListOf<Tx>()
    private val usedForks = mutableSetOf<String>()

    override fun has(cond: String) = when (cond) {
        "m" -> sex == Sex.M
        "f" -> sex == Sex.F
        "lit" -> lit
        "illit", "!lit" -> !lit
        "married" -> married
        "single", "!married" -> !married
        "kids" -> kidsAlive > 0
        "small" -> age < 4
        "school" -> kidsAlive > 0 && firstChildAt >= 0 && age - firstChildAt >= 8
        "grown" -> kidsAlive > 0 && firstChildAt >= 0 && age - firstChildAt >= 16
        "grand" -> kidsAlive > 0 && firstChildAt >= 0 && age - firstChildAt >= 36
        "!kids" -> kidsAlive == 0
        "monk" -> monk
        "town" -> town
        "sea" -> sawSea
        "poor" -> wealth <= 0
        "rich" -> wealth >= 2
        "r0" -> rank == 0
        "r1" -> rank == 1
        "r2" -> rank == 2
        "r3" -> rank == 3
        else -> cond in flags
    }

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
            // A chosen area weighs each era by how many of its people were born there.
            val area = World.area(wish.area)
            fun inArea(x: Era) = if (area == null) 1.0 else x.regions.filter { it.key in area.regions }.sumOf { it.w } / x.regions.sumOf { it.w }
            val eras = World.ERAS.filter { (wish.eraI == null || it.i == wish.eraI) && inArea(it) > 0 }.ifEmpty { World.ERAS }
            e = weighted(eras) { it.births * inArea(it) }
            val u = rnd.nextDouble().pow(if (e.i == 0) .6 else .75)
            y = (e.to - (e.to - e.from) * (1 - u)).roundToInt().coerceIn(e.from, e.to - 1)
        }
        era = e; year = y
        region = heir?.let { h ->
            val was = World.ALL_REGIONS.firstOrNull { it.key == h.region }
            e.regions.firstOrNull { it.key == h.region } ?: was?.let { w -> e.regions.filter { r -> r.tags.any { it in w.tags } }.maxByOrNull { it.w } }
        } ?: weighted(World.area(wish.area)?.let { a -> e.regions.filter { it.key in a.regions } }?.ifEmpty { null } ?: e.regions) { it.w }
        val cult = World.culture(region, y)
        val pool = e.estates.filter { s -> s.only == null || s.only.any { it in region.tags } }
        val estate = heir?.let { h ->
            val want = (h.rank + when { h.wealth >= 4 -> 1; h.rank == 0 && h.wealth >= 1 -> 1; h.wealth <= 0 && rnd.nextDouble() < .3 -> -1; else -> 0 }).coerceIn(0, 3)
            pool.filter { it.rank == want }.takeIf { it.isNotEmpty() }?.let { c -> weighted(c) { it.w } }
        } ?: weighted(pool) { it.w }
        rank = estate.rank
        val coin = rnd.nextBoolean()
        sex = (if (heir == null) wish.sex else null) ?: if (coin) Sex.M else Sex.F
        val p = Names.pool(cult)
        val first = (if (sex == Sex.M) p.male else p.female).random(rnd)
        val sur = if (Names.keepsSurnames(cult, e.i)) heir?.sur?.takeIf { heir.cult == cult } ?: p.sur.random(rnd) else null
        val name = Names.name(cult, e.i, sex, first, sur, rnd)
        death = if (rnd.nextDouble() < q0(e, region)) rnd.nextInt(5) else tri(e.die).roundToInt()
        // A life that would outrun today stops at today, unfinished.
        if (y + death > NOW) { death = NOW - y; alive = true }
        val share = e.births / World.TOTAL * region.w / e.regions.sumOf { it.w } * estate.w / pool.sumOf { it.w } * .5
        birth = Birth(seed, e, region, cult, estate, sex, y, first, sur, name, share, heir)
        // Scribes, clergy and clerks read for a living; their children are never the first in the family.
        parentLit = (rnd.nextDouble() < if (rank >= 3) (e.lit * 6).coerceAtMost(.95) else e.lit * .6) || estate.key in READERS
        kinLit = heir?.readers == true
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
        // Abroad, the lines of the home region no longer fit; only those true anywhere do.
        // Behind the monastery gate only the lines written for it fit: no dances, weddings or fields.
        val all = content.lines(era.group, k, region).filter {
            it.need.holds(this) && ("abroad" !in flags || it.tags.isEmpty()) && (!monk || it.need.names("monk")) &&
                // A toddler's page gets only toddler lines, and an older child never does.
                (k != Kind.CHILD || it.need.names("small") == (age < 4))
        }
        val fresh = all.filterNot { it in used }
        // Lines the reader met in recent lives wait their turn while there are others.
        val unread = fresh.filterNot { it.text.g(sex).ru in past.read }
        val l = (unread.ifEmpty { fresh.ifEmpty { all } }).randomOrNull(rnd) ?: return
        used += l
        add(age, l.text, mark)
        // A line about the coast was lived by it.
        if ("sea" in l.tags) sawSea = true
        for (s in l.need.sets) when (s) {
            "town" -> town = true
            "sea" -> sawSea = true
            else -> flags += s
        }
    }

    private class Act(val door: Door, val act: () -> Unit)
    private class Ask(val choice: Choice, val acts: List<() -> Unit>)

    private fun ask(age: Int, q: Tx, vararg doors: Act) = Ask(Choice(age, q.g(sex), doors.map { Door(it.door.text.g(sex), it.door.why?.g(sex)) }), doors.map { it.act })
    private fun door(ru: String, en: String, why: Tx?, act: () -> Unit) = Act(Door(Tx(ru, en), why), act)
    private fun why(ru: String, en: String) = Tx(ru, en)

    private enum class S { BIRTH, CHILD, WORK, YOUTH, MARRY17, MARRY24, JOY, CRISIS, ADULT, LEGACY, OLD, WISH, FORK }

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
        this.age = age
        var hist: Hist? = null
        if (age >= 4) {
            val all = content.hist.filter { it.id !in usedHist && it.fits(region, era.i, year + age) && ("abroad" !in flags || "*" in it.tags) }
            // Events no life has met yet come first: they fill the chronicle.
            val h = all.filterNot { it.id in past.seen }.ifEmpty { all }.randomOrNull(rnd)
            if (h != null && rnd.nextDouble() < if (h.meet) .85 else .45) {
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
            S.FORK -> { fork(age)?.let { return it }; if (hist == null) line(age, if (age >= 58) Kind.OLD else Kind.ADULT) }
        }
        return null
    }

    private fun work(): Ask {
        val e = era.i
        val f = sex == Sex.F
        // Before the printing age girls were not taught, in the oldest times not even in a scribe's house.
        val girl = f && e in 1..4 && !lit && rank > 0 && (rank < 3 || e < 3)
        val learn = when {
            e == 0 -> why("Письма ещё не придумали.", "Writing has not been invented yet.")
            e < 6 && lit -> why("Ты уже умеешь читать.", "You can read already.")
            rank == 0 && e < 6 -> why("Несвободным учиться запрещено.", "The unfree are forbidden to learn.")
            girl -> why("Девочек грамоте не учат.", "Girls are not taught their letters.")
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
                    lit = true; firstLit = !parentLit && !kinLit
                    if (firstLit) add(12, "{Выучился|Выучилась} читать — {первым|первой} в роду.", "You learn to read, the first in your family.")
                    else if (parentLit) add(12, "{Выучился|Выучилась} читать и писать, как отец.", "You learn to read and write, like your father.")
                    else add(12, "{Выучился|Выучилась} читать и писать. Грамотные в роду были и до тебя.", "You learn to read and write. There were readers in the family before you.")
                }
            },
        )
        // A girl may still try in secret, from a brother's lessons or a father's tablets; half of them are caught.
        if (girl) doors += door("Учиться тайком", "Learn in secret", null) {
            if (rnd.nextBoolean()) {
                lit = true; firstLit = !parentLit && !kinLit; flags += "secretlit"
                if (parentLit) add(12, "Подглядывала, как пишет отец, и тайком выучила буквы. Он знает и молчит.", "You watched your father write and learned the letters in secret. He knows and says nothing.")
                else add(12, "Брат по вечерам показывал тебе буквы, пока никто не видит. Ты выучила все.", "In the evenings your brother showed you the letters when no one was looking. You learned them all.")
            } else {
                add(12, "Тебя застали над письменами. Выпороли, и к письму больше не подпускают.", "You are caught bent over the writing. You are beaten and kept away from it from then on.")
            }
        }
        doors.addAll(listOf(
            door(if (e == 6) "Уехать к родне в большой город" else "Уйти в город, в ученики к мастеру", if (e == 6) "Go to relatives in the big city" else "Go to town as an apprentice", town) {
                this@Life.town = true
                if (rnd.nextBoolean()) wealth++
                if (rnd.nextDouble() < .35) sawSea = true
                if (e == 6) add(12, "{Уехал|Уехала} в город. Спишь у дальней родни на полу.", "You move to the city. You sleep on a distant relative's floor.")
                else add(12, "{Ушёл|Ушла} в город, в ученики. Мастер бьёт, но кормит.", "You go to town as an apprentice. The master beats you, but feeds you.")
            },
        ))
        if (e in 2..4) doors += door("Уйти в монастырь", "Enter a monastery", when {
            rank == 0 -> why("Несвободных не постригают.", "The unfree may not take vows.")
            region.tags.none { it in setOf("eu", "ru", "cn", "jp", "sea", "in") } -> why("Здесь нет монастырей.", "There are no monasteries here.")
            else -> null
        }) {
            monk = true
            if (rnd.nextDouble() < .7) { lit = true; firstLit = !parentLit && !kinLit }
            add(12, "{Ушёл|Ушла} в монастырь. Семьи не будет — будет тишина и молитва.", "You enter a monastery. There will be no family: there will be silence and prayer.")
        }
        return ask(12, Tx("Детство кончилось. Что дальше?", "Childhood is over. What next?"), *doors.toTypedArray())
    }

    private fun marry(age: Int): Ask {
        val e = era.i
        val f = sex == Sex.F
        // The unfree are married off by their master, not by their parents.
        val owned = rank == 0 && e in 1..5
        val forced = when {
            owned -> why("Кого тебе взять, решает хозяин.", "Your master decides whom you marry.")
            e in 1..4 && (f || rank >= 3 || e < 3) -> why("Выбирать тебе не дают: старшие уже сговорились.", "You are given no choice: the elders have already agreed.")
            else -> null
        }
        return ask(age, if (age < 20) Tx("Тебя сватают.", "A match is being made for you.") else Tx("Пора заводить семью.", "Time to start a family."),
            door(if (owned) "Как решил хозяин" else "Как решили родители", if (owned) "As your master decided" else "As your parents decided", null) {
                if (owned) byMaster = true else byParents = true
                if (owned) wed(age, "Хозяин свёл тебя с {невольницей|невольником} со своего двора. {Жену|Мужа} ты до этого видел{|а} только издали.", "Your master pairs you with {a woman|a man} from his own yard. Until then you had seen {her|him} only from afar.")
                else wed(age, "Свадьбу сыграли, как решили старшие. {Жену|Мужа} ты впервые {увидел|увидела} за неделю до неё.", "The wedding is held as the elders decided. You first saw your {wife|husband} a week before.")
            },
            door(if (e == 0) "Уйти к тому, кто нравится, в чужой род" else "По любви", if (e == 0) "Go to the one you like, in another band" else "For love", forced) {
                love = true
                if (e == 0) wed(age, "{Ушёл в чужой род за той, что нравилась.|Ушла в чужой род за тем, кто нравился.}", "You go to another band for the one you like.")
                else wed(age, "{Женился|Вышла замуж} по любви. Родня ворчала, потом привыкла.", "You marry for love. The family grumbles, then gets used to it.")
            },
            door("Не заводить семью", "Stay unmarried", when {
                owned -> why("Хозяин не спрашивает, хочешь ли ты семью.", "Your master does not ask whether you want a family.")
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
        add(age, ru, en)
        bear()
        val n = kids
        val at = age + 2
        when {
            n == 0 -> add(at, "Детей не было.", "There are no children.")
            n == kidsAlive -> add(at, "Родилось детей: $n.", "Children born: $n.")
            else -> add(at, "Родилось детей: $n. Выросли ${kidsAlive}.", "Children born: $n. Grew up: $kidsAlive.")
        }
    }

    /** A marriage and the children it brings, as the era allowed them to live. */
    private fun bear() {
        married = true; single = false
        val n = (era.fert + (rnd.nextDouble() - .5) * 4).roundToInt().coerceAtLeast(0)
        kids = n
        kidsAlive = if (era.i >= 6) n else (n * (1 - era.q0) * (.8 + rnd.nextDouble() * .4)).roundToInt().coerceAtMost(n)
        if (n > 0 && firstChildAt < 0) firstChildAt = age + 1
    }

    /** An everyday fork from the dilemmas that fit this era, place, person and age; null when none fits. */
    private fun fork(age: Int): Ask? {
        if (rnd.nextDouble() < .2) return null
        val all = content.dilemmas.filter {
            it.id !in usedForks && era.group in it.groups && age in it.ages && it.fits(region) && it.need.holds(this) &&
                ("abroad" !in flags || "*" in it.tags || it.tags.isEmpty()) &&
                // A monk has taken vows: only the forks of the cloister.
                (!monk || it.need.names("monk"))
        }
        // A fork met in a recent life (one of its outcomes is in that book) waits while others are left.
        val c = all.filterNot { d -> d.doors.any { g -> g.outs.any { it.text.g(sex).ru in past.read } } }.ifEmpty { all }
        if (c.isEmpty()) return null
        val d = weighted(c) { it.weight }
        usedForks += d.id
        val acts = d.doors.map { g ->
            Act(Door(g.text, g.shut.firstOrNull { sh -> sh.conds.all { has(it) } }?.why)) {
                val o = weighted(g.outs) { it.weight }
                add(age, o.text)
                effects(age, o.fx)
                g.ep?.let { eps += it.g(sex) }
                g.ask?.let { asks += it.g(sex) }
            }
        }
        if (acts.none { it.door.open }) return null
        return ask(age, d.q, *acts.toTypedArray())
    }

    private fun effects(age: Int, fx: List<String>) {
        for (f in fx) when {
            f.startsWith("wealth") -> wealth = (wealth + f.removePrefix("wealth").toInt()).coerceAtLeast(0)
            f.startsWith("risk") -> if (rnd.nextDouble() * 100 < f.removePrefix("risk").toInt()) death = minOf(death, age + 1 + rnd.nextInt(3))
            f == "lit" -> if (!lit) { lit = true; firstLit = !parentLit && !kinLit }
            f == "sea" -> sawSea = true
            f == "kind" -> kind = true
            f == "love" -> love = true
            f == "town" -> town = true
            f == "free" -> if (rank == 0) rank = 1
            f == "kids+1" -> { kids++; kidsAlive++; if (firstChildAt < 0) firstChildAt = age }
            f == "kids-1" -> if (kidsAlive > 0) kidsAlive--
            f == "married" -> { if (!married) bear(); flags -= "widowed" }
            else -> flags += f
        }
    }

    private fun crisis(): Ask {
        val unfree = rank == 0 && era.i < 6
        return ask(33, Tx("Беда. Что делать?", "Hard times. What now?"),
            door(if (monk) "Переждать в обители" else "Переждать дома", if (monk) "Sit it out in the monastery" else "Sit it out at home", null) {
                if (rnd.nextDouble() < .25) {
                    death = minOf(death, 33 + rnd.nextInt(3))
                    add(33, "Пережидали. Не все пережили.", "You wait it out. Not everyone lives through it.")
                } else {
                    wealth = (wealth - 1).coerceAtLeast(0)
                    add(33, "Пережидали. Выжили, но всё нажитое ушло.", "You wait it out. You live, but everything you had is gone.")
                }
            },
            door(when { era.i == 0 -> "Уйти всем родом за зверем"; monk -> "Уйти с братией в другую обитель"; unfree -> "Бежать"; else -> "Уйти всей семьёй в другие края" },
                when { era.i == 0 -> "Move the whole band after the game"; monk -> "Leave with the others for another monastery"; unfree -> "Run away"; else -> "Leave with the family for other lands" }, null) {
                if (rnd.nextDouble() < .4) sawSea = true
                if (monk) {
                    val lost = rnd.nextDouble() < .15
                    if (lost) death = minOf(death, 35)
                    add(33, if (lost) Tx("Ушли. Дорога забрала многих.", "You leave. The road takes many.")
                        else Tx("Ушли. Дорога забрала стариков, новая обитель приняла остальных.", "You leave. The road takes the old ones; another monastery takes in the rest."))
                } else if (unfree && rnd.nextDouble() < .4) {
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
            door(if (monk) "Раздать монастырский хлеб голодным" else "Поделиться последним с соседями", if (monk) "Give the monastery's bread to the hungry" else "Share the last you have with the neighbours",
                if (wealth <= 0 && era.i > 0) why("Делиться нечем: у вас самих пусто.", "There is nothing to share: your own stores are empty.") else null) {
                kind = true
                wealth = (wealth - 1).coerceAtLeast(0)
                if (monk) add(33, "Хлеб раздали у ворот. Обитель голодает вместе со всеми, и этого не забудут.", "The bread is given out at the gate. The monastery goes hungry with everyone, and it will not be forgotten.")
                else add(33, "{Отдал|Отдала} соседям половину запасов. Этого не забудут.", "You give the neighbours half of what you have. It will not be forgotten.")
            },
        )
    }

    private fun legacy(): Ask {
        if (monk) return cloisterLegacy()
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

    /** A monk leaves no children: what is left is the novices, the books and the stories. */
    private fun cloisterLegacy(): Ask {
        val unread = if (!lit) why("Ты и сам{|а} не умеешь читать.", "You cannot read yourself.") else null
        return ask(45, Tx("Что оставить после себя?", "What will you leave behind?"),
            door("Учить послушников грамоте", "Teach the novices to read", unread) {
                taught = true
                add(45, "Учишь послушников грамоте. Самый бестолковый потом пишет лучше всех.", "You teach the novices their letters. The slowest one ends up writing best of all.")
            },
            door("Переписать книгу для обители", "Copy a book for the monastery", unread) {
                add(45, "Год над одной книгой. Она будет стоять в библиотеке, когда тебя забудут.", "A year over one book. It will stand in the library when you are forgotten.")
            },
            door("Рассказывать истории", "Tell them stories", null) {
                val s = STORIES.random(rnd).g(sex)
                story = s
                add(45, Tx("Вечерами рассказываешь послушникам ${s.ru}. Они просят ещё.", "In the evenings you tell the novices ${s.en}. They ask for more."))
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
        door(if (monk) "Посадить дерево в монастырском саду" else "Посадить дерево у дома", if (monk) "Plant a tree in the monastery garden" else "Plant a tree by the house", when {
            era.i == 0 -> why("Дома нет: род кочует.", "There is no house: the band moves on.")
            rank == 0 && era.i < 6 && !monk -> why("Земля не твоя.", "The land is not yours.")
            else -> null
        }) {
            tree = true
            add(70, "{Посадил|Посадила} грушу. Плодов ты не увидишь.", "You plant a pear tree. You will not see its fruit.")
        },
    )

    private fun die(): End {
        val a = death
        // A fork's risk may have ended a life that was still running.
        if (alive && a < NOW - year) alive = false
        val how = when {
            a < 5 -> listOf(tx("Лихорадка.", "A fever."), tx("Не {пережил|пережила} зиму.", "The winter was too long."), tx("Жар и понос, три дня.", "Three days of fever and flux."))
            era.i == 6 && a < 60 -> listOf(tx("Сердце.", "The heart."), tx("Авария на дороге.", "A road accident."), tx("Болезнь, которую поздно заметили.", "An illness found too late."))
            a < 40 -> if (sex == Sex.F && married) listOf(tx("Горячка.", "A fever."), tx("Роды.", "Childbirth."), tx("Мор.", "Plague."))
                else listOf(tx("Горячка.", "A fever."), tx("Несчастье в поле.", "An accident in the field."), tx("Мор.", "Plague."))
            a < 60 -> listOf(tx("Горячка.", "A fever."), tx("Сердце.", "The heart."), tx("Чахотка.", "Consumption."))
            else -> listOf(tx("{Уснул|Уснула} и не {проснулся|проснулась}.", "Went to sleep and did not wake."), tx("Сердце.", "The heart."), tx("Старость.", "Old age."),
                when { monk -> tx("Тихо, под пение {братии|сестёр}.", "Quietly, while the others sang."); kidsAlive > 0 -> tx("Тихо, при детях.", "Quietly, with the children near."); else -> tx("Тихо, во сне.", "Quietly, in sleep.") })
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
            ep += eps.take(2)
            if (firstLit) ep += tx("{Первый|Первая} в роду, кто {научился|научилась} читать.", "The first in the family to read.")
            if ("secretlit" in flags) ep += tx("Грамоте выучилась тайком.", "Learned her letters in secret.")
            if (birth.estate.rank == 0 && rank > 0) ep += tx("{Родился|Родилась} несвободным, {умер|умерла} {свободным|свободной}.", "Born unfree, died free.")
            if (kids > 0) ep += if (kidsAlive < kids) tx("{Пережил|Пережила} ${Years.childrenAcc(kids - kidsAlive)}.", "Outlived ${Years.childrenEn(kids - kidsAlive)}.")
                else tx("{Вырастил|Вырастила} ${Years.childrenAcc(kids)}.", "Raised ${Years.childrenEn(kids)}.")
            if (wishSea) ep += tx("{Увидел|Увидела} море на старости лет.", "Saw the sea in old age.")
            else if (!sawSea) ep += tx("Моря так и не {увидел|увидела}.", "Never saw the sea.")
            if (kind) ep += tx("В голодный год {делился|делилась} последним.", "Shared the last of it in a hungry year.")
            if (love) ep += tx("{Выбрал|Выбрала} сердцем.", "Chose with the heart.")
            if (monk) ep += tx("{Жил|Жила} в тишине.", "Lived in silence.")
            if (taught) ep += if (monk) tx("Послушники читали благодаря {ему|ей}.", "The novices could read because of {him|her}.") else tx("Дети читали благодаря {ему|ей}.", "The children could read because of {him|her}.")
            if (tree) ep += tx("Груша у дома цветёт и сейчас.", "The pear tree by the house still blooms.")
            if (meet != null) ep += tx("Однажды {видел|видела} того, о ком потом писали книги.", "Once met someone later written about in books.")
            if ("faith" in flags) ep += tx("{Верил|Верила} крепко.", "Kept the faith.")
            if ("brave" in flags) ep += tx("Не {побоялся|побоялась}.", "Was not afraid.")
            if ("moved" in flags) ep += tx("{Умер|Умерла} далеко от места, где {родился|родилась}.", "Died far from the place of birth.")
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
            if (byMaster) qs += tx("За {него|неё} всё решал хозяин, даже с кем жить. Что в твоём последнем году — твой собственный выбор?", "A master decided everything for {him|her}, even whom to live with. What in your last year was your own choice?")
            if (byParents) qs += tx("За {него|неё} всё решили старшие. Что в твоём последнем году — твой собственный выбор?", "The elders decided everything for {him|her}. What in your last year was your own choice?")
            if (kids > 0) qs += Tx("У этого человека ${if (kids == 1) "был" else "было"} ${Years.childrenNom(kids)}. Кому стоит позвонить сегодня?", "This person had ${Years.childrenEn(kids)}. Who should you call today?")
            if (kind) qs += Tx("В голодный год этот человек делился последним. С кем можешь поделиться ты?", "In a hungry year this person shared the last of it. Who could you share with?")
            if (monk) qs += Tx("Эта жизнь прошла в тишине. Где тишина у тебя?", "This life passed in silence. Where is your silence?")
            if (meet != null) qs += Tx("Этот человек видел того, о ком потом писали книги. Кого из встреченных тобой стоит запомнить?", "This person met someone later written about. Who among the people you have met is worth remembering?")
        }
        if (a >= 5) {
            if (town) qs += Tx("Этот человек ушёл в город за другой жизнью. Куда хочется уйти тебе?", "This person went to town for another life. Where do you want to go?")
            if ("moved" in flags) qs += Tx("Этот человек всё бросил и ушёл. Что держит на месте тебя?", "This person left everything behind. What keeps you where you are?")
            if ("faith" in flags) qs += Tx("Этот человек крепко верил. Во что веришь ты?", "This person kept the faith. What do you believe in?")
            if (birth.estate.rank == 0) qs += Tx("Этот человек родился несвободным. Что ты сегодня можешь сделать, не спрашивая разрешения?", "This person was born unfree. What can you do today without asking anyone's leave?")
            if (a >= 80) qs += Tx("Этот человек прожил ${Years.yearsRu(a)}. Что хочется успеть к этому возрасту?", "This person lived to $a. What do you want to have done by that age?")
            if (alive) qs += Tx("Эта жизнь ещё идёт. Что хочется ей пожелать?", "This life is still going. What would you wish it?")
            qs += asks
        }
        // The life's own questions most of the time; now and then one of the questions any life can ask.
        val own = qs.drop(2)
        val bridge = if (own.isNotEmpty() && rnd.nextDouble() < .75) own.random(rnd) else (qs.take(2) + ANY_LIFE).random(rnd)

        return End(a, epitaph, bridge, if (birth.heir != null) null else Things.pick(era.group, region, rnd),
            kidsAlive, wealth, rank, lit, sawSea, meet, story, usedHist.toList())
    }

    companion object {
        /** The engine version written into each life: 1 was 1.0.0, 2 brought forks, needs and kept books. */
        const val VERSION = 2

        /** Estates whose fathers read for a living. */
        private val READERS = setOf("scribe", "clergy", "clerk", "office")

        /** The year the books are written up to. */
        const val NOW = 2026

        private val STOPS = listOf(
            0 to S.BIRTH, 2 to S.CHILD, 5 to S.CHILD, 8 to S.CHILD, 10 to S.CHILD, 12 to S.WORK, 14 to S.YOUTH, 16 to S.YOUTH,
            17 to S.MARRY17, 19 to S.FORK, 21 to S.JOY, 24 to S.MARRY24, 27 to S.FORK, 30 to S.ADULT, 33 to S.CRISIS, 36 to S.JOY,
            39 to S.FORK, 42 to S.ADULT, 45 to S.LEGACY, 48 to S.ADULT, 51 to S.FORK, 54 to S.JOY, 58 to S.OLD, 61 to S.OLD,
            64 to S.FORK, 67 to S.JOY, 70 to S.WISH, 73 to S.OLD, 76 to S.OLD, 80 to S.OLD, 84 to S.OLD, 88 to S.OLD, 92 to S.OLD,
            96 to S.OLD, 100 to S.OLD,
        )

        /** Questions any life can ask its reader. */
        private val ANY_LIFE = listOf(
            Tx("Какую вещь из твоего дома стоило бы передать правнукам?", "Which thing in your home would be worth handing down to your great-grandchildren?"),
            Tx("Чьи руки вырастили хлеб, который ты ешь?", "Whose hands grew the bread you eat?"),
            Tx("Какая дверь сегодня открыта для тебя, но была бы закрыта для этого человека?", "Which door is open to you today that would have been shut to this person?"),
            Tx("Чему этот человек научил бы тебя за один вечер?", "What could this person teach you in one evening?"),
            Tx("Какую историю о своей семье ты знаешь — и кому её расскажешь?", "Which story of your family do you know, and who will you tell it to?"),
            Tx("Что сегодня стоило бы сделать руками?", "What would be worth doing with your hands today?"),
            Tx("Кого из ушедших тебе не хватает?", "Who among those gone do you miss?"),
            Tx("О чём этот человек не мог даже мечтать, а у тебя это есть?", "What could this person not even dream of that you have?"),
            Tx("Когда вокруг в последний раз было по-настоящему темно и тихо?", "When was it last truly dark and quiet around you?"),
            Tx("Кому сегодня можно помочь так, как помогли бы в деревне?", "Who could you help today the way a village would?"),
            Tx("Какой праздник этот человек ждал бы весь год — а какой ждёшь ты?", "Which feast would this person have waited for all year, and which do you wait for?"),
            Tx("Что из того, что у тебя есть, этот человек счёл бы богатством?", "What that you own would this person have called wealth?"),
        )

        private val STORIES = listOf(
            Tx("о том, как прадед перехитрил волка", "how great-grandfather outwitted a wolf"),
            Tx("о годе, когда река не пришла", "about the year the river did not come"),
            Tx("как бабка видела хвостатую звезду", "how grandmother saw a star with a tail"),
            Tx("о далёком городе с золотыми крышами", "about a far city with golden roofs"),
            Tx("как ты в детстве {потерялся|потерялась} и {нашёлся|нашлась}", "how you were lost as a child and found again"),
            Tx("о кладе под старой грушей, которого никто не нашёл", "about the treasure under the old pear tree that nobody found"),
            Tx("о прабабке, которая пешком дошла до моря", "about the great-grandmother who walked all the way to the sea"),
            Tx("о зиме, когда замёрзли птицы", "about the winter the birds froze"),
            Tx("как дед один раз видел царя", "how grandfather once saw the king"),
            Tx("о соседе, который ушёл за удачей и вернулся через двадцать лет", "about the neighbour who went to seek his fortune and came back twenty years later"),
            Tx("о свадьбе, на которой сгорел сарай", "about the wedding at which the barn burned down"),
            Tx("о говорящей сороке", "about the talking magpie"),
            Tx("о годе, когда лёд на реке простоял до лета", "about the year the ice stayed on the river till summer"),
            Tx("о лекарке, которая знала все травы", "about the healer who knew every herb"),
            Tx("о том, откуда в роду эта вещь", "about where the family keepsake came from"),
        )

        fun read(content: Content, seed: Long, heir: Heir?, picks: List<Int>, shown: Int, witness: Boolean, past: Past = Past.NONE, wish: Wish = Wish()): Page =
            Life(content, seed, heir, witness, past, wish).read(picks, shown)

        /** Reads a life to its end with [pick] choosing at every fork, for the demo and the tests. */
        fun live(content: Content, seed: Long, heir: Heir?, witness: Boolean = false, past: Past = Past.NONE, pick: (Choice, Int) -> Int): Pair<Page, List<Int>> {
            val picks = mutableListOf<Int>()
            var shown = 1
            while (true) {
                val p = read(content, seed, heir, picks, shown, witness, past)
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
