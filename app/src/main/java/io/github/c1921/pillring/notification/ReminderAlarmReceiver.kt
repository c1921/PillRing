package io.github.c1921.pillring.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.c1921.pillring.R
import java.time.ZoneId

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderContract.ACTION_SHOW_REMINDER) {
            return
        }

        val reason = intent.getStringExtra(ReminderContract.EXTRA_REASON).orEmpty()
        val alarmKind = intent.getStringExtra(ReminderContract.EXTRA_ALARM_KIND)
            ?.takeIf { it.isNotBlank() }
            ?: return
        val planId = intent.getStringExtra(ReminderContract.EXTRA_PLAN_ID)
            ?.takeIf { it.isNotBlank() }
            ?: return

        when (alarmKind) {
            ReminderContract.ALARM_KIND_DAILY -> handleDailyReminder(
                context = context,
                planId = planId,
                reason = reason
            )

            ReminderContract.ALARM_KIND_FALLBACK -> handleFallbackReminder(
                context = context,
                planId = planId,
                reason = reason
            )

            else -> return
        }
    }

    private fun handleDailyReminder(
        context: Context,
        planId: String,
        reason: String
    ) {
        val plan = ReminderSessionStore.getPlan(context, planId) ?: return
        if (!plan.enabled) {
            return
        }

        ReminderSessionStore.markReminderTriggered(context, planId)
        val triggeredPlan = ReminderSessionStore.getPlan(context, planId) ?: return
        ReminderNotifier.showNotification(context = context, plan = triggeredPlan, reason = reason)

        val nextTriggerAtMs = ReminderTimeCalculator.computeNextTriggerAtMs(
            nowMs = System.currentTimeMillis(),
            zoneId = ZoneId.systemDefault(),
            plan = triggeredPlan
        )
        ReminderScheduler.scheduleDailyAt(
            context = context,
            plan = triggeredPlan,
            triggerAtMs = nextTriggerAtMs,
            reason = context.getString(R.string.reason_plan_scheduled)
        )
    }

    private fun handleFallbackReminder(
        context: Context,
        planId: String,
        reason: String
    ) {
        val plan = ReminderSessionStore.getPlan(context, planId) ?: return
        if (!plan.isReminderActive) {
            return
        }

        ReminderNotifier.showNotification(context = context, plan = plan, reason = reason)
    }
}
