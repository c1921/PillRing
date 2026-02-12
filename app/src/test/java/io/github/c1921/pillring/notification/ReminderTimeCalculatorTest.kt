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

    @Test
    fun computeNextTriggerAtMs_intervalMode_usesFutureStartDateAnchor() {
        val nowMs = ZonedDateTime.of(2026, 2, 10, 8, 0, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()
        val plan = intervalPlan(
            intervalDays = 1,
            startDateEpochDay = ZonedDateTime.of(2026, 2, 12, 0, 0, 0, 0, zoneId)
                .toLocalDate()
                .toEpochDay()
        )

        val actual = ReminderTimeCalculator.computeNextTriggerAtMs(
            nowMs = nowMs,
            zoneId = zoneId,
            plan = plan
        )

        val expected = ZonedDateTime.of(2026, 2, 12, 9, 30, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, actual)
    }

    @Test
    fun computeNextTriggerAtMs_intervalMode_rollsToNextCycleWhenTodayTimePassed() {
        val startDateEpochDay = ZonedDateTime.of(2026, 2, 10, 0, 0, 0, 0, zoneId)
            .toLocalDate()
            .toEpochDay()
        val nowMs = ZonedDateTime.of(2026, 2, 10, 10, 0, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()

        val actual = ReminderTimeCalculator.computeNextTriggerAtMs(
            nowMs = nowMs,
            zoneId = zoneId,
            plan = intervalPlan(intervalDays = 1, startDateEpochDay = startDateEpochDay)
        )

        val expected = ZonedDateTime.of(2026, 2, 12, 9, 30, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, actual)
    }

    @Test
    fun computeNextTriggerAtMs_intervalMode_nonTriggerDay_jumpsToNearestTriggerDay() {
        val startDateEpochDay = ZonedDateTime.of(2026, 2, 10, 0, 0, 0, 0, zoneId)
            .toLocalDate()
            .toEpochDay()
        val nowMs = ZonedDateTime.of(2026, 2, 11, 8, 0, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()

        val actual = ReminderTimeCalculator.computeNextTriggerAtMs(
            nowMs = nowMs,
            zoneId = zoneId,
            plan = intervalPlan(intervalDays = 2, startDateEpochDay = startDateEpochDay)
        )

        val expected = ZonedDateTime.of(2026, 2, 13, 9, 30, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, actual)
    }

    @Test
    fun computeNextTriggerAtMs_intervalMode_acceptsBoundaryOne() {
        val startDateEpochDay = ZonedDateTime.of(2026, 2, 10, 0, 0, 0, 0, zoneId)
            .toLocalDate()
            .toEpochDay()
        val nowMs = ZonedDateTime.of(2026, 2, 10, 8, 0, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()

        val actual = ReminderTimeCalculator.computeNextTriggerAtMs(
            nowMs = nowMs,
            zoneId = zoneId,
            plan = intervalPlan(intervalDays = 1, startDateEpochDay = startDateEpochDay)
        )

        val expected = ZonedDateTime.of(2026, 2, 10, 9, 30, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, actual)
    }

    @Test
    fun computeNextTriggerAtMs_intervalMode_acceptsBoundaryThreeHundredSixtyFive() {
        val startDateEpochDay = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, zoneId)
            .toLocalDate()
            .toEpochDay()
        val nowMs = ZonedDateTime.of(2026, 6, 1, 8, 0, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()

        val actual = ReminderTimeCalculator.computeNextTriggerAtMs(
            nowMs = nowMs,
            zoneId = zoneId,
            plan = intervalPlan(intervalDays = 365, startDateEpochDay = startDateEpochDay)
        )

        val expected = ZonedDateTime.of(2027, 1, 2, 9, 30, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, actual)
    }

    @Test
    fun computeNextTriggerAtMs_intervalMode_throwsForInvalidIntervalZero() {
        val startDateEpochDay = ZonedDateTime.of(2026, 2, 10, 0, 0, 0, 0, zoneId)
            .toLocalDate()
            .toEpochDay()

        assertThrows(IllegalArgumentException::class.java) {
            ReminderTimeCalculator.computeNextTriggerAtMs(
                nowMs = 0L,
                zoneId = zoneId,
                plan = intervalPlan(intervalDays = 0, startDateEpochDay = startDateEpochDay)
            )
        }
    }

    @Test
    fun computeNextTriggerAtMs_intervalMode_throwsForInvalidIntervalThreeHundredSixtySix() {
        val startDateEpochDay = ZonedDateTime.of(2026, 2, 10, 0, 0, 0, 0, zoneId)
            .toLocalDate()
            .toEpochDay()

        assertThrows(IllegalArgumentException::class.java) {
            ReminderTimeCalculator.computeNextTriggerAtMs(
                nowMs = 0L,
                zoneId = zoneId,
                plan = intervalPlan(intervalDays = 366, startDateEpochDay = startDateEpochDay)
            )
        }
    }

    private fun intervalPlan(
        intervalDays: Int,
        startDateEpochDay: Long
    ): ReminderPlan {
        return ReminderPlan(
            id = "id",
            name = "Plan",
            hour = 9,
            minute = 30,
            repeatMode = ReminderRepeatMode.INTERVAL_DAYS,
            intervalDays = intervalDays,
            startDateEpochDay = startDateEpochDay,
            enabled = true,
            notificationId = 1001,
            isReminderActive = false,
            suppressNextDeleteFallback = false
        )
    }
}
