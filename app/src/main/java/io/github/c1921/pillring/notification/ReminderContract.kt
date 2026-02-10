package io.github.c1921.pillring.notification

object ReminderContract {
    const val CHANNEL_ID = "pillring_reminder_channel"
    const val LEGACY_NOTIFICATION_ID = 1001

    const val ACTION_SHOW_REMINDER = "io.github.c1921.pillring.action.SHOW_REMINDER"
    const val ACTION_NOTIFICATION_DELETED = "io.github.c1921.pillring.action.NOTIFICATION_DELETED"

    const val EXTRA_REASON = "extra_reason"
    const val EXTRA_ALARM_KIND = "extra_alarm_kind"
    const val EXTRA_PLAN_ID = "extra_plan_id"

    const val REQUEST_CODE_ALARM_DAILY_LEGACY = 2001
    const val REQUEST_CODE_ALARM_FALLBACK_LEGACY = 2002
    const val REQUEST_CODE_DELETE_INTENT_LEGACY = 3001
    const val REQUEST_CODE_CONTENT_INTENT_LEGACY = 3002

    const val ALARM_KIND_DAILY = "daily"
    const val ALARM_KIND_FALLBACK = "fallback"

    const val DELETE_RESCHEDULE_DELAY_MS = 30_000L
}
