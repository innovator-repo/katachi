package app.katachi.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** One solved puzzle: how long it took, and how much help it needed. */
@Serializable
data class Result(val id: String, val seconds: Int, val hints: Int = 0, val tries: Int = 1)

@Serializable
data class Day(val date: String, val results: List<Result> = emptyList()) {
    fun solved(id: String) = results.any { it.id == id }
}

@Serializable
data class History(val days: List<Day> = emptyList()) {
    fun day(date: LocalDate): Day = days.firstOrNull { it.date == date.toString() } ?: Day(date.toString())

    fun withResult(date: LocalDate, result: Result): History {
        val day = day(date)
        if (day.solved(result.id)) return this
        val updated = day.copy(results = day.results + result)
        return copy(days = days.filterNot { it.date == day.date } + updated)
    }

    /** Days in a row, up to today, with the whole set solved. Today counts once it is done, and doesn't break the run before. */
    fun streak(today: LocalDate): Int {
        val complete = days.filter { it.results.size >= DailySet.TOTAL }.map { it.date }.toSet()
        var date = if (today.toString() in complete) today else today.minusDays(1)
        var run = 0
        while (date.toString() in complete) {
            run++
            date = date.minusDays(1)
        }
        return run
    }

    /** Mean seconds per tangram over the 7 days ending [end], or null with nothing solved. */
    fun tangramPace(end: LocalDate): Double? {
        val window = (0L until 7L).map { end.minusDays(it).toString() }.toSet()
        val tangrams = (0 until DailySet.TANGRAMS).map { DailySet.id(Kind.TANGRAM, it) }.toSet()
        val times = days.filter { it.date in window }
            .flatMap { d -> d.results.filter { it.id in tangrams }.map { it.seconds } }
        return if (times.isEmpty()) null else times.average()
    }
}

object Codec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(history: History): String = json.encodeToString(History.serializer(), history)

    fun decode(text: String): History = json.decodeFromString(History.serializer(), text)
}
