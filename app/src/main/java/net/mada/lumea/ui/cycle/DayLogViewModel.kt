package net.mada.lumea.ui.cycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mada.lumea.data.db.DailyLogEntity
import net.mada.lumea.data.db.HabitEntity
import net.mada.lumea.data.prefs.SettingsRepository
import net.mada.lumea.data.repo.CycleRepository
import net.mada.lumea.data.repo.JournalRepository
import java.time.LocalDate

/** Les symptômes proposés par défaut, regroupés par famille. */
val symptomGroups: List<Pair<String, List<String>>> = listOf(
    "Corps" to listOf("Crampes", "Mal de dos", "Maux de tête", "Seins sensibles", "Ballonnements", "Nausées", "Fatigue", "Acné"),
    "Humeur" to listOf("Irritable", "Anxieuse", "Sensible", "Calme", "Motivée", "Joyeuse"),
    "Autres" to listOf("Fringales", "Insomnie", "Libido haute", "Libido basse", "Pertes"),
)

data class DayLogState(
    val date: LocalDate = LocalDate.now(),
    val flow: Int = 0,
    val symptoms: Set<String> = emptySet(),
    val mood: Int = 0,
    val energy: Int = 0,
    val journal: String = "",
    val gratitude: String = "",
    val waterGlasses: Int = 0,
    val sleepHours: Float = 0f,
    val isPeriodDay: Boolean = false,
    val isLocked: Boolean = false,
)

class DayLogViewModel(
    private val cycleRepo: CycleRepository,
    private val journalRepo: JournalRepository,
    private val settingsRepo: SettingsRepository,
    val date: LocalDate,
) : ViewModel() {

    private val _state = MutableStateFlow(DayLogState(date = date))
    val state: StateFlow<DayLogState> = _state.asStateFlow()

    val habits: StateFlow<List<HabitEntity>> = journalRepo.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val checkedHabits: StateFlow<Set<Long>> = journalRepo.observeChecks(date)
        .map { checks -> checks.map { it.habitId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            val log = cycleRepo.log(date)
            val periods = cycleRepo.allPeriods()
            val periodLength = settingsRepo.settings.first().periodLength
            val isPeriod = periods.any { p ->
                val end = p.endDate ?: p.startDate.plusDays((periodLength - 1).toLong())
                !date.isBefore(p.startDate) && !date.isAfter(end)
            }
            _state.value = DayLogState(
                date = date,
                flow = log?.flow ?: 0,
                symptoms = log?.symptoms?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
                mood = log?.mood ?: 0,
                energy = log?.energy ?: 0,
                journal = log?.journal.orEmpty(),
                gratitude = log?.gratitude.orEmpty(),
                waterGlasses = log?.waterGlasses ?: 0,
                sleepHours = log?.sleepHours ?: 0f,
                isPeriodDay = isPeriod,
                isLocked = log?.isLocked ?: false,
            )
        }
    }

    /**
     * Choisir un flux non nul revient à dire « j'ai mes règles ce jour-là » :
     * on synchronise le calendrier pour éviter deux gestes séparés.
     */
    fun setFlow(value: Int) {
        val current = _state.value
        val newFlow = if (current.flow == value) 0 else value
        _state.value = current.copy(flow = newFlow)
        val shouldBePeriod = newFlow > 0
        if (shouldBePeriod != current.isPeriodDay) {
            viewModelScope.launch {
                cycleRepo.togglePeriodDay(date, settingsRepo.settings.first().periodLength)
                _state.value = _state.value.copy(isPeriodDay = shouldBePeriod)
            }
        }
    }

    fun toggleSymptom(symptom: String) {
        val current = _state.value.symptoms
        _state.value = _state.value.copy(
            symptoms = if (symptom in current) current - symptom else current + symptom
        )
    }

    fun setMood(value: Int) { _state.value = _state.value.copy(mood = if (_state.value.mood == value) 0 else value) }
    fun setEnergy(value: Int) { _state.value = _state.value.copy(energy = if (_state.value.energy == value) 0 else value) }
    fun setJournal(value: String) { _state.value = _state.value.copy(journal = value) }
    fun setGratitude(value: String) { _state.value = _state.value.copy(gratitude = value) }
    fun setWater(value: Int) { _state.value = _state.value.copy(waterGlasses = value.coerceIn(0, 12)) }
    fun setSleep(value: Float) { _state.value = _state.value.copy(sleepHours = value.coerceIn(0f, 14f)) }
    fun toggleLock() { _state.value = _state.value.copy(isLocked = !_state.value.isLocked) }

    fun toggleHabit(habitId: Long, checked: Boolean) = viewModelScope.launch {
        journalRepo.toggleHabit(habitId, date, checked)
    }

    fun save() {
        val current = _state.value
        viewModelScope.launch {
            cycleRepo.saveLog(
                DailyLogEntity(
                    date = current.date,
                    flow = current.flow,
                    symptoms = current.symptoms.joinToString(","),
                    mood = current.mood,
                    energy = current.energy,
                    journal = current.journal,
                    gratitude = current.gratitude,
                    waterGlasses = current.waterGlasses,
                    sleepHours = current.sleepHours,
                    // Sans cette ligne, enregistrer une journée protégée
                    // remettrait le verrou à zéro.
                    isLocked = current.isLocked,
                )
            )
        }
    }
}
