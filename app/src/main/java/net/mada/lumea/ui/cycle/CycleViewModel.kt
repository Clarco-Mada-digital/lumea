package net.mada.lumea.ui.cycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mada.lumea.data.db.DailyLogEntity
import net.mada.lumea.data.prefs.SettingsRepository
import net.mada.lumea.data.repo.CycleRepository
import net.mada.lumea.domain.cycle.CycleEngine
import net.mada.lumea.domain.cycle.CycleInsight
import net.mada.lumea.domain.cycle.CyclePhase
import net.mada.lumea.domain.cycle.DayPrediction
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class CycleUiState(
    val insight: CycleInsight? = null,
    val predictions: Map<LocalDate, DayPrediction> = emptyMap(),
    val logs: Map<LocalDate, DailyLogEntity> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CycleViewModel(
    private val repo: CycleRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    // On déborde d'une semaine de part et d'autre : la grille affiche les jours
    // des mois voisins, ils doivent être peints eux aussi.
    private val logs = _month
        .flatMapLatest { repo.observeLogs(it.atDay(1).minusDays(7), it.atEndOfMonth().plusDays(7)) }
        .map { list -> list.associateBy { it.date } }

    val state: StateFlow<CycleUiState> = combine(
        repo.observePeriods(),
        settingsRepo.settings,
        _month,
        logs,
    ) { periods, settings, month, logsByDate ->
        val engine = CycleEngine(settings.cycleLength, settings.periodLength, settings.lutealLength)
        CycleUiState(
            insight = engine.insight(periods),
            predictions = engine.predictions(
                periods,
                month.atDay(1).minusDays(7),
                month.atEndOfMonth().plusDays(7),
            ),
            logs = logsByDate,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CycleUiState())

    fun setMonth(value: YearMonth) { _month.value = value }

    /**
     * Prévient le widget qu'il affiche peut-être une valeur périmée.
     *
     * Posé ici plutôt que dans le dépôt : le widget a besoin d'un Context, et le
     * dépôt n'en a pas — c'est la couche UI qui en dispose.
     */
    var onDataChanged: () -> Unit = {}

    fun togglePeriodDay(date: LocalDate) = viewModelScope.launch {
        repo.togglePeriodDay(date, settingsRepo.settings.first().periodLength)
        onDataChanged()
    }

    /**
     * Confirme que les règles prévues sont bien arrivées, à la date indiquée.
     *
     * C'est ce qui fait avancer le calcul : sans ce point de repère, l'app
     * continue de prévoir à partir d'un cycle vieux de plusieurs semaines.
     */
    fun confirmPeriodStarted(date: LocalDate) = viewModelScope.launch {
        repo.startPeriod(date)
        _feedback.value = cycleReport(date)
        onDataChanged()
    }

    /** Les règles sont finies : c'est ce qui donne enfin leur durée réelle. */
    fun endPeriod(date: LocalDate = LocalDate.now()) = viewModelScope.launch {
        repo.endCurrentPeriod(date)
        _feedback.value = periodReport(date)
        onDataChanged()
    }

    /** Déclarées finies trop tôt : on rouvre. */
    fun reopenPeriod() = viewModelScope.launch { repo.reopenLastPeriod() }

    /**
     * Le message qui suit une confirmation : « plus long que d'habitude », « pile
     * dans la moyenne ». C'est ce qui transforme une case cochée en information.
     */
    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()
    fun clearFeedback() { _feedback.value = null }

    /** Compare le cycle qui vient de se terminer à la moyenne connue. */
    private suspend fun cycleReport(newStart: LocalDate): String {
        val insight = state.value.insight ?: return "Premier jour enregistré"
        val previousStart = insight.lastPeriodStart ?: return "Premier jour enregistré"
        val actual = ChronoUnit.DAYS.between(previousStart, newStart).toInt()
        if (actual !in 15..60) return "Premier jour enregistré"
        val expected = insight.averageCycleLength
        val delta = actual - expected
        return when {
            insight.isEstimate -> "Cycle de $actual jours enregistré. Encore un ou deux et les prévisions s'affinent."
            delta == 0 -> "Cycle de $actual jours : pile ta moyenne."
            delta > 0 -> "Cycle de $actual jours, soit $delta de plus que ta moyenne ($expected j)."
            else -> "Cycle de $actual jours, soit ${-delta} de moins que ta moyenne ($expected j)."
        }
    }

    /** Compare la durée des règles qui viennent de finir à la durée habituelle. */
    private suspend fun periodReport(endDate: LocalDate): String {
        val insight = state.value.insight ?: return "Règles terminées"
        val start = insight.currentPeriodStart ?: return "Règles terminées"
        val actual = ChronoUnit.DAYS.between(start, endDate).toInt() + 1
        val expected = insight.averagePeriodLength
        val delta = actual - expected
        return when {
            delta == 0 -> "Règles de $actual jours : ta durée habituelle."
            delta > 0 -> "Règles de $actual jours, soit $delta de plus que d'habitude ($expected j)."
            else -> "Règles de $actual jours, soit ${-delta} de moins que d'habitude ($expected j)."
        }
    }
}

/** Résumé textuel affiché en haut de l'écran cycle. */
fun CycleInsight.summaryLine(): String = when {
    lastPeriodStart == null -> "Marque le premier jour de tes règles pour démarrer"
    currentPeriodStart != null -> "Jour ${currentPeriodDay ?: 1} de tes règles"
    phase == CyclePhase.MENSTRUATION -> "Jour $dayOfCycle de ton cycle · règles"
    isLate -> "Jour $dayOfCycle · règles en retard de $daysLate ${jours(daysLate)}"
    daysUntilNextPeriod == 0 -> "Jour $dayOfCycle · règles attendues aujourd'hui"
    daysUntilNextPeriod != null -> "Jour $dayOfCycle · règles dans $daysUntilNextPeriod ${jours(daysUntilNextPeriod)}"
    else -> "Jour $dayOfCycle de ton cycle"
}

private fun jours(n: Int) = if (n > 1) "jours" else "jour"
