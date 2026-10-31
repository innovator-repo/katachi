package app.katachi

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import app.katachi.core.DailySet
import app.katachi.core.segments
import java.time.LocalDate

/** The Today widget: ten segments (solved / left) and a Solve button that opens katachi://puzzle/next. */
class TodayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) runCatching { manager.updateAppWidget(id, views(context)) }
    }

    companion object {
        private val SEGMENTS = intArrayOf(
            R.id.seg0, R.id.seg1, R.id.seg2, R.id.seg3, R.id.seg4,
            R.id.seg5, R.id.seg6, R.id.seg7, R.id.seg8, R.id.seg9,
        )

        /** Redraws every Today widget on the home screen; does nothing if there are none. */
        fun refresh(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidget::class.java))
                for (id in ids) manager.updateAppWidget(id, views(context))
            }
        }

        private fun views(context: Context): RemoteViews {
            // No data (fresh install, unreadable file) just means zero solved.
            val solved = runCatching { context.store.history.value.day(LocalDate.now()).results.size }.getOrDefault(0)
            val marks = segments(solved)
            val views = RemoteViews(context.packageName, R.layout.widget_today)
            for ((i, on) in marks.withIndex()) {
                views.setInt(SEGMENTS[i], "setBackgroundResource", if (on) R.drawable.widget_seg_on else R.drawable.widget_seg_off)
            }
            views.setTextViewText(R.id.widget_summary, context.getString(R.string.widget_summary, marks.count { it }, DailySet.TOTAL))
            val open = Intent(Intent.ACTION_VIEW, Uri.parse("katachi://puzzle/next"))
                .setClass(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val tap = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_solve, tap)
            views.setOnClickPendingIntent(R.id.widget_root, tap)
            return views
        }
    }
}
