package net.mada.lumea

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.mada.lumea.di.AppContainer
import net.mada.lumea.notif.Notifications
import net.mada.lumea.notif.ReminderScheduler

class LumeaApp : Application() {

    lateinit var container: AppContainer
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)

        scope.launch {
            ReminderScheduler.scheduleDailyCheck(this@LumeaApp)
            val settings = container.settings.settings.first()
            ReminderScheduler.scheduleJournalReminder(
                this@LumeaApp,
                settings.journalReminderMinute.takeIf { settings.journalReminder },
            )
        }
    }
}
