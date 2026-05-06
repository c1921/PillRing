package io.github.c1921.pillring.update

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateRepositoryCheckForUpdatesTest {
    @Test
    fun checkForUpdates_withHigherLatestVersion_returnsUpdateAvailable() = runBlocking {
        val store = FakeUpdateStore()
        val releaseClient = FakeReleaseClient(
            LatestReleasePayload(
                versionName = "0.2.0",
                releaseUrl = "https://example.com/release"
            )
        )
        val repository = AppUpdateRepository(
            nowProvider = { 10_000L },
            store = store,
            releaseClient = releaseClient
        )

        val result = repository.checkForUpdates(
            currentVersionName = "0.1.0",
            force = true
        )

        assertEquals(UpdateStatus.UPDATE_AVAILABLE, result.status)
        assertEquals("0.2.0", result.latestVersionName)
        assertEquals(1, releaseClient.fetchCalls)
        assertNotNull(store.savedResult)
    }

    @Test
    fun checkForUpdates_withRecentCachedResult_skipsNetworkWhenNotForced() = runBlocking {
        val cached = UpdateCheckResult(
            status = UpdateStatus.UP_TO_DATE,
            currentVersionName = "0.1.0",
            latestVersionName = "0.1.0",
            releaseUrl = "https://example.com/release",
            checkedAtEpochMs = 9_500L
        )
        val store = FakeUpdateStore(cachedResult = cached)
        val releaseClient = FakeReleaseClient(
            LatestReleasePayload(
                versionName = "0.2.0",
                releaseUrl = "https://example.com/release"
            )
        )
        val repository = AppUpdateRepository(
            nowProvider = { 10_000L },
            store = store,
            releaseClient = releaseClient
        )

        val result = repository.checkForUpdates(
            currentVersionName = "0.1.0",
            force = false
        )

        assertEquals(UpdateStatus.UP_TO_DATE, result.status)
        assertEquals(0, releaseClient.fetchCalls)
    }

    @Test
    fun checkForUpdates_failureDoesNotSaveResultOrThrottleNextAutoCheck() = runBlocking {
        val store = FakeUpdateStore()
        val releaseClient = FakeReleaseClient(null)
        val repository = AppUpdateRepository(
            nowProvider = { 10_000L },
            store = store,
            releaseClient = releaseClient
        )

        val failedResult = repository.checkForUpdates(
            currentVersionName = "0.1.0",
            force = false
        )

        assertEquals(UpdateStatus.FAILED, failedResult.status)
        assertNull(store.savedResult)
        assertEquals(false, repository.shouldSkipAutoCheck())

        releaseClient.latestRelease = LatestReleasePayload(
            versionName = "0.2.0",
            releaseUrl = "https://example.com/release"
        )

        val retryResult = repository.checkForUpdates(
            currentVersionName = "0.1.0",
            force = false
        )

        assertEquals(UpdateStatus.UPDATE_AVAILABLE, retryResult.status)
        assertEquals(2, releaseClient.fetchCalls)
        assertNotNull(store.savedResult)
    }

    private class FakeReleaseClient(
        var latestRelease: LatestReleasePayload?
    ) : LatestReleaseClient {
        var fetchCalls: Int = 0

        override fun fetchLatestRelease(): LatestReleasePayload? {
            fetchCalls += 1
            return latestRelease
        }
    }

    private class FakeUpdateStore(
        private var cachedResult: UpdateCheckResult? = null
    ) : UpdateStore {
        var savedResult: UpdateCheckResult? = null

        override fun readCachedResult(currentVersionName: String): UpdateCheckResult? {
            return cachedResult
        }

        override fun saveResult(result: UpdateCheckResult) {
            savedResult = result
            cachedResult = result
        }

        override fun lastCheckEpochMs(): Long? {
            return cachedResult?.checkedAtEpochMs
        }
    }
}
