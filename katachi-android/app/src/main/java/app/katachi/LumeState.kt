package app.katachi

import android.content.Context
import app.katachi.core.DailyTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * "Floor mode today": Kasane's low-energy-day switch. It is remembered as the date it was turned
 * on for, so it lapses by itself at midnight.
 */
class LumeState(context: Context) {
    private val prefs = context.getSharedPreferences("lume", Context.MODE_PRIVATE)
    private val floorDate = MutableStateFlow(prefs.getString(KEY, null))
    val floorOn: StateFlow<String?> = floorDate.asStateFlow()

    fun isFloor(date: LocalDate = LocalDate.now()) = floorDate.value == date.toString()

    fun target(date: LocalDate = LocalDate.now()) = DailyTarget.of(isFloor(date))

    fun setFloor(on: Boolean, date: LocalDate = LocalDate.now()) {
        val value = if (on) date.toString() else null
        prefs.edit().apply { if (value == null) remove(KEY) else putString(KEY, value) }.apply()
        floorDate.value = value
    }

    private companion object {
        const val KEY = "floor_date"
    }
}
