package io.github.c1921.pillring.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTimeCalculatorTest {
    private val zoneId: ZoneId = ZoneId.of("UTC")

    @Test
    fun computeNextDailyTriggerAtMs_usesTodayWhenTimeIsInFuture() {
        val nowMs = ZonedDateTime.of(2026, 2, 10, 8, 15, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()

        val actual = ReminderTimeCalculator.computeNextDailyTriggerAtMs(
            nowMs = nowMs,
            zoneId = zoneId,
            hour = 9,
            minute = 30
        )

        val expected = ZonedDateTime.of(2026, 2, 10, 9, 30, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, actual)
    }

    @Test
    fun computeNextDailyTriggerAtMs_rollsToNextDayWhenTimeEqualsNow() {
        val nowMs = ZonedDateTime.of(2026, 2, 10, 9, 30, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()

        val actual = ReminderTimeCalculator.computeNextDailyTriggerAtMs(
            nowMs = nowMs,
            zoneId = zoneId,
            hour = 9,
            minute = 30
        )

        val expected = ZonedDateTime.of(2026, 2, 11, 9, 30, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, actual)
    }

    @Test
    fun computeNextDailyTriggerAtMs_rollsToNextDayWhenTimeIsInPast() {
        val nowMs = ZonedDateTime.of(2026, 2, 10, 11, 0, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()

        val actual = ReminderTimeCalculator.computeNextDailyTriggerAtMs(
            nowMs = nowMs,
            zoneId = zoneId,
            hour = 9,
            minute = 30
        )

        val expected = ZonedDateTime.of(2026, 2, 11, 9, 30, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, actual)
    }

    @Test
    fun computeNextDailyTriggerAtMs_throwsForInvalidHour() {
        assertThrows(IllegalArgumentException::class.java) {
            ReminderTimeCalculator.computeNextDailyTriggerAtMs(
                nowMs = 0L,
                zoneId = zoneId,
                hour = 24,
                minute = 0
            )
        }
    }
}
