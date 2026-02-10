package io.github.c1921.pillring.notification

import android.content.Context

object ReminderSessionStore {
    private const val PREFS_NAME = "reminder_session_store"
    private const val KEY_REMINDER_ACTIVE = "is_reminder_active"
    private const val KEY_SUPPRESS_NEXT_DELETE_FALLBACK = "suppress_next_delete_fallback"
    private const val KEY_PLAN_ENABLED = "is_plan_enabled"
    private const val KEY_SELECTED_HOUR = "selected_hour"
    private const val KEY_SELECTED_MINUTE = "selected_minute"

    fun isReminderActive(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_REMINDER_ACTIVE, false)
    }

    fun markReminderTriggered(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_REMINDER_ACTIVE, true)
            .putBoolean(KEY_SUPPRESS_NEXT_DELETE_FALLBACK, false)
            .apply()
    }

    fun markReminderConfirmed(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_REMINDER_ACTIVE, false)
            .putBoolean(KEY_SUPPRESS_NEXT_DELETE_FALLBACK, true)
            .apply()
    }

    fun consumeSuppressDeleteFallback(context: Context): Boolean {
        val preferences = prefs(context)
        val shouldSuppress = preferences.getBoolean(KEY_SUPPRESS_NEXT_DELETE_FALLBACK, false)
        if (shouldSuppress) {
            preferences.edit()
                .putBoolean(KEY_SUPPRESS_NEXT_DELETE_FALLBACK, false)
                .apply()
        }
        return shouldSuppress
    }

    fun isPlanEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_PLAN_ENABLED, false)
    }

    fun setPlanEnabled(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context).edit()
            .putBoolean(KEY_PLAN_ENABLED, enabled)
            .apply()
    }

    fun getSelectedTime(context: Context): Pair<Int, Int>? {
        val preferences = prefs(context)
        if (!preferences.contains(KEY_SELECTED_HOUR) || !preferences.contains(KEY_SELECTED_MINUTE)) {
            return null
        }
        val hour = preferences.getInt(KEY_SELECTED_HOUR, 0)
        val minute = preferences.getInt(KEY_SELECTED_MINUTE, 0)
        return hour to minute
    }

    fun setSelectedTime(
        context: Context,
        hour: Int,
        minute: Int
    ) {
        prefs(context).edit()
            .putInt(KEY_SELECTED_HOUR, hour)
            .putInt(KEY_SELECTED_MINUTE, minute)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
