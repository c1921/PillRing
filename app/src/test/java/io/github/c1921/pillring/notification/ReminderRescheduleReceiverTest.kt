package io.github.c1921.pillring.notification

import android.app.AlarmManager
import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderRescheduleReceiverTest {
    @Test
    fun isRescheduleAction_acceptsExactAlarmPermissionStateChanged() {
        assertTrue(
            ReminderRescheduleReceiver.isRescheduleAction(
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
            )
        )
    }

    @Test
    fun isRescheduleAction_acceptsExistingSystemRescheduleActions() {
        assertTrue(ReminderRescheduleReceiver.isRescheduleAction(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(
            ReminderRescheduleReceiver.isRescheduleAction(Intent.ACTION_MY_PACKAGE_REPLACED)
        )
        assertTrue(ReminderRescheduleReceiver.isRescheduleAction(Intent.ACTION_TIME_CHANGED))
        assertTrue(ReminderRescheduleReceiver.isRescheduleAction(Intent.ACTION_TIMEZONE_CHANGED))
    }

    @Test
    fun isRescheduleAction_rejectsUnknownOrMissingActions() {
        assertFalse(ReminderRescheduleReceiver.isRescheduleAction(null))
        assertFalse(ReminderRescheduleReceiver.isRescheduleAction(Intent.ACTION_AIRPLANE_MODE_CHANGED))
    }
}
