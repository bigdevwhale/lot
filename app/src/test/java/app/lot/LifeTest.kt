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
}
