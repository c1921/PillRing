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

        when (alarmKind) {
            ReminderContract.ALARM_KIND_DAILY -> handleDailyReminder(context, reason)
            ReminderContract.ALARM_KIND_FALLBACK -> handleFallbackReminder(context, reason)
            else -> {
                // Backward compatibility for older alarms without alarm-kind extra.
                ReminderNotifier.showNotification(context = context, reason = reason)
            }
        }
    }

    private fun handleDailyReminder(context: Context, reason: String) {
        if (!ReminderSessionStore.isPlanEnabled(context)) {
            return
        }

        ReminderSessionStore.markReminderTriggered(context)
        ReminderNotifier.showNotification(context = context, reason = reason)

        val selectedTime = ReminderSessionStore.getSelectedTime(context) ?: return
        val nextTriggerAtMs = ReminderTimeCalculator.computeNextDailyTriggerAtMs(
            nowMs = System.currentTimeMillis(),
            zoneId = ZoneId.systemDefault(),
            hour = selectedTime.first,
            minute = selectedTime.second
        )
        ReminderScheduler.scheduleDailyAt(
            context = context,
            triggerAtMs = nextTriggerAtMs,
            reason = context.getString(R.string.reason_daily_scheduled)
        )
    }

    private fun handleFallbackReminder(context: Context, reason: String) {
        if (!ReminderSessionStore.isReminderActive(context)) {
            return
        }

        ReminderNotifier.showNotification(context = context, reason = reason)
    }
}
