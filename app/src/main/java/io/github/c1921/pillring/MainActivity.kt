package io.github.c1921.pillring

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.c1921.pillring.notification.ReminderContract
import io.github.c1921.pillring.notification.ReminderNotifier
import io.github.c1921.pillring.notification.ReminderScheduler
import io.github.c1921.pillring.notification.ReminderSessionStore
import io.github.c1921.pillring.permission.PermissionAction
import io.github.c1921.pillring.permission.PermissionHealthChecker
import io.github.c1921.pillring.permission.PermissionHealthItem
import io.github.c1921.pillring.permission.PermissionSettingsNavigator
import io.github.c1921.pillring.permission.PermissionState
import io.github.c1921.pillring.ui.theme.PillRingTheme

private enum class AppScreen {
    HOME,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasNotificationPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        setContent {
            var isReminderActive by rememberSaveable {
                mutableStateOf(ReminderSessionStore.isReminderActive(this@MainActivity))
            }
            var permissionItems by remember {
                mutableStateOf(buildPermissionItems())
            }
            var currentScreen by rememberSaveable {
                mutableStateOf(AppScreen.HOME)
            }
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        isReminderActive = ReminderSessionStore.isReminderActive(this@MainActivity)
                        permissionItems = buildPermissionItems()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            PillRingTheme {
                BackHandler(enabled = currentScreen == AppScreen.SETTINGS) {
                    currentScreen = AppScreen.HOME
                }

                when (currentScreen) {
                    AppScreen.HOME -> {
                        ReminderTestScreen(
                            isReminderActive = isReminderActive,
                            onScheduleClick = {
                                if (scheduleReminder()) {
                                    isReminderActive = true
                                    permissionItems = buildPermissionItems()
                                }
                            },
                            onConfirmStopClick = {
                                confirmStopReminder()
                                isReminderActive = false
                                permissionItems = buildPermissionItems()
                            },
                            onOpenSettingsClick = {
                                permissionItems = buildPermissionItems()
                                currentScreen = AppScreen.SETTINGS
                            }
                        )
                    }

                    AppScreen.SETTINGS -> {
                        SettingsScreen(
                            permissionItems = permissionItems,
                            onBackClick = { currentScreen = AppScreen.HOME },
                            onOpenPermissionSettings = ::openPermissionSettings
                        )
                    }
                }
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
            openPermissionSettings(PermissionAction.OPEN_EXACT_ALARM_SETTINGS)
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
        }

        Toast.makeText(
            this,
            getString(R.string.msg_exact_alarm_failed),
            Toast.LENGTH_SHORT
        ).show()
        openPermissionSettings(PermissionAction.OPEN_EXACT_ALARM_SETTINGS)
        return false
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

    private fun buildPermissionItems(): List<PermissionHealthItem> {
        return PermissionHealthChecker.buildItems(this)
    }

    private fun openPermissionSettings(action: PermissionAction) {
        val opened = PermissionSettingsNavigator.open(this, action)
        if (!opened) {
            Toast.makeText(
                this,
                getString(R.string.msg_open_settings_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
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
    onConfirmStopClick: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
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
            Button(
                onClick = onOpenSettingsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.btn_open_settings_page))
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    permissionItems: List<PermissionHealthItem>,
    onBackClick: () -> Unit,
    onOpenPermissionSettings: (PermissionAction) -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_page_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(text = stringResource(R.string.settings_page_description))
            Button(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.btn_back_to_test_page))
            }
            PermissionHealthPanel(
                items = permissionItems,
                onOpenPermissionSettings = onOpenPermissionSettings
            )
        }
    }
}

@Composable
private fun PermissionHealthPanel(
    items: List<PermissionHealthItem>,
    onOpenPermissionSettings: (PermissionAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.permission_health_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.permission_health_description),
            style = MaterialTheme.typography.bodyMedium
        )

        items.forEach { item ->
            PermissionHealthCard(
                item = item,
                onOpenPermissionSettings = onOpenPermissionSettings
            )
        }
    }
}

@Composable
private fun PermissionHealthCard(
    item: PermissionHealthItem,
    onOpenPermissionSettings: (PermissionAction) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = item.statusText,
                color = permissionStateColor(item.state),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = item.detailText,
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = { onOpenPermissionSettings(item.action) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = item.actionLabel)
            }
        }
    }
}

@Composable
private fun permissionStateColor(state: PermissionState): Color {
    return when (state) {
        PermissionState.OK -> MaterialTheme.colorScheme.primary
        PermissionState.NEEDS_ACTION -> MaterialTheme.colorScheme.error
        PermissionState.MANUAL_CHECK -> MaterialTheme.colorScheme.tertiary
        PermissionState.UNAVAILABLE -> MaterialTheme.colorScheme.outline
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderTestScreenPreview() {
    PillRingTheme {
        ReminderTestScreen(
            isReminderActive = false,
            onScheduleClick = {},
            onConfirmStopClick = {},
            onOpenSettingsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PillRingTheme {
        SettingsScreen(
            permissionItems = listOf(
                PermissionHealthItem(
                    id = "notification",
                    title = "Notification permission",
                    statusText = "Needs action",
                    detailText = "Allow notifications to receive reminder popups.",
                    state = PermissionState.NEEDS_ACTION,
                    actionLabel = "Open settings",
                    action = PermissionAction.OPEN_NOTIFICATION_SETTINGS
                )
            ),
            onBackClick = {},
            onOpenPermissionSettings = {}
        )
    }
}
