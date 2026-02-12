package io.github.c1921.pillring.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlanJsonCodecTest {
    @Test
    fun parsePlans_invalidJson_returnsEmptyList() {
        val result = ReminderPlanJsonCodec.parsePlans("{not-json")

        assertTrue(result.isEmpty())
    }

    @Test
    fun parsePlans_invalidIntervalConfig_fallsBackToDaily() {
        val payload = """
            [
              {
                "id": "p1",
                "name": "Plan",
                "hour": 8,
                "minute": 0,
                "repeat_mode": "INTERVAL_DAYS",
                "interval_days": 0,
                "start_date_epoch_day": null,
                "enabled": true,
                "notification_id": 1001,
                "is_reminder_active": false,
                "suppress_next_delete_fallback": false
              }
            ]
        """.trimIndent()

        val result = ReminderPlanJsonCodec.parsePlans(payload)

        assertEquals(1, result.size)
        assertEquals(ReminderRepeatMode.DAILY, result.first().repeatMode)
        assertEquals(1, result.first().intervalDays)
        assertNull(result.first().startDateEpochDay)
    }

    @Test
    fun normalizeRepeatConfig_acceptsIntervalBoundaries() {
        val normalizedOne = ReminderPlanJsonCodec.normalizeRepeatConfig(
            repeatMode = ReminderRepeatMode.INTERVAL_DAYS,
            intervalDays = 1,
            startDateEpochDay = 10L,
            strict = true
        )
        val normalizedThreeSixtyFive = ReminderPlanJsonCodec.normalizeRepeatConfig(
            repeatMode = ReminderRepeatMode.INTERVAL_DAYS,
            intervalDays = 365,
            startDateEpochDay = 10L,
            strict = true
        )

        assertEquals(ReminderRepeatMode.INTERVAL_DAYS, normalizedOne.repeatMode)
        assertEquals(1, normalizedOne.intervalDays)
        assertEquals(ReminderRepeatMode.INTERVAL_DAYS, normalizedThreeSixtyFive.repeatMode)
        assertEquals(365, normalizedThreeSixtyFive.intervalDays)
    }
}
