package app.lot.data

import app.lot.AppContainer
import app.lot.domain.Heir
import app.lot.domain.Life
import kotlin.random.Random

/**
 * The demo build (`-Pdemo`) starts with about thirty lives already lived, two family lines, some answers in the notebook,
 * and a life open at a fork where a door is shut.
 */
object DemoSeed {
    private val ANSWERS = listOf(
        "Летом, в Батуми. Надо чаще." to "Last summer, in Batumi. Should be more often.",
        "Бабушке. Позвоню в воскресенье." to "Grandma. I'll call on Sunday.",
        "Горячий душ. И то, что дети здоровы." to "A hot shower. And that the children are well.",
        "Никто. Даже я не помню." to "Nobody. Not even me.",
        "Переезд. Сам решил." to "The move. My own decision.",
    )

    suspend fun seedIfEmpty(c: AppContainer, ru: Boolean = true) {
        val repo = c.repo
        if (repo.refresh().lives.isNotEmpty()) return
        c.settings.update { it.copy(onboarded = true) }
        val rnd = Random(1917)
        var day = System.currentTimeMillis() - 40L * DAY
        var answered = 0
        var line: Long? = null
        repeat(30) { i ->
            // Every few lives, carry on a family line instead of starting fresh.
            val s = repo.refresh()
            val prev = line?.let { s.byId[it] }
            val heirOf = prev?.takeIf { Heir.possible(it.year, it.kids) && i % 3 != 0 }?.id
            repo.draw(witness = false, heirOf = heirOf, seed = rnd.nextLong(), now = day)
            var guard = 0
            while (guard++ < 60) {
                val st = repo.refresh()
                val p = st.page ?: break
                if (p.end != null) break
                val ch = p.pending
                if (ch != null) repo.pick(ch.doors.indices.filter { ch.doors[it].open }.random(rnd), day) else repo.turn(day)
            }
            val done = repo.refresh()
            val id = done.cur?.lifeId
            if (id != null) {
                if (answered < ANSWERS.size && rnd.nextDouble() < .35) {
                    repo.answer(id, ANSWERS[answered].let { if (ru) it.first else it.second }); answered++
                }
                val row = done.byId[id]
                line = if (row != null && Heir.possible(row.year, row.kids)) id else null
            }
            day += DAY + rnd.nextLong(DAY)
        }
        // The open book: a life at a fork with at least one shut door.
        var seed = 40L
        while (true) {
            val (page, _) = Life.live(repo.content, seed, null) { _, _ -> 0 }
            if (page.end!!.age >= 40 && page.birth.era.i in 1..4) {
                val p = Life.read(repo.content, seed, null, emptyList(), 30, false)
                if (p.pending != null && p.pending.doors.any { !it.open } && p.pending.age in 12..17) break
            }
            seed++
        }
        repo.draw(witness = false, seed = seed, now = System.currentTimeMillis() - 60_000)
        while (repo.refresh().page?.canTurn == true) repo.turn()
    }

    private const val DAY = 86_400_000L
}
