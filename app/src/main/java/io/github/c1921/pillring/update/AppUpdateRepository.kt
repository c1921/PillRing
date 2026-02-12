package io.github.c1921.pillring.update

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppUpdateRepository(
    context: Context,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val store: AppUpdateStore = AppUpdateStore(context)
) {
    fun getCachedUiState(currentVersionName: String): UpdateUiState {
        val normalizedCurrentVersionName = normalizeVersionName(currentVersionName)
        val cached = store.readCachedResult(normalizedCurrentVersionName)
            ?: return UpdateUiState.idle(normalizedCurrentVersionName)
        val resolved = resolveResultForCurrentVersion(cached, normalizedCurrentVersionName)
        return UpdateUiState.fromResult(resolved)
    }

    fun shouldSkipAutoCheck(): Boolean {
        val lastCheckedAt = store.lastCheckEpochMs() ?: return false
        val now = nowProvider()
        return now - lastCheckedAt < AUTO_CHECK_INTERVAL_MS
    }

    suspend fun checkForUpdates(
        currentVersionName: String,
        force: Boolean
    ): UpdateCheckResult {
        val normalizedCurrentVersionName = normalizeVersionName(currentVersionName)
        val cached = store.readCachedResult(normalizedCurrentVersionName)

        if (!force && shouldSkipAutoCheck() && cached != null) {
            return resolveResultForCurrentVersion(cached, normalizedCurrentVersionName)
        }

        val checkedAtEpochMs = nowProvider()
        val result = withContext(Dispatchers.IO) {
            val latestRelease = fetchLatestRelease() ?: return@withContext UpdateCheckResult(
                status = UpdateStatus.FAILED,
                currentVersionName = normalizedCurrentVersionName,
                checkedAtEpochMs = checkedAtEpochMs
            )

            val versionCompare = compareVersionNames(
                normalizedCurrentVersionName,
                latestRelease.versionName
            )

            if (versionCompare == null) {
                return@withContext UpdateCheckResult(
                    status = UpdateStatus.FAILED,
                    currentVersionName = normalizedCurrentVersionName,
                    latestVersionName = latestRelease.versionName,
                    releaseUrl = latestRelease.releaseUrl,
                    checkedAtEpochMs = checkedAtEpochMs
                )
            }

            val status = if (versionCompare < 0) {
                UpdateStatus.UPDATE_AVAILABLE
            } else {
                UpdateStatus.UP_TO_DATE
            }

            UpdateCheckResult(
                status = status,
                currentVersionName = normalizedCurrentVersionName,
                latestVersionName = latestRelease.versionName,
                releaseUrl = latestRelease.releaseUrl,
                checkedAtEpochMs = checkedAtEpochMs
            )
        }

        store.saveResult(result)
        return resolveResultForCurrentVersion(result, normalizedCurrentVersionName)
    }

    private fun resolveResultForCurrentVersion(
        result: UpdateCheckResult,
        currentVersionName: String
    ): UpdateCheckResult {
        if (result.status != UpdateStatus.UPDATE_AVAILABLE) {
            return result.copy(currentVersionName = currentVersionName)
        }

        val latestVersionName = result.latestVersionName ?: return result.copy(
            currentVersionName = currentVersionName
        )
        val compare = compareVersionNames(currentVersionName, latestVersionName)
            ?: return result.copy(currentVersionName = currentVersionName)

        val resolvedStatus = if (compare < 0) {
            UpdateStatus.UPDATE_AVAILABLE
        } else {
            UpdateStatus.UP_TO_DATE
        }

        return result.copy(
            status = resolvedStatus,
            currentVersionName = currentVersionName
        )
    }

    private fun fetchLatestRelease(): LatestReleasePayload? {
        return try {
            val url = URL(LATEST_RELEASE_API_URL)
            val connection = url.openConnection() as? HttpURLConnection ?: return null
            connection.requestMethod = "GET"
            connection.connectTimeout = REQUEST_TIMEOUT_MS
            connection.readTimeout = REQUEST_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", USER_AGENT)

            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return null
                }
                val payload = connection.inputStream.bufferedReader().use { it.readText() }
                parseLatestReleasePayload(payload)
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val DEFAULT_RELEASE_PAGE_URL = "https://github.com/c1921/PillRing/releases"
        const val AUTO_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

        private const val LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/c1921/PillRing/releases/latest"
        private const val REQUEST_TIMEOUT_MS = 5000
        private const val USER_AGENT = "PillRing-Android"
    }
}

internal data class LatestReleasePayload(
    val versionName: String,
    val releaseUrl: String
)

internal fun parseLatestReleasePayload(payload: String): LatestReleasePayload? {
    val isDraft = extractBooleanField(payload, "draft") ?: false
    val isPrerelease = extractBooleanField(payload, "prerelease") ?: false
    if (isDraft || isPrerelease) {
        return null
    }

    val tagName = normalizeVersionName(extractStringField(payload, "tag_name") ?: return null)
    if (tagName.isBlank()) {
        return null
    }

    val releaseUrl = (extractStringField(payload, "html_url")
        ?: AppUpdateRepository.DEFAULT_RELEASE_PAGE_URL)
        .trim()
        .ifBlank { AppUpdateRepository.DEFAULT_RELEASE_PAGE_URL }

    return LatestReleasePayload(
        versionName = tagName,
        releaseUrl = releaseUrl
    )
}

internal fun compareVersionNames(
    currentVersionName: String,
    latestVersionName: String
): Int? {
    val currentParts = parseVersionParts(currentVersionName) ?: return null
    val latestParts = parseVersionParts(latestVersionName) ?: return null
    val totalParts = maxOf(currentParts.size, latestParts.size)

    for (index in 0 until totalParts) {
        val currentPart = currentParts.getOrElse(index) { 0 }
        val latestPart = latestParts.getOrElse(index) { 0 }
        if (currentPart != latestPart) {
            return currentPart.compareTo(latestPart)
        }
    }
    return 0
}

internal fun parseVersionParts(versionName: String): List<Int>? {
    val normalized = normalizeVersionName(versionName)
    if (normalized.isBlank()) {
        return null
    }

    val core = normalized
        .substringBefore('-')
        .substringBefore('+')
    if (core.isBlank()) {
        return null
    }

    val rawParts = core.split(".")
    if (rawParts.isEmpty()) {
        return null
    }

    val parsed = mutableListOf<Int>()
    for (part in rawParts) {
        if (part.isBlank() || part.any { !it.isDigit() }) {
            return null
        }
        parsed += part.toIntOrNull() ?: return null
    }

    return parsed
}

internal fun normalizeVersionName(versionName: String): String {
    return versionName
        .trim()
        .removePrefix("v")
        .removePrefix("V")
}

private fun extractStringField(payload: String, fieldName: String): String? {
    val pattern = "\"${Regex.escape(fieldName)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
    val rawValue = Regex(pattern).find(payload)?.groupValues?.getOrNull(1) ?: return null
    return rawValue
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
}

private fun extractBooleanField(payload: String, fieldName: String): Boolean? {
    val pattern = "\"${Regex.escape(fieldName)}\"\\s*:\\s*(true|false)"
    val rawValue = Regex(pattern, RegexOption.IGNORE_CASE)
        .find(payload)
        ?.groupValues
        ?.getOrNull(1)
        ?.lowercase()
        ?: return null
    return rawValue == "true"
}
