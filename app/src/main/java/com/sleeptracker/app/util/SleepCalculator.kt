package com.sleeptracker.app.util

import com.sleeptracker.app.data.model.SleepSession
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

data class SleepInsights(
    val averageDurationMillis: Long,
    val longestDurationMillis: Long,
    val shortestDurationMillis: Long,
    val totalDurationMillis: Long,
    val consistencyPercent: Int,
    val currentStreakDays: Int,
    val sleepDebtMinutes: Int,
    val averageBedtimeMinutesOfDay: Int?,
    val averageWakeMinutesOfDay: Int?,
    val sessionCount: Int
)

object SleepCalculator {

    private fun completed(sessions: List<SleepSession>) = sessions.filter { !it.isActive }

    fun computeInsights(sessions: List<SleepSession>, sleepGoalMinutes: Int): SleepInsights {
        val done = completed(sessions)
        if (done.isEmpty()) {
            return SleepInsights(0, 0, 0, 0, 0, 0, sleepGoalMinutes, null, null, 0)
        }

        val durations = done.map { it.durationMillis }
        val total = durations.sum()
        val average = total / done.size
        val longest = durations.max()
        val shortest = durations.min()

        // Consistency: how tightly bedtimes cluster, expressed as 0-100%.
        val bedtimeMinutes = done.map { minutesSinceMidnight(it.startInstant, it.zone) }
        val consistency = consistencyScore(bedtimeMinutes)

        val streak = currentStreakDays(done)

        val avgDurationMinutes = (average / 60000L).toInt()
        val debtMinutes = (sleepGoalMinutes - avgDurationMinutes).coerceAtLeast(0)

        val avgBedtime = circularAverageMinutes(bedtimeMinutes)
        val wakeMinutes = done.mapNotNull { s -> s.endInstant?.let { minutesSinceMidnight(it, s.zone) } }
        val avgWake = if (wakeMinutes.isNotEmpty()) circularAverageMinutes(wakeMinutes) else null

        return SleepInsights(
            averageDurationMillis = average,
            longestDurationMillis = longest,
            shortestDurationMillis = shortest,
            totalDurationMillis = total,
            consistencyPercent = consistency,
            currentStreakDays = streak,
            sleepDebtMinutes = debtMinutes,
            averageBedtimeMinutesOfDay = avgBedtime,
            averageWakeMinutesOfDay = avgWake,
            sessionCount = done.size
        )
    }

    private fun minutesSinceMidnight(instant: Instant, zone: ZoneId): Int {
        val time: LocalTime = instant.atZone(zone).toLocalTime()
        return time.hour * 60 + time.minute
    }

    /** Treats bed/wake times as points on a 24h circle to avoid midnight-wraparound skew. */
    private fun circularAverageMinutes(minutesList: List<Int>): Int {
        if (minutesList.isEmpty()) return 0
        var sumSin = 0.0
        var sumCos = 0.0
        minutesList.forEach { m ->
            val angle = 2.0 * Math.PI * (m / 1440.0)
            sumSin += kotlin.math.sin(angle)
            sumCos += kotlin.math.cos(angle)
        }
        var angle = kotlin.math.atan2(sumSin / minutesList.size, sumCos / minutesList.size)
        if (angle < 0) angle += 2.0 * Math.PI
        return ((angle / (2.0 * Math.PI)) * 1440.0).roundToLong().toInt().coerceIn(0, 1439)
    }

    private fun consistencyScore(minutesList: List<Int>): Int {
        if (minutesList.size < 2) return 100
        // Standard deviation on the circular distribution, converted to a 0-100 score.
        val avg = circularAverageMinutes(minutesList)
        val diffs = minutesList.map { m ->
            val raw = kotlin.math.abs(m - avg)
            kotlin.math.min(raw, 1440 - raw)
        }
        val variance = diffs.map { it.toDouble() * it }.average()
        val stdDevMinutes = kotlin.math.sqrt(variance)
        // 0 minutes stddev -> 100%, 180+ minutes (3h) stddev -> ~0%
        val score = (100.0 - (stdDevMinutes / 180.0) * 100.0).coerceIn(0.0, 100.0)
        return score.roundToLong().toInt()
    }

    private fun currentStreakDays(done: List<SleepSession>): Int {
        if (done.isEmpty()) return 0
        val sortedDesc = done.sortedByDescending { it.startEpochMillis }
        var streak = 0
        var expectedDay = ZonedDateTime.now(sortedDesc.first().zone).toLocalDate()
        for (session in sortedDesc) {
            val day = session.startInstant.atZone(session.zone).toLocalDate()
            val diff = ChronoUnit.DAYS.between(day, expectedDay)
            if (diff == 0L) {
                streak++
                expectedDay = expectedDay.minusDays(1)
            } else if (diff == 1L) {
                // allows the "today not yet slept" case to still count yesterday's streak
                streak++
                expectedDay = day.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    /** Groups completed sessions into day buckets (yyyy-MM-dd) summing duration, for bar charts. */
    fun dailyTotals(sessions: List<SleepSession>): Map<String, Long> {
        val done = completed(sessions)
        return done.groupBy { s ->
            val d = s.startInstant.atZone(s.zone).toLocalDate()
            d.toString()
        }.mapValues { (_, list) -> list.sumOf { it.durationMillis } }
    }
}
