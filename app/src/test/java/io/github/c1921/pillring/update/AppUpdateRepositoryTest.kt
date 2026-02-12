package io.github.c1921.pillring.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateRepositoryTest {
    @Test
    fun compareVersionNames_returnsNegativeWhenLatestIsHigher() {
        val result = compareVersionNames(
            currentVersionName = "0.1.0",
            latestVersionName = "0.1.1"
        )

        assertEquals(-1, result)
    }

    @Test
    fun compareVersionNames_returnsPositiveWhenCurrentIsHigher() {
        val result = compareVersionNames(
            currentVersionName = "1.2.10",
            latestVersionName = "1.2.2"
        )

        assertEquals(1, result)
    }

    @Test
    fun compareVersionNames_supportsPrefixAndMissingPatchPart() {
        val result = compareVersionNames(
            currentVersionName = "v1.0",
            latestVersionName = "1.0.0"
        )

        assertEquals(0, result)
    }

    @Test
    fun compareVersionNames_returnsNullWhenVersionIsInvalid() {
        val result = compareVersionNames(
            currentVersionName = "0.1.0",
            latestVersionName = "vNext"
        )

        assertNull(result)
    }

    @Test
    fun parseLatestReleasePayload_parsesStableRelease() {
        val payload = """
            {
              "tag_name": "v0.2.0",
              "html_url": "https://github.com/c1921/PillRing/releases/tag/v0.2.0",
              "draft": false,
              "prerelease": false
            }
        """.trimIndent()

        val result = parseLatestReleasePayload(payload)

        assertEquals("0.2.0", result?.versionName)
        assertEquals(
            "https://github.com/c1921/PillRing/releases/tag/v0.2.0",
            result?.releaseUrl
        )
    }

    @Test
    fun parseLatestReleasePayload_returnsNullWhenPrerelease() {
        val payload = """
            {
              "tag_name": "v0.2.0-beta.1",
              "html_url": "https://github.com/c1921/PillRing/releases/tag/v0.2.0-beta.1",
              "draft": false,
              "prerelease": true
            }
        """.trimIndent()

        val result = parseLatestReleasePayload(payload)

        assertNull(result)
    }
}
