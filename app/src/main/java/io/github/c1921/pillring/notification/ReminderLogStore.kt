package io.github.c1921.pillring.notification

import android.content.Context
import java.util.UUID

object ReminderLogStore {
    const val MAX_LOG_ENTRIES = 200

    private const val PREFS_NAME = "reminder_log_store"
    private const val KEY_ENTRIES_JSON = "entries_json"

    fun getEntries(context: Context): List<ReminderLogEntry> {
        return readEntries(dataSource(context)).asReversed()
    }

    fun recordReminderTriggered(
        context: Context,
        plan: ReminderPlan,
        occurredAtEpochMs: Long = System.currentTimeMillis()
    ) {
        appendEntry(
            context = context,
            entry = buildEntry(
                type = ReminderLogEventType.REMINDER_TRIGGERED,
                plan = plan,
                occurredAtEpochMs = occurredAtEpochMs
            )
        )
    }

    fun recordManualConfirmation(
        context: Context,
        plan: ReminderPlan,
        occurredAtEpochMs: Long = System.currentTimeMillis()
    ) {
        appendEntry(
            context = context,
            entry = buildEntry(
                type = ReminderLogEventType.MANUAL_CONFIRMED,
                plan = plan,
                occurredAtEpochMs = occurredAtEpochMs
            )
        )
    }

    private fun buildEntry(
        type: ReminderLogEventType,
        plan: ReminderPlan,
        occurredAtEpochMs: Long
    ): ReminderLogEntry {
        return ReminderLogEntry(
            id = UUID.randomUUID().toString(),
            type = type,
            planId = plan.id,
            planName = plan.name,
            reminderHour = plan.hour,
            reminderMinute = plan.minute,
            occurredAtEpochMs = occurredAtEpochMs
        )
    }

    private fun appendEntry(
        context: Context,
        entry: ReminderLogEntry
    ) {
        runCatching {
            synchronized(this) {
                val dataSource = dataSource(context)
                val entries = readEntries(dataSource) + entry
                val trimmedEntries = ReminderLogJsonCodec.trimToMaxEntries(
                    entries = entries,
                    maxEntries = MAX_LOG_ENTRIES
                )
                saveEntries(
                    dataSource = dataSource,
                    entries = trimmedEntries
                )
            }
        }
    }

    private fun readEntries(dataSource: ReminderPreferencesDataSource): List<ReminderLogEntry> {
        return ReminderLogJsonCodec.parseEntries(dataSource.getString(KEY_ENTRIES_JSON))
    }

    private fun saveEntries(
        dataSource: ReminderPreferencesDataSource,
        entries: List<ReminderLogEntry>
    ) {
        dataSource.edit(commitSynchronously = true) {
            putString(KEY_ENTRIES_JSON, ReminderLogJsonCodec.encodeEntries(entries))
        }
    }

    private fun dataSource(context: Context): ReminderPreferencesDataSource {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ReminderPreferencesDataSource(preferences)
    }
}

