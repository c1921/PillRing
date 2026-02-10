package io.github.c1921.pillring.notification

import java.time.Instant
import java.time.ZoneId

object ReminderTimeCalculator {
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
}
