package io.github.c1921.pillring.notification

import android.content.SharedPreferences
import androidx.core.content.edit

internal class ReminderPreferencesDataSource(
    private val preferences: SharedPreferences
) {
    fun contains(key: String): Boolean = preferences.contains(key)

    fun getString(key: String): String? = preferences.getString(key, null)

    fun getInt(
        key: String,
        defaultValue: Int
    ): Int = preferences.getInt(key, defaultValue)

    fun edit(
        commitSynchronously: Boolean = false,
        action: SharedPreferences.Editor.() -> Unit
    ) {
        preferences.edit(
            commit = commitSynchronously,
            action = action
        )
    }
}
