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
    private val content = File("src/main/assets").let { Content.read(File(it, "lines.tsv").inputStream(), File(it, "history.tsv").inputStream()) }

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
            assertTrue(p.entries.last().text.ru.startsWith("Умер"))
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

    @Test fun genitive() {
        assertEquals("Виджая", Names.genitive("Виджай"))
        assertEquals("Никиты", Names.genitive("Никита"))
        assertEquals("Луки", Names.genitive("Лука"))
        assertEquals("Хасана", Names.genitive("Хасан"))
        assertEquals("Ур-Нанше", Names.genitive("Ур-Нанше"))
    }

    @Test fun everyRegionTagHasLinesOrFallsBack() {
        for (e in World.ERAS) for (r in e.regions) for (k in app.lot.domain.Kind.entries) {
            assertTrue("${e.name.en} ${r.key} $k", content.lines(e.group, k, r).isNotEmpty())
        }
        assertTrue(content.hist.size > 60)
    }
}
