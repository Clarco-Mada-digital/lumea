package net.mada.lumea.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mada.lumea.data.db.DailyLogEntity
import net.mada.lumea.data.db.HabitEntity
import net.mada.lumea.data.repo.JournalRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class HabitStreak(val habit: HabitEntity, val doneToday: Boolean, val streak: Int, val last30: Int)

data class JournalUiState(
    val entries: List<DailyLogEntity> = emptyList(),
    val habits: List<HabitStreak> = emptyList(),
    val moodAverage: Float = 0f,
    val entriesThisMonth: Int = 0,
    val currentJournalStreak: Int = 0,
)

class JournalViewModel(private val repo: JournalRepository) : ViewModel() {

    val state: StateFlow<JournalUiState> = combine(
        repo.observeEntries(),
        repo.observeHabits(),
        repo.observeChecksBetween(LocalDate.now().minusDays(60), LocalDate.now()),
    ) { entries, habits, checks ->
        val today = LocalDate.now()
        val checksByHabit = checks.groupBy { it.habitId }.mapValues { (_, v) -> v.map { it.date }.toSet() }

        JournalUiState(
            entries = entries,
            habits = habits.map { habit ->
                val dates = checksByHabit[habit.id].orEmpty()
                HabitStreak(
                    habit = habit,
                    doneToday = today in dates,
                    streak = streakLength(dates, today),
                    last30 = dates.count { ChronoUnit.DAYS.between(it, today) in 0..29 },
                )
            },
            moodAverage = entries.filter { it.mood > 0 }.take(30).map { it.mood }.average()
                .takeIf { !it.isNaN() }?.toFloat() ?: 0f,
            entriesThisMonth = entries.count {
                it.date.year == today.year && it.date.month == today.month && it.journal.isNotBlank()
            },
            currentJournalStreak = streakLength(
                entries.filter { it.journal.isNotBlank() }.map { it.date }.toSet(),
                today,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())

    fun toggleHabitToday(habitId: Long, checked: Boolean) = viewModelScope.launch {
        repo.toggleHabit(habitId, LocalDate.now(), checked)
    }
}

/**
 * Longueur de la série en cours. On tolère que le jour même ne soit pas encore fait :
 * une série ne doit pas « casser » à minuit avant d'avoir eu sa chance.
 */
internal fun streakLength(dates: Set<LocalDate>, today: LocalDate): Int {
    var cursor = if (today in dates) today else today.minusDays(1)
    var count = 0
    while (cursor in dates) {
        count++
        cursor = cursor.minusDays(1)
    }
    return count
}
