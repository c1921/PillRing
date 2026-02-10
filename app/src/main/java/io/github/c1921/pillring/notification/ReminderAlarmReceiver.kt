package io.github.c1921.pillring.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderContract.ACTION_SHOW_REMINDER) {
            return
        }

        val mode = ReminderMode.fromName(intent.getStringExtra(ReminderContract.EXTRA_MODE))
        val reason = intent.getStringExtra(ReminderContract.EXTRA_REASON).orEmpty()

        ReminderNotifier.showNotification(
            context = context,
            mode = mode,
            reason = reason
        )
    }
}
