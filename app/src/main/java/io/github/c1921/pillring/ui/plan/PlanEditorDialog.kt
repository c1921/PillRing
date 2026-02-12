package io.github.c1921.pillring.ui.plan

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.c1921.pillring.R
import io.github.c1921.pillring.notification.ReminderRepeatMode
import io.github.c1921.pillring.ui.UiTestTags
import io.github.c1921.pillring.ui.common.formatReminderDate
import io.github.c1921.pillring.ui.common.formatReminderTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
internal fun PlanEditorDialog(
    planId: String?,
    initialName: String,
    initialHour: Int,
    initialMinute: Int,
    initialRepeatMode: ReminderRepeatMode,
    initialIntervalDays: Int,
    initialStartDateEpochDay: Long?,
    maxNameLength: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, ReminderRepeatMode, Int, Long?) -> Unit
) {
    val todayEpochDay = remember {
        LocalDate.now(ZoneId.systemDefault()).toEpochDay()
    }
    var name by rememberSaveable(initialName, planId) {
        mutableStateOf(initialName)
    }
    var hour by rememberSaveable(initialHour, planId) {
        mutableIntStateOf(initialHour)
    }
    var minute by rememberSaveable(initialMinute, planId) {
        mutableIntStateOf(initialMinute)
    }
    var repeatMode by rememberSaveable(initialRepeatMode, planId) {
        mutableStateOf(initialRepeatMode)
    }
    var intervalDaysInput by rememberSaveable(initialIntervalDays, planId) {
        mutableStateOf(initialIntervalDays.toString())
    }
    var startDateEpochDay by rememberSaveable(initialStartDateEpochDay, planId) {
        mutableStateOf(initialStartDateEpochDay)
    }
    var showTimePicker by rememberSaveable(planId) {
        mutableStateOf(false)
    }
    var showStartDatePicker by rememberSaveable(planId) {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    val selectedTimeText = remember(hour, minute, context) {
        formatReminderTime(
            context = context,
            hour = hour,
            minute = minute
        )
    }
    val intervalDays = intervalDaysInput.toIntOrNull()
    val isIntervalDaysValid = intervalDays in 1..365
    val requiresIntervalConfig = repeatMode == ReminderRepeatMode.INTERVAL_DAYS
    val selectedStartDateText = remember(startDateEpochDay) {
        startDateEpochDay?.let { formatReminderDate(LocalDate.ofEpochDay(it)) }
    }
    val canSave = name.trim().isNotEmpty() && (
        !requiresIntervalConfig || (isIntervalDaysValid && startDateEpochDay != null)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (planId == null) {
                        R.string.dialog_add_plan_title
                    } else {
                        R.string.dialog_edit_plan_title
                    }
                )
            )
        },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { changed ->
                        if (changed.length <= maxNameLength) {
                            name = changed
                        }
                    },
                    label = { Text(stringResource(R.string.label_plan_name)) },
                    supportingText = {
                        Text(
                            text = stringResource(
                                R.string.label_name_length,
                                name.length,
                                maxNameLength
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PickerValueButton(
                    text = stringResource(R.string.label_selected_time, selectedTimeText),
                    onClick = {
                        showTimePicker = true
                    },
                    buttonTestTag = UiTestTags.PLAN_EDITOR_SELECT_TIME_BUTTON,
                    valueTextTestTag = UiTestTags.PLAN_EDITOR_SELECTED_TIME
                )
                Text(
                    text = stringResource(R.string.label_repeat_mode),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = repeatMode == ReminderRepeatMode.DAILY,
                            onClick = {
                                repeatMode = ReminderRepeatMode.DAILY
                            },
                            role = Role.RadioButton
                        )
                        .testTag(UiTestTags.PLAN_EDITOR_REPEAT_MODE_DAILY),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = repeatMode == ReminderRepeatMode.DAILY,
                        onClick = null
                    )
                    Text(
                        text = stringResource(R.string.option_repeat_daily),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = repeatMode == ReminderRepeatMode.INTERVAL_DAYS,
                            onClick = {
                                repeatMode = ReminderRepeatMode.INTERVAL_DAYS
                                if (startDateEpochDay == null) {
                                    startDateEpochDay = todayEpochDay
                                }
                            },
                            role = Role.RadioButton
                        )
                        .testTag(UiTestTags.PLAN_EDITOR_REPEAT_MODE_INTERVAL_DAYS),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = repeatMode == ReminderRepeatMode.INTERVAL_DAYS,
                        onClick = null
                    )
                    Text(
                        text = stringResource(R.string.option_repeat_interval_days),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (repeatMode == ReminderRepeatMode.INTERVAL_DAYS) {
                    OutlinedTextField(
                        value = intervalDaysInput,
                        onValueChange = { changed ->
                            if (changed.length <= 3 && changed.all { it.isDigit() }) {
                                intervalDaysInput = changed
                            }
                        },
                        label = { Text(stringResource(R.string.label_interval_days)) },
                        supportingText = {
                            Text(text = stringResource(R.string.label_interval_days_supporting))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = intervalDaysInput.isNotEmpty() && !isIntervalDaysValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(UiTestTags.PLAN_EDITOR_INTERVAL_DAYS_INPUT)
                    )
                    val startDateLabel = selectedStartDateText
                        ?: stringResource(R.string.label_start_date_not_selected)
                    PickerValueButton(
                        text = stringResource(R.string.label_selected_start_date, startDateLabel),
                        onClick = {
                            showStartDatePicker = true
                        },
                        buttonTestTag = UiTestTags.PLAN_EDITOR_SELECT_START_DATE_BUTTON,
                        valueTextTestTag = UiTestTags.PLAN_EDITOR_SELECTED_START_DATE
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        hour,
                        minute,
                        repeatMode,
                        if (repeatMode == ReminderRepeatMode.INTERVAL_DAYS) {
                            requireNotNull(intervalDays)
                        } else {
                            1
                        },
                        if (repeatMode == ReminderRepeatMode.INTERVAL_DAYS) {
                            startDateEpochDay
                        } else {
                            null
                        }
                    )
                },
                enabled = canSave
            ) {
                Text(text = stringResource(R.string.dialog_btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_btn_cancel))
            }
        }
    )

    if (showTimePicker) {
        PlanTimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = DateFormat.is24HourFormat(context),
            onDismiss = {
                showTimePicker = false
            },
            onConfirm = { selectedHour, selectedMinute ->
                hour = selectedHour
                minute = selectedMinute
                showTimePicker = false
            }
        )
    }

    if (showStartDatePicker) {
        PlanStartDatePickerDialog(
            initialStartDateEpochDay = startDateEpochDay ?: todayEpochDay,
            onDismiss = {
                showStartDatePicker = false
            },
            onConfirm = { selectedEpochDay ->
                startDateEpochDay = selectedEpochDay
                showStartDatePicker = false
            }
        )
    }
}

@Composable
private fun PickerValueButton(
    text: String,
    onClick: () -> Unit,
    buttonTestTag: String,
    valueTextTestTag: String
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(buttonTestTag)
    ) {
        Text(
            text = text,
            modifier = Modifier.testTag(valueTextTestTag)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlanTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.btn_select_time))
        },
        text = {
            TimePicker(
                state = pickerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.PLAN_TIME_PICKER)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(pickerState.hour, pickerState.minute)
                },
                modifier = Modifier.testTag(UiTestTags.PLAN_TIME_PICKER_CONFIRM)
            ) {
                Text(text = stringResource(R.string.dialog_btn_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(UiTestTags.PLAN_TIME_PICKER_CANCEL)
            ) {
                Text(text = stringResource(R.string.dialog_btn_cancel))
            }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlanStartDatePickerDialog(
    initialStartDateEpochDay: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.ofEpochDay(initialStartDateEpochDay)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedDateMillis = pickerState.selectedDateMillis ?: return@TextButton
                    val selectedEpochDay = Instant.ofEpochMilli(selectedDateMillis)
                        .atOffset(ZoneOffset.UTC)
                        .toLocalDate()
                        .toEpochDay()
                    onConfirm(selectedEpochDay)
                },
                modifier = Modifier.testTag(UiTestTags.PLAN_START_DATE_PICKER_CONFIRM)
            ) {
                Text(text = stringResource(R.string.dialog_btn_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(UiTestTags.PLAN_START_DATE_PICKER_CANCEL)
            ) {
                Text(text = stringResource(R.string.dialog_btn_cancel))
            }
        }
    ) {
        DatePicker(
            state = pickerState,
            modifier = Modifier.testTag(UiTestTags.PLAN_START_DATE_PICKER)
        )
    }
}
