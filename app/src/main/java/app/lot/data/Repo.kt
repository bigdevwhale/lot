package app.lot.data

import app.lot.domain.Content
import app.lot.domain.Heir
import app.lot.domain.Life
import app.lot.domain.Page
import app.lot.domain.Things
import app.lot.domain.Tx
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
) {
    fun encode() = listOf(seed, heirOf ?: "", witness, picks.joinToString("."), shown, drawnAt, lifeId ?: "").joinToString("|")

    companion object {
        fun decode(s: String): Cur? = runCatching {
            val p = s.split('|')
            Cur(p[0].toLong(), p[1].toLongOrNull(), p[2].toBoolean(), if (p[3].isEmpty()) emptyList() else p[3].split('.').map { it.toInt() },
                p[4].toInt(), p[5].toLong(), p[6].toLongOrNull())
        }.getOrNull()
    }
}

class State(val lives: List<LifeRow>, val things: List<ThingRow>, val cur: Cur?, val page: Page?) {
    val byId = lives.associateBy { it.id }
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
        val page = cur?.let { read(it, lives, things) }
        return State(lives, things, cur, page).also { _state.value = it }
    }

    fun heir(row: LifeRow?, things: List<ThingRow>): Heir? = row?.let { r ->
        val thing = things.firstOrNull { it.id == r.thingId }?.let { Things.of(it.key).name } ?: Tx("", "")
        Heir(r.id, r.name, r.sexOf, r.year, r.region, r.cult, r.rank, r.wealth, r.sur, r.thingId, thing, r.story, r.dynasty, r.gen)
    }

    fun read(c: Cur, lives: List<LifeRow>, things: List<ThingRow>): Page =
        Life.read(content, c.seed, heir(lives.firstOrNull { it.id == c.heirOf }, things), c.picks, c.shown, c.witness)

    /** The whole book of an archived life. */
    fun book(row: LifeRow, s: State): Page =
        Life.read(content, row.seed, heir(row.heirOf?.let { s.byId[it] }, s.things), row.pickList, Int.MAX_VALUE, row.witness)

    /** Pulls a lot from the sack. [heirOf] makes it the great-grandchild of that life. */
    suspend fun draw(witness: Boolean, heirOf: Long? = null, seed: Long = Random.nextLong(), now: Long = System.currentTimeMillis()): State = lock.withLock {
        save(Cur(seed, heirOf, witness, emptyList(), 1, now))
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
        ))
        save(c.copy(lifeId = id))
        return refresh()
    }

    private suspend fun save(c: Cur) = dao.meta(MetaRow(CUR, c.encode()))

    private companion object {
        const val CUR = "cur"
    }
}
