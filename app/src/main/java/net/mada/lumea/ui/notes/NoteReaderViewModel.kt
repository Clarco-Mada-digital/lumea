package net.mada.lumea.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mada.lumea.data.db.NoteEntity
import net.mada.lumea.data.repo.NoteRepository

class NoteReaderViewModel(
    private val repo: NoteRepository,
    noteId: Long,
) : ViewModel() {

    val note: StateFlow<NoteEntity?> = repo.observeNote(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleLock() {
        val current = note.value ?: return
        viewModelScope.launch { repo.save(current.copy(isLocked = !current.isLocked)) }
    }

    /** Cocher une ligne depuis la lecture : c'est un geste de consultation, pas d'édition. */
    fun toggleChecklistItem(index: Int) {
        val current = note.value ?: return
        val items = NoteEditorState(body = current.body).checklistItems
            .mapIndexed { i, item -> if (i == index) item.copy(checked = !item.checked) else item }
        val body = items.joinToString("\n") { (if (it.checked) "[x] " else "[ ] ") + it.text }
        viewModelScope.launch { repo.save(current.copy(body = body)) }
    }
}
