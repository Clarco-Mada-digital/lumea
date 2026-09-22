package net.mada.lumea.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.mada.lumea.data.db.EventEntity
import net.mada.lumea.data.repo.EventRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class EventEditorState(
    val id: Long = 0,
    val title: String = "",
    val notes: String = "",
    val location: String = "",
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.now().withMinute(0).plusHours(1),
    val endTime: LocalTime = LocalTime.now().withMinute(0).plusHours(2),
    val allDay: Boolean = false,
    val colorIndex: Int = 0,
    val reminderMinutes: Int? = 15,
    val repeat: String = "NONE",
    val loaded: Boolean = false,
)

class EventEditorViewModel(
    private val repo: EventRepository,
    private val eventId: Long,
    initialDate: LocalDate?,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EventEditorState(date = initialDate ?: LocalDate.now(), loaded = eventId == 0L)
    )
    val state: StateFlow<EventEditorState> = _state.asStateFlow()

    init {
        if (eventId != 0L) {
            viewModelScope.launch {
                repo.get(eventId)?.let { event ->
                    val zone = ZoneId.systemDefault()
                    val start = Instant.ofEpochMilli(event.startAt).atZone(zone)
                    val end = Instant.ofEpochMilli(event.endAt).atZone(zone)
                    _state.value = EventEditorState(
                        id = event.id,
                        title = event.title,
                        notes = event.notes,
                        location = event.location,
                        date = start.toLocalDate(),
                        startTime = start.toLocalTime(),
                        endTime = end.toLocalTime(),
                        allDay = event.allDay,
                        colorIndex = event.colorIndex,
                        reminderMinutes = event.reminderMinutes,
                        repeat = event.repeat,
                        loaded = true,
                    )
                }
            }
        }
    }

    fun setTitle(value: String) { _state.value = _state.value.copy(title = value) }
    fun setNotes(value: String) { _state.value = _state.value.copy(notes = value) }
    fun setLocation(value: String) { _state.value = _state.value.copy(location = value) }
    fun setDate(value: LocalDate) { _state.value = _state.value.copy(date = value) }
    fun setColor(index: Int) { _state.value = _state.value.copy(colorIndex = index) }
    fun setReminder(minutes: Int?) { _state.value = _state.value.copy(reminderMinutes = minutes) }
    fun setRepeat(value: String) { _state.value = _state.value.copy(repeat = value) }
    fun setAllDay(value: Boolean) { _state.value = _state.value.copy(allDay = value) }

    fun setStartTime(value: LocalTime) {
        val current = _state.value
        // La fin suit le début pour ne jamais se retrouver avec un événement négatif.
        val end = if (value.isAfter(current.endTime)) value.plusHours(1) else current.endTime
        _state.value = current.copy(startTime = value, endTime = end)
    }

    fun setEndTime(value: LocalTime) {
        val current = _state.value
        if (value.isBefore(current.startTime)) return
        _state.value = current.copy(endTime = value)
    }

    /** Renvoie l'événement enregistré (avec son id) pour que l'écran pose l'alarme. */
    fun save(onSaved: (EventEntity) -> Unit) {
        val current = _state.value
        if (current.title.isBlank()) return
        val zone = ZoneId.systemDefault()
        val startAt = if (current.allDay) current.date.atStartOfDay(zone).toInstant().toEpochMilli()
        else current.date.atTime(current.startTime).atZone(zone).toInstant().toEpochMilli()
        val endAt = if (current.allDay) current.date.atTime(LocalTime.of(23, 59)).atZone(zone).toInstant().toEpochMilli()
        else current.date.atTime(current.endTime).atZone(zone).toInstant().toEpochMilli()

        viewModelScope.launch {
            val entity = EventEntity(
                id = current.id,
                title = current.title.trim(),
                notes = current.notes.trim(),
                location = current.location.trim(),
                startAt = startAt,
                endAt = endAt,
                allDay = current.allDay,
                colorIndex = current.colorIndex,
                reminderMinutes = current.reminderMinutes,
                repeat = current.repeat,
            )
            val id = repo.save(entity)
            onSaved(entity.copy(id = if (current.id == 0L) id else current.id))
        }
    }

    fun delete(onDeleted: (Long) -> Unit) {
        val id = _state.value.id
        if (id == 0L) return
        viewModelScope.launch {
            repo.delete(id)
            onDeleted(id)
        }
    }
}
