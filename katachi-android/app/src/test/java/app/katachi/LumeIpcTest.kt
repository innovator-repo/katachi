package app.katachi

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.katachi.core.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = KatachiApp::class, sdk = [34])
class LumeIpcTest {
    private val app get() = ApplicationProvider.getApplicationContext<KatachiApp>()
    private val status = Uri.parse("content://app.katachi.provider/status")

    @Before fun reset() = app.lumeState.setFloor(false)

    private fun row(): Map<String, Int> {
        app.contentResolver.query(status, null, null, null, null)!!.use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            return c.columnNames.associateWith { c.getInt(c.getColumnIndexOrThrow(it)) }
        }
    }

    @Test fun floorModeBroadcastSetsOnePuzzleTarget() {
        assertEquals(5, app.lumeState.target())
        FloorModeReceiver().onReceive(app, Intent(FloorModeReceiver.ACTION).putExtra("on", true))
        assertEquals(1, app.lumeState.target())
        FloorModeReceiver().onReceive(app, Intent(FloorModeReceiver.ACTION).putExtra("on", false))
        assertEquals(5, app.lumeState.target())
    }

    @Test fun floorModeLapsesOnAnotherDay() {
        app.lumeState.setFloor(true, LocalDate.now().minusDays(1))
        assertEquals(5, app.lumeState.target())
    }

    @Test fun floorModeReceiverIsGuardedByTheSignaturePermission() {
        val receivers = app.packageManager.queryBroadcastReceivers(Intent(FloorModeReceiver.ACTION), 0)
        assertTrue(receivers.isNotEmpty())
        assertEquals(KasaneLink.PERMISSION, receivers.first().activityInfo.permission)
    }

    @Test fun providerReturnsOneRowWithTheContractColumns() {
        Robolectric.setupContentProvider(StatusProvider::class.java, "app.katachi.provider")
        val before = row()
        assertEquals(setOf("solved", "target", "doneToday"), before.keys)
        assertEquals(mapOf("solved" to 0, "target" to 5, "doneToday" to 0), before)

        app.lumeState.setFloor(true)
        app.store.record(LocalDate.now(), Result("t0", 30))
        assertEquals(mapOf("solved" to 1, "target" to 1, "doneToday" to 1), row())
    }

    @Test fun puzzleNextDeepLinkResolvesToTheMainActivity() {
        val view = Intent(Intent.ACTION_VIEW, Uri.parse("katachi://puzzle/next")).addCategory(Intent.CATEGORY_BROWSABLE)
        val match = app.packageManager.resolveActivity(view, 0)
        assertNotNull(match)
        assertEquals(MainActivity::class.java.name, match!!.activityInfo.name)
    }

    @Test fun providerAndReceiverNeedTheSignaturePermission() {
        val provider = app.packageManager.resolveContentProvider("app.katachi.provider", 0)!!
        assertEquals(KasaneLink.PERMISSION, provider.readPermission)
        assertTrue(provider.exported)
    }

    /** Records each broadcast with the permission it was sent under. */
    private class Recorder(base: Context) : ContextWrapper(base) {
        val sent = mutableListOf<Pair<Intent, String?>>()
        override fun sendBroadcast(intent: Intent, receiverPermission: String?) {
            sent += intent to receiverPermission
        }
    }

    @Test fun kasaneBroadcastsAreExplicitAndPermissionGuarded() {
        val ctx = Recorder(app)
        KasaneLink.report(ctx, LocalDate.of(2026, 1, 2), 3)
        KasaneLink.metrics(ctx, 3, 5, 41000L)
        assertEquals(2, ctx.sent.size)
        for ((intent, permission) in ctx.sent) {
            assertEquals("app.kasane", intent.`package`)
            assertEquals(KasaneLink.PERMISSION, permission)
        }
        val (count, metrics) = ctx.sent.map { it.first }
        assertEquals("app.kasane.action.LOG_COUNT", count.action)
        assertEquals("puzzles", count.getStringExtra("item"))
        assertEquals(3, count.getIntExtra("count", -1))
        assertEquals("app.kasane.action.ECO_METRICS", metrics.action)
        assertEquals("katachi", metrics.getStringExtra("source"))
        assertEquals("""{"solved":3,"target":5,"solveTimeMs":41000}""", metrics.getStringExtra("payload"))
    }

    @Test fun solvingSendsBothBroadcasts() {
        app.store.record(LocalDate.now(), Result("t1", 12))
        val actions = org.robolectric.Shadows.shadowOf(app).broadcastIntents.map { it.action }
        assertTrue("app.kasane.action.LOG_COUNT" in actions)
        assertTrue("app.kasane.action.ECO_METRICS" in actions)
    }
}
