package io.github.c1921.pillring.notification

import org.json.JSONArray
import org.json.JSONObject

internal object ReminderLogJsonCodec {
    private const val ENTRY_ID = "id"
    private const val ENTRY_TYPE = "type"
    private const val ENTRY_PLAN_ID = "plan_id"
    private const val ENTRY_PLAN_NAME = "plan_name"
    private const val ENTRY_REMINDER_HOUR = "reminder_hour"
    private const val ENTRY_REMINDER_MINUTE = "reminder_minute"
    private const val ENTRY_OCCURRED_AT_EPOCH_MS = "occurred_at_epoch_ms"

    fun parseEntries(entriesJson: String?): List<ReminderLogEntry> {
        if (entriesJson.isNullOrBlank()) {
            return emptyList()
        }

        return try {
            val array = JSONArray(entriesJson)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString(ENTRY_ID).trim()
                    if (id.isEmpty()) {
                        continue
                    }

                    val type = item.optString(ENTRY_TYPE).toLogEventTypeOrNull()
                        ?: continue
                    val planId = item.optString(ENTRY_PLAN_ID).trim()
                    if (planId.isEmpty()) {
                        continue
                    }

                    val hour = item.optInt(ENTRY_REMINDER_HOUR, -1)
                    val minute = item.optInt(ENTRY_REMINDER_MINUTE, -1)
                    if (hour !in 0..23 || minute !in 0..59) {
                        continue
                    }

                    val occurredAtEpochMs = if (
                        item.has(ENTRY_OCCURRED_AT_EPOCH_MS) &&
                        !item.isNull(ENTRY_OCCURRED_AT_EPOCH_MS)
                    ) {
                        item.optLong(ENTRY_OCCURRED_AT_EPOCH_MS, -1L)
                    } else {
                        -1L
                    }
                    if (occurredAtEpochMs < 0L) {
                        continue
                    }

                    val planName = item.optString(ENTRY_PLAN_NAME).trim().ifEmpty { "Plan" }
                    add(
                        ReminderLogEntry(
                            id = id,
                            type = type,
                            planId = planId,
                            planName = planName,
                            reminderHour = hour,
                            reminderMinute = minute,
                            occurredAtEpochMs = occurredAtEpochMs
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeEntries(entries: List<ReminderLogEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put(ENTRY_ID, entry.id)
                    .put(ENTRY_TYPE, entry.type.name)
                    .put(ENTRY_PLAN_ID, entry.planId)
                    .put(ENTRY_PLAN_NAME, entry.planName)
                    .put(ENTRY_REMINDER_HOUR, entry.reminderHour)
                    .put(ENTRY_REMINDER_MINUTE, entry.reminderMinute)
                    .put(ENTRY_OCCURRED_AT_EPOCH_MS, entry.occurredAtEpochMs)
            )
        }
        return array.toString()
    }

    fun trimToMaxEntries(
        entries: List<ReminderLogEntry>,
        maxEntries: Int
    ): List<ReminderLogEntry> {
        require(maxEntries > 0) { "maxEntries must be positive" }
        return if (entries.size <= maxEntries) {
            entries
        } else {
            entries.takeLast(maxEntries)
        }
    }
}

private fun String.toLogEventTypeOrNull(): ReminderLogEventType? {
    return ReminderLogEventType.entries.firstOrNull { it.name == this }
}

