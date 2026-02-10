package io.github.c1921.pillring

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.c1921.pillring.notification.ReminderContract
import io.github.c1921.pillring.notification.ReminderNotifier
import io.github.c1921.pillring.notification.ReminderScheduler
import io.github.c1921.pillring.notification.ReminderSessionStore
import io.github.c1921.pillring.ui.theme.PillRingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasNotificationPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        setContent {
            var isReminderActive by rememberSaveable {
                mutableStateOf(ReminderSessionStore.isReminderActive(this))
            }
            PillRingTheme {
                ReminderTestScreen(
                    isReminderActive = isReminderActive,
                    onScheduleClick = {
                        if (scheduleReminder()) {
                            isReminderActive = true
                        }
                    },
                    onConfirmStopClick = {
                        confirmStopReminder()
                        isReminderActive = false
                    }
                )
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun scheduleReminder(): Boolean {
        if (!hasNotificationPermission()) {
            Toast.makeText(
                this,
                getString(R.string.msg_notification_permission_required),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (!ReminderScheduler.canScheduleExactAlarms(this)) {
            Toast.makeText(
                this,
                getString(R.string.msg_exact_alarm_required),
                Toast.LENGTH_SHORT
            ).show()
            openExactAlarmSettings()
            return false
        }

        val scheduled = ReminderScheduler.scheduleExact(
            context = this,
            delayMs = ReminderContract.INITIAL_TRIGGER_DELAY_MS,
            reason = getString(R.string.reason_manual_ongoing)
        )

        if (scheduled) {
            ReminderSessionStore.markReminderTriggered(this)
            Toast.makeText(
                this,
                getString(R.string.msg_scheduled_ongoing),
                Toast.LENGTH_SHORT
            ).show()
            return true
        } else {
            Toast.makeText(
                this,
                getString(R.string.msg_exact_alarm_failed),
                Toast.LENGTH_SHORT
            ).show()
            openExactAlarmSettings()
            return false
        }
    }

    private fun confirmStopReminder() {
        ReminderSessionStore.markReminderConfirmed(this)
        ReminderScheduler.cancelScheduledReminder(this)
        ReminderNotifier.cancelReminderNotification(this)
        Toast.makeText(
            this,
            getString(R.string.msg_reminder_stopped),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openExactAlarmSettings() {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    getString(R.string.msg_notification_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
}

@Composable
private fun ReminderTestScreen(
    isReminderActive: Boolean,
    onScheduleClick: () -> Unit,
    onConfirmStopClick: () -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.test_page_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(text = stringResource(R.string.test_page_description))
            Text(
                text = stringResource(
                    if (isReminderActive) {
                        R.string.status_reminder_active
                    } else {
                        R.string.status_reminder_idle
                    }
                )
            )

            if (isReminderActive) {
                Button(
                    onClick = onConfirmStopClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.btn_confirm_stop_reminder))
                }
            } else {
                Button(
                    onClick = onScheduleClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.btn_schedule_ongoing))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderTestScreenPreview() {
    PillRingTheme {
        ReminderTestScreen(
            isReminderActive = false,
            onScheduleClick = {},
            onConfirmStopClick = {}
        )
    }
}
