package app.katachi

import android.content.Context
import app.katachi.core.Codec
import app.katachi.core.History
import app.katachi.core.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.LocalDate

/** Every solved puzzle, kept in one JSON file and written atomically on every change. */
class Store(private val context: Context) {
    private val file = File(context.filesDir, "history.json")
    private val state = MutableStateFlow(load())
    val history: StateFlow<History> = state.asStateFlow()

    private fun load(): History = runCatching { Codec.decode(file.readText()) }.getOrElse { History() }

    @Synchronized
    fun record(date: LocalDate, result: Result) {
        val next = state.value.withResult(date, result)
        if (next == state.value) return
        val temp = File(file.parentFile, "history.json.tmp")
        temp.writeText(Codec.encode(next))
        temp.renameTo(file)
        state.value = next
        val solved = next.day(date).results.size
        KasaneLink.report(context, date, solved)
        KasaneLink.metrics(context, solved, context.lumeState.target(date), result.seconds * 1000L)
    }
}

val Context.store: Store get() = (applicationContext as KatachiApp).store
val Context.lumeState: LumeState get() = (applicationContext as KatachiApp).lumeState
