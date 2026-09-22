package net.mada.lumea.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import net.mada.lumea.data.db.EventEntity
import net.mada.lumea.domain.agenda.Recurrence
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Programme les rappels avec AlarmManager.
 *
 * Deux familles d'alarmes : une par événement à venir (au plus 60, replanifiées à
 * chaque modification et au démarrage), et une alarme quotidienne qui recalcule les
 * prévisions de cycle et replanifie la suite — c'est elle qui rend le système
 * auto-réparant si le téléphone a été éteint longtemps.
 */
object ReminderScheduler {

    const val ACTION_EVENT = "net.mada.lumea.EVENT"
    const val ACTION_DAILY = "net.mada.lumea.DAILY"
    const val ACTION_JOURNAL = "net.mada.lumea.JOURNAL"
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"
    const val EXTRA_ID = "id"

    private const val DAILY_REQUEST = 1_000_001
    private const val JOURNAL_REQUEST = 1_000_002
    private val DAILY_CHECK_TIME: LocalTime = LocalTime.of(8, 30)

    fun scheduleEvent(context: Context, event: EventEntity) {
        val minutes = event.reminderMinutes ?: return cancelEvent(context, event.id)
        val now = System.currentTimeMillis()
        // Pour une série, on ne pose l'alarme que sur la prochaine occurrence ;
        // la vérification quotidienne reposera la suivante.
        val startAt = if (event.repeat == "NONE") event.startAt
        else Recurrence.nextOccurrence(event, now) ?: return
        val triggerAt = startAt - minutes * 60_000L
        if (triggerAt <= now) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_EVENT
            putExtra(EXTRA_ID, event.id)
            putExtra(EXTRA_TITLE, event.title)
            putExtra(EXTRA_TEXT, reminderText(event, minutes))
        }
        setAlarm(context, triggerAt, pending(context, event.id.toInt(), intent))
    }

    fun cancelEvent(context: Context, eventId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = ACTION_EVENT }
        alarmManager(context).cancel(pending(context, eventId.toInt(), intent))
    }

    fun scheduleDailyCheck(context: Context) {
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = ACTION_DAILY }
        setAlarm(context, nextOccurrence(DAILY_CHECK_TIME), pending(context, DAILY_REQUEST, intent))
    }

    fun scheduleJournalReminder(context: Context, minuteOfDay: Int?) {
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = ACTION_JOURNAL }
        val pendingIntent = pending(context, JOURNAL_REQUEST, intent)
        if (minuteOfDay == null) {
            alarmManager(context).cancel(pendingIntent)
            return
        }
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        setAlarm(context, nextOccurrence(time), pendingIntent)
    }

    private fun reminderText(event: EventEntity, minutes: Int): String = when {
        event.allDay -> "Aujourd'hui" + event.location.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
        minutes == 0 -> "Ça commence maintenant"
        minutes < 60 -> "Dans $minutes min"
        minutes % 60 == 0 -> "Dans ${minutes / 60} h"
        else -> "Dans ${minutes / 60} h ${minutes % 60}"
    }

    private fun nextOccurrence(time: LocalTime): Long {
        val zone = ZoneId.systemDefault()
        var next = LocalDate.now().atTime(time).atZone(zone)
        if (next.toInstant().toEpochMilli() <= System.currentTimeMillis()) next = next.plusDays(1)
        return next.toInstant().toEpochMilli()
    }

    private fun setAlarm(context: Context, triggerAt: Long, pendingIntent: PendingIntent) {
        val manager = alarmManager(context)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (exactAllowed) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            // Sans la permission d'alarme exacte, on accepte une fenêtre de 10 minutes
            // plutôt que d'envoyer l'utilisatrice dans les réglages système.
            manager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 10 * 60_000L, pendingIntent)
        }
    }

    private fun pending(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)
}
