package app.lot.data

import app.lot.domain.Content
import app.lot.domain.Entry
import app.lot.domain.Heir
import app.lot.domain.Life
import app.lot.domain.Mark
import app.lot.domain.Page
import app.lot.domain.Past
import app.lot.domain.Things
import app.lot.domain.Tx
import app.lot.domain.Wish
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/** The life being read now: everything else is replayed from these. [lifeId] is set once it has ended and gone to the archive. */
data class Cur(
    val seed: Long,
    val heirOf: Long?,
    val witness: Boolean,
    val picks: List<Int>,
    val shown: Int,
    val drawnAt: Long,
    val lifeId: Long? = null,
    /** The engine version the life was drawn with. */
    val ver: Int = Life.VERSION,
    /** The era, area and sex asked for before the draw. */
    val wish: Wish = Wish(),
) {
    fun encode() = listOf(seed, heirOf ?: "", witness, picks.joinToString("."), shown, drawnAt, lifeId ?: "", ver, wish.encode()).joinToString("|")

    companion object {
        fun decode(s: String): Cur? = runCatching {
            val p = s.split('|')
            Cur(p[0].toLong(), p[1].toLongOrNull(), p[2].toBoolean(), if (p[3].isEmpty()) emptyList() else p[3].split('.').map { it.toInt() },
                p[4].toInt(), p[5].toLong(), p[6].toLongOrNull(), p.getOrNull(7)?.toIntOrNull() ?: 1, Wish.decode(p.getOrNull(8)))
        }.getOrNull()
    }
}

class State(val lives: List<LifeRow>, val things: List<ThingRow>, val cur: Cur?, val page: Page?, private val seenOf: Map<Long, List<Int>> = emptyMap()) {
    val byId = lives.associateBy { it.id }

    /** Every real event met in any life, with the first life that met it: the chronicle and the encounters. */
    val seen: Map<Int, LifeRow> = buildMap { for (l in lives) for (h in seenOf[l.id].orEmpty()) if (h !in this) put(h, l) }
    fun holders(thingId: Long) = lives.filter { it.thingId == thingId }
    fun thing(id: Long) = things.firstOrNull { it.id == id }
}

class Repo(private val db: LotDb, val content: Content) {

    private val dao = db.dao()
    private val lock = Mutex()
    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state

    suspend fun refresh(): State {
        val lives = dao.lives()
        val things = dao.things()
        val cur = dao.meta().firstOrNull { it.k == CUR }?.v?.let(Cur::decode)
        val seen = lives.associate { it.id to it.seenIds }
        val page = cur?.let { read(it, lives, things) }
        return State(lives, things, cur, page, seen).also { _state.value = it }
    }

    fun heir(row: LifeRow?, things: List<ThingRow>): Heir? = row?.let { r ->
        val thing = things.firstOrNull { it.id == r.thingId }?.let { Things.of(it.key).name } ?: Tx("", "")
        Heir(r.id, r.name, r.sexOf, r.year, r.region, r.cult, r.rank, r.wealth, r.sur, r.thingId, thing, r.story, r.dynasty, r.gen)
    }

    fun read(c: Cur, lives: List<LifeRow>, things: List<ThingRow>): Page {
        val heir = heir(lives.firstOrNull { it.id == c.heirOf }, things)
        return Life.read(content, c.seed, heir, c.picks, c.shown, c.witness, past(c, lives), c.wish)
    }

    /** The reader's past at the moment [c] was drawn: books of the last lives before it, events of all of them. */
    private fun past(c: Cur, lives: List<LifeRow>): Past {
        val before = lives.filter { it.endedAt <= c.drawnAt && it.id != c.lifeId }
        val read = before.filter { it.book.isNotEmpty() }.takeLast(RECENT).flatMap { decode(it.book) }.map { it.text.ru }.toSet()
        val seen = before.flatMap { it.seenIds }.toSet()
        return Past(read, seen)
    }

    /** The whole book of an archived life, as it was written; a life from 1.0.0 kept no book and is read again. */
    fun book(row: LifeRow, s: State): List<Entry> = if (row.book.isNotEmpty()) decode(row.book)
    else Life.read(content, row.seed, heir(s.lives.firstOrNull { it.id == row.heirOf }, s.things), row.pickList, Int.MAX_VALUE, row.witness).entries

    /** Pulls a lot from the sack. [heirOf] makes it the great-grandchild of that life. */
    suspend fun draw(witness: Boolean, heirOf: Long? = null, seed: Long = Random.nextLong(), now: Long = System.currentTimeMillis(), wish: Wish = Wish()): State = lock.withLock {
        save(Cur(seed, heirOf, witness, emptyList(), 1, now, null, Life.VERSION, if (heirOf == null) wish else Wish()))
        refresh()
    }

    /** Turns to the next page of the book. */
    suspend fun turn(now: Long = System.currentTimeMillis()): State = lock.withLock {
        val s = refresh()
        val c = s.cur ?: return@withLock s
        if (s.page?.canTurn != true) return@withLock s
        settle(c.copy(shown = c.shown + 1), now)
    }

    suspend fun pick(door: Int, now: Long = System.currentTimeMillis()): State = lock.withLock {
        val s = refresh()
        val c = s.cur ?: return@withLock s
        val p = s.page?.pending ?: return@withLock s
        if (p.doors.getOrNull(door)?.open != true) return@withLock s
        settle(c.copy(picks = c.picks + door), now)
    }

    suspend fun answer(lifeId: Long, text: String) = lock.withLock {
        dao.answer(lifeId, text.trim())
        refresh()
    }

    /** Saves the new place in the book; a life that has just ended goes to the archive with its thing. */
    private suspend fun settle(c: Cur, now: Long): State {
        val lives = dao.lives()
        val things = dao.things()
        val page = read(c, lives, things)
        val end = page.end
        if (end == null || c.lifeId != null) { save(c); return refresh() }
        val b = page.birth
        val heir = b.heir
        val thingId = if (end.thing == null && heir != null) heir.thingId else dao.insert(ThingRow(key = (end.thing ?: Things.group(b.era.group).first()).key, createdAt = now))
        val id = dao.insert(LifeRow(
            seed = c.seed, heirOf = c.heirOf, witness = c.witness, picks = c.picks.joinToString(","),
            nameRu = b.name.ru, nameEn = b.name.en, sex = b.sex.name, year = b.year, death = end.age, eraI = b.era.i,
            region = b.region.key, cult = b.cult, estate = b.estate.key, rank = end.rank, wealth = end.wealth,
            surRu = b.sur?.ru, surEn = b.sur?.en, kids = end.kidsAlive, lit = end.lit, sawSea = end.sawSea,
            epitaphRu = end.epitaph.ru, epitaphEn = end.epitaph.en, bridgeRu = end.bridge.ru, bridgeEn = end.bridge.en,
            thingId = thingId, storyRu = end.story?.ru, storyEn = end.story?.en, meet = end.meet,
            dynasty = heir?.dynasty ?: c.seed, gen = (heir?.gen ?: 0) + 1, endedAt = now,
            ver = Life.VERSION, book = encode(page.entries), seen = end.seen.joinToString(","),
        ))
        save(c.copy(lifeId = id))
        return refresh()
    }

    private suspend fun save(c: Cur) = dao.meta(MetaRow(CUR, c.encode()))

    private companion object {
        const val CUR = "cur"
        /** How many recent books a new life tries not to repeat. */
        const val RECENT = 40
        private const val FIELD = '\u001F'
        private const val ROW = '\u001E'

        fun encode(entries: List<Entry>) = entries.joinToString(ROW.toString()) { e -> listOf(e.age, e.mark.ordinal, e.text.ru, e.text.en).joinToString(FIELD.toString()) }

        fun decode(s: String) = s.split(ROW).map { r -> r.split(FIELD).let { Entry(it[0].toInt(), Tx(it[2], it[3]), Mark.entries[it[1].toInt()]) } }
    }
}
