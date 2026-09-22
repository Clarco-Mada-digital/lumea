package net.mada.lumea.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mada.lumea.data.db.FolderEntity
import net.mada.lumea.data.db.NoteEntity
import net.mada.lumea.data.repo.NoteRepository
import java.time.LocalDate

data class ChecklistItem(val text: String, val checked: Boolean)

data class NoteEditorState(
    val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val tags: String = "",
    val folderId: Long? = null,
    val colorIndex: Int = 0,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isChecklist: Boolean = false,
    val isLocked: Boolean = false,
    val linkedDate: LocalDate? = null,
) {
    /**
     * Les cases à cocher sont stockées dans le corps de la note, une par ligne,
     * préfixées par « [x] » ou « [ ] ». Une note reste donc lisible même exportée
     * en texte brut, et le passage liste ↔ texte ne perd rien.
     */
    val checklistItems: List<ChecklistItem>
        get() = body.lines().map { line ->
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("[x] ", ignoreCase = true) -> ChecklistItem(trimmed.drop(4), true)
                trimmed.startsWith("[ ] ") -> ChecklistItem(trimmed.drop(4), false)
                else -> ChecklistItem(line, false)
            }
        }
}

class NoteEditorViewModel(
    private val repo: NoteRepository,
    private val noteId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(NoteEditorState())
    val state: StateFlow<NoteEditorState> = _state.asStateFlow()

    val folders: StateFlow<List<FolderEntity>> = repo.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (noteId != 0L) {
            viewModelScope.launch {
                repo.get(noteId)?.let { note ->
                    _state.value = NoteEditorState(
                        id = note.id,
                        title = note.title,
                        body = note.body,
                        tags = note.tags,
                        folderId = note.folderId,
                        colorIndex = note.colorIndex,
                        isPinned = note.isPinned,
                        isFavorite = note.isFavorite,
                        isChecklist = note.isChecklist,
                        isLocked = note.isLocked,
                        linkedDate = note.linkedDate,
                    )
                }
            }
        }
    }

    fun setTitle(value: String) { _state.value = _state.value.copy(title = value) }
    fun setBody(value: String) { _state.value = _state.value.copy(body = value) }
    fun setTags(value: String) { _state.value = _state.value.copy(tags = value) }
    fun setFolder(id: Long?) { _state.value = _state.value.copy(folderId = id) }
    fun setColor(index: Int) { _state.value = _state.value.copy(colorIndex = index) }
    fun togglePin() { _state.value = _state.value.copy(isPinned = !_state.value.isPinned) }
    fun toggleFavorite() { _state.value = _state.value.copy(isFavorite = !_state.value.isFavorite) }
    fun toggleLock() { _state.value = _state.value.copy(isLocked = !_state.value.isLocked) }

    /** Passer en mode liste normalise le corps pour que chaque ligne ait son préfixe. */
    fun toggleChecklist() {
        val current = _state.value
        if (current.isChecklist) {
            val plain = current.checklistItems.joinToString("\n") { it.text }
            _state.value = current.copy(isChecklist = false, body = plain)
        } else {
            val items = current.body.lines().map { ChecklistItem(it, false) }
            _state.value = current.copy(isChecklist = true, body = items.encode())
        }
    }

    fun toggleChecklistItem(index: Int) = updateItems { items ->
        items.mapIndexed { i, item -> if (i == index) item.copy(checked = !item.checked) else item }
    }

    fun editChecklistItem(index: Int, text: String) = updateItems { items ->
        items.mapIndexed { i, item -> if (i == index) item.copy(text = text) else item }
    }

    fun removeChecklistItem(index: Int) = updateItems { items ->
        items.filterIndexed { i, _ -> i != index }
    }

    fun addChecklistItem() = updateItems { it + ChecklistItem("", false) }

    private fun updateItems(transform: (List<ChecklistItem>) -> List<ChecklistItem>) {
        _state.value = _state.value.copy(body = transform(_state.value.checklistItems).encode())
    }

    fun save() {
        val current = _state.value
        // Une note entièrement vide ne mérite pas d'exister.
        if (current.title.isBlank() && current.body.isBlank()) {
            if (current.id != 0L) viewModelScope.launch { repo.delete(current.id) }
            return
        }
        viewModelScope.launch {
            val id = repo.save(
                NoteEntity(
                    id = current.id,
                    title = current.title.trim(),
                    body = current.body,
                    folderId = current.folderId,
                    tags = current.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.joinToString(","),
                    colorIndex = current.colorIndex,
                    isPinned = current.isPinned,
                    isFavorite = current.isFavorite,
                    isChecklist = current.isChecklist,
                    isLocked = current.isLocked,
                    linkedDate = current.linkedDate,
                )
            )
            if (current.id == 0L) _state.value = current.copy(id = id)
        }
    }

    fun delete() {
        val id = _state.value.id
        if (id != 0L) viewModelScope.launch { repo.delete(id) }
    }
}

private fun List<ChecklistItem>.encode(): String =
    joinToString("\n") { (if (it.checked) "[x] " else "[ ] ") + it.text }
