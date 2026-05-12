package io.github.c1921.pillring.permission

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.c1921.pillring.R
import io.github.c1921.pillring.notification.ReminderContract
import io.github.c1921.pillring.notification.ReminderLogEventType
import io.github.c1921.pillring.notification.ReminderLogStore
import io.github.c1921.pillring.notification.ReminderNotifier
import io.github.c1921.pillring.notification.ReminderScheduler
import io.github.c1921.pillring.notification.ReminderSessionStore
import io.github.c1921.pillring.notification.ReminderTimeCalculator
import java.time.ZoneId

object PermissionHealthChecker {
    fun buildItems(context: Context): List<PermissionHealthItem> {
        val items = mutableListOf<PermissionHealthItem>()
        val notificationSnapshot = buildNotificationSnapshot(context)
        items += buildNotificationItem(context, notificationSnapshot)
        items += buildNotificationChannelItem(context, notificationSnapshot)
        items += buildExactAlarmItem(context)
        items += buildBatteryOptimizationItem(context)
        items += buildBackgroundRestrictionItem(context)

        if (isXiaomiFamily()) {
            items += buildXiaomiManualItems(context)
        }

        return items
    }

    fun buildReminderTimingInfo(
        context: Context,
        nowMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PermissionReminderTimingInfo {
        val nextReminder = ReminderSessionStore.getPlans(context)
            .asSequence()
            .filter { plan -> plan.enabled }
            .mapNotNull { plan ->
                val triggerAtMs = runCatching {
                    ReminderTimeCalculator.computeNextTriggerAtMs(
                        nowMs = nowMs,
                        zoneId = zoneId,
                        plan = plan
                    )
                }.getOrNull() ?: return@mapNotNull null
                plan to triggerAtMs
            }
            .minByOrNull { (_, triggerAtMs) -> triggerAtMs }

        val lastTriggered = ReminderLogStore.getEntries(context)
            .firstOrNull { entry -> entry.type == ReminderLogEventType.REMINDER_TRIGGERED }

        return PermissionReminderTimingInfo(
            nextPlanName = nextReminder?.first?.name,
            nextTriggerAtEpochMs = nextReminder?.second,
            lastTriggeredPlanName = lastTriggered?.planName,
            lastTriggeredAtEpochMs = lastTriggered?.occurredAtEpochMs
        )
    }

    fun shouldOpenHealthPageAfterReminderEnabled(items: List<PermissionHealthItem>): Boolean {
        return PermissionHealthEvaluator.shouldOpenHealthPageAfterReminderEnabled(items)
    }

    private fun buildNotificationItem(
        context: Context,
        snapshot: NotificationHealthSnapshot
    ): PermissionHealthItem {
        val state = PermissionHealthEvaluator.notificationPermissionState(snapshot)
        return PermissionHealthItem(
            id = "notification_permission",
            title = context.getString(R.string.permission_item_notification_title),
            statusText = stateText(context, state),
            detailText = context.getString(R.string.permission_item_notification_detail),
            state = state,
            actionLabel = context.getString(R.string.permission_action_open_settings),
            action = PermissionAction.OPEN_NOTIFICATION_SETTINGS
        )
    }

    private fun buildNotificationChannelItem(
        context: Context,
        snapshot: NotificationHealthSnapshot
    ): PermissionHealthItem {
        val state = PermissionHealthEvaluator.notificationChannelState(snapshot)
        return PermissionHealthItem(
            id = "notification_channel",
            title = context.getString(R.string.permission_item_notification_channel_title),
            statusText = stateText(context, state),
            detailText = context.getString(R.string.permission_item_notification_channel_detail),
            state = state,
            actionLabel = context.getString(R.string.permission_action_open_settings),
            action = PermissionAction.OPEN_NOTIFICATION_CHANNEL_SETTINGS
        )
    }

    private fun buildExactAlarmItem(context: Context): PermissionHealthItem {
        val allowed = ReminderScheduler.canScheduleExactAlarms(context)
        val state = if (allowed) PermissionState.OK else PermissionState.NEEDS_ACTION
        return PermissionHealthItem(
            id = "exact_alarm_permission",
            title = context.getString(R.string.permission_item_exact_alarm_title),
            statusText = stateText(context, state),
            detailText = context.getString(R.string.permission_item_exact_alarm_detail),
            state = state,
            actionLabel = context.getString(R.string.permission_action_open_settings),
            action = PermissionAction.OPEN_EXACT_ALARM_SETTINGS
        )
    }

    private fun buildBatteryOptimizationItem(context: Context): PermissionHealthItem {
        val powerManager = context.getSystemService(PowerManager::class.java)
        val ignoringOptimization = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        val state = if (ignoringOptimization) PermissionState.OK else PermissionState.NEEDS_ACTION
        return PermissionHealthItem(
            id = "battery_optimization",
            title = context.getString(R.string.permission_item_battery_optimization_title),
            statusText = stateText(context, state),
            detailText = context.getString(R.string.permission_item_battery_optimization_detail),
            state = state,
            actionLabel = context.getString(R.string.permission_action_open_settings),
            action = PermissionAction.OPEN_BATTERY_OPTIMIZATION_SETTINGS
        )
    }

    private fun buildBackgroundRestrictionItem(context: Context): PermissionHealthItem {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val restricted = activityManager.isBackgroundRestricted
        val state = if (restricted) PermissionState.NEEDS_ACTION else PermissionState.OK
        return PermissionHealthItem(
            id = "background_restriction",
            title = context.getString(R.string.permission_item_background_restriction_title),
            statusText = stateText(context, state),
            detailText = context.getString(R.string.permission_item_background_restriction_detail),
            state = state,
            actionLabel = context.getString(R.string.permission_action_open_settings),
            action = PermissionAction.OPEN_APP_DETAILS_SETTINGS
        )
    }

    private fun buildXiaomiManualItems(context: Context): List<PermissionHealthItem> {
        return listOf(
            buildXiaomiAutostartItem(context),
            buildXiaomiBatterySaverItem(context),
            buildXiaomiNotificationBehaviorItem(context),
            buildXiaomiLocalBackgroundNotificationItem(context)
        )
    }

    private fun buildXiaomiAutostartItem(context: Context): PermissionHealthItem {
        val state = PermissionState.MANUAL_CHECK
        return PermissionHealthItem(
            id = "xiaomi_autostart",
            title = context.getString(R.string.permission_item_autostart_title),
            statusText = stateText(context, state),
            detailText = context.getString(R.string.permission_item_autostart_detail),
            state = state,
            actionLabel = context.getString(R.string.permission_action_open_settings),
            action = PermissionAction.OPEN_XIAOMI_AUTOSTART_SETTINGS
        )
    }

    private fun buildXiaomiBatterySaverItem(context: Context): PermissionHealthItem {
        val state = PermissionState.MANUAL_CHECK
        return PermissionHealthItem(
            id = "xiaomi_battery_saver",
            title = context.getString(R.string.permission_item_xiaomi_battery_saver_title),
            statusText = stateText(context, state),
            detailText = context.getString(R.string.permission_item_xiaomi_battery_saver_detail),
            state = state,
            actionLabel = context.getString(R.string.permission_action_open_settings),
            action = PermissionAction.OPEN_XIAOMI_BATTERY_SETTINGS
        )
    }

    private fun buildXiaomiNotificationBehaviorItem(context: Context): PermissionHealthItem {
        val state = PermissionState.MANUAL_CHECK
        return PermissionHealthItem(
            id = "xiaomi_notification_behavior",
            title = context.getString(R.string.permission_item_xiaomi_notification_behavior_title),
            statusText = stateText(context, state),
            detailText = context.getString(
                R.string.permission_item_xiaomi_notification_behavior_detail
            ),
            state = state,
            actionLabel = context.getString(R.string.permission_action_open_settings),
            action = PermissionAction.OPEN_NOTIFICATION_CHANNEL_SETTINGS
        )
    }

    private fun buildXiaomiLocalBackgroundNotificationItem(
        context: Context
    ): PermissionHealthItem {
        val state = PermissionState.MANUAL_CHECK
        return PermissionHealthItem(
            id = "xiaomi_local_background_notification",
            title = context.getString(
                R.string.permission_item_xiaomi_local_background_notification_title
            ),
            statusText = stateText(context, state),
            detailText = context.getString(
                R.string.permission_item_xiaomi_local_background_notification_detail
            ),
            state = state,
            actionLabel = context.getString(R.string.permission_action_open_settings),
            action = PermissionAction.OPEN_APP_DETAILS_SETTINGS
        )
    }

    private fun buildNotificationSnapshot(context: Context): NotificationHealthSnapshot {
        val runtimePermissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        val appNotificationsEnabled = NotificationManagerCompat.from(context)
            .areNotificationsEnabled()
        val reminderChannelImportance = readReminderChannelImportance(context)

        return NotificationHealthSnapshot(
            runtimePermissionGranted = runtimePermissionGranted,
            appNotificationsEnabled = appNotificationsEnabled,
            reminderChannelImportance = reminderChannelImportance
        )
    }

    private fun readReminderChannelImportance(context: Context): Int? {
        runCatching {
            ReminderNotifier.ensureChannel(context)
        }
        return context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(ReminderContract.CHANNEL_ID)
            ?.importance
    }

    private fun stateText(context: Context, state: PermissionState): String {
        return when (state) {
            PermissionState.OK -> context.getString(R.string.permission_status_ok)
            PermissionState.NEEDS_ACTION -> context.getString(R.string.permission_status_needs_action)
            PermissionState.MANUAL_CHECK -> context.getString(R.string.permission_status_manual_check)
            PermissionState.UNAVAILABLE -> context.getString(R.string.permission_status_unavailable)
        }
    }

    private fun isXiaomiFamily(): Boolean {
        val vendor = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase()
        val xiaomiTokens = listOf("xiaomi", "redmi", "poco")
        return xiaomiTokens.any { token -> vendor.contains(token) }
    }
}
