package io.github.c1921.pillring.notification

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ReminderTimeCalculator {
    fun computeNextTriggerAtMs(
        nowMs: Long,
        zoneId: ZoneId,
        plan: ReminderPlan
    ): Long {
        return when (plan.repeatMode) {
            ReminderRepeatMode.DAILY -> computeNextDailyTriggerAtMs(
                nowMs = nowMs,
                zoneId = zoneId,
                hour = plan.hour,
                minute = plan.minute
            )

            ReminderRepeatMode.INTERVAL_DAYS -> computeNextIntervalTriggerAtMs(
                nowMs = nowMs,
                zoneId = zoneId,
                hour = plan.hour,
                minute = plan.minute,
                intervalDays = plan.intervalDays,
                startDateEpochDay = requireNotNull(plan.startDateEpochDay) {
                    "startDateEpochDay must not be null for interval mode"
                }
            )
        }
    }

    fun computeNextDailyTriggerAtMs(
        nowMs: Long,
        zoneId: ZoneId,
        hour: Int,
        minute: Int
    ): Long {
        require(hour in 0..23) { "hour must be in 0..23" }
        require(minute in 0..59) { "minute must be in 0..59" }

        val now = Instant.ofEpochMilli(nowMs).atZone(zoneId)
        var candidate = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }

        return candidate.toInstant().toEpochMilli()
    }

    private fun computeNextIntervalTriggerAtMs(
        nowMs: Long,
        zoneId: ZoneId,
        hour: Int,
        minute: Int,
        intervalDays: Int,
        startDateEpochDay: Long
    ): Long {
        require(hour in 0..23) { "hour must be in 0..23" }
        require(minute in 0..59) { "minute must be in 0..59" }
        require(intervalDays in 1..365) { "intervalDays must be in 1..365" }

        val now = Instant.ofEpochMilli(nowMs).atZone(zoneId)
        val todayEpochDay = now.toLocalDate().toEpochDay()
        val stepDays = intervalDays + 1L

        val firstCandidateEpochDay = when {
            todayEpochDay <= startDateEpochDay -> startDateEpochDay
            else -> {
                val elapsedDays = todayEpochDay - startDateEpochDay
                val remainder = elapsedDays % stepDays
                if (remainder == 0L) {
                    todayEpochDay
                } else {
                    todayEpochDay + (stepDays - remainder)
                }
            }
        }

        var candidate = LocalDate.ofEpochDay(firstCandidateEpochDay)
            .atTime(hour, minute)
            .atZone(zoneId)

        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(stepDays)
        }

        return candidate.toInstant().toEpochMilli()
    }
}
