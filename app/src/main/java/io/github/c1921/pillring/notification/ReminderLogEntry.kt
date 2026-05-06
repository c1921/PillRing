package io.github.c1921.pillring.notification

data class ReminderLogEntry(
    val id: String,
    val type: ReminderLogEventType,
    val planId: String,
    val planName: String,
    val reminderHour: Int,
    val reminderMinute: Int,
    val occurredAtEpochMs: Long
)

enum class ReminderLogEventType {
    REMINDER_TRIGGERED,
    MANUAL_CONFIRMED
}

