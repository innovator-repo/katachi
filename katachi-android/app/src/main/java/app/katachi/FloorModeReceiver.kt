package app.katachi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Kasane's "low-energy day" switch: while on, one puzzle is today's target. */
class FloorModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION || !intent.hasExtra("on")) return
        context.lumeState.setFloor(intent.getBooleanExtra("on", false))
    }

    companion object {
        const val ACTION = "app.lume.action.FLOOR_MODE"
    }
}
