package net.mada.lumea.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mada.lumea.data.db.EventEntity
import net.mada.lumea.data.repo.EventRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModel(private val repo: EventRepository) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _selectedDay = MutableStateFlow(LocalDate.now())
    val selectedDay: StateFlow<LocalDate> = _selectedDay.asStateFlow()

    /** Tous les événements du mois affiché, regroupés par jour pour piquer le calendrier. */
    val eventsByDay: StateFlow<Map<LocalDate, List<EventEntity>>> = _month
        .flatMapLatest { repo.observeForMonth(it.atDay(1)) }
        .map { events -> events.groupBy { it.startAt.toLocalDate() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val dayEvents: StateFlow<List<EventEntity>> = _selectedDay
        .flatMapLatest { repo.observeForDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setMonth(value: YearMonth) { _month.value = value }

    fun selectDay(date: LocalDate) {
        _selectedDay.value = date
        if (YearMonth.from(date) != _month.value) _month.value = YearMonth.from(date)
    }

    fun toggleDone(event: EventEntity) = viewModelScope.launch {
        repo.setDone(event.id, !event.isDone)
    }
}

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
