package io.github.c1921.pillring.permission

import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionHealthEvaluatorTest {
    @Test
    fun notificationPermissionState_okWhenRuntimeAppAndChannelAreEnabled() {
        val snapshot = NotificationHealthSnapshot(
            runtimePermissionGranted = true,
            appNotificationsEnabled = true,
            reminderChannelImportance = NotificationManager.IMPORTANCE_HIGH
        )

        val actual = PermissionHealthEvaluator.notificationPermissionState(snapshot)

        assertEquals(PermissionState.OK, actual)
    }

    @Test
    fun notificationPermissionState_needsActionWhenAppNotificationsDisabled() {
        val snapshot = NotificationHealthSnapshot(
            runtimePermissionGranted = true,
            appNotificationsEnabled = false,
            reminderChannelImportance = NotificationManager.IMPORTANCE_HIGH
        )

        val actual = PermissionHealthEvaluator.notificationPermissionState(snapshot)

        assertEquals(PermissionState.NEEDS_ACTION, actual)
    }

    @Test
    fun notificationPermissionState_needsActionWhenChannelDisabled() {
        val snapshot = NotificationHealthSnapshot(
            runtimePermissionGranted = true,
            appNotificationsEnabled = true,
            reminderChannelImportance = NotificationManager.IMPORTANCE_NONE
        )

        val actual = PermissionHealthEvaluator.notificationPermissionState(snapshot)

        assertEquals(PermissionState.NEEDS_ACTION, actual)
    }

    @Test
    fun notificationChannelState_needsActionOnlyWhenChannelDisabled() {
        val disabledSnapshot = NotificationHealthSnapshot(
            runtimePermissionGranted = true,
            appNotificationsEnabled = true,
            reminderChannelImportance = NotificationManager.IMPORTANCE_NONE
        )
        val enabledSnapshot = disabledSnapshot.copy(
            reminderChannelImportance = NotificationManager.IMPORTANCE_HIGH
        )

        assertEquals(
            PermissionState.NEEDS_ACTION,
            PermissionHealthEvaluator.notificationChannelState(disabledSnapshot)
        )
        assertEquals(
            PermissionState.OK,
            PermissionHealthEvaluator.notificationChannelState(enabledSnapshot)
        )
    }

    @Test
    fun shouldOpenHealthPageAfterReminderEnabled_trueForNeedsActionOrManualCheck() {
        assertTrue(
            PermissionHealthEvaluator.shouldOpenHealthPageAfterReminderEnabled(
                listOf(item(state = PermissionState.NEEDS_ACTION))
            )
        )
        assertTrue(
            PermissionHealthEvaluator.shouldOpenHealthPageAfterReminderEnabled(
                listOf(item(state = PermissionState.MANUAL_CHECK))
            )
        )
        assertFalse(
            PermissionHealthEvaluator.shouldOpenHealthPageAfterReminderEnabled(
                listOf(item(state = PermissionState.OK))
            )
        )
    }

    @Test
    fun xiaomiManualItemIds_includeExpectedHyperOsChecks() {
        assertEquals(
            listOf(
                "xiaomi_autostart",
                "xiaomi_battery_saver",
                "xiaomi_notification_behavior",
                "xiaomi_local_background_notification"
            ),
            PermissionHealthEvaluator.xiaomiManualItemIds
        )
    }

    private fun item(state: PermissionState): PermissionHealthItem {
        return PermissionHealthItem(
            id = state.name,
            title = state.name,
            statusText = state.name,
            detailText = state.name,
            state = state,
            actionLabel = "Open",
            action = PermissionAction.OPEN_APP_DETAILS_SETTINGS
        )
    }
}
