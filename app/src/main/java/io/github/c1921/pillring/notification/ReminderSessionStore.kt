package io.github.c1921.pillring.notification

import android.content.Context
import java.util.UUID

object ReminderSessionStore {
    const val MAX_PLAN_COUNT = 10

    private const val PREFS_NAME = "reminder_session_store"

    private const val KEY_PLANS_JSON = "plans_json"
    private const val KEY_NEXT_NOTIFICATION_ID = "next_notification_id"

    private const val DEFAULT_NOTIFICATION_ID_START = 1001

    fun getPlans(context: Context): List<ReminderPlan> {
        ensureInitialized(context)
        return readPlans(dataSource(context))
    }

    fun getPlan(
        context: Context,
        planId: String
    ): ReminderPlan? {
        return getPlans(context).firstOrNull { it.id == planId }
    }

    fun addPlan(
        context: Context,
        name: String,
        hour: Int,
        minute: Int,
        enabled: Boolean = true,
        repeatMode: ReminderRepeatMode = ReminderRepeatMode.DAILY,
        intervalDays: Int = 1,
        startDateEpochDay: Long? = null
    ): ReminderPlan {
        validateTime(hour = hour, minute = minute)
        val normalizedRepeatConfig = ReminderPlanJsonCodec.normalizeRepeatConfig(
            repeatMode = repeatMode,
            intervalDays = intervalDays,
            startDateEpochDay = startDateEpochDay,
            strict = true
        )
        val normalizedName = name.trim().takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("plan name must not be blank")

        ensureInitialized(context)
        synchronized(this) {
            val dataSource = dataSource(context)
            val plans = readPlans(dataSource).toMutableList()
            if (plans.size >= MAX_PLAN_COUNT) {
                throw IllegalStateException("maximum plan count reached")
            }

            val plan = ReminderPlan(
                id = UUID.randomUUID().toString(),
                name = normalizedName,
                hour = hour,
                minute = minute,
                repeatMode = normalizedRepeatConfig.repeatMode,
                intervalDays = normalizedRepeatConfig.intervalDays,
                startDateEpochDay = normalizedRepeatConfig.startDateEpochDay,
                enabled = enabled,
                notificationId = allocateNotificationId(dataSource),
                isReminderActive = false,
                suppressNextDeleteFallback = false
            )
            plans += plan
            savePlans(dataSource = dataSource, plans = plans)
            return plan
        }
    }

    fun updatePlan(
        context: Context,
        plan: ReminderPlan
    ) {
        validateTime(hour = plan.hour, minute = plan.minute)
        val normalizedRepeatConfig = ReminderPlanJsonCodec.normalizeRepeatConfig(
            repeatMode = plan.repeatMode,
            intervalDays = plan.intervalDays,
            startDateEpochDay = plan.startDateEpochDay,
            strict = true
        )
        val normalizedName = plan.name.trim().takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("plan name must not be blank")

        ensureInitialized(context)
        synchronized(this) {
            val dataSource = dataSource(context)
            val plans = readPlans(dataSource).toMutableList()
            val targetIndex = plans.indexOfFirst { it.id == plan.id }
            if (targetIndex < 0) {
                return
            }
            plans[targetIndex] = plan.copy(
                name = normalizedName,
                repeatMode = normalizedRepeatConfig.repeatMode,
                intervalDays = normalizedRepeatConfig.intervalDays,
                startDateEpochDay = normalizedRepeatConfig.startDateEpochDay
            )
            savePlans(dataSource = dataSource, plans = plans)
        }
    }

    fun deletePlan(
        context: Context,
        planId: String
    ) {
        ensureInitialized(context)
        synchronized(this) {
            val dataSource = dataSource(context)
            val plans = readPlans(dataSource).toMutableList()
            val removed = plans.removeAll { it.id == planId }
            if (removed) {
                savePlans(dataSource = dataSource, plans = plans)
            }
        }
    }

    fun movePlanUp(
        context: Context,
        planId: String
    ) {
        ensureInitialized(context)
        synchronized(this) {
            val dataSource = dataSource(context)
            val plans = readPlans(dataSource).toMutableList()
            val index = plans.indexOfFirst { it.id == planId }
            if (index <= 0) {
                return
            }
            val current = plans[index]
            plans[index] = plans[index - 1]
            plans[index - 1] = current
            savePlans(dataSource = dataSource, plans = plans)
        }
    }

    fun movePlanDown(
        context: Context,
        planId: String
    ) {
        ensureInitialized(context)
        synchronized(this) {
            val dataSource = dataSource(context)
            val plans = readPlans(dataSource).toMutableList()
            val index = plans.indexOfFirst { it.id == planId }
            if (index < 0 || index >= plans.lastIndex) {
                return
            }
            val current = plans[index]
            plans[index] = plans[index + 1]
            plans[index + 1] = current
            savePlans(dataSource = dataSource, plans = plans)
        }
    }

    fun markReminderTriggered(
        context: Context,
        planId: String
    ) {
        updatePlanById(context = context, planId = planId) { plan ->
            plan.copy(
                isReminderActive = true,
                suppressNextDeleteFallback = false
            )
        }
    }

    fun markReminderConfirmed(
        context: Context,
        planId: String
    ) {
        updatePlanById(context = context, planId = planId) { plan ->
            plan.copy(
                isReminderActive = false,
                suppressNextDeleteFallback = true
            )
        }
    }

    fun consumeSuppressDeleteFallback(
        context: Context,
        planId: String
    ): Boolean {
        ensureInitialized(context)
        synchronized(this) {
            val dataSource = dataSource(context)
            val plans = readPlans(dataSource).toMutableList()
            val index = plans.indexOfFirst { it.id == planId }
            if (index < 0) {
                return false
            }
            val shouldSuppress = plans[index].suppressNextDeleteFallback
            if (!shouldSuppress) {
                return false
            }
            plans[index] = plans[index].copy(suppressNextDeleteFallback = false)
            savePlans(dataSource = dataSource, plans = plans)
            return true
        }
    }

    fun isAnyReminderActive(context: Context): Boolean {
        return getPlans(context).any { it.isReminderActive }
    }

    private fun updatePlanById(
        context: Context,
        planId: String,
        transform: (ReminderPlan) -> ReminderPlan
    ) {
        ensureInitialized(context)
        synchronized(this) {
            val dataSource = dataSource(context)
            val plans = readPlans(dataSource).toMutableList()
            val index = plans.indexOfFirst { it.id == planId }
            if (index < 0) {
                return
            }
            plans[index] = transform(plans[index])
            savePlans(dataSource = dataSource, plans = plans)
        }
    }

    private fun ensureInitialized(context: Context) {
        val dataSource = dataSource(context)
        val initialized = dataSource.contains(KEY_PLANS_JSON) &&
            dataSource.contains(KEY_NEXT_NOTIFICATION_ID)
        if (initialized) {
            return
        }

        synchronized(this) {
            val latestDataSource = dataSource(context)
            val latestInitialized = latestDataSource.contains(KEY_PLANS_JSON) &&
                latestDataSource.contains(KEY_NEXT_NOTIFICATION_ID)
            if (latestInitialized) {
                return
            }

            val existingPlans = readPlans(latestDataSource)
            val hasPlansJson = latestDataSource.contains(KEY_PLANS_JSON)
            val hasNextNotificationId = latestDataSource.contains(KEY_NEXT_NOTIFICATION_ID)

            latestDataSource.edit(commitSynchronously = true) {
                if (!hasPlansJson) {
                    putString(KEY_PLANS_JSON, ReminderPlanJsonCodec.encodePlans(existingPlans))
                }
                if (!hasNextNotificationId) {
                    val nextNotificationId = (existingPlans.maxOfOrNull { it.notificationId }
                        ?: (DEFAULT_NOTIFICATION_ID_START - 1)) + 1
                    putInt(KEY_NEXT_NOTIFICATION_ID, nextNotificationId)
                }
            }
        }
    }

    private fun readPlans(dataSource: ReminderPreferencesDataSource): List<ReminderPlan> {
        return ReminderPlanJsonCodec.parsePlans(dataSource.getString(KEY_PLANS_JSON))
    }

    private fun savePlans(
        dataSource: ReminderPreferencesDataSource,
        plans: List<ReminderPlan>
    ) {
        dataSource.edit(commitSynchronously = true) {
            putString(KEY_PLANS_JSON, ReminderPlanJsonCodec.encodePlans(plans))
        }
    }

    private fun allocateNotificationId(dataSource: ReminderPreferencesDataSource): Int {
        val nextId = dataSource.getInt(KEY_NEXT_NOTIFICATION_ID, DEFAULT_NOTIFICATION_ID_START)
        dataSource.edit(commitSynchronously = true) {
            putInt(KEY_NEXT_NOTIFICATION_ID, nextId + 1)
        }
        return nextId
    }

    private fun validateTime(
        hour: Int,
        minute: Int
    ) {
        require(hour in 0..23) { "hour must be in 0..23" }
        require(minute in 0..59) { "minute must be in 0..59" }
    }

    private fun dataSource(context: Context): ReminderPreferencesDataSource {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ReminderPreferencesDataSource(preferences)
    }
}
