package net.mada.lumea.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.mada.lumea.di.container
import net.mada.lumea.domain.cycle.CycleEngine
import net.mada.lumea.domain.pregnancy.ALL_CARE_ACTS
import net.mada.lumea.domain.pregnancy.CareStatus
import net.mada.lumea.domain.pregnancy.INFANT_VACCINES
import net.mada.lumea.domain.pregnancy.POSTNATAL_VISITS
import net.mada.lumea.domain.pregnancy.nextCareAction
import net.mada.lumea.domain.pregnancy.postpartumProgress
import net.mada.lumea.domain.pregnancy.pregnancyProgress
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderScheduler.ACTION_EVENT -> {
                val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, 0L)
                Notifications.show(
                    context,
                    Notifications.CHANNEL_EVENTS,
                    id.toInt(),
                    intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Rappel",
                    intent.getStringExtra(ReminderScheduler.EXTRA_TEXT) ?: "",
                )
            }

            ReminderScheduler.ACTION_JOURNAL -> {
                Notifications.show(
                    context,
                    Notifications.CHANNEL_JOURNAL,
                    JOURNAL_NOTIF_ID,
                    "Ta journée en trois lignes ?",
                    "Une humeur, une gratitude, et c'est déjà beaucoup.",
                )
                ReminderScheduler.scheduleJournalReminder(
                    context,
                    runBlockingMinute(context),
                )
            }

            ReminderScheduler.ACTION_DAILY -> handleDailyCheck(context)

            else -> Unit
        }
    }

    /**
     * Chaque matin : replanifie les alarmes d'événements (les alarmes n'étant posées
     * qu'à court terme) et prévient si les règles approchent.
     */
    private fun handleDailyCheck(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext.container()
                app.events.pendingReminders().forEach { ReminderScheduler.scheduleEvent(context, it) }

                val settings = app.settings.settings.first()
                if (settings.periodReminder) {
                    val engine = CycleEngine(
                        settings.cycleLength,
                        settings.periodLength,
                        settings.lutealLength,
                    )
                    val insight = engine.insight(app.cycle.allPeriods())
                    val today = LocalDate.now()
                    val next = insight.nextPeriodStart
                    if (next != null) {
                        val days = ChronoUnit.DAYS.between(today, next).toInt()
                        if (days == settings.periodReminderDaysBefore) {
                            Notifications.show(
                                context,
                                Notifications.CHANNEL_CYCLE,
                                CYCLE_NOTIF_ID,
                                if (days == 0) "Tes règles sont prévues aujourd'hui"
                                else "Tes règles arrivent dans $days jour${if (days > 1) "s" else ""}",
                                "Pense à ce qu'il te faut. Prévision indicative, basée sur tes derniers cycles.",
                            )
                        }
                    }
                    if (settings.fertileReminder) {
                        val ovulation = insight.ovulationDate
                        if (ovulation != null && ChronoUnit.DAYS.between(today, ovulation).toInt() == 1) {
                            Notifications.show(
                                context,
                                Notifications.CHANNEL_CYCLE,
                                CYCLE_NOTIF_ID + 1,
                                "Ovulation estimée demain",
                                "Tu es dans ta fenêtre fertile estimée.",
                            )
                        }
                    }
                }
                notifyLateCare(context, app)
                // Le jour de cycle change chaque nuit : le widget suit.
                net.mada.lumea.widget.LumeaWidget.refreshAll(context)

                // Une copie par semaine, en silence. Voir [AutoBackup] : elle
                // protège de l'effacement, pas du téléphone perdu.
                app.autoBackup.runIfDue()
                remindToCopyBackup(context, app)
            } finally {
                ReminderScheduler.scheduleDailyCheck(context)
                pending.finish()
            }
        }
    }

    /**
     * Relance sur les rendez-vous de suivi manqués.
     *
     * C'est le trou que le carnet ne comblait pas : on pouvait cocher, mais rien
     * ne rappelait une CPN ratée. Or à Madagascar moins d'une femme sur deux
     * atteint la quatrième consultation dans certains districts — et l'abandon se
     * joue précisément là, sur un rendez-vous manqué que personne ne relance.
     *
     * Une seule notification, pas une par acte : on rappelle qu'il y a du retard,
     * le détail est dans le carnet. Et rien du tout quand tout est à jour.
     */
    private suspend fun notifyLateCare(context: Context, app: net.mada.lumea.di.AppContainer) {
        val ongoing = app.pregnancy.observeOngoing().first() ?: return
        val done = app.pregnancy.observeCare(ongoing.id).first().map { it.code }.toSet()

        val next = if (ongoing.status == "POSTPARTUM") {
            val birth = ongoing.birthDate ?: return
            nextCareAction(
                anchor = birth,
                acts = POSTNATAL_VISITS + INFANT_VACCINES,
                doneCodes = done,
                currentWeek = postpartumProgress(birth).weeksSince,
            )
        } else {
            val start = ongoing.lastPeriodStart ?: return
            nextCareAction(
                anchor = start,
                acts = ALL_CARE_ACTS,
                doneCodes = done,
                currentWeek = pregnancyProgress(start).weeks,
            )
        } ?: return

        if (next.status != CareStatus.EN_RETARD) return

        Notifications.show(
            context,
            Notifications.CHANNEL_CYCLE,
            CARE_NOTIF_ID,
            if (next.lateCount > 1) "${next.lateCount} rendez-vous en retard"
            else "Un rendez-vous en retard",
            next.act.title + " — un rendez-vous manqué se rattrape, va au CSB dès que tu peux.",
        )
    }

    /**
     * Rappelle, une fois par mois, de mettre une sauvegarde à l'abri.
     *
     * La copie automatique vit dans le téléphone : elle ne survit pas à sa perte.
     * Sortir un fichier de l'appareil est le seul geste que l'application ne peut
     * pas faire à la place de quelqu'un — d'où ce rappel, et pas plus souvent que
     * mensuel pour qu'il garde du poids.
     */
    private suspend fun remindToCopyBackup(context: Context, app: net.mada.lumea.di.AppContainer) {
        val today = LocalDate.now()
        if (today.dayOfMonth != 1) return
        val hasData = app.cycle.allPeriods().isNotEmpty()
        if (!hasData) return

        Notifications.show(
            context,
            Notifications.CHANNEL_JOURNAL,
            BACKUP_NOTIF_ID,
            "Mets une sauvegarde à l'abri",
            "Lumea en garde une sur ce téléphone — mais elle partirait avec lui. " +
                "Exporte-la sur une carte SD ou envoie-la-toi, depuis les réglages.",
        )
    }

    /** L'heure du rappel de journal, relue pour reprogrammer le lendemain. */
    private fun runBlockingMinute(context: Context): Int = kotlinx.coroutines.runBlocking {
        context.applicationContext.container().settings.settings.first().journalReminderMinute
    }

    private companion object {
        const val CYCLE_NOTIF_ID = 900_001
        const val JOURNAL_NOTIF_ID = 900_010
        const val CARE_NOTIF_ID = 900_020
        const val BACKUP_NOTIF_ID = 900_030
    }
}
