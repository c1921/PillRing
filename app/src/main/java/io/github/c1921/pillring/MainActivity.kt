package io.github.c1921.pillring

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.c1921.pillring.notification.ReminderPlan
import io.github.c1921.pillring.notification.PlanMutationFailureReason
import io.github.c1921.pillring.notification.PlanMutationResult
import io.github.c1921.pillring.notification.PlanMutationSuccessType
import io.github.c1921.pillring.notification.ReminderPlanCoordinator
import io.github.c1921.pillring.notification.ReminderRepeatMode
import io.github.c1921.pillring.notification.ReminderContract
import io.github.c1921.pillring.notification.ReminderScheduler
import io.github.c1921.pillring.locale.AppLanguage
import io.github.c1921.pillring.locale.AppLanguageManager
import io.github.c1921.pillring.permission.PermissionAction
import io.github.c1921.pillring.permission.PermissionHealthChecker
import io.github.c1921.pillring.permission.PermissionHealthItem
import io.github.c1921.pillring.permission.PermissionSettingsNavigator
import io.github.c1921.pillring.update.AppUpdateRepository
import io.github.c1921.pillring.update.UpdateStatus
import io.github.c1921.pillring.update.UpdateUiState
import io.github.c1921.pillring.ui.home.ReminderHomeScreen
import io.github.c1921.pillring.ui.plan.PlanEditorDialog
import io.github.c1921.pillring.ui.reminder.ReminderConfirmScreen
import io.github.c1921.pillring.ui.settings.AboutSettingsScreen
import io.github.c1921.pillring.ui.settings.LanguageSettingsScreen
import io.github.c1921.pillring.ui.settings.PermissionSettingsScreen
import io.github.c1921.pillring.ui.settings.SettingsOverviewScreen
import io.github.c1921.pillring.ui.theme.PillRingTheme
import java.time.LocalDateTime
import kotlinx.coroutines.launch

private const val MAX_PLAN_NAME_LENGTH = 30

private enum class AppScreen {
    HOME,
    SETTINGS_OVERVIEW,
    SETTINGS_LANGUAGE,
    SETTINGS_PERMISSION,
    SETTINGS_ABOUT,
    REMINDER_CONFIRM
}

private data class PlanEditorState(
    val planId: String?,
    val initialName: String,
    val initialHour: Int,
    val initialMinute: Int,
    val initialRepeatMode: ReminderRepeatMode,
    val initialIntervalDays: Int,
    val initialStartDateEpochDay: Long?
)

class MainActivity : ComponentActivity() {
    private var pendingReminderConfirmPlanId: String? by mutableStateOf(null)
    private val planCoordinator: ReminderPlanCoordinator by lazy {
        ReminderPlanCoordinator(this)
    }

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
            val updateRepository = remember {
                AppUpdateRepository(this@MainActivity)
            }
            var updateUiState by remember {
                mutableStateOf(updateRepository.getCachedUiState(BuildConfig.VERSION_NAME))
            }
            val mainScope = rememberCoroutineScope()
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

            LaunchedEffect(Unit) {
                if (updateRepository.shouldSkipAutoCheck()) {
                    updateUiState = updateRepository.getCachedUiState(BuildConfig.VERSION_NAME)
                    return@LaunchedEffect
                }

                updateUiState = UpdateUiState.checking(BuildConfig.VERSION_NAME)
                val checkResult = updateRepository.checkForUpdates(
                    currentVersionName = BuildConfig.VERSION_NAME,
                    force = false
                )
                updateUiState = UpdateUiState.fromResult(checkResult)
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

            val onUpdateClick: () -> Unit = {
                when (updateUiState.status) {
                    UpdateStatus.CHECKING -> Unit
                    UpdateStatus.UPDATE_AVAILABLE -> {
                        val releaseUrl = updateUiState.releaseUrl
                            ?: AppUpdateRepository.DEFAULT_RELEASE_PAGE_URL
                        if (!openUpdateReleasePage(releaseUrl)) {
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.msg_open_update_page_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    else -> {
                        mainScope.launch {
                            updateUiState = UpdateUiState.checking(BuildConfig.VERSION_NAME)
                            val checkResult = updateRepository.checkForUpdates(
                                currentVersionName = BuildConfig.VERSION_NAME,
                                force = true
                            )
                            updateUiState = UpdateUiState.fromResult(checkResult)
                            when (checkResult.status) {
                                UpdateStatus.UPDATE_AVAILABLE -> {
                                    val releaseUrl = checkResult.releaseUrl
                                        ?: AppUpdateRepository.DEFAULT_RELEASE_PAGE_URL
                                    if (!openUpdateReleasePage(releaseUrl)) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            getString(R.string.msg_open_update_page_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                UpdateStatus.UP_TO_DATE -> {
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.msg_update_up_to_date),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                UpdateStatus.FAILED -> {
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.msg_update_check_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                UpdateStatus.IDLE,
                                UpdateStatus.CHECKING -> Unit
                            }
                        }
                    }
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
                        AppScreen.SETTINGS_PERMISSION,
                        AppScreen.SETTINGS_ABOUT -> {
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
                            maxPlans = planCoordinator.maxPlanCount,
                            onAddPlanClick = {
                                if (plans.size >= planCoordinator.maxPlanCount) {
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
                                    initialMinute = plan.minute,
                                    initialRepeatMode = plan.repeatMode,
                                    initialIntervalDays = plan.intervalDays,
                                    initialStartDateEpochDay = plan.startDateEpochDay
                                )
                            },
                            onDeletePlanClick = { plan ->
                                deletePlan(plan.id)
                                plans = loadPlans()
                                permissionItems = buildPermissionItems()
                            },
                            onMoveUpClick = { plan ->
                                planCoordinator.movePlanUp(plan.id)
                                plans = loadPlans()
                            },
                            onMoveDownClick = { plan ->
                                planCoordinator.movePlanDown(plan.id)
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
                            },
                            onOpenReminderConfirmFromPlanCard = { plan ->
                                val targetPlan = plans.firstOrNull { it.id == plan.id }
                                if (targetPlan?.isReminderActive == true) {
                                    reminderConfirmPlanId = targetPlan.id
                                    currentScreen = AppScreen.REMINDER_CONFIRM
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.msg_reminder_confirm_entry_invalid),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }

                    AppScreen.SETTINGS_OVERVIEW -> {
                        SettingsOverviewScreen(
                            permissionItems = permissionItems,
                            selectedLanguage = selectedLanguage,
                            effectiveLanguageForSummary = effectiveLanguageForSummary,
                            appVersionName = BuildConfig.VERSION_NAME,
                            onBackClick = { currentScreen = AppScreen.HOME },
                            onLanguageClick = { currentScreen = AppScreen.SETTINGS_LANGUAGE },
                            onPermissionClick = { currentScreen = AppScreen.SETTINGS_PERMISSION },
                            onAboutClick = { currentScreen = AppScreen.SETTINGS_ABOUT }
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

                    AppScreen.SETTINGS_ABOUT -> {
                        AboutSettingsScreen(
                            appVersionName = BuildConfig.VERSION_NAME,
                            updateUiState = updateUiState,
                            onUpdateClick = onUpdateClick,
                            onBackClick = { currentScreen = AppScreen.SETTINGS_OVERVIEW }
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
                                onConfirmCommitted = {
                                    confirmStopReminder(reminderPlan.id)
                                },
                                onAutoExit = {
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
                        initialRepeatMode = editor.initialRepeatMode,
                        initialIntervalDays = editor.initialIntervalDays,
                        initialStartDateEpochDay = editor.initialStartDateEpochDay,
                        maxNameLength = MAX_PLAN_NAME_LENGTH,
                        onDismiss = {
                            planEditorState = null
                        },
                        onSave = {
                                name,
                                hour,
                                minute,
                                repeatMode,
                                intervalDays,
                                startDateEpochDay ->
                            val saved = if (editor.planId == null) {
                                addPlan(
                                    name = name,
                                    hour = hour,
                                    minute = minute,
                                    repeatMode = repeatMode,
                                    intervalDays = intervalDays,
                                    startDateEpochDay = startDateEpochDay
                                )
                            } else {
                                editPlan(
                                    planId = editor.planId,
                                    name = name,
                                    hour = hour,
                                    minute = minute,
                                    repeatMode = repeatMode,
                                    intervalDays = intervalDays,
                                    startDateEpochDay = startDateEpochDay
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
        return planCoordinator.getPlans()
    }

    private fun buildDefaultPlanEditorState(nextIndex: Int): PlanEditorState {
        val defaultTime = LocalDateTime.now().plusMinutes(1)
        return PlanEditorState(
            planId = null,
            initialName = getString(R.string.default_plan_name, nextIndex),
            initialHour = defaultTime.hour,
            initialMinute = defaultTime.minute,
            initialRepeatMode = ReminderRepeatMode.DAILY,
            initialIntervalDays = 1,
            initialStartDateEpochDay = null
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
        minute: Int,
        repeatMode: ReminderRepeatMode,
        intervalDays: Int,
        startDateEpochDay: Long?
    ): Boolean {
        if (name.isBlank()) {
            Toast.makeText(
                this,
                getString(R.string.msg_plan_name_required),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (loadPlans().size >= planCoordinator.maxPlanCount) {
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

        return when (
            val result = planCoordinator.addPlan(
                name = name,
                hour = hour,
                minute = minute,
                repeatMode = repeatMode,
                intervalDays = intervalDays,
                startDateEpochDay = startDateEpochDay,
                scheduleReason = getString(R.string.reason_plan_enabled)
            )
        ) {
            is PlanMutationResult.Success -> {
                val planName = result.planName ?: name.trim()
                Toast.makeText(
                    this,
                    getString(R.string.msg_plan_added_enabled, planName),
                    Toast.LENGTH_SHORT
                ).show()
                true
            }

            is PlanMutationResult.Failure -> {
                when (result.reason) {
                    PlanMutationFailureReason.NAME_REQUIRED -> {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_plan_name_required),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    PlanMutationFailureReason.PLAN_LIMIT_REACHED -> {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_plan_limit_reached),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    PlanMutationFailureReason.SAVE_FAILED -> {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_plan_save_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    PlanMutationFailureReason.SCHEDULE_FAILED -> {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_exact_alarm_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        openPermissionSettings(PermissionAction.OPEN_EXACT_ALARM_SETTINGS)
                    }

                    PlanMutationFailureReason.PLAN_NOT_FOUND -> Unit
                }
                false
            }
        }
    }

    private fun editPlan(
        planId: String,
        name: String,
        hour: Int,
        minute: Int,
        repeatMode: ReminderRepeatMode,
        intervalDays: Int,
        startDateEpochDay: Long?
    ): Boolean {
        val currentPlan = planCoordinator.getPlans().firstOrNull { it.id == planId } ?: return false
        if (name.isBlank()) {
            Toast.makeText(
                this,
                getString(R.string.msg_plan_name_required),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (currentPlan.enabled && !ensureReminderSchedulingAllowed()) {
            return false
        }

        return when (
            val result = planCoordinator.editPlan(
                planId = planId,
                name = name,
                hour = hour,
                minute = minute,
                repeatMode = repeatMode,
                intervalDays = intervalDays,
                startDateEpochDay = startDateEpochDay,
                scheduleReason = getString(R.string.reason_time_updated)
            )
        ) {
            is PlanMutationResult.Success -> {
                val toastMessage = if (currentPlan.enabled) {
                    getString(R.string.msg_time_updated_rescheduled)
                } else {
                    getString(R.string.msg_plan_updated)
                }
                Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
                true
            }

            is PlanMutationResult.Failure -> {
                when (result.reason) {
                    PlanMutationFailureReason.NAME_REQUIRED -> {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_plan_name_required),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    PlanMutationFailureReason.SCHEDULE_FAILED -> {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_time_updated_reschedule_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        openPermissionSettings(PermissionAction.OPEN_EXACT_ALARM_SETTINGS)
                    }

                    PlanMutationFailureReason.SAVE_FAILED -> {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_plan_save_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    PlanMutationFailureReason.PLAN_LIMIT_REACHED,
                    PlanMutationFailureReason.PLAN_NOT_FOUND -> Unit
                }
                false
            }
        }
    }

    private fun setPlanEnabled(
        planId: String,
        enabled: Boolean
    ): Boolean {
        if (enabled && !ensureReminderSchedulingAllowed()) {
            return false
        }

        return when (
            val result = planCoordinator.setPlanEnabled(
                planId = planId,
                enabled = enabled,
                scheduleReason = getString(R.string.reason_plan_enabled)
            )
        ) {
            is PlanMutationResult.Success -> {
                when (result.type) {
                    PlanMutationSuccessType.ENABLED -> {
                        val planName = result.planName ?: return false
                        Toast.makeText(
                            this,
                            getString(R.string.msg_plan_enabled_name, planName),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    PlanMutationSuccessType.DISABLED -> {
                        val planName = result.planName ?: return false
                        Toast.makeText(
                            this,
                            getString(R.string.msg_plan_disabled_name, planName),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    PlanMutationSuccessType.NO_OP -> Unit
                    else -> Unit
                }
                true
            }

            is PlanMutationResult.Failure -> {
                when (result.reason) {
                    PlanMutationFailureReason.SCHEDULE_FAILED -> {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_exact_alarm_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        openPermissionSettings(PermissionAction.OPEN_EXACT_ALARM_SETTINGS)
                    }

                    PlanMutationFailureReason.SAVE_FAILED -> {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_plan_save_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    PlanMutationFailureReason.NAME_REQUIRED,
                    PlanMutationFailureReason.PLAN_LIMIT_REACHED,
                    PlanMutationFailureReason.PLAN_NOT_FOUND -> Unit
                }
                false
            }
        }
    }

    private fun deletePlan(planId: String): Boolean {
        val result = planCoordinator.deletePlan(planId)
        return if (result is PlanMutationResult.Success) {
            val planName = result.planName ?: return false
            Toast.makeText(
                this,
                getString(R.string.msg_plan_deleted, planName),
                Toast.LENGTH_SHORT
            ).show()
            true
        } else {
            false
        }
    }

    private fun confirmStopReminder(planId: String) {
        val result = planCoordinator.confirmStopReminder(planId)
        if (result is PlanMutationResult.Success) {
            val planName = result.planName ?: return
            Toast.makeText(
                this,
                getString(R.string.msg_reminder_stopped_plan, planName),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun rescheduleEnabledPlansSilently() {
        planCoordinator.rescheduleEnabledPlans(
            reason = getString(R.string.reason_plan_enabled)
        )
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

    private fun openUpdateReleasePage(releaseUrl: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, releaseUrl.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            startActivity(intent)
            true
        } catch (_: Exception) {
            false
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
