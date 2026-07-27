package com.tripro.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tripro.app.MainActivity
import com.tripro.app.R
import kotlin.random.Random

object NotificationHelper {
    const val CHANNEL_ID = "trip_updates"
    const val EXTRA_TRIP_ID = "tripId"
    const val EXTRA_DATE = "date"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Builds and shows a notification whose tap action reopens MainActivity with tripId/
     * date extras — TriProNavGraph reads those (via PendingDeepLink) and navigates straight
     * to the relevant trip or day. Used both for foreground FCM messages (which Android does
     * NOT auto-display — see TriProMessagingService) and could be reused for any other
     * local notification the app wants to show.
     */
    fun showNotification(context: Context, title: String, body: String, tripId: String?, date: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            tripId?.let { putExtra(EXTRA_TRIP_ID, it) }
            date?.let { putExtra(EXTRA_DATE, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            NotificationManagerCompat.from(context).notify(Random.nextInt(), notification)
        }
        // If the permission isn't granted on API 33+, we silently skip showing it — there's
        // no good in-context place to prompt for it from inside a background FCM callback.
        // The permission is requested proactively right after sign-in; see MainActivity.
    }
}
