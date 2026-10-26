package app.lot

import app.lot.domain.Content
import app.lot.domain.Heir
import app.lot.domain.Life
import app.lot.domain.Names
import app.lot.domain.World
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class LifeTest {
    private val content = File("src/main/assets").let { a ->
        Content.read(File(a, "lines").listFiles()!!.sorted().map { it.inputStream() }, File(a, "history.tsv").inputStream(),
            a.listFiles()!!.filter { it.name.startsWith("dilemmas") && it.name.endsWith(".txt") }.sorted().map { it.inputStream() })
    }

    @Test fun replaysTheSameLife() {
        val (a, picks) = Life.live(content, 42L, null) { ch, i -> ch.doors.indices.filter { ch.doors[it].open }.let { it[i % it.size] } }
        val b = Life.read(content, 42L, null, picks, Int.MAX_VALUE, false)
        assertEquals(a.entries, b.entries)
        assertEquals(a.end!!.epitaph, b.end!!.epitaph)
    }

    @Test fun everyLifeEndsAndEveryForkHasAnOpenDoor() {
        val rnd = Random(5)
        repeat(3000) { n ->
            val (p, _) = Life.live(content, n.toLong(), null, witness = n % 4 == 0) { ch, _ ->
                assertTrue(ch.doors.any { it.open })
                ch.doors.indices.filter { ch.doors[it].open }.random(rnd)
            }
            val end = p.end!!
            assertTrue(end.age in 0..100)
            assertTrue(p.entries.last().text.ru.let { it.startsWith("Умер") || it.startsWith("Сейчас") })
            assertTrue(p.end!!.seen.distinct().size == p.end!!.seen.size)
            for (e in p.entries) {
                assertTrue("unmarked ${e.text}", '{' !in e.text.ru && '{' !in e.text.en)
                assertTrue(e.text.ru.isNotBlank() && e.text.en.isNotBlank())
            }
        }
    }

    /** The sack is fair: over many draws the eras come out in the shares of everyone ever born. */
    @Test fun drawsFollowTheBirths() {
        val n = 20000
        val counts = IntArray(World.ERAS.size)
        repeat(n) { counts[Life.read(content, it.toLong() * 7919, null, emptyList(), 1, false).birth.era.i]++ }
        for (e in World.ERAS) {
            val want = e.births / World.TOTAL
            val got = counts[e.i].toDouble() / n
            assertTrue("${e.name.en}: $got vs $want", kotlin.math.abs(got - want) < .015)
        }
    }

    @Test fun greatGrandchildrenInherit() {
        val first = Life.read(content, 11L, null, emptyList(), 1, false).birth
        val heir = Heir(1, first.name, first.sex, 1700, "ru", "ru", 1, 2, Names.pool("ru").sur[0], 5, app.lot.domain.Tx("ложка", "a spoon"), null, 11L, 1)
        repeat(50) { s ->
            val b = Life.read(content, s.toLong(), heir, emptyList(), 1, false).birth
            assertTrue(b.year in 1770..1794)
            assertEquals("ru", b.region.key)
            assertTrue(b.name.ru.contains("Иванов"))
        }
    }

    /** Half a year of play: a life a day for 180 days, read with fresh eyes. How often does a line come back? */
    @Test fun halfAYearStaysFresh() {
        val rnd = Random(180)
        val seen = mutableMapOf<String, Int>()
        var total = 0
        var forks = 0
        val books = ArrayDeque<List<String>>()
        val events = mutableSetOf<Int>()
        repeat(180) { n ->
            val past = app.lot.domain.Past(books.flatten().toSet(), events.toSet())
            val (p, _) = Life.live(content, rnd.nextLong(), null, past = past) { ch, _ -> forks++; ch.doors.indices.filter { ch.doors[it].open }.random(rnd) }
            books.addLast(p.entries.map { it.text.ru }); if (books.size > 40) books.removeFirst()
            events += p.end!!.seen
            for (e in p.entries.filter { it.mark == app.lot.domain.Mark.PLAIN }) { total++; seen[e.text.ru] = (seen[e.text.ru] ?: 0) + 1 }
        }
        val repeats = seen.values.sumOf { it - 1 }
        println("half a year: $total plain entries, ${seen.size} distinct, repeats ${100 * repeats / total}%, forks $forks")
        assertTrue("too many repeats: $repeats of $total", repeats * 100 / total < 45)
    }

    @Test fun everyForkParses() {
        assertTrue(content.dilemmas.size >= 40)
        for (d in content.dilemmas) {
            assertTrue(d.id, d.doors.size in 2..3)
            assertTrue(d.id, d.doors.any { it.shut.isEmpty() })
            for (g in d.doors) for (o in g.outs) assertTrue(d.id, '{' !in app.lot.domain.Tx.mark(o.text.ru, app.lot.domain.Sex.F))
        }
    }

    /** A wish narrows the lot and keeps the weights inside it; a wish that never existed is not possible to choose. */
    @Test fun wishesNarrowTheLot() {
        val ru = app.lot.domain.Wish(area = "ru")
        repeat(300) { n ->
            val b = Life.read(content, n.toLong(), null, emptyList(), 1, false, wish = ru).birth
            assertTrue(b.region.key == "ru")
            assertTrue(b.era.i >= 2)
        }
        val girl = app.lot.domain.Wish(eraI = 1, area = "in", sex = app.lot.domain.Sex.F)
        repeat(300) { n ->
            val b = Life.read(content, n.toLong(), null, emptyList(), 1, false, wish = girl).birth
            assertEquals(1, b.era.i); assertEquals("in", b.region.key); assertEquals(app.lot.domain.Sex.F, b.sex)
        }
        // China holds far more people than Japan, so a lot from "Asia's east" without an era still lands mostly in the old eras.
        val jp = (0 until 400).map { Life.read(content, it.toLong(), null, emptyList(), 1, false, wish = app.lot.domain.Wish(area = "jp")).birth.era.i }
        assertTrue(jp.all { it >= 2 })
        assertTrue(!World.fits(0, "ru") && World.fits(null, "ru") && World.fits(6, "au"))
    }

    /**
     * Read the way the app reads: a page at a time, a door picked from what the page shows. The door that was shown open must be
     * the one that acts, and the book must never rewrite what it already wrote.
     */
    @Test fun thePickedDoorIsTheOneThatActs() {
        val rnd = Random(7)
        repeat(400) { n ->
            val seed = rnd.nextLong()
            val wish = if (n % 3 == 0) app.lot.domain.Wish(area = "eu") else app.lot.domain.Wish()
            val picks = mutableListOf<Int>()
            var shown = 1
            var before = emptyList<app.lot.domain.Entry>()
            while (true) {
                val p = Life.read(content, seed, null, picks, shown, false, wish = wish)
                assertEquals("life $n rewrote its book", before, p.entries.take(before.size))
                before = p.entries
                if (p.end != null) break
                val ch = p.pending
                if (ch == null) { shown++; continue }
                val open = ch.doors.indices.filter { ch.doors[it].open }
                val door = open.random(rnd)
                picks += door
                val after = Life.read(content, seed, null, picks, shown, false, wish = wish)
                assertTrue("life $n: door $door of ${ch.q.en} did not act", after.pending == null)
            }
        }
    }

    /** Whoever enters a monastery is never wed, matched or given children afterwards. */
    @Test fun aMonkStaysUnwed() {
        val rnd = Random(5)
        val worldly = Regex("Свадьбу сыграли|Женился|Вышла замуж|Родилось детей|ты невеста|тебе (невесту|жениха)|Тебя сосватали|Тебя выдали замуж|твоей свадьбе|огню жениха")
        val village = Regex("свадьб|пляс|танц|ярмарк|сватают|при детях", RegexOption.IGNORE_CASE)
        var monks = 0
        repeat(4000) { n ->
            val (p, _) = Life.live(content, n.toLong() * 17, null) { ch, _ ->
                val open = ch.doors.indices.filter { ch.doors[it].open }
                open.firstOrNull { ch.doors[it].text.en == "Enter a monastery" } ?: open.random(rnd)
            }
            val at = p.entries.indexOfFirst { it.text.en.startsWith("You enter a") && "monastery" in it.text.en }
            if (at < 0) return@repeat
            monks++
            val late = p.entries.drop(at + 1).firstOrNull { worldly.containsMatchIn(it.text.ru) }
            assertTrue("life $n took vows and then: ${late?.text?.ru}", late == null)
            // Behind the gate the book has no dances, fairs or feasts of the village; real events still reach it.
            val loud = p.entries.drop(at + 1).firstOrNull { it.mark == app.lot.domain.Mark.PLAIN && !it.text.ru.contains("рассказываешь", ignoreCase = true) && village.containsMatchIn(it.text.ru) }
            assertTrue("life $n took vows and then: ${loud?.text?.ru}", loud == null)
        }
        assertTrue(monks > 20)
    }

    /** A great-grandchild of a reader is never "the first in the family" to read; a girl kept from letters may try in secret. */
    @Test fun lettersRunInTheFamily() {
        val rnd = Random(4)
        var secret = 0
        repeat(1500) { n ->
            val heir = Heir(1, app.lot.domain.Tx("Анна", "Anna"), app.lot.domain.Sex.F, -900, "me", "me", 3, 2, null, 5, app.lot.domain.Tx("ложка", "a spoon"), null, 11L, 1, readers = true)
            val (p, _) = Life.live(content, n.toLong() * 23, heir) { ch, _ ->
                val open = ch.doors.indices.filter { ch.doors[it].open }
                open.firstOrNull { ch.doors[it].text.en.startsWith("Learn") } ?: open.random(rnd)
            }
            val first = p.entries.firstOrNull { "в роду" in it.text.ru && "перв" in it.text.ru }
            assertTrue("life $n: ${first?.text?.ru}", first == null)
            assertTrue("first in family in the epitaph of life $n", "Первая в роду" !in p.end!!.epitaph.ru && "Первый в роду" !in p.end!!.epitaph.ru)
            if (p.entries.any { it.text.en.startsWith("You are caught bent") || "in secret" in it.text.en || "your brother showed you" in it.text.en }) secret++
        }
        assertTrue("no girl ever tried in secret", secret > 0)
    }

    /** The unfree are matched by their master, never "as the parents decided"; the elders' fork about a sister is not theirs. */
    @Test fun aMasterMatchesTheUnfree() {
        val rnd = Random(6)
        var owned = 0
        repeat(3000) { n ->
            val qs = mutableListOf<app.lot.domain.Choice>()
            val (p, _) = Life.live(content, n.toLong() * 19, null) { ch, _ -> qs += ch; ch.doors.indices.filter { ch.doors[it].open }.random(rnd) }
            if (p.birth.estate.rank != 0 || p.birth.era.i !in 1..5) return@repeat
            // Before 19 nothing can have freed the life yet.
            val wed = qs.firstOrNull { c -> c.age < 19 && c.doors.any { it.text.en.startsWith("As your") } } ?: return@repeat
            owned++
            assertTrue("life $n: ${wed.doors.map { it.text.en }}", wed.doors.none { it.text.en == "As your parents decided" })
            assertTrue("life $n met the elders' sister fork", qs.none { it.q.en.startsWith("The elders have decided to give your sister") })
        }
        assertTrue(owned > 20)
    }

    /** A line never names the age it is written at: the same line may fall at 2, 5, 8 or 10. */
    @Test fun linesDoNotNameTheirAge() {
        val age = Regex("Тебе [а-яё ]{0,12}(год|года|лет)\\b|You are (one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)\\b")
        val named = content.lines.filter { age.containsMatchIn(it.text.ru) || age.containsMatchIn(it.text.en) }.map { it.text.ru }
        assertTrue(named.joinToString("\n"), named.isEmpty())
    }

    /** A daughter is not married off, nor grandchildren met, the year the children are born. */
    @Test fun childrenTakeYearsToGrow() {
        val sexes = listOf(app.lot.domain.Sex.M, app.lot.domain.Sex.F)
        fun texts(t: String) = content.lines.filter { it.need.names(t) }.flatMap { l -> sexes.map { l.text.g(it).ru } }.toSet()
        val wait = mapOf(texts("school") to 7, texts("grown") to 15, texts("grand") to 35)
        val rnd = Random(8)
        var checked = 0
        repeat(3000) { n ->
            val (p, _) = Life.live(content, n.toLong() * 29, null) { ch, _ -> ch.doors.indices.filter { ch.doors[it].open }.random(rnd) }
            val born = p.entries.firstOrNull { it.text.ru.startsWith("Родилось детей") }?.age ?: return@repeat
            for (e in p.entries) for ((set, years) in wait) if (e.text.ru in set) {
                checked++
                assertTrue("life $n: children born at $born, and at ${e.age}: ${e.text.ru}", e.age - born >= years)
            }
        }
        assertTrue(checked > 50)
    }

    /** At two a child is a toddler: no goats to herd, no lessons, no milk teeth falling out. */
    @Test fun aToddlerIsAToddler() {
        val sexes = listOf(app.lot.domain.Sex.M, app.lot.domain.Sex.F)
        val small = content.lines.filter { it.need.names("small") }.flatMap { l -> sexes.map { l.text.g(it).ru } }.toSet()
        val older = content.lines.filter { it.kind == app.lot.domain.Kind.CHILD && !it.need.names("small") }.flatMap { l -> sexes.map { l.text.g(it).ru } }.toSet()
        val rnd = Random(12)
        repeat(2000) { n ->
            val (p, _) = Life.live(content, n.toLong() * 31, null) { ch, _ -> ch.doors.indices.filter { ch.doors[it].open }.random(rnd) }
            for (e in p.entries) {
                assertTrue("life $n at ${e.age}: ${e.text.ru}", !(e.age < 4 && e.text.ru in older))
                assertTrue("life $n at ${e.age}: ${e.text.ru}", !(e.age >= 4 && e.text.ru in small))
            }
        }
    }

    /** A life whose book has been to the sea is never mourned as one that never saw it. */
    @Test fun whoSawTheSeaIsNotSaidNeverTo() {
        val sexes = listOf(app.lot.domain.Sex.M, app.lot.domain.Sex.F)
        val seaside = content.lines.filter { "sea" in it.need.sets }.flatMap { l -> sexes.map { l.text.g(it).ru } }.toSet()
        val rnd = Random(3)
        var met = 0
        repeat(3000) { n ->
            val (p, _) = Life.live(content, n.toLong() * 13, null) { ch, _ -> ch.doors.indices.filter { ch.doors[it].open }.random(rnd) }
            if (p.entries.none { it.text.ru in seaside }) return@repeat
            met++
            assertTrue("life $n went to the sea, and yet: ${p.end!!.epitaph.ru}", "Моря так и не" !in p.end!!.epitaph.ru)
        }
        assertTrue(met > 20)
    }

    /** Once a life has moved to town (a flat, a hostel, an apprenticeship), lines and forks about fields, harvests and rent stop. */
    @Test fun townLivesLeaveTheLandBehind() {
        val sexes = listOf(app.lot.domain.Sex.M, app.lot.domain.Sex.F)
        fun texts(lines: List<app.lot.domain.Line>) = lines.flatMap { l -> sexes.map { l.text.g(it).ru } }.toSet()
        val moves = texts(content.lines.filter { "town" in it.need.sets }) +
            setOf("Уехал в город, в ученики. Мастер бьёт, но кормит.", "Ушла в город, в ученики. Мастер бьёт, но кормит.",
                "Ушёл в город, в ученики. Мастер бьёт, но кормит.", "Уехал в город. Спишь у дальней родни на полу.", "Уехала в город. Спишь у дальней родни на полу.",
                "В порту ты остаёшься насовсем. Домой шлёшь поклоны с купцами.")
        val land = texts(content.lines.filter { it.need.holdsOnlyOutsideTown() })
        val rnd = Random(11)
        var moved = 0
        repeat(1500) { n ->
            val (p, _) = Life.live(content, n.toLong() * 31, null) { ch, _ -> ch.doors.indices.filter { ch.doors[it].open }.random(rnd) }
            val at = p.entries.indexOfFirst { it.text.ru in moves }
            if (at < 0) return@repeat
            moved++
            val late = p.entries.drop(at + 1).firstOrNull { it.text.ru in land }
            assertTrue("life $n moved to town at ${p.entries[at].age} and then: ${late?.text?.ru}", late == null)
        }
        assertTrue(moved > 20)
    }

    @Test fun genitive() {
        assertEquals("Виджая", Names.genitive("Виджай"))
        assertEquals("Никиты", Names.genitive("Никита"))
        assertEquals("Луки", Names.genitive("Лука"))
        assertEquals("Хасана", Names.genitive("Хасан"))
        assertEquals("Ур-Нанше", Names.genitive("Ур-Нанше"))
    }

    @Test fun everyRegionTagHasLinesOrFallsBack() {
        for (e in World.ERAS) for (r in e.regions) for (k in app.lot.domain.Kind.entries) {
            assertTrue("${e.name.en} ${r.key} $k", content.lines(e.group, k, r).count { it.need === app.lot.domain.Need.ANY } >= 2)
        }
        assertTrue(content.hist.size > 60)
    }

    /** Whoever lives through them meets a crisis, a legacy and a last wish, each told for its own time and place. */
    @Test fun everyLifeMeetsItsFixedForks() {
        val rnd = Random(13)
        val crises = mutableMapOf<app.lot.domain.Group, MutableSet<String>>()
        repeat(4000) { n ->
            val asked = mutableListOf<Pair<Int, String>>()
            val (p, _) = Life.live(content, n.toLong() * 37 + 3, null) { ch, _ ->
                asked += ch.age to ch.q.ru
                ch.doors.indices.filter { ch.doors[it].open }.random(rnd)
            }
            val b = p.birth
            val a = p.end!!.age
            val who = "life $n, ${b.era.name.en} ${b.region.key} ${b.estate.key} ${b.sex}: ${asked.map { it.first }}"
            val crisis = asked.firstOrNull { it.first in setOf(30, 33, 36, 42) }
            if (a > 42) assertTrue("no crisis in $who", crisis != null)
            if (a > 48) assertTrue("no legacy in $who", asked.any { it.first in setOf(45, 48) })
            if (a > 70) assertTrue("no last wish in $who", asked.any { it.first == 70 })
            crisis?.let { crises.getOrPut(b.era.group) { mutableSetOf() } += it.second }
        }
        for ((g, qs) in crises) assertTrue("$g: only ${qs.size} crises", qs.size >= 4)
    }
}
