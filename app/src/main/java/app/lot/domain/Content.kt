package app.lot.domain

import java.io.InputStream

enum class Kind(val c: Char) { CHILD('c'), JOY('j'), YOUTH('y'), ADULT('a'), OLD('o'), HARD('x') }

class Line(val group: Group, val kind: Kind, val tags: Set<String>, val text: Tx) {
    /** "sea" asks for a coast; other tags ask for a region that carries one of them. */
    fun fits(r: Region) = tags.isEmpty() || tags.any { if (it == "sea") r.sea else it in r.tags }
}

/** A real event. [meet] marks someone later written about, met in passing. */
class Hist(val id: Int, val tags: Set<String>, val from: Int, val to: Int, val meet: Boolean, val text: Tx) {
    fun fits(r: Region, eraI: Int, year: Int) =
        year in (from - 1)..(to + 1) && (if ("*" in tags) eraI >= 5 else tags.any { it in r.tags })
}

/** Everyday lines and real history, read from assets/lines.tsv and assets/history.tsv. */
class Content(val lines: List<Line>, val hist: List<Hist>) {
    private val byKey = lines.groupBy { it.group to it.kind }

    fun lines(g: Group, k: Kind, r: Region) = byKey[g to k].orEmpty().filter { it.fits(r) }

    companion object {
        fun read(lines: InputStream, history: InputStream): Content {
            val l = rows(lines).map { c ->
                Line(Group.valueOf(c[0].uppercase()), Kind.entries.first { it.c == c[1][0] }, tags(c[2]), Tx(c[3], c[4]))
            }
            val h = rows(history).mapIndexed { i, c -> Hist(i, tags(c[0]), c[1].toInt(), c[2].toInt(), c[3] == "1", Tx(c[4], c[5])) }
            return Content(l, h)
        }

        private fun rows(s: InputStream) = s.bufferedReader(Charsets.UTF_8).readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }.map { it.split('\t') }

        private fun tags(s: String) = if (s.isBlank()) emptySet() else s.trim().split(' ').toSet()
    }
}
