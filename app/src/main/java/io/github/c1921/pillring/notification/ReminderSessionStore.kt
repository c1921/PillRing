package io.github.c1921.pillring.notification

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.UUID

object ReminderSessionStore {
    const val MAX_PLAN_COUNT = 10

    private const val PREFS_NAME = "reminder_session_store"

    private const val KEY_SCHEMA_VERSION = "schema_version"
    private const val KEY_PLANS_JSON = "plans_json"
    private const val KEY_NEXT_NOTIFICATION_ID = "next_notification_id"

    private const val SCHEMA_VERSION = 2
    private const val DEFAULT_NOTIFICATION_ID_START = 1001
    private const val LEGACY_PLAN_NAME = "Plan 1"

    // Legacy keys for migration.
    private const val KEY_REMINDER_ACTIVE = "is_reminder_active"
    private const val KEY_SUPPRESS_NEXT_DELETE_FALLBACK = "suppress_next_delete_fallback"
    private const val KEY_PLAN_ENABLED = "is_plan_enabled"
    private const val KEY_SELECTED_HOUR = "selected_hour"
    private const val KEY_SELECTED_MINUTE = "selected_minute"

    private const val PLAN_ID = "id"
    private const val PLAN_NAME = "name"
    private const val PLAN_HOUR = "hour"
    private const val PLAN_MINUTE = "minute"
    private const val PLAN_ENABLED = "enabled"
    private const val PLAN_NOTIFICATION_ID = "notification_id"
    private const val PLAN_IS_REMINDER_ACTIVE = "is_reminder_active"
    private const val PLAN_SUPPRESS_NEXT_DELETE_FALLBACK = "suppress_next_delete_fallback"

    fun getPlans(context: Context): List<ReminderPlan> {
        ensureMigrated(context)
        return readPlans(prefs(context))
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
        enabled: Boolean = true
    ): ReminderPlan {
        validateTime(hour = hour, minute = minute)
        val normalizedName = name.trim().takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("plan name must not be blank")

        ensureMigrated(context)
        synchronized(this) {
            val preferences = prefs(context)
            val plans = readPlans(preferences).toMutableList()
            if (plans.size >= MAX_PLAN_COUNT) {
                throw IllegalStateException("maximum plan count reached")
            }

            val plan = ReminderPlan(
                id = UUID.randomUUID().toString(),
                name = normalizedName,
                hour = hour,
                minute = minute,
                enabled = enabled,
                notificationId = allocateNotificationId(preferences),
                isReminderActive = false,
                suppressNextDeleteFallback = false
            )
            plans += plan
            savePlans(preferences = preferences, plans = plans)
            return plan
        }
    }

    fun updatePlan(
        context: Context,
        plan: ReminderPlan
    ) {
        validateTime(hour = plan.hour, minute = plan.minute)
        val normalizedName = plan.name.trim().takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("plan name must not be blank")

        ensureMigrated(context)
        synchronized(this) {
            val preferences = prefs(context)
            val plans = readPlans(preferences).toMutableList()
            val targetIndex = plans.indexOfFirst { it.id == plan.id }
            if (targetIndex < 0) {
                return
            }
            plans[targetIndex] = plan.copy(name = normalizedName)
            savePlans(preferences = preferences, plans = plans)
        }
    }

    fun deletePlan(
        context: Context,
        planId: String
    ) {
        ensureMigrated(context)
        synchronized(this) {
            val preferences = prefs(context)
            val plans = readPlans(preferences).toMutableList()
            val removed = plans.removeAll { it.id == planId }
            if (removed) {
                savePlans(preferences = preferences, plans = plans)
            }
        }
    }

    fun movePlanUp(
        context: Context,
        planId: String
    ) {
        ensureMigrated(context)
        synchronized(this) {
            val preferences = prefs(context)
            val plans = readPlans(preferences).toMutableList()
            val index = plans.indexOfFirst { it.id == planId }
            if (index <= 0) {
                return
            }
            val current = plans[index]
            plans[index] = plans[index - 1]
            plans[index - 1] = current
            savePlans(preferences = preferences, plans = plans)
        }
    }

    fun movePlanDown(
        context: Context,
        planId: String
    ) {
        ensureMigrated(context)
        synchronized(this) {
            val preferences = prefs(context)
            val plans = readPlans(preferences).toMutableList()
            val index = plans.indexOfFirst { it.id == planId }
            if (index < 0 || index >= plans.lastIndex) {
                return
            }
            val current = plans[index]
            plans[index] = plans[index + 1]
            plans[index + 1] = current
            savePlans(preferences = preferences, plans = plans)
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
        ensureMigrated(context)
        synchronized(this) {
            val preferences = prefs(context)
            val plans = readPlans(preferences).toMutableList()
            val index = plans.indexOfFirst { it.id == planId }
            if (index < 0) {
                return false
            }
            val shouldSuppress = plans[index].suppressNextDeleteFallback
            if (!shouldSuppress) {
                return false
            }
            plans[index] = plans[index].copy(suppressNextDeleteFallback = false)
            savePlans(preferences = preferences, plans = plans)
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
        ensureMigrated(context)
        synchronized(this) {
            val preferences = prefs(context)
            val plans = readPlans(preferences).toMutableList()
            val index = plans.indexOfFirst { it.id == planId }
            if (index < 0) {
                return
            }
            plans[index] = transform(plans[index])
            savePlans(preferences = preferences, plans = plans)
        }
    }

    private fun ensureMigrated(context: Context) {
        val preferences = prefs(context)
        val alreadyMigrated = preferences.getInt(KEY_SCHEMA_VERSION, 0) >= SCHEMA_VERSION &&
            preferences.contains(KEY_PLANS_JSON) &&
            preferences.contains(KEY_NEXT_NOTIFICATION_ID)
        if (alreadyMigrated) {
            return
        }

        synchronized(this) {
            val latestPrefs = prefs(context)
            val latestMigrated = latestPrefs.getInt(KEY_SCHEMA_VERSION, 0) >= SCHEMA_VERSION &&
                latestPrefs.contains(KEY_PLANS_JSON) &&
                latestPrefs.contains(KEY_NEXT_NOTIFICATION_ID)
            if (latestMigrated) {
                return
            }

            val existingPlans = readPlans(latestPrefs)
            val migratedPlans = when {
                existingPlans.isNotEmpty() -> existingPlans
                hasLegacyPlanData(latestPrefs) -> listOf(buildLegacyPlan(latestPrefs))
                else -> emptyList()
            }
            val nextNotificationId = (migratedPlans.maxOfOrNull { it.notificationId }
                ?: (DEFAULT_NOTIFICATION_ID_START - 1)) + 1

            latestPrefs.edit()
                .putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
                .putString(KEY_PLANS_JSON, encodePlans(migratedPlans))
                .putInt(KEY_NEXT_NOTIFICATION_ID, nextNotificationId)
                .remove(KEY_REMINDER_ACTIVE)
                .remove(KEY_SUPPRESS_NEXT_DELETE_FALLBACK)
                .remove(KEY_PLAN_ENABLED)
                .remove(KEY_SELECTED_HOUR)
                .remove(KEY_SELECTED_MINUTE)
                .apply()
        }
    }

    private fun hasLegacyPlanData(preferences: android.content.SharedPreferences): Boolean {
        return preferences.contains(KEY_SELECTED_HOUR) ||
            preferences.contains(KEY_SELECTED_MINUTE) ||
            preferences.contains(KEY_PLAN_ENABLED) ||
            preferences.contains(KEY_REMINDER_ACTIVE) ||
            preferences.contains(KEY_SUPPRESS_NEXT_DELETE_FALLBACK)
    }

    private fun buildLegacyPlan(preferences: android.content.SharedPreferences): ReminderPlan {
        val hasHour = preferences.contains(KEY_SELECTED_HOUR)
        val hasMinute = preferences.contains(KEY_SELECTED_MINUTE)
        val (hour, minute) = if (hasHour && hasMinute) {
            preferences.getInt(KEY_SELECTED_HOUR, 0) to preferences.getInt(KEY_SELECTED_MINUTE, 0)
        } else {
            val defaultTime = LocalDateTime.now().plusMinutes(1)
            defaultTime.hour to defaultTime.minute
        }

        return ReminderPlan(
            id = UUID.randomUUID().toString(),
            name = LEGACY_PLAN_NAME,
            hour = hour,
            minute = minute,
            enabled = preferences.getBoolean(KEY_PLAN_ENABLED, false),
            notificationId = DEFAULT_NOTIFICATION_ID_START,
            isReminderActive = preferences.getBoolean(KEY_REMINDER_ACTIVE, false),
            suppressNextDeleteFallback = preferences.getBoolean(
                KEY_SUPPRESS_NEXT_DELETE_FALLBACK,
                false
            )
        )
    }

    private fun readPlans(preferences: android.content.SharedPreferences): List<ReminderPlan> {
        val plansJson = preferences.getString(KEY_PLANS_JSON, null)
        return parsePlans(plansJson)
    }

    private fun savePlans(
        preferences: android.content.SharedPreferences,
        plans: List<ReminderPlan>
    ) {
        preferences.edit()
            .putString(KEY_PLANS_JSON, encodePlans(plans))
            .apply()
    }

    private fun parsePlans(plansJson: String?): List<ReminderPlan> {
        if (plansJson.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            val array = JSONArray(plansJson)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString(PLAN_ID).trim()
                    if (id.isEmpty()) {
                        continue
                    }
                    val hour = item.optInt(PLAN_HOUR, -1)
                    val minute = item.optInt(PLAN_MINUTE, -1)
                    if (hour !in 0..23 || minute !in 0..59) {
                        continue
                    }
                    val notificationId = item.optInt(PLAN_NOTIFICATION_ID, -1)
                    if (notificationId <= 0) {
                        continue
                    }
                    val name = item.optString(PLAN_NAME).trim()
                    add(
                        ReminderPlan(
                            id = id,
                            name = name.ifEmpty { "Plan" },
                            hour = hour,
                            minute = minute,
                            enabled = item.optBoolean(PLAN_ENABLED, false),
                            notificationId = notificationId,
                            isReminderActive = item.optBoolean(PLAN_IS_REMINDER_ACTIVE, false),
                            suppressNextDeleteFallback = item.optBoolean(
                                PLAN_SUPPRESS_NEXT_DELETE_FALLBACK,
                                false
                            )
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodePlans(plans: List<ReminderPlan>): String {
        val array = JSONArray()
        plans.forEach { plan ->
            array.put(
                JSONObject()
                    .put(PLAN_ID, plan.id)
                    .put(PLAN_NAME, plan.name)
                    .put(PLAN_HOUR, plan.hour)
                    .put(PLAN_MINUTE, plan.minute)
                    .put(PLAN_ENABLED, plan.enabled)
                    .put(PLAN_NOTIFICATION_ID, plan.notificationId)
                    .put(PLAN_IS_REMINDER_ACTIVE, plan.isReminderActive)
                    .put(PLAN_SUPPRESS_NEXT_DELETE_FALLBACK, plan.suppressNextDeleteFallback)
            )
        }
        return array.toString()
    }

    private fun allocateNotificationId(preferences: android.content.SharedPreferences): Int {
        val nextId = preferences.getInt(KEY_NEXT_NOTIFICATION_ID, DEFAULT_NOTIFICATION_ID_START)
        preferences.edit()
            .putInt(KEY_NEXT_NOTIFICATION_ID, nextId + 1)
            .apply()
        return nextId
    }

    private fun validateTime(
        hour: Int,
        minute: Int
    ) {
        require(hour in 0..23) { "hour must be in 0..23" }
        require(minute in 0..59) { "minute must be in 0..59" }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
