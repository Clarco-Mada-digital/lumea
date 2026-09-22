package net.mada.lumea.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.data.db.FolderEntity
import net.mada.lumea.data.db.NoteEntity
import net.mada.lumea.ui.components.ColorPickerRow
import net.mada.lumea.ui.components.EmptyState
import net.mada.lumea.ui.components.FloatingNavBarSpace
import net.mada.lumea.ui.components.Illustration
import net.mada.lumea.ui.containerViewModel
import net.mada.lumea.ui.theme.accent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
) {
    val vm = containerViewModel { NotesViewModel(it.notes) }
    val filter by vm.filter.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()

    var showNewFolder by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (filter.archived) "Archives" else "Mes notes") },
                actions = {
                    IconButton(onClick = { vm.toggleFavorites() }) {
                        Icon(
                            if (filter.favoritesOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favoris",
                            tint = if (filter.favoritesOnly) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { vm.setArchived(!filter.archived) }) {
                        Icon(
                            if (filter.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                            contentDescription = "Archives",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (!filter.archived) {
                ExtendedFloatingActionButton(
                    onClick = onNewNote,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Note") },
                    modifier = Modifier.padding(bottom = FloatingNavBarSpace - 26.dp),
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Rechercher un mot, un tag…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            if (!filter.archived) {
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = filter.folderId == null,
                            onClick = { vm.setFolder(null) },
                            label = { Text("Tout") },
                        )
                    }
                    items(folders, key = { it.id }) { folder ->
                        FilterChip(
                            selected = filter.folderId == folder.id,
                            onClick = {
                                if (filter.folderId == folder.id) folderToDelete = folder
                                else vm.setFolder(folder.id)
                            },
                            label = { Text("${folder.emoji} ${folder.name}") },
                            leadingIcon = {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accent(folder.colorIndex))
                                )
                            },
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { showNewFolder = true },
                            label = { Text("Nouveau dossier") },
                            leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(16.dp)) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (notes.isEmpty()) {
                EmptyState(
                    illustration = Illustration.NOTES,
                    title = when {
                        filter.query.isNotEmpty() -> "Aucun résultat"
                        filter.archived -> "Aucune note archivée"
                        filter.favoritesOnly -> "Pas encore de favoris"
                        else -> "Ta première note t'attend"
                    },
                    subtitle = when {
                        filter.query.isNotEmpty() -> "Essaie un autre mot-clé."
                        filter.archived -> "Les notes que tu archives se rangent ici."
                        filter.favoritesOnly -> "Touche le cœur d'une note pour la retrouver ici."
                        else -> "Idées, listes, souvenirs : appuie sur « Note » pour commencer."
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = FloatingNavBarSpace + 76.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onOpenNote(note.id) },
                            onTogglePin = { vm.togglePin(note) },
                            onToggleFavorite = { vm.toggleFavorite(note) },
                            onArchive = { vm.archive(note) },
                            onDelete = { vm.delete(note) },
                        )
                    }
                }
            }
        }
    }

    if (showNewFolder) {
        NewFolderDialog(
            onDismiss = { showNewFolder = false },
            onCreate = { name, emoji, color ->
                vm.addFolder(name, emoji, color)
                showNewFolder = false
            },
        )
    }

    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Supprimer « ${folder.name} » ?") },
            text = { Text("Les notes de ce dossier ne seront pas supprimées, elles reviennent dans « Tout ».") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteFolder(folder)
                    folderToDelete = null
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) { Text("Annuler") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (note.colorIndex == 0) MaterialTheme.colorScheme.surfaceContainer
            else accent(note.colorIndex).copy(alpha = 0.18f)
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (note.isLocked) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = "Note protégée",
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 0.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    if (note.isLocked) note.title.ifBlank { "Note protégée" }
                    else note.title.ifBlank { "Sans titre" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (note.isPinned) {
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = "Épinglée",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Une note protégée ne laisse rien voir de son contenu dans la liste.
            val preview = if (note.isLocked) emptyList()
            else note.body.lines().filter { it.isNotBlank() }.take(6)
            if (preview.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                preview.forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note.isChecklist) {
                            Icon(
                                Icons.Rounded.CheckBox,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (line.trimStart().startsWith("[x]"))
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            line.removePrefix("[x] ").removePrefix("[ ] "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (note.isLocked) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Touche pour déverrouiller",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            if (!note.isLocked && note.tags.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    note.tags.split(",").filter { it.isNotBlank() }.joinToString(" ") { "#${it.trim()}" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    note.updatedAt.asShortDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (note.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favori",
                        modifier = Modifier.size(16.dp),
                        tint = if (note.isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (note.isPinned) "Désépingler" else "Épingler") },
                            onClick = { onTogglePin(); menuOpen = false },
                            leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text(if (note.isArchived) "Désarchiver" else "Archiver") },
                            onClick = { onArchive(); menuOpen = false },
                            leadingIcon = {
                                Icon(
                                    if (note.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                    contentDescription = null,
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer") },
                            onClick = { confirmDelete = true; menuOpen = false },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer cette note ?") },
            text = { Text("Cette action est définitive.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); confirmDelete = false }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun NewFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📁") }
    var color by remember { mutableStateOf(0) }
    val emojis = listOf("📁", "💖", "🌸", "📚", "💼", "🎀", "✨", "🍀", "🎯", "🏡")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau dossier") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojis) { candidate ->
                        FilterChip(
                            selected = candidate == emoji,
                            onClick = { emoji = candidate },
                            label = { Text(candidate) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                ColorPickerRow(selected = color, onSelect = { color = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, emoji, color) },
                enabled = name.isNotBlank(),
            ) { Text("Créer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** « 14:32 » si c'est aujourd'hui, « 12 sept. » sinon. */
internal fun Long.asShortDate(): String {
    val date = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
    val today = LocalDate.now()
    val pattern = if (date.toLocalDate() == today) "HH:mm" else "d MMM"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.FRENCH))
}
