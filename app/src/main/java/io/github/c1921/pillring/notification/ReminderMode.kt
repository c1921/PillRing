package io.github.c1921.pillring.notification

enum class ReminderMode {
    ONGOING,
    DISMISSIBLE_TEST;

    companion object {
        fun fromName(raw: String?): ReminderMode {
            return entries.firstOrNull { it.name == raw } ?: ONGOING
        }
    }
}
