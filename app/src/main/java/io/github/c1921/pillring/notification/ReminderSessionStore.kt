package io.github.c1921.pillring.notification

import android.content.Context

object ReminderSessionStore {
    private const val PREFS_NAME = "reminder_session_store"
    private const val KEY_REMINDER_ACTIVE = "is_reminder_active"
    private const val KEY_SUPPRESS_NEXT_DELETE_FALLBACK = "suppress_next_delete_fallback"

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

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
