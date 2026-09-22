package net.mada.lumea.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.mada.lumea.di.container

/** Les alarmes ne survivent pas au redémarrage : on les repose toutes. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext.container()
                app.events.pendingReminders().forEach { ReminderScheduler.scheduleEvent(context, it) }
                ReminderScheduler.scheduleDailyCheck(context)
                val settings = app.settings.settings.first()
                ReminderScheduler.scheduleJournalReminder(
                    context,
                    settings.journalReminderMinute.takeIf { settings.journalReminder },
                )
            } finally {
                pending.finish()
            }
        }
    }
}
