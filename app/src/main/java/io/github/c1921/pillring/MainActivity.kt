package io.github.c1921.pillring

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.c1921.pillring.notification.ReminderNotifier
import io.github.c1921.pillring.notification.ReminderPlan
import io.github.c1921.pillring.notification.ReminderScheduler
import io.github.c1921.pillring.notification.ReminderContract
import io.github.c1921.pillring.notification.ReminderSessionStore
import io.github.c1921.pillring.notification.ReminderTimeCalculator
import io.github.c1921.pillring.locale.AppLanguage
import io.github.c1921.pillring.locale.AppLanguageManager
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val MAX_PLAN_NAME_LENGTH = 30
private const val REMINDER_CONFIRM_HOLD_DURATION_MS = 1200
private const val REMINDER_CONFIRM_HAPTIC_PULSE_INTERVAL_MS = 250L
private const val REMINDER_CONFIRM_RELEASE_ANIMATION_MS = 180

private enum class AppScreen {
    HOME,
    SETTINGS_OVERVIEW,
    SETTINGS_LANGUAGE,
    SETTINGS_PERMISSION,
    REMINDER_CONFIRM
}

private enum class HoldToConfirmState {
    IDLE,
    HOLDING,
    COMPLETED
}

private data class PlanEditorState(
    val planId: String?,
    val initialName: String,
    val initialHour: Int,
    val initialMinute: Int
)

class MainActivity : ComponentActivity() {
    private var pendingReminderConfirmPlanId: String? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingReminderConfirmPlanId = extractReminderConfirmPlanId(intent)
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
            var selectedLanguage by remember {
                mutableStateOf(AppLanguageManager.getSelectedLanguage(this@MainActivity))
            }
            var effectiveLanguageForSummary by remember {
                mutableStateOf(
                    AppLanguageManager.getEffectiveLanguageForSummary(this@MainActivity)
                )
            }
            var currentScreen by rememberSaveable {
                mutableStateOf(AppScreen.HOME)
            }
            var reminderConfirmPlanId by rememberSaveable {
                mutableStateOf<String?>(null)
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
                        selectedLanguage =
                            AppLanguageManager.getSelectedLanguage(this@MainActivity)
                        effectiveLanguageForSummary =
                            AppLanguageManager.getEffectiveLanguageForSummary(this@MainActivity)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(pendingReminderConfirmPlanId, plans) {
                val requestedPlanId = pendingReminderConfirmPlanId ?: return@LaunchedEffect
                pendingReminderConfirmPlanId = null

                val targetPlan = plans.firstOrNull { it.id == requestedPlanId }
                if (targetPlan?.isReminderActive == true) {
                    reminderConfirmPlanId = targetPlan.id
                    currentScreen = AppScreen.REMINDER_CONFIRM
                } else {
                    reminderConfirmPlanId = null
                    currentScreen = AppScreen.HOME
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.msg_reminder_confirm_entry_invalid),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            PillRingTheme {
                BackHandler(enabled = currentScreen != AppScreen.HOME) {
                    when (currentScreen) {
                        AppScreen.HOME -> Unit
                        AppScreen.SETTINGS_OVERVIEW -> {
                            reminderConfirmPlanId = null
                            currentScreen = AppScreen.HOME
                        }

                        AppScreen.SETTINGS_LANGUAGE,
                        AppScreen.SETTINGS_PERMISSION -> {
                            currentScreen = AppScreen.SETTINGS_OVERVIEW
                        }

                        AppScreen.REMINDER_CONFIRM -> {
                            reminderConfirmPlanId = null
                            currentScreen = AppScreen.HOME
                        }
                    }
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
                            onOpenSettingsClick = {
                                permissionItems = buildPermissionItems()
                                currentScreen = AppScreen.SETTINGS_OVERVIEW
                            }
                        )
                    }

                    AppScreen.SETTINGS_OVERVIEW -> {
                        SettingsOverviewScreen(
                            permissionItems = permissionItems,
                            selectedLanguage = selectedLanguage,
                            effectiveLanguageForSummary = effectiveLanguageForSummary,
                            onBackClick = { currentScreen = AppScreen.HOME },
                            onLanguageClick = { currentScreen = AppScreen.SETTINGS_LANGUAGE },
                            onPermissionClick = { currentScreen = AppScreen.SETTINGS_PERMISSION }
                        )
                    }

                    AppScreen.SETTINGS_LANGUAGE -> {
                        LanguageSettingsScreen(
                            selectedLanguage = selectedLanguage,
                            effectiveLanguageForSummary = effectiveLanguageForSummary,
                            onBackClick = { currentScreen = AppScreen.SETTINGS_OVERVIEW },
                            onLanguageSelected = { language ->
                                val changed = AppLanguageManager.applyLanguage(
                                    context = this@MainActivity,
                                    language = language
                                )
                                selectedLanguage =
                                    AppLanguageManager.getSelectedLanguage(this@MainActivity)
                                effectiveLanguageForSummary =
                                    AppLanguageManager.getEffectiveLanguageForSummary(
                                        this@MainActivity
                                    )
                                if (changed) {
                                    recreate()
                                }
                            }
                        )
                    }

                    AppScreen.SETTINGS_PERMISSION -> {
                        PermissionSettingsScreen(
                            permissionItems = permissionItems,
                            onBackClick = { currentScreen = AppScreen.SETTINGS_OVERVIEW },
                            onOpenPermissionSettings = ::openPermissionSettings,
                        )
                    }

                    AppScreen.REMINDER_CONFIRM -> {
                        val reminderPlan = plans.firstOrNull { it.id == reminderConfirmPlanId }
                        if (reminderPlan?.isReminderActive == true) {
                            ReminderConfirmScreen(
                                plan = reminderPlan,
                                onBackClick = {
                                    reminderConfirmPlanId = null
                                    currentScreen = AppScreen.HOME
                                },
                                onConfirmClick = {
                                    confirmStopReminder(reminderPlan.id)
                                    plans = loadPlans()
                                    permissionItems = buildPermissionItems()
                                    reminderConfirmPlanId = null
                                    currentScreen = AppScreen.HOME
                                }
                            )
                        } else {
                            LaunchedEffect(reminderConfirmPlanId, plans) {
                                if (reminderConfirmPlanId != null) {
                                    reminderConfirmPlanId = null
                                    currentScreen = AppScreen.HOME
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.msg_reminder_confirm_entry_invalid),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Keep processing the fresh intent without replacing ActivityScenario's launch intent.
        pendingReminderConfirmPlanId = extractReminderConfirmPlanId(intent)
    }

    private fun extractReminderConfirmPlanId(intent: Intent?): String? {
        if (intent?.action != ReminderContract.ACTION_OPEN_REMINDER_CONFIRM) {
            return null
        }
        return intent.getStringExtra(ReminderContract.EXTRA_PLAN_ID)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
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
    onOpenSettingsClick: () -> Unit
) {
    val hasActiveReminder = plans.any { it.isReminderActive }
    val canAddPlan = plans.size < maxPlans
    val reminderStatusText = stringResource(
        if (hasActiveReminder) {
            R.string.status_reminder_active_multi
        } else {
            R.string.status_reminder_idle
        }
    )
    val reminderStatusContainerColor = if (hasActiveReminder) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val reminderStatusContentColor = if (hasActiveReminder) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.test_page_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettingsClick,
                        modifier = Modifier.testTag(UiTestTags.HOME_SETTINGS_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_open_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (canAddPlan) {
                        onAddPlanClick()
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text(text = stringResource(R.string.btn_add_plan))
                },
                containerColor = if (canAddPlan) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (canAddPlan) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.testTag(UiTestTags.HOME_ADD_PLAN_FAB)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_overview_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.test_page_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.label_plan_count, plans.size, maxPlans),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = reminderStatusContainerColor
                        ) {
                            Text(
                                text = reminderStatusText,
                                color = reminderStatusContentColor,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            if (plans.isEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.empty_plan_list),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(R.string.btn_add_plan),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                        onPlanEnabledChange = { enabled -> onPlanEnabledChange(plan, enabled) }
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
    onPlanEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val timeText = remember(plan.hour, plan.minute, context) {
        formatReminderTime(
            context = context,
            hour = plan.hour,
            minute = plan.minute
        )
    }
    var showMenu by rememberSaveable(plan.id) {
        mutableStateOf(false)
    }
    val planStatusContainerColor = if (plan.enabled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val planStatusContentColor = if (plan.enabled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.label_selected_time, timeText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = planStatusContainerColor
                    ) {
                        Text(
                            text = stringResource(
                                if (plan.enabled) {
                                    R.string.status_plan_enabled
                                } else {
                                    R.string.status_plan_disabled
                                }
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = planStatusContentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Switch(
                        checked = plan.enabled,
                        onCheckedChange = onPlanEnabledChange
                    )
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag(UiTestTags.PLAN_CARD_OVERFLOW_BUTTON)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.cd_more_plan_actions)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(R.string.btn_move_up))
                                },
                                enabled = index > 0,
                                onClick = {
                                    showMenu = false
                                    onMoveUpClick()
                                },
                                modifier = Modifier.testTag(UiTestTags.PLAN_CARD_ACTION_MOVE_UP)
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(R.string.btn_move_down))
                                },
                                enabled = index < totalCount - 1,
                                onClick = {
                                    showMenu = false
                                    onMoveDownClick()
                                },
                                modifier = Modifier.testTag(UiTestTags.PLAN_CARD_ACTION_MOVE_DOWN)
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.btn_delete_plan),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                },
                                modifier = Modifier.testTag(UiTestTags.PLAN_CARD_ACTION_DELETE)
                            )
                        }
                    }
                }
            }

            FilledTonalButton(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.btn_edit_plan),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ReminderConfirmScreen(
    plan: ReminderPlan,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit
) {
    val context = LocalContext.current
    val reminderTimeText = remember(plan.hour, plan.minute, context) {
        formatReminderTime(
            context = context,
            hour = plan.hour,
            minute = plan.minute
        )
    }
    var holdState by remember {
        mutableStateOf(HoldToConfirmState.IDLE)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.REMINDER_CONFIRM_SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.reminder_confirm_page_title),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.REMINDER_CONFIRM_SECONDARY_CONTENT),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.reminder_confirm_secondary_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.reminder_confirm_plan_label, plan.name),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.reminder_confirm_time_label, reminderTimeText),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.reminder_confirm_long_press_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                HoldToConfirmButton(
                    text = stringResource(R.string.btn_reminder_hold_to_confirm),
                    onConfirm = onConfirmClick,
                    onStateChanged = { state ->
                        holdState = state
                    }
                )
                Spacer(modifier = Modifier.size(16.dp))
                AnimatedContent(
                    targetState = holdState,
                    label = "reminder_confirm_hold_hint"
                ) { state ->
                    val textResId = when (state) {
                        HoldToConfirmState.IDLE -> R.string.reminder_confirm_hold_hint_idle
                        HoldToConfirmState.HOLDING -> R.string.reminder_confirm_hold_hint_holding
                        HoldToConfirmState.COMPLETED -> R.string.reminder_confirm_hold_hint_completed
                    }
                    Text(
                        text = stringResource(textResId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(UiTestTags.REMINDER_CONFIRM_HOLD_HINT)
                    )
                }
            }
        }
    }
}

@Composable
private fun HoldToConfirmButton(
    text: String,
    onConfirm: () -> Unit,
    onStateChanged: (HoldToConfirmState) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    val progress = remember {
        Animatable(0f)
    }
    var holdState by remember {
        mutableStateOf(HoldToConfirmState.IDLE)
    }
    var isHolding by remember {
        mutableStateOf(false)
    }
    var hasConfirmed by remember {
        mutableStateOf(false)
    }
    var progressJob by remember {
        mutableStateOf<Job?>(null)
    }
    var hapticJob by remember {
        mutableStateOf<Job?>(null)
    }

    LaunchedEffect(holdState) {
        onStateChanged(holdState)
    }

    DisposableEffect(Unit) {
        onDispose {
            progressJob?.cancel()
            hapticJob?.cancel()
        }
    }

    fun startHold() {
        progressJob?.cancel()
        hapticJob?.cancel()
        isHolding = true
        hasConfirmed = false
        holdState = HoldToConfirmState.HOLDING

        progressJob = coroutineScope.launch {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = REMINDER_CONFIRM_HOLD_DURATION_MS,
                    easing = LinearEasing
                )
            )
            if (isHolding && !hasConfirmed) {
                hasConfirmed = true
                holdState = HoldToConfirmState.COMPLETED
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnConfirm()
            }
        }
        hapticJob = coroutineScope.launch {
            while (isActive && isHolding && !hasConfirmed) {
                delay(REMINDER_CONFIRM_HAPTIC_PULSE_INTERVAL_MS)
                if (isHolding && !hasConfirmed) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }
    }

    fun stopHold() {
        isHolding = false
        hapticJob?.cancel()
        hapticJob = null
        if (!hasConfirmed) {
            progressJob?.cancel()
            progressJob = null
            coroutineScope.launch {
                progress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = REMINDER_CONFIRM_RELEASE_ANIMATION_MS,
                        easing = FastOutSlowInEasing
                    )
                )
                holdState = HoldToConfirmState.IDLE
            }
        }
    }

    val buttonScale by animateFloatAsState(
        targetValue = if (isHolding && !hasConfirmed) 0.94f else 1f,
        animationSpec = spring(),
        label = "reminder_confirm_button_scale"
    )
    val buttonContainerColor by animateColorAsState(
        targetValue = when {
            hasConfirmed -> MaterialTheme.colorScheme.secondaryContainer
            isHolding -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 180),
        label = "reminder_confirm_button_container_color"
    )
    val buttonContentColor by animateColorAsState(
        targetValue = when {
            hasConfirmed -> MaterialTheme.colorScheme.onSecondaryContainer
            isHolding -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(durationMillis = 180),
        label = "reminder_confirm_button_content_color"
    )

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = {
                progress.value
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag(UiTestTags.REMINDER_CONFIRM_HOLD_PROGRESS),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeWidth = 8.dp,
            strokeCap = StrokeCap.Round
        )
        Surface(
            modifier = Modifier
                .size(112.dp)
                .scale(buttonScale)
                .testTag(UiTestTags.REMINDER_CONFIRM_PRIMARY_BUTTON)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            startHold()
                            tryAwaitRelease()
                            stopHold()
                        }
                    )
                },
            shape = CircleShape,
            color = buttonContainerColor,
            shadowElevation = if (isHolding && !hasConfirmed) 2.dp else 8.dp,
            tonalElevation = if (isHolding && !hasConfirmed) 2.dp else 8.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = buttonContentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
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
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsOverviewScreen(
    permissionItems: List<PermissionHealthItem>,
    selectedLanguage: AppLanguage,
    effectiveLanguageForSummary: AppLanguage,
    onBackClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onPermissionClick: () -> Unit
) {
    val languageSummary = languageSummaryText(
        selectedLanguage = selectedLanguage,
        effectiveLanguageForSummary = effectiveLanguageForSummary
    )
    val permissionSummary = permissionOverviewSummary(permissionItems)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_page_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(UiTestTags.SETTINGS_BACK_BUTTON)
                    ) {
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
            item {
                SettingsOverviewItem(
                    title = stringResource(R.string.settings_language_title),
                    summary = languageSummary,
                    icon = Icons.Outlined.Language,
                    testTag = UiTestTags.SETTINGS_LANGUAGE_ITEM,
                    iconTestTag = UiTestTags.SETTINGS_LANGUAGE_ICON,
                    onClick = onLanguageClick
                )
            }
            item {
                SettingsOverviewItem(
                    title = stringResource(R.string.permission_health_title),
                    summary = permissionSummary,
                    icon = Icons.Outlined.VerifiedUser,
                    testTag = UiTestTags.SETTINGS_PERMISSION_ITEM,
                    iconTestTag = UiTestTags.SETTINGS_PERMISSION_ICON,
                    onClick = onPermissionClick
                )
            }
        }
    }
}

@Composable
private fun SettingsOverviewItem(
    title: String,
    summary: String,
    icon: ImageVector,
    testTag: String,
    iconTestTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(iconTestTag)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LanguageSettingsScreen(
    selectedLanguage: AppLanguage,
    effectiveLanguageForSummary: AppLanguage,
    onBackClick: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    val summary = languageSummaryText(
        selectedLanguage = selectedLanguage,
        effectiveLanguageForSummary = effectiveLanguageForSummary
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_language_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(UiTestTags.SETTINGS_BACK_BUTTON)
                    ) {
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_language_dialog_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LanguageOptionRow(
                            text = stringResource(R.string.settings_language_option_system),
                            selected = selectedLanguage == AppLanguage.SYSTEM,
                            testTag = UiTestTags.SETTINGS_LANGUAGE_OPTION_SYSTEM,
                            onClick = { onLanguageSelected(AppLanguage.SYSTEM) }
                        )
                        LanguageOptionRow(
                            text = stringResource(R.string.settings_language_option_english),
                            selected = selectedLanguage == AppLanguage.ENGLISH,
                            testTag = UiTestTags.SETTINGS_LANGUAGE_OPTION_ENGLISH,
                            onClick = { onLanguageSelected(AppLanguage.ENGLISH) }
                        )
                        LanguageOptionRow(
                            text = stringResource(R.string.settings_language_option_chinese),
                            selected = selectedLanguage == AppLanguage.CHINESE_SIMPLIFIED,
                            testTag = UiTestTags.SETTINGS_LANGUAGE_OPTION_CHINESE,
                            onClick = { onLanguageSelected(AppLanguage.CHINESE_SIMPLIFIED) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PermissionSettingsScreen(
    permissionItems: List<PermissionHealthItem>,
    onBackClick: () -> Unit,
    onOpenPermissionSettings: (PermissionAction) -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.SETTINGS_PERMISSION_PAGE),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.permission_health_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(UiTestTags.SETTINGS_BACK_BUTTON)
                    ) {
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                PermissionHealthPanel(
                    items = permissionItems,
                    onOpenPermissionSettings = onOpenPermissionSettings
                )
            }
        }
    }
}

@Composable
private fun permissionOverviewSummary(items: List<PermissionHealthItem>): String {
    val needsActionCount = items.count { it.state == PermissionState.NEEDS_ACTION }
    if (needsActionCount > 0) {
        return stringResource(
            R.string.settings_permission_summary_needs_action,
            needsActionCount
        )
    }

    val manualCheckCount = items.count { it.state == PermissionState.MANUAL_CHECK }
    if (manualCheckCount > 0) {
        return stringResource(
            R.string.settings_permission_summary_manual_check,
            manualCheckCount
        )
    }

    return stringResource(R.string.settings_permission_summary_all_ok)
}

@Composable
private fun LanguageOptionRow(
    text: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .testTag(testTag)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun languageSummaryText(
    selectedLanguage: AppLanguage,
    effectiveLanguageForSummary: AppLanguage
): String {
    return when (selectedLanguage) {
        AppLanguage.SYSTEM -> {
            val effectiveLanguageLabel = stringResource(
                if (effectiveLanguageForSummary == AppLanguage.CHINESE_SIMPLIFIED) {
                    R.string.settings_language_effective_chinese
                } else {
                    R.string.settings_language_effective_english
                }
            )
            stringResource(
                R.string.settings_language_summary_follow_system,
                effectiveLanguageLabel
            )
        }

        AppLanguage.ENGLISH -> stringResource(R.string.settings_language_summary_english)
        AppLanguage.CHINESE_SIMPLIFIED -> stringResource(R.string.settings_language_summary_chinese)
    }
}

@Composable
private fun PermissionHealthPanel(
    items: List<PermissionHealthItem>,
    onOpenPermissionSettings: (PermissionAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.permission_health_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.permission_health_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = permissionStateContainerColor(item.state)
            ) {
                Text(
                    text = item.statusText,
                    color = permissionStateColor(item.state),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Text(
                text = item.detailText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = { onOpenPermissionSettings(item.action) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = item.actionLabel)
            }
        }
    }
}

@Composable
private fun permissionStateContainerColor(state: PermissionState): Color {
    return when (state) {
        PermissionState.OK -> MaterialTheme.colorScheme.secondaryContainer
        PermissionState.NEEDS_ACTION -> MaterialTheme.colorScheme.errorContainer
        PermissionState.MANUAL_CHECK -> MaterialTheme.colorScheme.tertiaryContainer
        PermissionState.UNAVAILABLE -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
}

@Composable
private fun permissionStateColor(state: PermissionState): Color {
    return when (state) {
        PermissionState.OK -> MaterialTheme.colorScheme.onSecondaryContainer
        PermissionState.NEEDS_ACTION -> MaterialTheme.colorScheme.onErrorContainer
        PermissionState.MANUAL_CHECK -> MaterialTheme.colorScheme.onTertiaryContainer
        PermissionState.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
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
            onOpenSettingsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsOverviewScreenPreview() {
    PillRingTheme {
        SettingsOverviewScreen(
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
            selectedLanguage = AppLanguage.SYSTEM,
            effectiveLanguageForSummary = AppLanguage.ENGLISH,
            onBackClick = {},
            onLanguageClick = {},
            onPermissionClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LanguageSettingsScreenPreview() {
    PillRingTheme {
        LanguageSettingsScreen(
            selectedLanguage = AppLanguage.SYSTEM,
            effectiveLanguageForSummary = AppLanguage.ENGLISH,
            onBackClick = {},
            onLanguageSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionSettingsScreenPreview() {
    PillRingTheme {
        PermissionSettingsScreen(
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
