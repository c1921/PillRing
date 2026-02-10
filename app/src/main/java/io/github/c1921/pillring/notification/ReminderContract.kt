package io.github.c1921.pillring.notification

object ReminderContract {
    const val CHANNEL_ID = "pillring_reminder_channel"

    const val ACTION_SHOW_REMINDER = "io.github.c1921.pillring.action.SHOW_REMINDER"
    const val ACTION_NOTIFICATION_DELETED = "io.github.c1921.pillring.action.NOTIFICATION_DELETED"
    const val ACTION_OPEN_REMINDER_CONFIRM = "io.github.c1921.pillring.action.OPEN_REMINDER_CONFIRM"

    const val EXTRA_REASON = "extra_reason"
    const val EXTRA_ALARM_KIND = "extra_alarm_kind"
    const val EXTRA_PLAN_ID = "extra_plan_id"

    const val ALARM_KIND_DAILY = "daily"
    const val ALARM_KIND_FALLBACK = "fallback"

    const val DELETE_RESCHEDULE_DELAY_MS = 30_000L
}
