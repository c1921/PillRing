package io.github.c1921.pillring.permission

import android.app.NotificationManager

internal data class NotificationHealthSnapshot(
    val runtimePermissionGranted: Boolean,
    val appNotificationsEnabled: Boolean,
    val reminderChannelImportance: Int?
)

internal object PermissionHealthEvaluator {
    val xiaomiManualItemIds = listOf(
        "xiaomi_autostart",
        "xiaomi_battery_saver",
        "xiaomi_notification_behavior",
        "xiaomi_local_background_notification"
    )

    fun notificationPermissionState(snapshot: NotificationHealthSnapshot): PermissionState {
        return if (
            snapshot.runtimePermissionGranted &&
            snapshot.appNotificationsEnabled &&
            snapshot.reminderChannelImportance != NotificationManager.IMPORTANCE_NONE
        ) {
            PermissionState.OK
        } else {
            PermissionState.NEEDS_ACTION
        }
    }

    fun notificationChannelState(snapshot: NotificationHealthSnapshot): PermissionState {
        return if (snapshot.reminderChannelImportance == NotificationManager.IMPORTANCE_NONE) {
            PermissionState.NEEDS_ACTION
        } else {
            PermissionState.OK
        }
    }

    fun shouldOpenHealthPageAfterReminderEnabled(items: List<PermissionHealthItem>): Boolean {
        return items.any { item ->
            item.state == PermissionState.NEEDS_ACTION || item.state == PermissionState.MANUAL_CHECK
        }
    }
}
