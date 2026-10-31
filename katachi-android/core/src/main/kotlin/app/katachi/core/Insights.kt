package app.katachi.core

import java.time.LocalDate

/** The puzzle kind an id such as "t0", "m2" or "b1" belongs to, or null for anything else. */
fun kindOfId(id: String): Kind? = Kind.entries.firstOrNull { id.isNotEmpty() && id[0] == it.name.first().lowercaseChar() }

/** How many puzzles of [kind] this day has solved. */
fun Day.solvedOf(kind: Kind): Int = results.count { kindOfId(it.id) == kind }

/** The fastest solve of [kind] on this day, in seconds, or null if none. */
fun Day.bestSeconds(kind: Kind? = null): Int? =
    results.filter { kind == null || kindOfId(it.id) == kind }.minOfOrNull { it.seconds }

/** Everything ever solved, counting each puzzle once. */
fun History.totalSolved(): Int = days.sumOf { it.results.size }

/** The fastest solve ever of [kind], in seconds, or null if there is none yet. */
fun History.bestSeconds(kind: Kind): Int? = days.mapNotNull { it.bestSeconds(kind) }.minOrNull()

/** Puzzles solved on each of the [count] days ending at [end], oldest first. Missing days are 0. */
fun History.dailyCounts(end: LocalDate, count: Int = 14): List<Int> =
    (count - 1 downTo 0).map { day(end.minusDays(it.toLong())).results.size }

/** The longest run of whole-set days ever recorded (today does not need to be done). */
fun History.bestStreak(): Int {
    val complete = days.filter { it.results.size >= DailySet.TOTAL }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.sorted()
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    for (date in complete) {
        run = if (previous != null && previous.plusDays(1) == date) run + 1 else 1
        if (run > best) best = run
        previous = date
    }
    return best
}

/** The first puzzle of the day, in play order, that is not solved yet; null when the set is done. */
fun Day.nextUnsolved(): Pair<Kind, Int>? = DailySet.order.firstOrNull { (kind, i) -> !solved(DailySet.id(kind, i)) }

/** "Puzzle N of 10" number for the hero: the next unsolved puzzle's place in the set, or null when done. */
fun Day.nextNumber(): Int? = nextUnsolved()?.let { DailySet.order.indexOf(it) + 1 }

/** The row the status provider answers with. Column names are a contract with the other apps. */
object StatusRow {
    val columns = listOf("solved", "target", "doneToday", "streak")

    fun of(solved: Int, target: Int, streak: Int): Map<String, Int> = linkedMapOf(
        "solved" to solved,
        "target" to target,
        "doneToday" to if (solved >= target) 1 else 0,
        "streak" to streak,
    )
}

/** The Today widget's ten segments: true for solved, false for left. Always [DailySet.TOTAL] long. */
fun segments(solved: Int, total: Int = DailySet.TOTAL): List<Boolean> {
    val done = solved.coerceIn(0, total)
    return List(total) { it < done }
}
