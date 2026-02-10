package io.github.c1921.pillring.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.c1921.pillring.R

class NotificationDeleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderContract.ACTION_NOTIFICATION_DELETED) {
            return
        }

        if (!ReminderScheduler.canScheduleExactAlarms(context)) {
            return
        }

        val previousReason = intent.getStringExtra(ReminderContract.EXTRA_REASON)
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.reason_unknown)
        val fallbackReason = context.getString(
            R.string.reason_delete_fallback,
            previousReason
        )

        ReminderScheduler.scheduleExact(
            context = context,
            delayMs = ReminderContract.DELETE_RESCHEDULE_DELAY_MS,
            mode = ReminderMode.ONGOING,
            reason = fallbackReason
        )
    }
}
