package io.github.c1921.pillring.notification

import android.content.Context
import java.time.ZoneId

enum class PlanMutationFailureReason {
    NAME_REQUIRED,
    PLAN_LIMIT_REACHED,
    PLAN_NOT_FOUND,
    SAVE_FAILED,
    SCHEDULE_FAILED
}

enum class PlanMutationSuccessType {
    ADDED,
    UPDATED,
    ENABLED,
    DISABLED,
    DELETED,
    REMINDER_CONFIRMED,
    NO_OP
}

sealed interface PlanMutationResult {
    data class Success(
        val type: PlanMutationSuccessType,
        val planName: String? = null
    ) : PlanMutationResult

    data class Failure(
        val reason: PlanMutationFailureReason
    ) : PlanMutationResult
}

internal interface ReminderPlanStore {
    val maxPlanCount: Int

    fun getPlans(): List<ReminderPlan>

    fun getPlan(planId: String): ReminderPlan?

    fun addPlan(
        name: String,
        hour: Int,
        minute: Int,
        enabled: Boolean,
        repeatMode: ReminderRepeatMode,
        intervalDays: Int,
        startDateEpochDay: Long?
    ): ReminderPlan

    fun updatePlan(plan: ReminderPlan)

    fun deletePlan(planId: String)

    fun movePlanUp(planId: String)

    fun movePlanDown(planId: String)

    fun markReminderConfirmed(planId: String)
}

internal class SharedPrefsReminderPlanStore(
    private val context: Context
) : ReminderPlanStore {
    override val maxPlanCount: Int
        get() = ReminderSessionStore.MAX_PLAN_COUNT

    override fun getPlans(): List<ReminderPlan> {
        return ReminderSessionStore.getPlans(context)
    }

    override fun getPlan(planId: String): ReminderPlan? {
        return ReminderSessionStore.getPlan(context, planId)
    }

    override fun addPlan(
        name: String,
        hour: Int,
        minute: Int,
        enabled: Boolean,
        repeatMode: ReminderRepeatMode,
        intervalDays: Int,
        startDateEpochDay: Long?
    ): ReminderPlan {
        return ReminderSessionStore.addPlan(
            context = context,
            name = name,
            hour = hour,
            minute = minute,
            enabled = enabled,
            repeatMode = repeatMode,
            intervalDays = intervalDays,
            startDateEpochDay = startDateEpochDay
        )
    }

    override fun updatePlan(plan: ReminderPlan) {
        ReminderSessionStore.updatePlan(context, plan)
    }

    override fun deletePlan(planId: String) {
        ReminderSessionStore.deletePlan(context, planId)
    }

    override fun movePlanUp(planId: String) {
        ReminderSessionStore.movePlanUp(context = context, planId = planId)
    }

    override fun movePlanDown(planId: String) {
        ReminderSessionStore.movePlanDown(context = context, planId = planId)
    }

    override fun markReminderConfirmed(planId: String) {
        ReminderSessionStore.markReminderConfirmed(context, planId)
    }
}

internal interface ReminderAlarmService {
    fun scheduleDailyAt(
        plan: ReminderPlan,
        triggerAtMs: Long,
        reason: String
    ): Boolean

    fun cancelPlanReminders(plan: ReminderPlan)

    fun cancelPlanFallbackReminder(plan: ReminderPlan)
}

internal class AndroidReminderAlarmService(
    private val context: Context
) : ReminderAlarmService {
    override fun scheduleDailyAt(
        plan: ReminderPlan,
        triggerAtMs: Long,
        reason: String
    ): Boolean {
        return ReminderScheduler.scheduleDailyAt(
            context = context,
            plan = plan,
            triggerAtMs = triggerAtMs,
            reason = reason
        )
    }

    override fun cancelPlanReminders(plan: ReminderPlan) {
        ReminderScheduler.cancelPlanReminders(context = context, plan = plan)
    }

    override fun cancelPlanFallbackReminder(plan: ReminderPlan) {
        ReminderScheduler.cancelPlanFallbackReminder(context = context, plan = plan)
    }
}

internal interface ReminderNotificationService {
    fun showNotification(
        plan: ReminderPlan,
        reason: String
    )

    fun cancelReminderNotification(plan: ReminderPlan)
}

internal class AndroidReminderNotificationService(
    private val context: Context
) : ReminderNotificationService {
    override fun showNotification(
        plan: ReminderPlan,
        reason: String
    ) {
        ReminderNotifier.showNotification(
            context = context,
            plan = plan,
            reason = reason
        )
    }

    override fun cancelReminderNotification(plan: ReminderPlan) {
        ReminderNotifier.cancelReminderNotification(
            context = context,
            plan = plan
        )
    }
}

internal class ReminderPlanCoordinator(
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() },
    private val store: ReminderPlanStore,
    private val alarmService: ReminderAlarmService,
    private val notificationService: ReminderNotificationService
) {
    constructor(
        context: Context,
        nowProvider: () -> Long = { System.currentTimeMillis() },
        zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() }
    ) : this(
        nowProvider = nowProvider,
        zoneIdProvider = zoneIdProvider,
        store = SharedPrefsReminderPlanStore(context),
        alarmService = AndroidReminderAlarmService(context),
        notificationService = AndroidReminderNotificationService(context)
    )

    val maxPlanCount: Int
        get() = store.maxPlanCount

    fun getPlans(): List<ReminderPlan> {
        return store.getPlans()
    }

    fun addPlan(
        name: String,
        hour: Int,
        minute: Int,
        repeatMode: ReminderRepeatMode,
        intervalDays: Int,
        startDateEpochDay: Long?,
        scheduleReason: String
    ): PlanMutationResult {
        if (name.isBlank()) {
            return PlanMutationResult.Failure(PlanMutationFailureReason.NAME_REQUIRED)
        }
        if (store.getPlans().size >= store.maxPlanCount) {
            return PlanMutationResult.Failure(PlanMutationFailureReason.PLAN_LIMIT_REACHED)
        }

        val plan = try {
            store.addPlan(
                name = name.trim(),
                hour = hour,
                minute = minute,
                enabled = true,
                repeatMode = repeatMode,
                intervalDays = intervalDays,
                startDateEpochDay = startDateEpochDay
            )
        } catch (_: Exception) {
            return PlanMutationResult.Failure(PlanMutationFailureReason.SAVE_FAILED)
        }

        val scheduled = scheduleNextPlanReminder(
            plan = plan,
            reason = scheduleReason
        )
        if (!scheduled) {
            store.deletePlan(plan.id)
            return PlanMutationResult.Failure(PlanMutationFailureReason.SCHEDULE_FAILED)
        }

        return PlanMutationResult.Success(
            type = PlanMutationSuccessType.ADDED,
            planName = plan.name
        )
    }

    fun editPlan(
        planId: String,
        name: String,
        hour: Int,
        minute: Int,
        repeatMode: ReminderRepeatMode,
        intervalDays: Int,
        startDateEpochDay: Long?,
        scheduleReason: String
    ): PlanMutationResult {
        val currentPlan = store.getPlan(planId)
            ?: return PlanMutationResult.Failure(PlanMutationFailureReason.PLAN_NOT_FOUND)
        if (name.isBlank()) {
            return PlanMutationResult.Failure(PlanMutationFailureReason.NAME_REQUIRED)
        }

        val updatedPlan = currentPlan.copy(
            name = name.trim(),
            hour = hour,
            minute = minute,
            repeatMode = repeatMode,
            intervalDays = intervalDays,
            startDateEpochDay = startDateEpochDay
        )

        if (currentPlan.enabled) {
            val scheduled = scheduleNextPlanReminder(
                plan = updatedPlan,
                reason = scheduleReason
            )
            if (!scheduled) {
                return PlanMutationResult.Failure(PlanMutationFailureReason.SCHEDULE_FAILED)
            }
        }

        try {
            store.updatePlan(updatedPlan)
        } catch (_: Exception) {
            return PlanMutationResult.Failure(PlanMutationFailureReason.SAVE_FAILED)
        }

        if (updatedPlan.enabled && updatedPlan.isReminderActive) {
            notificationService.showNotification(
                plan = updatedPlan,
                reason = scheduleReason
            )
        }

        return PlanMutationResult.Success(
            type = PlanMutationSuccessType.UPDATED,
            planName = updatedPlan.name
        )
    }

    fun setPlanEnabled(
        planId: String,
        enabled: Boolean,
        scheduleReason: String
    ): PlanMutationResult {
        val plan = store.getPlan(planId)
            ?: return PlanMutationResult.Failure(PlanMutationFailureReason.PLAN_NOT_FOUND)

        if (plan.enabled == enabled) {
            return PlanMutationResult.Success(
                type = PlanMutationSuccessType.NO_OP,
                planName = plan.name
            )
        }

        if (enabled) {
            val enabledPlan = plan.copy(enabled = true)
            try {
                store.updatePlan(enabledPlan)
            } catch (_: Exception) {
                return PlanMutationResult.Failure(PlanMutationFailureReason.SAVE_FAILED)
            }
            val scheduled = scheduleNextPlanReminder(
                plan = enabledPlan,
                reason = scheduleReason
            )
            if (!scheduled) {
                store.updatePlan(plan)
                return PlanMutationResult.Failure(PlanMutationFailureReason.SCHEDULE_FAILED)
            }
            return PlanMutationResult.Success(
                type = PlanMutationSuccessType.ENABLED,
                planName = plan.name
            )
        }

        try {
            store.updatePlan(plan.copy(enabled = false))
            store.markReminderConfirmed(plan.id)
        } catch (_: Exception) {
            return PlanMutationResult.Failure(PlanMutationFailureReason.SAVE_FAILED)
        }

        alarmService.cancelPlanReminders(plan)
        notificationService.cancelReminderNotification(plan)
        return PlanMutationResult.Success(
            type = PlanMutationSuccessType.DISABLED,
            planName = plan.name
        )
    }

    fun deletePlan(planId: String): PlanMutationResult {
        val plan = store.getPlan(planId)
            ?: return PlanMutationResult.Failure(PlanMutationFailureReason.PLAN_NOT_FOUND)
        store.deletePlan(planId)
        alarmService.cancelPlanReminders(plan)
        notificationService.cancelReminderNotification(plan)
        return PlanMutationResult.Success(
            type = PlanMutationSuccessType.DELETED,
            planName = plan.name
        )
    }

    fun confirmStopReminder(planId: String): PlanMutationResult {
        val plan = store.getPlan(planId)
            ?: return PlanMutationResult.Failure(PlanMutationFailureReason.PLAN_NOT_FOUND)
        store.markReminderConfirmed(plan.id)
        alarmService.cancelPlanFallbackReminder(plan)
        notificationService.cancelReminderNotification(plan)
        return PlanMutationResult.Success(
            type = PlanMutationSuccessType.REMINDER_CONFIRMED,
            planName = plan.name
        )
    }

    fun movePlanUp(planId: String) {
        store.movePlanUp(planId)
    }

    fun movePlanDown(planId: String) {
        store.movePlanDown(planId)
    }

    fun rescheduleEnabledPlans(reason: String) {
        store.getPlans().forEach { plan ->
            if (!plan.enabled) {
                return@forEach
            }
            scheduleNextPlanReminder(
                plan = plan,
                reason = reason
            )
        }
    }

    private fun scheduleNextPlanReminder(
        plan: ReminderPlan,
        reason: String
    ): Boolean {
        val triggerAtMs = ReminderTimeCalculator.computeNextTriggerAtMs(
            nowMs = nowProvider(),
            zoneId = zoneIdProvider(),
            plan = plan
        )

        return alarmService.scheduleDailyAt(
            plan = plan,
            triggerAtMs = triggerAtMs,
            reason = reason
        )
    }
}
