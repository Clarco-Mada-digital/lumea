package net.mada.lumea.ui.cycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.mada.lumea.data.prefs.SettingsRepository
import net.mada.lumea.data.repo.CycleRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class SetupStep { LAST_PERIOD, PERIOD_LENGTH, PREVIOUS_PERIOD, SUMMARY }

data class CycleSetupState(
    val step: SetupStep = SetupStep.LAST_PERIOD,
    val lastStart: LocalDate? = null,
    val periodLength: Int = 5,
    val previousStart: LocalDate? = null,
) {
    /**
     * Longueur du cycle déduite de l'écart entre les deux débuts de règles.
     * Null tant qu'on n'a pas deux dates plausibles : mieux vaut assumer 28 jours
     * que d'afficher un cycle de 3 ou de 90 jours.
     */
    val computedCycleLength: Int?
        get() {
            val last = lastStart ?: return null
            val previous = previousStart ?: return null
            val gap = ChronoUnit.DAYS.between(previous, last).toInt()
            return gap.takeIf { it in 20..45 }
        }
}

class CycleSetupViewModel(
    private val cycleRepo: CycleRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CycleSetupState())
    val state: StateFlow<CycleSetupState> = _state.asStateFlow()

    fun setLastStart(date: LocalDate) {
        val current = _state.value
        /*
         * Changer les dernières règles peut rendre les précédentes absurdes :
         * si elles se retrouvent après — ou à moins de dix jours — la longueur de
         * cycle déduite n'aurait aucun sens. On efface plutôt que de calculer
         * n'importe quoi en silence.
         */
        val previousStillValid = current.previousStart?.let {
            it.isBefore(date.minusDays(9))
        } ?: true

        _state.value = current.copy(
            lastStart = date,
            previousStart = if (previousStillValid) current.previousStart else null,
        )
    }

    fun setPeriodLength(days: Int) {
        _state.value = _state.value.copy(periodLength = days)
    }

    fun setPreviousStart(date: LocalDate?) {
        _state.value = _state.value.copy(previousStart = date)
    }

    fun canContinue(): Boolean = when (_state.value.step) {
        SetupStep.LAST_PERIOD -> _state.value.lastStart != null
        // On peut avancer sans date précédente : « je ne sais pas » est une réponse valable.
        else -> true
    }

    fun next() {
        val current = _state.value.step
        val nextStep = SetupStep.entries.getOrNull(current.ordinal + 1) ?: return
        _state.value = _state.value.copy(step = nextStep)
    }

    /** Renvoie false s'il n'y a plus d'étape en arrière : l'écran doit alors se fermer. */
    fun back(): Boolean {
        val previous = SetupStep.entries.getOrNull(_state.value.step.ordinal - 1) ?: return false
        _state.value = _state.value.copy(step = previous)
        return true
    }

    fun finish(onDone: () -> Unit) {
        val current = _state.value
        val last = current.lastStart ?: return onDone()

        viewModelScope.launch {
            settingsRepo.setPeriodLength(current.periodLength)
            current.computedCycleLength?.let { settingsRepo.setCycleLength(it) }

            // On enregistre les règles précédentes aussi : deux cycles connus valent
            // mieux qu'un réglage, et l'app affinera ses moyennes toute seule ensuite.
            current.previousStart?.let { previous ->
                cycleRepo.savePeriod(previous, previous.plusDays((current.periodLength - 1).toLong()))
            }
            cycleRepo.savePeriod(last, last.plusDays((current.periodLength - 1).toLong()))
            onDone()
        }
    }
}
