package io.github.c1921.pillring.update

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AppUpdateRepository(
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val store: UpdateStore,
    private val releaseClient: LatestReleaseClient = GitHubLatestReleaseClient()
) {
    constructor(
        context: Context,
        nowProvider: () -> Long = { System.currentTimeMillis() },
        store: UpdateStore = AppUpdateStore(context),
        releaseClient: LatestReleaseClient = GitHubLatestReleaseClient()
    ) : this(
        nowProvider = nowProvider,
        store = store,
        releaseClient = releaseClient
    )

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
            val latestRelease = releaseClient.fetchLatestRelease() ?: return@withContext UpdateCheckResult(
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

    companion object {
        const val DEFAULT_RELEASE_PAGE_URL = "https://github.com/c1921/PillRing/releases"
        const val AUTO_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

        internal const val LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/c1921/PillRing/releases/latest"
        internal const val REQUEST_TIMEOUT_MS = 5000
        internal const val USER_AGENT = "PillRing-Android"
    }
}

interface LatestReleaseClient {
    fun fetchLatestRelease(): LatestReleasePayload?
}

interface UpdateStore {
    fun readCachedResult(currentVersionName: String): UpdateCheckResult?

    fun saveResult(result: UpdateCheckResult)

    fun lastCheckEpochMs(): Long?
}

internal class GitHubLatestReleaseClient(
    private val apiUrl: String = AppUpdateRepository.LATEST_RELEASE_API_URL,
    private val requestTimeoutMs: Int = AppUpdateRepository.REQUEST_TIMEOUT_MS,
    private val userAgent: String = AppUpdateRepository.USER_AGENT
) : LatestReleaseClient {
    override fun fetchLatestRelease(): LatestReleasePayload? {
        return try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as? HttpURLConnection ?: return null
            connection.requestMethod = "GET"
            connection.connectTimeout = requestTimeoutMs
            connection.readTimeout = requestTimeoutMs
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", userAgent)

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
}

data class LatestReleasePayload(
    val versionName: String,
    val releaseUrl: String
)

internal fun parseLatestReleasePayload(payload: String): LatestReleasePayload? {
    return try {
        val root = JSONObject(payload)
        val isDraft = root.optBoolean("draft", false)
        val isPrerelease = root.optBoolean("prerelease", false)
        if (isDraft || isPrerelease) {
            return null
        }

        val tagName = normalizeVersionName(root.optString("tag_name", ""))
        if (tagName.isBlank()) {
            return null
        }

        val releaseUrl = root.optString(
            "html_url",
            AppUpdateRepository.DEFAULT_RELEASE_PAGE_URL
        ).trim().ifBlank { AppUpdateRepository.DEFAULT_RELEASE_PAGE_URL }

        LatestReleasePayload(
            versionName = tagName,
            releaseUrl = releaseUrl
        )
    } catch (_: Exception) {
        null
    }
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
