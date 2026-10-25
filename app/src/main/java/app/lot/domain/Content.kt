package app.lot.domain

import java.io.InputStream

enum class Kind(val c: Char) { CHILD('c'), JOY('j'), YOUTH('y'), ADULT('a'), OLD('o'), HARD('x') }

/** What a line or a fork asks of the person: sex, station, letters, family. Read from `need` columns (see tools/CONTENT.md). */
class Need(tokens: Collection<String>) {
    private val ranks = tokens.filter { it.matches(Regex("r[0-3]")) }.map { it[1] - '0' }.toSet()
    /** `after:1930` and `before:-500` keep a line to the years it belongs to. */
    private val after = tokens.firstOrNull { it.startsWith("after:") }?.substringAfter(':')?.toInt()
    private val before = tokens.firstOrNull { it.startsWith("before:") }?.substringAfter(':')?.toInt()
    private val rest = tokens.filterNot { it.matches(Regex("r[0-3]")) || ':' in it || it.startsWith("+") }.toSet()
    /** `+town` and the like: what a line does to the life once it is written (a line about the new flat makes the life a town life). */
    val sets = tokens.filter { it.startsWith("+") }.map { it.drop(1) }

    /** True for a need that a town life can never meet (it asks for `!town`). */
    fun holdsOnlyOutsideTown() = "!town" in rest

    private fun Person.check(t: String) = if (t.startsWith("!") && t.drop(1) !in DOOR_NEGATIONS) !has(t.drop(1)) else has(t)

    fun holds(p: Person) = (ranks.isEmpty() || p.rank in ranks) && rest.all { p.check(it) } &&
        (after == null || p.now >= after) && (before == null || p.now < before)

    companion object {
        val ANY = Need(emptyList())
        /** Negations the person answers itself, as door conditions do. */
        private val DOOR_NEGATIONS = setOf("lit", "married", "kids")
        fun of(s: String) = if (s.isBlank()) ANY else Need(s.trim().split(' '))
    }
}

/** The facts about a life that lines, forks and doors look at. */
interface Person {
    val rank: Int
    /** The year the life has reached. */
    val now: Int
    /** m f lit illit married single kids monk town, plus the door words !lit poor rich !married !kids. */
    fun has(cond: String): Boolean
}

class Line(val group: Group, val kind: Kind, val tags: Set<String>, val text: Tx, val need: Need) {
    /** "sea" asks for a coast; other tags ask for a region that carries one of them. */
    fun fits(r: Region) = tags.isEmpty() || tags.any { if (it == "sea") r.sea else it in r.tags }
}

/** A real event. [meet] marks someone later written about, met in passing. Its id is its row, so rows are only ever appended. */
class Hist(val id: Int, val tags: Set<String>, val from: Int, val to: Int, val meet: Boolean, val text: Tx) {
    fun fits(r: Region, eraI: Int, year: Int) =
        year in (from - 1)..(to + 1) && (if ("*" in tags) eraI >= 5 else tags.any { it in r.tags })

    /** The middle of its years, for sorting the chronicle. */
    val mid get() = (from + to) / 2
}

class Outcome(val weight: Double, val text: Tx, val fx: List<String>)

class Shut(val conds: List<String>, val why: Tx)

class Gate(val text: Tx, val shut: List<Shut>, val outs: List<Outcome>, val ep: Tx?, val ask: Tx?)

/** An everyday fork from dilemmas*.txt. */
class Dilemma(
    val id: String,
    val groups: Set<Group>,
    val tags: Set<String>,
    val need: Need,
    val ages: IntRange,
    val weight: Double,
    val q: Tx,
    val doors: List<Gate>,
) {
    fun fits(r: Region) = "*" in tags || tags.isEmpty() || tags.any { it in r.tags }
}

/** Everyday lines, real history and forks, read from assets: the lines folder, history.tsv and the dilemmas files. */
class Content(val lines: List<Line>, val hist: List<Hist>, val dilemmas: List<Dilemma> = emptyList()) {
    private val byKey = lines.groupBy { it.group to it.kind }

    fun lines(g: Group, k: Kind, r: Region) = byKey[g to k].orEmpty().filter { it.fits(r) }

    companion object {
        fun read(lines: List<InputStream>, history: InputStream, dilemmas: List<InputStream>): Content {
            val l = lines.flatMap { rows(it) }.map { c ->
                Line(Group.valueOf(c[0].uppercase()), Kind.entries.first { it.c == c[1][0] }, tags(c[2]), Tx(c[3], c[4]), Need.of(c.getOrElse(5) { "" }))
            }
            val h = rows(history).mapIndexed { i, c -> Hist(i, tags(c[0]), c[1].toInt(), c[2].toInt(), c[3] == "1", Tx(c[4], c[5])) }
            val d = dilemmas.flatMap { parseDilemmas(it.bufferedReader(Charsets.UTF_8).readText()) }
            return Content(l, h, d)
        }

        private fun rows(s: InputStream) = s.bufferedReader(Charsets.UTF_8).readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }.map { it.split('\t') }

        private fun tags(s: String) = if (s.isBlank()) emptySet() else s.trim().split(' ').toSet()

        /** A `|` inside a {he|she} mark is part of the text, not a separator. */
        private val BAR = Regex("""\|(?![^{]*\})""")

        private fun bars(s: String) = s.split(BAR).map { it.trim() }

        private fun pair(s: String) = bars(s).let { Tx(it[0], it[1]) }

        fun parseDilemmas(src: String): List<Dilemma> = src.split(Regex("\\n\\s*\\n")).mapNotNull { block ->
            val rows = block.trim().lines().filter { it.isNotBlank() && !it.startsWith("#") }
            if (rows.isEmpty()) return@mapNotNull null
            val head = mutableMapOf<String, String>()
            val doors = mutableListOf<MutableMap<String, Any?>>()
            for (r in rows) {
                val key = r.substringBefore(':').trim()
                val v = r.substringAfter(':').trim()
                when {
                    key == "door" -> doors += mutableMapOf("text" to v, "shut" to mutableListOf<String>(), "out" to mutableListOf<String>())
                    key in setOf("shut", "out") && doors.isNotEmpty() -> @Suppress("UNCHECKED_CAST") (doors.last()[key] as MutableList<String>).add(v)
                    key in setOf("ep", "ask") && doors.isNotEmpty() -> doors.last()[key] = v
                    else -> head[key] = v
                }
            }
            val (a, b) = head.getValue("ages").split('-').map { it.trim().toInt() }
            Dilemma(
                id = head.getValue("id"),
                groups = head.getValue("groups").split(' ').filter { it.isNotBlank() }.map { Group.valueOf(it.uppercase()) }.toSet(),
                tags = tags(head["tags"] ?: ""),
                need = Need.of(head["need"] ?: ""),
                ages = a..b,
                weight = head["weight"]?.toDoubleOrNull() ?: 1.0,
                q = pair(head.getValue("q")),
                doors = doors.map { d ->
                    @Suppress("UNCHECKED_CAST")
                    Gate(
                        text = pair(d["text"] as String),
                        shut = (d["shut"] as List<String>).map { s -> bars(s).let { Shut(it[0].split(' ').filter { c -> c.isNotBlank() }, Tx(it[1], it[2])) } },
                        outs = (d["out"] as List<String>).map { o ->
                            val c = bars(o)
                            Outcome(c[0].toDouble(), Tx(c[1], c[2]), c.getOrElse(3) { "" }.split(' ').filter { it.isNotBlank() })
                        },
                        ep = (d["ep"] as String?)?.let(::pair),
                        ask = (d["ask"] as String?)?.let(::pair),
                    )
                },
            )
        }
    }
}
