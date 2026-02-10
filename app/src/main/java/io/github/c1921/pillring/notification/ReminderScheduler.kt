package io.github.c1921.pillring.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleExact(
        context: Context,
        delayMs: Long,
        reason: String
    ): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAtMs = System.currentTimeMillis() + delayMs
        val alarmIntent = buildAlarmPendingIntent(context, reason)

        return try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                alarmIntent
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancelScheduledReminder(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderContract.ACTION_SHOW_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ReminderContract.REQUEST_CODE_ALARM_REMINDER,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun buildAlarmPendingIntent(
        context: Context,
        reason: String
    ): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderContract.ACTION_SHOW_REMINDER
            putExtra(ReminderContract.EXTRA_REASON, reason)
        }

        return PendingIntent.getBroadcast(
            context,
            ReminderContract.REQUEST_CODE_ALARM_REMINDER,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
