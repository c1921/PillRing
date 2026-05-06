package io.github.c1921.pillring.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderLogJsonCodecTest {
    @Test
    fun parseEntries_invalidJson_returnsEmptyList() {
        val result = ReminderLogJsonCodec.parseEntries("{not-json")

        assertTrue(result.isEmpty())
    }

    @Test
    fun encodeEntries_thenParseEntries_roundTripsEntries() {
        val entries = listOf(
            ReminderLogEntry(
                id = "log-1",
                type = ReminderLogEventType.REMINDER_TRIGGERED,
                planId = "p1",
                planName = "Morning",
                reminderHour = 8,
                reminderMinute = 30,
                occurredAtEpochMs = 1_000L
            ),
            ReminderLogEntry(
                id = "log-2",
                type = ReminderLogEventType.MANUAL_CONFIRMED,
                planId = "p1",
                planName = "Morning",
                reminderHour = 8,
                reminderMinute = 30,
                occurredAtEpochMs = 2_000L
            )
        )

        val parsed = ReminderLogJsonCodec.parseEntries(
            ReminderLogJsonCodec.encodeEntries(entries)
        )

        assertEquals(entries, parsed)
    }

    @Test
    fun parseEntries_unknownType_skipsEntry() {
        val payload = """
            [
              {
                "id": "log-1",
                "type": "UNKNOWN",
                "plan_id": "p1",
                "plan_name": "Morning",
                "reminder_hour": 8,
                "reminder_minute": 30,
                "occurred_at_epoch_ms": 1000
              }
            ]
        """.trimIndent()

        val result = ReminderLogJsonCodec.parseEntries(payload)

        assertTrue(result.isEmpty())
    }

    @Test
    fun trimToMaxEntries_keepsNewestEntriesByInsertionOrder() {
        val entries = (1..5).map { index ->
            ReminderLogEntry(
                id = "log-$index",
                type = ReminderLogEventType.REMINDER_TRIGGERED,
                planId = "p1",
                planName = "Morning",
                reminderHour = 8,
                reminderMinute = 30,
                occurredAtEpochMs = index.toLong()
            )
        }

        val trimmed = ReminderLogJsonCodec.trimToMaxEntries(
            entries = entries,
            maxEntries = 3
        )

        assertEquals(listOf("log-3", "log-4", "log-5"), trimmed.map { it.id })
    }
}

