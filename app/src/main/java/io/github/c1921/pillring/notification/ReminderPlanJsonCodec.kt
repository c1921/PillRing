package io.github.c1921.pillring.notification

import org.json.JSONArray
import org.json.JSONObject

internal object ReminderPlanJsonCodec {
    private const val PLAN_ID = "id"
    private const val PLAN_NAME = "name"
    private const val PLAN_HOUR = "hour"
    private const val PLAN_MINUTE = "minute"
    private const val PLAN_REPEAT_MODE = "repeat_mode"
    private const val PLAN_INTERVAL_DAYS = "interval_days"
    private const val PLAN_START_DATE_EPOCH_DAY = "start_date_epoch_day"
    private const val PLAN_ENABLED = "enabled"
    private const val PLAN_NOTIFICATION_ID = "notification_id"
    private const val PLAN_IS_REMINDER_ACTIVE = "is_reminder_active"
    private const val PLAN_SUPPRESS_NEXT_DELETE_FALLBACK = "suppress_next_delete_fallback"

    fun parsePlans(plansJson: String?): List<ReminderPlan> {
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
                    val repeatMode = item.optString(
                        PLAN_REPEAT_MODE,
                        ReminderRepeatMode.DAILY.name
                    ).toRepeatModeOrNull() ?: ReminderRepeatMode.DAILY
                    val intervalDays = item.optInt(PLAN_INTERVAL_DAYS, 1)
                    val startDateEpochDay = if (
                        item.has(PLAN_START_DATE_EPOCH_DAY) &&
                        !item.isNull(PLAN_START_DATE_EPOCH_DAY)
                    ) {
                        item.optLong(PLAN_START_DATE_EPOCH_DAY)
                    } else {
                        null
                    }
                    val normalizedRepeatConfig = normalizeRepeatConfig(
                        repeatMode = repeatMode,
                        intervalDays = intervalDays,
                        startDateEpochDay = startDateEpochDay,
                        strict = false
                    )
                    val name = item.optString(PLAN_NAME).trim()
                    add(
                        ReminderPlan(
                            id = id,
                            name = name.ifEmpty { "Plan" },
                            hour = hour,
                            minute = minute,
                            repeatMode = normalizedRepeatConfig.repeatMode,
                            intervalDays = normalizedRepeatConfig.intervalDays,
                            startDateEpochDay = normalizedRepeatConfig.startDateEpochDay,
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

    fun encodePlans(plans: List<ReminderPlan>): String {
        val array = JSONArray()
        plans.forEach { plan ->
            array.put(
                JSONObject()
                    .put(PLAN_ID, plan.id)
                    .put(PLAN_NAME, plan.name)
                    .put(PLAN_HOUR, plan.hour)
                    .put(PLAN_MINUTE, plan.minute)
                    .put(PLAN_REPEAT_MODE, plan.repeatMode.name)
                    .put(PLAN_INTERVAL_DAYS, plan.intervalDays)
                    .put(PLAN_START_DATE_EPOCH_DAY, plan.startDateEpochDay)
                    .put(PLAN_ENABLED, plan.enabled)
                    .put(PLAN_NOTIFICATION_ID, plan.notificationId)
                    .put(PLAN_IS_REMINDER_ACTIVE, plan.isReminderActive)
                    .put(PLAN_SUPPRESS_NEXT_DELETE_FALLBACK, plan.suppressNextDeleteFallback)
            )
        }
        return array.toString()
    }

    fun normalizeRepeatConfig(
        repeatMode: ReminderRepeatMode,
        intervalDays: Int,
        startDateEpochDay: Long?,
        strict: Boolean
    ): RepeatConfig {
        if (repeatMode == ReminderRepeatMode.DAILY) {
            return RepeatConfig(
                repeatMode = ReminderRepeatMode.DAILY,
                intervalDays = 1,
                startDateEpochDay = null
            )
        }

        val isIntervalValid = intervalDays in 1..365
        val isStartDateValid = startDateEpochDay != null

        if (isIntervalValid && isStartDateValid) {
            return RepeatConfig(
                repeatMode = ReminderRepeatMode.INTERVAL_DAYS,
                intervalDays = intervalDays,
                startDateEpochDay = startDateEpochDay
            )
        }

        if (strict) {
            require(isIntervalValid) { "intervalDays must be in 1..365" }
            require(isStartDateValid) { "startDateEpochDay must not be null for interval mode" }
        }

        return RepeatConfig(
            repeatMode = ReminderRepeatMode.DAILY,
            intervalDays = 1,
            startDateEpochDay = null
        )
    }

    data class RepeatConfig(
        val repeatMode: ReminderRepeatMode,
        val intervalDays: Int,
        val startDateEpochDay: Long?
    )
}

private fun String.toRepeatModeOrNull(): ReminderRepeatMode? {
    return ReminderRepeatMode.entries.firstOrNull { it.name == this }
}
