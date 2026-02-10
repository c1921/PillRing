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

    fun scheduleExactAt(
        context: Context,
        triggerAtMs: Long,
        reason: String,
        alarmKind: String
    ): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val alarmIntent = buildAlarmPendingIntent(context, reason, alarmKind) ?: return false

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

    fun scheduleDailyAt(
        context: Context,
        triggerAtMs: Long,
        reason: String
    ): Boolean {
        return scheduleExactAt(
            context = context,
            triggerAtMs = triggerAtMs,
            reason = reason,
            alarmKind = ReminderContract.ALARM_KIND_DAILY
        )
    }

    fun scheduleFallbackAfter(
        context: Context,
        delayMs: Long,
        reason: String
    ): Boolean {
        return scheduleExactAt(
            context = context,
            triggerAtMs = System.currentTimeMillis() + delayMs,
            reason = reason,
            alarmKind = ReminderContract.ALARM_KIND_FALLBACK
        )
    }

    fun cancelDailyReminder(context: Context) {
        cancelReminderByKind(context, ReminderContract.ALARM_KIND_DAILY)
    }

    fun cancelFallbackReminder(context: Context) {
        cancelReminderByKind(context, ReminderContract.ALARM_KIND_FALLBACK)
    }

    fun cancelAllScheduledReminders(context: Context) {
        cancelDailyReminder(context)
        cancelFallbackReminder(context)
    }

    private fun cancelReminderByKind(
        context: Context,
        alarmKind: String
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = buildAlarmPendingIntent(
            context = context,
            reason = "",
            alarmKind = alarmKind,
            extraFlags = PendingIntent.FLAG_NO_CREATE
        ) ?: return
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun buildAlarmPendingIntent(
        context: Context,
        reason: String,
        alarmKind: String,
        extraFlags: Int = 0
    ): PendingIntent? {
        val requestCode = when (alarmKind) {
            ReminderContract.ALARM_KIND_DAILY -> ReminderContract.REQUEST_CODE_ALARM_DAILY
            ReminderContract.ALARM_KIND_FALLBACK -> ReminderContract.REQUEST_CODE_ALARM_FALLBACK
            else -> return null
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderContract.ACTION_SHOW_REMINDER
            putExtra(ReminderContract.EXTRA_REASON, reason)
            putExtra(ReminderContract.EXTRA_ALARM_KIND, alarmKind)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT or extraFlags
        )
    }
}
