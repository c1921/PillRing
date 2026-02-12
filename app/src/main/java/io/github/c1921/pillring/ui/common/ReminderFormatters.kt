package io.github.c1921.pillring.ui.common

import android.content.Context
import android.text.format.DateFormat
import io.github.c1921.pillring.R
import io.github.c1921.pillring.notification.ReminderRepeatMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal fun formatReminderTime(
    context: Context,
    hour: Int,
    minute: Int
): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return LocalTime.of(hour, minute).format(formatter)
}

internal fun formatRepeatSummary(
    context: Context,
    repeatMode: ReminderRepeatMode,
    intervalDays: Int,
    startDateEpochDay: Long?
): String {
    return when (repeatMode) {
        ReminderRepeatMode.DAILY -> context.getString(R.string.label_repeat_summary_daily)
        ReminderRepeatMode.INTERVAL_DAYS -> {
            if (intervalDays !in 1..365 || startDateEpochDay == null) {
                context.getString(R.string.label_repeat_summary_daily)
            } else {
                context.resources.getQuantityString(
                    R.plurals.label_repeat_summary_interval,
                    intervalDays,
                    intervalDays,
                    formatReminderDate(LocalDate.ofEpochDay(startDateEpochDay))
                )
            }
        }
    }
}

internal fun formatReminderDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
    return date.format(formatter)
}
