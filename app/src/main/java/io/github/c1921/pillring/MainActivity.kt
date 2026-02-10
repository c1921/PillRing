package io.github.c1921.pillring

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.c1921.pillring.notification.ReminderNotifier
import io.github.c1921.pillring.notification.ReminderPlan
import io.github.c1921.pillring.notification.ReminderScheduler
import io.github.c1921.pillring.notification.ReminderSessionStore
import io.github.c1921.pillring.notification.ReminderTimeCalculator
import io.github.c1921.pillring.permission.PermissionAction
import io.github.c1921.pillring.permission.PermissionHealthChecker
import io.github.c1921.pillring.permission.PermissionHealthItem
import io.github.c1921.pillring.permission.PermissionSettingsNavigator
import io.github.c1921.pillring.permission.PermissionState
import io.github.c1921.pillring.ui.UiTestTags
import io.github.c1921.pillring.ui.theme.PillRingTheme
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MAX_PLAN_NAME_LENGTH = 30

private enum class AppScreen {
    HOME,
    SETTINGS
}

private data class PlanEditorState(
    val planId: String?,
    val initialName: String,
    val initialHour: Int,
    val initialMinute: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasNotificationPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        rescheduleEnabledPlansSilently()

        enableEdgeToEdge()
        setContent {
            var plans by remember {
                mutableStateOf(loadPlans())
            }
            var permissionItems by remember {
                mutableStateOf(buildPermissionItems())
            }
            var currentScreen by rememberSaveable {
                mutableStateOf(AppScreen.HOME)
            }
            var planEditorState by remember {
                mutableStateOf<PlanEditorState?>(null)
            }
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        plans = loadPlans()
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
                        ReminderHomeScreen(
                            plans = plans,
                            maxPlans = ReminderSessionStore.MAX_PLAN_COUNT,
                            onAddPlanClick = {
                                if (plans.size >= ReminderSessionStore.MAX_PLAN_COUNT) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.msg_plan_limit_reached),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    planEditorState = buildDefaultPlanEditorState(plans.size + 1)
                                }
                            },
                            onEditPlanClick = { plan ->
                                planEditorState = PlanEditorState(
                                    planId = plan.id,
                                    initialName = plan.name,
                                    initialHour = plan.hour,
                                    initialMinute = plan.minute
                                )
                            },
                            onDeletePlanClick = { plan ->
                                deletePlan(plan.id)
                                plans = loadPlans()
                                permissionItems = buildPermissionItems()
                            },
                            onMoveUpClick = { plan ->
                                ReminderSessionStore.movePlanUp(
                                    context = this@MainActivity,
                                    planId = plan.id
                                )
                                plans = loadPlans()
                            },
                            onMoveDownClick = { plan ->
                                ReminderSessionStore.movePlanDown(
                                    context = this@MainActivity,
                                    planId = plan.id
                                )
                                plans = loadPlans()
                            },
                            onPlanEnabledChange = { plan, enabled ->
                                setPlanEnabled(planId = plan.id, enabled = enabled)
                                plans = loadPlans()
                                permissionItems = buildPermissionItems()
                            },
                            onConfirmStopClick = { plan ->
                                confirmStopReminder(plan.id)
                                plans = loadPlans()
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

                planEditorState?.let { editor ->
                    PlanEditorDialog(
                        planId = editor.planId,
                        initialName = editor.initialName,
                        initialHour = editor.initialHour,
                        initialMinute = editor.initialMinute,
                        maxNameLength = MAX_PLAN_NAME_LENGTH,
                        onDismiss = {
                            planEditorState = null
                        },
                        onSave = { name, hour, minute ->
                            val saved = if (editor.planId == null) {
                                addPlan(
                                    name = name,
                                    hour = hour,
                                    minute = minute
                                )
                            } else {
                                editPlan(
                                    planId = editor.planId,
                                    name = name,
                                    hour = hour,
                                    minute = minute
                                )
                            }

                            plans = loadPlans()
                            permissionItems = buildPermissionItems()
                            if (saved) {
                                planEditorState = null
                            }
                        }
                    )
                }
            }
        }
    }

    private fun loadPlans(): List<ReminderPlan> {
        return ReminderSessionStore.getPlans(this)
    }

    private fun buildDefaultPlanEditorState(nextIndex: Int): PlanEditorState {
        val defaultTime = LocalDateTime.now().plusMinutes(1)
        return PlanEditorState(
            planId = null,
            initialName = getString(R.string.default_plan_name, nextIndex),
            initialHour = defaultTime.hour,
            initialMinute = defaultTime.minute
        )
    }

    private fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureReminderSchedulingAllowed(): Boolean {
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

        return true
    }

    private fun addPlan(
        name: String,
        hour: Int,
        minute: Int
    ): Boolean {
        if (name.isBlank()) {
            Toast.makeText(
                this,
                getString(R.string.msg_plan_name_required),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (loadPlans().size >= ReminderSessionStore.MAX_PLAN_COUNT) {
            Toast.makeText(
                this,
                getString(R.string.msg_plan_limit_reached),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (!ensureReminderSchedulingAllowed()) {
            return false
        }

        val plan = try {
            ReminderSessionStore.addPlan(
                context = this,
                name = name.trim(),
                hour = hour,
                minute = minute,
                enabled = true
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                getString(R.string.msg_plan_save_failed),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        val scheduled = scheduleNextDailyReminder(
            plan = plan,
            reason = getString(R.string.reason_plan_enabled)
        )
        if (scheduled) {
            Toast.makeText(
                this,
                getString(R.string.msg_plan_added_enabled, plan.name),
                Toast.LENGTH_SHORT
            ).show()
            return true
        }

        ReminderSessionStore.deletePlan(context = this, planId = plan.id)
        Toast.makeText(
            this,
            getString(R.string.msg_exact_alarm_failed),
            Toast.LENGTH_SHORT
        ).show()
        openPermissionSettings(PermissionAction.OPEN_EXACT_ALARM_SETTINGS)
        return false
    }

    private fun editPlan(
        planId: String,
        name: String,
        hour: Int,
        minute: Int
    ): Boolean {
        val currentPlan = ReminderSessionStore.getPlan(this, planId) ?: return false
        if (name.isBlank()) {
            Toast.makeText(
                this,
                getString(R.string.msg_plan_name_required),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        val updatedPlan = currentPlan.copy(
            name = name.trim(),
            hour = hour,
            minute = minute
        )

        if (currentPlan.enabled) {
            if (!ensureReminderSchedulingAllowed()) {
                return false
            }
            val scheduled = scheduleNextDailyReminder(
                plan = updatedPlan,
                reason = getString(R.string.reason_time_updated)
            )
            if (!scheduled) {
                Toast.makeText(
                    this,
                    getString(R.string.msg_time_updated_reschedule_failed),
                    Toast.LENGTH_SHORT
                ).show()
                openPermissionSettings(PermissionAction.OPEN_EXACT_ALARM_SETTINGS)
                return false
            }
            ReminderSessionStore.updatePlan(this, updatedPlan)
            if (updatedPlan.isReminderActive) {
                ReminderNotifier.showNotification(
                    context = this,
                    plan = updatedPlan,
                    reason = getString(R.string.reason_time_updated)
                )
            }
            Toast.makeText(
                this,
                getString(R.string.msg_time_updated_rescheduled),
                Toast.LENGTH_SHORT
            ).show()
            return true
        }

        ReminderSessionStore.updatePlan(this, updatedPlan)
        Toast.makeText(
            this,
            getString(R.string.msg_plan_updated),
            Toast.LENGTH_SHORT
        ).show()
        return true
    }

    private fun setPlanEnabled(
        planId: String,
        enabled: Boolean
    ): Boolean {
        val plan = ReminderSessionStore.getPlan(this, planId) ?: return false
        if (plan.enabled == enabled) {
            return true
        }

        if (enabled) {
            if (!ensureReminderSchedulingAllowed()) {
                return false
            }
            val enabledPlan = plan.copy(enabled = true)
            ReminderSessionStore.updatePlan(this, enabledPlan)
            val scheduled = scheduleNextDailyReminder(
                plan = enabledPlan,
                reason = getString(R.string.reason_plan_enabled)
            )
            if (!scheduled) {
                ReminderSessionStore.updatePlan(this, plan)
                Toast.makeText(
                    this,
                    getString(R.string.msg_exact_alarm_failed),
                    Toast.LENGTH_SHORT
                ).show()
                openPermissionSettings(PermissionAction.OPEN_EXACT_ALARM_SETTINGS)
                return false
            }
            Toast.makeText(
                this,
                getString(R.string.msg_plan_enabled_name, plan.name),
                Toast.LENGTH_SHORT
            ).show()
            return true
        }

        ReminderSessionStore.updatePlan(this, plan.copy(enabled = false))
        ReminderSessionStore.markReminderConfirmed(this, plan.id)
        ReminderScheduler.cancelPlanReminders(context = this, plan = plan)
        ReminderNotifier.cancelReminderNotification(context = this, plan = plan)
        Toast.makeText(
            this,
            getString(R.string.msg_plan_disabled_name, plan.name),
            Toast.LENGTH_SHORT
        ).show()
        return true
    }

    private fun deletePlan(planId: String): Boolean {
        val plan = ReminderSessionStore.getPlan(this, planId) ?: return false
        ReminderSessionStore.deletePlan(context = this, planId = planId)
        ReminderScheduler.cancelPlanReminders(context = this, plan = plan)
        ReminderNotifier.cancelReminderNotification(context = this, plan = plan)
        Toast.makeText(
            this,
            getString(R.string.msg_plan_deleted, plan.name),
            Toast.LENGTH_SHORT
        ).show()
        return true
    }

    private fun confirmStopReminder(planId: String) {
        val plan = ReminderSessionStore.getPlan(this, planId) ?: return
        ReminderSessionStore.markReminderConfirmed(this, plan.id)
        ReminderScheduler.cancelPlanFallbackReminder(context = this, plan = plan)
        ReminderNotifier.cancelReminderNotification(context = this, plan = plan)
        Toast.makeText(
            this,
            getString(R.string.msg_reminder_stopped_plan, plan.name),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun scheduleNextDailyReminder(
        plan: ReminderPlan,
        reason: String
    ): Boolean {
        val triggerAtMs = ReminderTimeCalculator.computeNextDailyTriggerAtMs(
            nowMs = System.currentTimeMillis(),
            zoneId = ZoneId.systemDefault(),
            hour = plan.hour,
            minute = plan.minute
        )

        return ReminderScheduler.scheduleDailyAt(
            context = this,
            plan = plan,
            triggerAtMs = triggerAtMs,
            reason = reason
        )
    }

    private fun rescheduleEnabledPlansSilently() {
        val plans = loadPlans()
        plans.forEach { plan ->
            if (!plan.enabled) {
                return@forEach
            }
            scheduleNextDailyReminder(
                plan = plan,
                reason = getString(R.string.reason_plan_enabled)
            )
        }
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
@OptIn(ExperimentalMaterial3Api::class)
private fun ReminderHomeScreen(
    plans: List<ReminderPlan>,
    maxPlans: Int,
    onAddPlanClick: () -> Unit,
    onEditPlanClick: (ReminderPlan) -> Unit,
    onDeletePlanClick: (ReminderPlan) -> Unit,
    onMoveUpClick: (ReminderPlan) -> Unit,
    onMoveDownClick: (ReminderPlan) -> Unit,
    onPlanEnabledChange: (ReminderPlan, Boolean) -> Unit,
    onConfirmStopClick: (ReminderPlan) -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    val hasActiveReminder = plans.any { it.isReminderActive }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.test_page_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    TextButton(onClick = onOpenSettingsClick) {
                        Text(text = stringResource(R.string.btn_open_settings_page))
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
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.test_page_description),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.label_plan_count, plans.size, maxPlans),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(
                                if (hasActiveReminder) {
                                    R.string.status_reminder_active_multi
                                } else {
                                    R.string.status_reminder_idle
                                }
                            ),
                            color = if (hasActiveReminder) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onAddPlanClick,
                    enabled = plans.size < maxPlans,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.btn_add_plan))
                }
            }

            if (plans.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.empty_plan_list),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = plans,
                    key = { _, plan -> plan.id }
                ) { index, plan ->
                    PlanCard(
                        plan = plan,
                        index = index,
                        totalCount = plans.size,
                        onEditClick = { onEditPlanClick(plan) },
                        onDeleteClick = { onDeletePlanClick(plan) },
                        onMoveUpClick = { onMoveUpClick(plan) },
                        onMoveDownClick = { onMoveDownClick(plan) },
                        onPlanEnabledChange = { enabled -> onPlanEnabledChange(plan, enabled) },
                        onConfirmStopClick = { onConfirmStopClick(plan) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: ReminderPlan,
    index: Int,
    totalCount: Int,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoveUpClick: () -> Unit,
    onMoveDownClick: () -> Unit,
    onPlanEnabledChange: (Boolean) -> Unit,
    onConfirmStopClick: () -> Unit
) {
    val context = LocalContext.current
    val timeText = remember(plan.hour, plan.minute, context) {
        formatReminderTime(
            context = context,
            hour = plan.hour,
            minute = plan.minute
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.label_selected_time, timeText),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(
                            if (plan.enabled) {
                                R.string.status_plan_enabled
                            } else {
                                R.string.status_plan_disabled
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = plan.enabled,
                    onCheckedChange = onPlanEnabledChange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.btn_edit_plan))
                }
                OutlinedButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.btn_delete_plan))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onMoveUpClick,
                    enabled = index > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.btn_move_up))
                }
                OutlinedButton(
                    onClick = onMoveDownClick,
                    enabled = index < totalCount - 1,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.btn_move_down))
                }
            }

            if (plan.isReminderActive) {
                FilledTonalButton(
                    onClick = onConfirmStopClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.btn_confirm_stop_plan_reminder))
                }
            }
        }
    }
}

@Composable
private fun PlanEditorDialog(
    planId: String?,
    initialName: String,
    initialHour: Int,
    initialMinute: Int,
    maxNameLength: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int) -> Unit
) {
    var name by rememberSaveable(initialName, planId) {
        mutableStateOf(initialName)
    }
    var hour by rememberSaveable(initialHour, planId) {
        mutableStateOf(initialHour)
    }
    var minute by rememberSaveable(initialMinute, planId) {
        mutableStateOf(initialMinute)
    }
    var showTimePicker by rememberSaveable(planId) {
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.label_selected_time, selectedTimeText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag(UiTestTags.PLAN_EDITOR_SELECTED_TIME)
                    )
                }
                FilledTonalButton(
                    onClick = {
                        showTimePicker = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.PLAN_EDITOR_SELECT_TIME_BUTTON)
                ) {
                    Text(text = stringResource(R.string.btn_select_time))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name.trim(), hour, minute)
                },
                enabled = name.trim().isNotEmpty()
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

private fun formatReminderTime(
    context: Context,
    hour: Int,
    minute: Int
): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return LocalTime.of(hour, minute).format(formatter)
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
private fun ReminderHomeScreenPreview() {
    PillRingTheme {
        ReminderHomeScreen(
            plans = listOf(
                ReminderPlan(
                    id = "1",
                    name = "Morning pills",
                    hour = 8,
                    minute = 30,
                    enabled = true,
                    notificationId = 1001,
                    isReminderActive = false,
                    suppressNextDeleteFallback = false
                ),
                ReminderPlan(
                    id = "2",
                    name = "After lunch",
                    hour = 13,
                    minute = 0,
                    enabled = false,
                    notificationId = 1002,
                    isReminderActive = true,
                    suppressNextDeleteFallback = false
                )
            ),
            maxPlans = ReminderSessionStore.MAX_PLAN_COUNT,
            onAddPlanClick = {},
            onEditPlanClick = {},
            onDeletePlanClick = {},
            onMoveUpClick = {},
            onMoveDownClick = {},
            onPlanEnabledChange = { _, _ -> },
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
