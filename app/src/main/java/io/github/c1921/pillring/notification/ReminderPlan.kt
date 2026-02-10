package io.github.c1921.pillring.notification

data class ReminderPlan(
    val id: String,
    val name: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val notificationId: Int,
    val isReminderActive: Boolean,
    val suppressNextDeleteFallback: Boolean
)
