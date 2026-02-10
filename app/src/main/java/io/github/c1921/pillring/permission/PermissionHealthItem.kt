package io.github.c1921.pillring.permission

data class PermissionHealthItem(
    val id: String,
    val title: String,
    val statusText: String,
    val detailText: String,
    val state: PermissionState,
    val actionLabel: String,
    val action: PermissionAction
)
