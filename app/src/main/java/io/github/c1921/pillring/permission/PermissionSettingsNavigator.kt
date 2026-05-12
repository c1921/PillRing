package io.github.c1921.pillring.permission

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import io.github.c1921.pillring.notification.ReminderContract

object PermissionSettingsNavigator {
    fun open(context: Context, action: PermissionAction): Boolean {
        return when (action) {
            PermissionAction.OPEN_NOTIFICATION_SETTINGS -> openNotificationSettings(context)
            PermissionAction.OPEN_NOTIFICATION_CHANNEL_SETTINGS ->
                openNotificationChannelSettings(context)
            PermissionAction.OPEN_EXACT_ALARM_SETTINGS -> openExactAlarmSettings(context)
            PermissionAction.OPEN_BATTERY_OPTIMIZATION_SETTINGS -> openBatteryOptimizationSettings(context)
            PermissionAction.OPEN_APP_DETAILS_SETTINGS -> openAppDetailsSettings(context)
            PermissionAction.OPEN_XIAOMI_AUTOSTART_SETTINGS -> openXiaomiAutostartSettings(context)
            PermissionAction.OPEN_XIAOMI_BATTERY_SETTINGS -> openXiaomiBatterySettings(context)
        }
    }

    private fun openNotificationSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safeStartActivity(context, intent)
    }

    private fun openNotificationChannelSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, ReminderContract.CHANNEL_ID)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safeStartActivity(context, intent) || openNotificationSettings(context)
    }

    private fun openExactAlarmSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safeStartActivity(context, intent)
    }

    private fun openBatteryOptimizationSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safeStartActivity(context, intent)
    }

    private fun openAppDetailsSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safeStartActivity(context, intent)
    }

    private fun openXiaomiAutostartSettings(context: Context): Boolean {
        val intent = Intent().apply {
            component = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
            putExtra("package_name", context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safeStartActivity(context, intent) || openAppDetailsSettings(context)
    }

    private fun openXiaomiBatterySettings(context: Context): Boolean {
        val intent = Intent().apply {
            component = ComponentName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
            )
            putExtra("package_name", context.packageName)
            putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return safeStartActivity(context, intent) || openAppDetailsSettings(context)
    }

    private fun safeStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
