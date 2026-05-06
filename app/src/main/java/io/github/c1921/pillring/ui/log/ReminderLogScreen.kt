package io.github.c1921.pillring.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.c1921.pillring.R
import io.github.c1921.pillring.notification.ReminderLogEntry
import io.github.c1921.pillring.notification.ReminderLogEventType
import io.github.c1921.pillring.ui.UiTestTags
import io.github.c1921.pillring.ui.common.formatReminderDateTime
import io.github.c1921.pillring.ui.common.formatReminderTime
import io.github.c1921.pillring.ui.theme.PillRingTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ReminderLogScreen(
    entries: List<ReminderLogEntry>,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.REMINDER_LOG_SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.reminder_log_page_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (entries.isEmpty()) {
                item {
                    EmptyReminderLogCard()
                }
            } else {
                items(
                    items = entries,
                    key = { entry -> entry.id }
                ) { entry ->
                    ReminderLogEntryCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun EmptyReminderLogCard() {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTestTags.REMINDER_LOG_EMPTY),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.reminder_log_empty_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.reminder_log_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReminderLogEntryCard(entry: ReminderLogEntry) {
    val context = LocalContext.current
    val occurredAtText = remember(entry.occurredAtEpochMs) {
        formatReminderDateTime(entry.occurredAtEpochMs)
    }
    val reminderTimeText = remember(entry.reminderHour, entry.reminderMinute, context) {
        formatReminderTime(
            context = context,
            hour = entry.reminderHour,
            minute = entry.reminderMinute
        )
    }
    val eventTitleResId = when (entry.type) {
        ReminderLogEventType.REMINDER_TRIGGERED -> R.string.reminder_log_event_triggered
        ReminderLogEventType.MANUAL_CONFIRMED -> R.string.reminder_log_event_confirmed
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTestTags.REMINDER_LOG_ENTRY),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (entry.type) {
                    ReminderLogEventType.REMINDER_TRIGGERED -> MaterialTheme.colorScheme.primaryContainer
                    ReminderLogEventType.MANUAL_CONFIRMED -> MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Text(
                    text = stringResource(eventTitleResId),
                    style = MaterialTheme.typography.labelLarge,
                    color = when (entry.type) {
                        ReminderLogEventType.REMINDER_TRIGGERED -> MaterialTheme.colorScheme.onPrimaryContainer
                        ReminderLogEventType.MANUAL_CONFIRMED -> MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Text(
                text = stringResource(R.string.reminder_log_plan_label, entry.planName),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.reminder_log_recorded_at_label, occurredAtText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.reminder_log_scheduled_time_label, reminderTimeText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderLogScreenPreview() {
    PillRingTheme {
        ReminderLogScreen(
            entries = listOf(
                ReminderLogEntry(
                    id = "1",
                    type = ReminderLogEventType.REMINDER_TRIGGERED,
                    planId = "p1",
                    planName = "Morning pills",
                    reminderHour = 8,
                    reminderMinute = 30,
                    occurredAtEpochMs = System.currentTimeMillis()
                ),
                ReminderLogEntry(
                    id = "2",
                    type = ReminderLogEventType.MANUAL_CONFIRMED,
                    planId = "p1",
                    planName = "Morning pills",
                    reminderHour = 8,
                    reminderMinute = 30,
                    occurredAtEpochMs = System.currentTimeMillis()
                )
            ),
            onBackClick = {}
        )
    }
}

