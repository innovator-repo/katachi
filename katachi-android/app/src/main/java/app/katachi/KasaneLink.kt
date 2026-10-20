package app.katachi

import android.content.Context
import android.content.Intent
import app.katachi.core.EcoMetrics
import java.time.LocalDate

/**
 * Tells Kasane how many of today's puzzles are solved, so its "Pattern puzzles"
 * count fills itself in, and how fast the last one went. Nothing happens if Kasane
 * isn't installed or doesn't hold the ecosystem permission.
 */
object KasaneLink {
    private const val PACKAGE = "app.kasane"
    const val PERMISSION = "app.lume.permission.ECOSYSTEM_SYNC"

    fun report(context: Context, date: LocalDate, solved: Int) {
        val intent = Intent("app.kasane.action.LOG_COUNT")
            .setPackage(PACKAGE)
            .putExtra("item", "puzzles")
            .putExtra("date", date.toString())
            .putExtra("count", solved)
        send(context, intent)
    }

    fun metrics(context: Context, solved: Int, target: Int, solveTimeMs: Long) {
        val intent = Intent("app.kasane.action.ECO_METRICS")
            .setPackage(PACKAGE)
            .putExtra("source", "katachi")
            .putExtra("payload", EcoMetrics.payload(solved, target, solveTimeMs))
        send(context, intent)
    }

    private fun send(context: Context, intent: Intent) {
        runCatching { context.sendBroadcast(intent, PERMISSION) }
    }
}
