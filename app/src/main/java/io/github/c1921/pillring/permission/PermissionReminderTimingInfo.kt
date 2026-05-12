package io.github.c1921.pillring.permission

data class PermissionReminderTimingInfo(
    val nextPlanName: String?,
    val nextTriggerAtEpochMs: Long?,
    val lastTriggeredPlanName: String?,
    val lastTriggeredAtEpochMs: Long?
)
