package io.github.c1921.pillring.notification

object ReminderContract {
    const val CHANNEL_ID = "pillring_reminder_channel"
    const val NOTIFICATION_ID = 1001

    const val ACTION_SHOW_REMINDER = "io.github.c1921.pillring.action.SHOW_REMINDER"
    const val ACTION_NOTIFICATION_DELETED = "io.github.c1921.pillring.action.NOTIFICATION_DELETED"

    const val EXTRA_REASON = "extra_reason"

    const val REQUEST_CODE_ALARM_REMINDER = 2001
    const val REQUEST_CODE_DELETE_INTENT = 3001
    const val REQUEST_CODE_CONTENT_INTENT = 3002

    const val INITIAL_TRIGGER_DELAY_MS = 10_000L
    const val DELETE_RESCHEDULE_DELAY_MS = 30_000L
}
