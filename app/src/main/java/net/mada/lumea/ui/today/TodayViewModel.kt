package net.mada.lumea.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mada.lumea.data.db.DailyLogEntity
import net.mada.lumea.data.db.EventEntity
import net.mada.lumea.data.db.NoteEntity
import net.mada.lumea.data.prefs.SettingsRepository
import net.mada.lumea.data.repo.CycleRepository
import net.mada.lumea.data.repo.EventRepository
import net.mada.lumea.data.repo.JournalRepository
import net.mada.lumea.data.repo.NoteRepository
import net.mada.lumea.domain.cycle.CycleEngine
import net.mada.lumea.domain.cycle.CycleInsight
import net.mada.lumea.ui.journal.HabitStreak
import java.time.LocalDate

data class TodayUiState(
    val insight: CycleInsight? = null,
    val events: List<EventEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val habits: List<HabitStreak> = emptyList(),
    val log: DailyLogEntity? = null,
)

class TodayViewModel(
    notesRepo: NoteRepository,
    private val eventsRepo: EventRepository,
    cycleRepo: CycleRepository,
    private val journalRepo: JournalRepository,
    settingsRepo: SettingsRepository,
) : ViewModel() {

    private val today = LocalDate.now()

    private val cycleFlow = combine(cycleRepo.observePeriods(), settingsRepo.settings) { periods, settings ->
        CycleEngine(settings.cycleLength, settings.periodLength, settings.lutealLength).insight(periods)
    }

    private val habitsFlow = combine(
        journalRepo.observeHabits(),
        journalRepo.observeChecks(today),
    ) { habits, checks ->
        val done = checks.map { it.habitId }.toSet()
        habits.map { HabitStreak(it, it.id in done, 0, 0) }
    }

    val state: StateFlow<TodayUiState> = combine(
        cycleFlow,
        eventsRepo.observeForDay(today),
        // Quelques notes récentes suffisent sur l'accueil : les épinglées remontent d'elles-mêmes.
        notesRepo.observeNotes("", null, false, false).map { it.take(4) },
        habitsFlow,
        cycleRepo.observeLog(today),
    ) { insight, events, notes, habits, log ->
        TodayUiState(insight, events, notes, habits, log)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun toggleEventDone(event: EventEntity) = viewModelScope.launch {
        eventsRepo.setDone(event.id, !event.isDone)
    }

    fun toggleHabit(habitId: Long, checked: Boolean) = viewModelScope.launch {
        journalRepo.toggleHabit(habitId, today, checked)
    }

    fun openLessons() = Unit
}
