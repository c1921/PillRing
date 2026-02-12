package io.github.c1921.pillring.update

import android.content.Context
import androidx.core.content.edit

class AppUpdateStore(context: Context) : UpdateStore {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun readCachedResult(currentVersionName: String): UpdateCheckResult? {
        if (!preferences.contains(KEY_LAST_CHECK_EPOCH_MS)) {
            return null
        }

        val checkedAtEpochMs = preferences.getLong(KEY_LAST_CHECK_EPOCH_MS, -1L)
        if (checkedAtEpochMs <= 0L) {
            return null
        }

        val status = parseStatus(preferences.getString(KEY_LAST_CHECK_STATUS, null))
            ?: UpdateStatus.FAILED

        return UpdateCheckResult(
            status = status,
            currentVersionName = currentVersionName,
            latestVersionName = preferences.getString(KEY_LATEST_VERSION_NAME, null),
            releaseUrl = preferences.getString(KEY_LATEST_RELEASE_URL, null),
            checkedAtEpochMs = checkedAtEpochMs
        )
    }

    override fun saveResult(result: UpdateCheckResult) {
        preferences.edit {
            putLong(KEY_LAST_CHECK_EPOCH_MS, result.checkedAtEpochMs)
            putString(KEY_LAST_CHECK_STATUS, result.status.name)
            putString(KEY_LATEST_VERSION_NAME, result.latestVersionName)
            putString(KEY_LATEST_RELEASE_URL, result.releaseUrl)
        }
    }

    override fun lastCheckEpochMs(): Long? {
        if (!preferences.contains(KEY_LAST_CHECK_EPOCH_MS)) {
            return null
        }
        val value = preferences.getLong(KEY_LAST_CHECK_EPOCH_MS, -1L)
        return value.takeIf { it > 0L }
    }

    private fun parseStatus(raw: String?): UpdateStatus? {
        if (raw.isNullOrBlank()) {
            return null
        }
        return try {
            UpdateStatus.valueOf(raw)
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        private const val PREFS_NAME = "app_update_store"
        private const val KEY_LAST_CHECK_EPOCH_MS = "last_check_epoch_ms"
        private const val KEY_LATEST_VERSION_NAME = "latest_version_name"
        private const val KEY_LATEST_RELEASE_URL = "latest_release_url"
        private const val KEY_LAST_CHECK_STATUS = "last_check_status"
    }
}
