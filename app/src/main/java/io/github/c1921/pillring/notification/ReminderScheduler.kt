package io.github.c1921.pillring.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object ReminderScheduler {
    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    fun scheduleDailyAt(
        context: Context,
        plan: ReminderPlan,
        triggerAtMs: Long,
        reason: String
    ): Boolean {
        return scheduleExactAt(
            context = context,
            plan = plan,
            triggerAtMs = triggerAtMs,
            reason = reason,
            alarmKind = ReminderContract.ALARM_KIND_DAILY
        )
    }

    fun scheduleFallbackAfter(
        context: Context,
        plan: ReminderPlan,
        delayMs: Long,
        reason: String
    ): Boolean {
        return scheduleExactAt(
            context = context,
            plan = plan,
            triggerAtMs = System.currentTimeMillis() + delayMs,
            reason = reason,
            alarmKind = ReminderContract.ALARM_KIND_FALLBACK
        )
    }

    fun cancelPlanDailyReminder(
        context: Context,
        plan: ReminderPlan
    ) {
        cancelReminderByKind(
            context = context,
            plan = plan,
            alarmKind = ReminderContract.ALARM_KIND_DAILY
        )
    }

    fun cancelPlanFallbackReminder(
        context: Context,
        plan: ReminderPlan
    ) {
        cancelReminderByKind(
            context = context,
            plan = plan,
            alarmKind = ReminderContract.ALARM_KIND_FALLBACK
        )
    }

    fun cancelPlanReminders(
        context: Context,
        plan: ReminderPlan
    ) {
        cancelPlanDailyReminder(context = context, plan = plan)
        cancelPlanFallbackReminder(context = context, plan = plan)
    }

    private fun scheduleExactAt(
        context: Context,
        plan: ReminderPlan,
        triggerAtMs: Long,
        reason: String,
        alarmKind: String
    ): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val alarmIntent = buildAlarmPendingIntent(
            context = context,
            plan = plan,
            reason = reason,
            alarmKind = alarmKind
        ) ?: return false

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

    private fun cancelReminderByKind(
        context: Context,
        plan: ReminderPlan,
        alarmKind: String
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = buildAlarmPendingIntent(
            context = context,
            plan = plan,
            reason = "",
            alarmKind = alarmKind,
            extraFlags = PendingIntent.FLAG_NO_CREATE
        ) ?: return

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildAlarmPendingIntent(
        context: Context,
        plan: ReminderPlan,
        reason: String,
        alarmKind: String,
        extraFlags: Int = 0
    ): PendingIntent? {
        val requestCode = when (alarmKind) {
            ReminderContract.ALARM_KIND_DAILY -> requestCodeForDaily(plan)
            ReminderContract.ALARM_KIND_FALLBACK -> requestCodeForFallback(plan)
            else -> return null
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderContract.ACTION_SHOW_REMINDER
            putExtra(ReminderContract.EXTRA_REASON, reason)
            putExtra(ReminderContract.EXTRA_ALARM_KIND, alarmKind)
            putExtra(ReminderContract.EXTRA_PLAN_ID, plan.id)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT or extraFlags
        )
    }

    private fun requestCodeForDaily(plan: ReminderPlan): Int = plan.notificationId * 10 + 1

    private fun requestCodeForFallback(plan: ReminderPlan): Int = plan.notificationId * 10 + 2
}
