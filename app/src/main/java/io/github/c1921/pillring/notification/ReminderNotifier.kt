package io.github.c1921.pillring.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.c1921.pillring.MainActivity
import io.github.c1921.pillring.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReminderNotifier {
    fun ensureChannel(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ReminderContract.CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showNotification(
        context: Context,
        plan: ReminderPlan,
        reason: String
    ) {
        if (!hasNotificationPermission(context)) {
            return
        }

        ensureChannel(context)
        val timeText = formatReminderTime(
            context = context,
            hour = plan.hour,
            minute = plan.minute
        )
        val contentText = context.getString(
            R.string.notification_text_with_plan_time,
            plan.name,
            timeText
        )

        val contentIntent = PendingIntent.getActivity(
            context,
            requestCodeForContent(plan),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ReminderContract.EXTRA_PLAN_ID, plan.id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val deleteIntent = PendingIntent.getBroadcast(
            context,
            requestCodeForDelete(plan),
            Intent(context, NotificationDeleteReceiver::class.java).apply {
                action = ReminderContract.ACTION_NOTIFICATION_DELETED
                putExtra(ReminderContract.EXTRA_REASON, reason)
                putExtra(ReminderContract.EXTRA_PLAN_ID, plan.id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, ReminderContract.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pillring)
            .setContentTitle(context.getString(R.string.notification_title_with_plan, plan.name))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(plan.notificationId, notification)
    }

    fun cancelReminderNotification(
        context: Context,
        plan: ReminderPlan
    ) {
        NotificationManagerCompat.from(context).cancel(plan.notificationId)
    }

    fun cancelLegacyReminderNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(ReminderContract.LEGACY_NOTIFICATION_ID)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCodeForDelete(plan: ReminderPlan): Int = plan.notificationId * 10 + 3

    private fun requestCodeForContent(plan: ReminderPlan): Int = plan.notificationId * 10 + 4

    private fun formatReminderTime(
        context: Context,
        hour: Int,
        minute: Int
    ): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        return LocalTime.of(hour, minute).format(formatter)
    }
}
