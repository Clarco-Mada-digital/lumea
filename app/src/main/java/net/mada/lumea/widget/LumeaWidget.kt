package net.mada.lumea.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.mada.lumea.MainActivity
import net.mada.lumea.R
import net.mada.lumea.di.container
import net.mada.lumea.domain.cycle.CycleEngine
import net.mada.lumea.domain.pregnancy.postpartumProgress
import net.mada.lumea.domain.pregnancy.pregnancyProgress

/**
 * Le widget d'écran d'accueil : où on en est, sans ouvrir l'application.
 *
 * Deux règles de conception, toutes deux dictées par le public visé.
 *
 * **Discrétion.** Un widget qui affiche « grossesse : 12 semaines » sur l'écran
 * d'accueil trahit sa propriétaire auprès de quiconque regarde son téléphone —
 * un parent, un conjoint, une camarade. Le widget n'emploie donc jamais les mots
 * « grossesse », « règles » ou « cycle » en clair : un chiffre, une unité
 * abrégée, et c'est tout. Un mode encore plus discret masque même le chiffre.
 *
 * **Honnêteté.** Il n'affiche rien plutôt que d'afficher une valeur périmée :
 * sans données, il invite simplement à ouvrir l'app.
 */
class LumeaWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        refresh(context, manager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            refresh(context, manager, manager.getAppWidgetIds(component(context)))
        }
    }

    /**
     * Lit l'état courant et redessine.
     *
     * La lecture touche la base chiffrée, donc elle part sur un fil
     * d'arrière-plan et le widget est mis à jour ensuite. `goAsync` garde le
     * receiver en vie le temps de la requête.
     */
    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            val state = runCatching { readState(context) }.getOrNull()
            val views = render(context, state)
            ids.forEach { manager.updateAppWidget(it, views) }
            pending.finish()
        }
    }

    private suspend fun readState(context: Context): WidgetState {
        val app = context.applicationContext.container()
        val settings = app.settings.settings.first()

        if (settings.widgetDiscreet) return WidgetState.Discreet

        val ongoing = app.pregnancy.observeOngoing().first()
        if (ongoing != null) {
            if (ongoing.status == "POSTPARTUM") {
                val birth = ongoing.birthDate
                if (birth != null) {
                    val after = postpartumProgress(birth)
                    return WidgetState.Value(
                        value = if (after.monthsSince < 1) "${after.weeksSince} sem."
                        else "${after.monthsSince} mois",
                        label = "DEPUIS LA NAISSANCE",
                        hint = "",
                    )
                }
            } else {
                val start = ongoing.lastPeriodStart
                if (start != null) {
                    val progress = pregnancyProgress(start)
                    return WidgetState.Value(
                        value = "${progress.weeks} SA",
                        label = "SEMAINES",
                        hint = "Terme : ${progress.dueDate.format(SHORT_DATE)}",
                    )
                }
            }
        }

        val engine = CycleEngine(
            settings.cycleLength,
            settings.periodLength,
            settings.lutealLength,
        )
        val insight = engine.insight(app.cycle.allPeriods())
        if (insight.lastPeriodStart == null) return WidgetState.Empty

        return WidgetState.Value(
            value = "J${insight.dayOfCycle}",
            label = "JOUR DU CYCLE",
            hint = when {
                insight.currentPeriodStart != null -> "En cours"
                insight.isLate -> "Retard de ${insight.daysLate} j"
                insight.daysUntilNextPeriod != null ->
                    "Dans ${insight.daysUntilNextPeriod} j"
                else -> ""
            },
        )
    }

    private fun render(context: Context, state: WidgetState?): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_cycle).apply {
            when (state) {
                is WidgetState.Value -> {
                    setTextViewText(R.id.widget_value, state.value)
                    setTextViewText(R.id.widget_label, state.label)
                    setTextViewText(R.id.widget_hint, state.hint)
                }
                WidgetState.Discreet -> {
                    setTextViewText(R.id.widget_value, "•")
                    setTextViewText(R.id.widget_label, "LUMEA")
                    setTextViewText(R.id.widget_hint, "")
                }
                else -> {
                    setTextViewText(R.id.widget_value, "—")
                    setTextViewText(R.id.widget_label, "LUMEA")
                    setTextViewText(R.id.widget_hint, "Touche pour commencer")
                }
            }

            setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        }

    private sealed interface WidgetState {
        data class Value(val value: String, val label: String, val hint: String) : WidgetState
        data object Discreet : WidgetState
        data object Empty : WidgetState
    }

    companion object {
        private const val ACTION_REFRESH = "net.mada.lumea.WIDGET_REFRESH"
        private val SHORT_DATE =
            java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.FRENCH)

        private fun component(context: Context) =
            ComponentName(context, LumeaWidget::class.java)

        /**
         * À appeler quand les données changent : enregistrement de règles,
         * ouverture ou fermeture d'un suivi, bascule du mode discret.
         *
         * Sans cet appel, le widget garderait sa dernière valeur jusqu'au
         * prochain redémarrage — et afficherait un jour de cycle périmé, ce qui
         * est pire que de ne rien afficher.
         */
        fun refreshAll(context: Context) {
            context.sendBroadcast(
                Intent(context, LumeaWidget::class.java).setAction(ACTION_REFRESH)
            )
        }
    }
}
