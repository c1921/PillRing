package io.github.c1921.pillring.notification

data class ReminderPlan(
    val id: String,
    val name: String,
    val hour: Int,
    val minute: Int,
    val repeatMode: ReminderRepeatMode = ReminderRepeatMode.DAILY,
    val intervalDays: Int = 1,
    val startDateEpochDay: Long? = null,
    val enabled: Boolean,
    val notificationId: Int,
    val isReminderActive: Boolean,
    val suppressNextDeleteFallback: Boolean
)
