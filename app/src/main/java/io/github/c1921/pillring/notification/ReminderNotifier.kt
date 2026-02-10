package io.github.c1921.pillring.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.c1921.pillring.MainActivity
import io.github.c1921.pillring.R

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

    fun showNotification(context: Context, mode: ReminderMode, reason: String) {
        if (!hasNotificationPermission(context)) {
            return
        }

        ensureChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            ReminderContract.REQUEST_CODE_CONTENT_INTENT,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val deleteIntent = PendingIntent.getBroadcast(
            context,
            ReminderContract.REQUEST_CODE_DELETE_INTENT,
            Intent(context, NotificationDeleteReceiver::class.java).apply {
                action = ReminderContract.ACTION_NOTIFICATION_DELETED
                putExtra(ReminderContract.EXTRA_REASON, reason)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val titleRes = when (mode) {
            ReminderMode.ONGOING -> R.string.notification_title_ongoing
            ReminderMode.DISMISSIBLE_TEST -> R.string.notification_title_dismissible
        }
        val textRes = when (mode) {
            ReminderMode.ONGOING -> R.string.notification_text_ongoing
            ReminderMode.DISMISSIBLE_TEST -> R.string.notification_text_dismissible
        }

        val notification = NotificationCompat.Builder(context, ReminderContract.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pillring)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(textRes))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(textRes)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(mode == ReminderMode.ONGOING)
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(ReminderContract.NOTIFICATION_ID, notification)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
