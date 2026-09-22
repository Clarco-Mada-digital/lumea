package net.mada.lumea.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mada.lumea.data.db.FolderEntity
import net.mada.lumea.data.db.NoteEntity
import net.mada.lumea.data.repo.NoteRepository

data class NotesFilter(
    val query: String = "",
    val folderId: Long? = null,
    val favoritesOnly: Boolean = false,
    val archived: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NotesViewModel(private val repo: NoteRepository) : ViewModel() {

    private val _filter = MutableStateFlow(NotesFilter())
    val filter: StateFlow<NotesFilter> = _filter

    val folders: StateFlow<List<FolderEntity>> = repo.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = _filter
        // On laisse la frappe se poser avant de relancer la requête SQL.
        .debounce { if (it.query.isEmpty()) 0L else 200L }
        .flatMapLatest { f ->
            repo.observeNotes(f.query.trim(), f.folderId, f.favoritesOnly, f.archived)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) { _filter.value = _filter.value.copy(query = value) }
    fun setFolder(id: Long?) { _filter.value = _filter.value.copy(folderId = id) }
    fun toggleFavorites() { _filter.value = _filter.value.copy(favoritesOnly = !_filter.value.favoritesOnly) }
    fun setArchived(value: Boolean) { _filter.value = _filter.value.copy(archived = value, folderId = null) }

    fun togglePin(note: NoteEntity) = viewModelScope.launch { repo.setPinned(note, !note.isPinned) }
    fun toggleFavorite(note: NoteEntity) = viewModelScope.launch { repo.setFavorite(note, !note.isFavorite) }
    fun archive(note: NoteEntity) = viewModelScope.launch { repo.setArchived(note, !note.isArchived) }
    fun delete(note: NoteEntity) = viewModelScope.launch { repo.delete(note.id) }

    fun addFolder(name: String, emoji: String, colorIndex: Int) = viewModelScope.launch {
        repo.saveFolder(FolderEntity(name = name.trim(), emoji = emoji, colorIndex = colorIndex))
    }

    fun deleteFolder(folder: FolderEntity) = viewModelScope.launch {
        if (_filter.value.folderId == folder.id) setFolder(null)
        repo.deleteFolder(folder)
    }
}
