package io.github.c1921.pillring.update

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UPDATE_AVAILABLE,
    UP_TO_DATE,
    FAILED
}

data class UpdateCheckResult(
    val status: UpdateStatus,
    val currentVersionName: String,
    val latestVersionName: String? = null,
    val releaseUrl: String? = null,
    val checkedAtEpochMs: Long
)

data class UpdateUiState(
    val status: UpdateStatus,
    val currentVersionName: String,
    val latestVersionName: String? = null,
    val releaseUrl: String? = null,
    val lastCheckedAtEpochMs: Long? = null
) {
    companion object {
        fun idle(currentVersionName: String): UpdateUiState {
            return UpdateUiState(
                status = UpdateStatus.IDLE,
                currentVersionName = currentVersionName
            )
        }

        fun checking(currentVersionName: String): UpdateUiState {
            return UpdateUiState(
                status = UpdateStatus.CHECKING,
                currentVersionName = currentVersionName
            )
        }

        fun fromResult(result: UpdateCheckResult): UpdateUiState {
            return UpdateUiState(
                status = result.status,
                currentVersionName = result.currentVersionName,
                latestVersionName = result.latestVersionName,
                releaseUrl = result.releaseUrl,
                lastCheckedAtEpochMs = result.checkedAtEpochMs
            )
        }
    }
}
