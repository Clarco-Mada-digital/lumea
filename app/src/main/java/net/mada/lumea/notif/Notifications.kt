package net.mada.lumea.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import net.mada.lumea.MainActivity
import net.mada.lumea.R

object Notifications {

    const val CHANNEL_EVENTS = "events"
    const val CHANNEL_CYCLE = "cycle"
    const val CHANNEL_JOURNAL = "journal"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            Triple(CHANNEL_EVENTS, R.string.channel_events_name, R.string.channel_events_desc),
            Triple(CHANNEL_CYCLE, R.string.channel_cycle_name, R.string.channel_cycle_desc),
            Triple(CHANNEL_JOURNAL, R.string.channel_journal_name, R.string.channel_journal_desc),
        ).forEach { (id, name, desc) ->
            manager.createNotificationChannel(
                NotificationChannel(id, context.getString(name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(desc)
                }
            )
        }
    }

    fun show(context: Context, channel: String, id: Int, title: String, text: String) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
