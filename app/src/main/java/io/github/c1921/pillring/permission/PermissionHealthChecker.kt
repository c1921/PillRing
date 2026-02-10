package io.github.c1921.pillring.permission

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import io.github.c1921.pillring.R
import io.github.c1921.pillring.notification.ReminderScheduler

object PermissionHealthChecker {
    fun buildItems(context: Context): List<PermissionHealthItem> {
        val items = mutableListOf<PermissionHealthItem>()
        items += buildNotificationItem(context)
        items += buildExactAlarmItem(context)
        items += buildBatteryOptimizationItem(context)
        items += buildBackgroundRestrictionItem(context)

        if (isXiaomiFamily()) {
            items += buildXiaomiAutostartItem(context)
        }

        return items
    }

    private fun buildNotificationItem(context: Context): PermissionHealthItem {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        val state = if (granted) PermissionState.OK else PermissionState.NEEDS_ACTION
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
