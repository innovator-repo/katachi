package app.katachi

import android.content.Context
import android.content.Intent
import java.time.LocalDate

/**
 * Tells Kasane how many of today's puzzles are solved, so its "Pattern puzzles"
 * count fills itself in. Nothing happens if Kasane isn't installed or is too
 * old to listen.
 */
object KasaneLink {
    private const val PACKAGE = "app.kasane"
    private const val ACTION = "app.kasane.action.LOG_COUNT"

    fun report(context: Context, date: LocalDate, solved: Int) {
        val intent = Intent(ACTION)
            .setPackage(PACKAGE)
            .putExtra("item", "puzzles")
            .putExtra("date", date.toString())
            .putExtra("count", solved)
        runCatching { context.sendBroadcast(intent) }
    }
}
