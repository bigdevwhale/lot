package app.lot

import androidx.test.core.app.ApplicationProvider
import app.lot.data.DemoSeed
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepoTest {
    @Test fun pickingADoorMovesTheBookOn() = runBlocking {
        val c = ApplicationProvider.getApplicationContext<LotApp>().container
        DemoSeed.seedIfEmpty(c)
        val before = c.repo.refresh()
        val ch = before.page!!.pending
        assertNotNull(ch)
        val door = ch!!.doors.indexOfFirst { it.open }
        val after = c.repo.pick(door)
        assertEquals(listOf(door), after.cur!!.picks)
        assertEquals(null, after.page!!.pending)
    }
}
