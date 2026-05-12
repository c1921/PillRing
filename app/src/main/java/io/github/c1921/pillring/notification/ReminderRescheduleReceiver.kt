package io.github.c1921.pillring.notification

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.c1921.pillring.R

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!isRescheduleAction(intent.action)) {
            return
        }

        ReminderPlanCoordinator(context.applicationContext).rescheduleEnabledPlans(
            reason = context.getString(R.string.reason_system_reschedule)
        )
    }

    internal companion object {
        fun isRescheduleAction(action: String?): Boolean {
            return action in RESCHEDULE_ACTIONS
        }

        private val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
    }
}
