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

        val planId = intent.getStringExtra(ReminderContract.EXTRA_PLAN_ID)
            ?.takeIf { it.isNotBlank() }
            ?: return
        val plan = ReminderSessionStore.getPlan(context, planId) ?: return

        if (ReminderSessionStore.consumeSuppressDeleteFallback(context, plan.id)) {
            return
        }

        val latestPlan = ReminderSessionStore.getPlan(context, plan.id) ?: return
        if (!latestPlan.isReminderActive) {
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

        ReminderScheduler.scheduleFallbackAfter(
            context = context,
            plan = latestPlan,
            delayMs = ReminderContract.DELETE_RESCHEDULE_DELAY_MS,
            reason = fallbackReason
        )
    }
}
