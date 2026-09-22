package net.mada.lumea.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.data.prefs.Settings
import net.mada.lumea.ui.components.Overtitle
import net.mada.lumea.ui.components.richtext.MarkdownViewer
import net.mada.lumea.ui.containerViewModel
import net.mada.lumea.ui.lock.UnlockPrompt
import net.mada.lumea.ui.lock.rememberUnlockState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Lecture d'une note. C'est l'écran qu'on atteint en touchant une note : on vient
 * le plus souvent pour relire, pas pour modifier. Le clavier ne s'ouvre donc pas,
 * et les cases à cocher restent actionnables — cocher une course n'est pas
 * « modifier » au sens où on l'entend.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteReaderScreen(
    noteId: Long,
    settings: Settings,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val vm = containerViewModel(key = "reader-$noteId") { NoteReaderViewModel(it.notes, noteId) }
    val note by vm.note.collectAsStateWithLifecycle()
    val unlock = rememberUnlockState(note?.isLocked == true)

    val current = note
    if (current == null) {
        Scaffold(topBar = { TopAppBar(title = { }, navigationIcon = { BackButton(onBack) }) }) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Note introuvable", style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    // Tant que le contenu est masqué, on ne propose ni d'enlever le
                    // verrou ni de modifier : ce serait contourner la protection.
                    if (!unlock.blocked) IconButton(onClick = vm::toggleLock) {
                        Icon(
                            if (current.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = if (current.isLocked) "Retirer la protection"
                            else "Protéger cette note",
                            tint = if (current.isLocked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (!unlock.blocked) {
                ExtendedFloatingActionButton(
                    onClick = onEdit,
                    icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    text = { Text("Modifier") },
                )
            }
        },
    ) { padding ->
        if (unlock.blocked) {
            UnlockPrompt(unlock, settings.biometricEnabled, Modifier.padding(padding))
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Overtitle(current.updatedAt.readableDate(), Modifier.weight(1f))
                    if (current.isPinned) {
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = "Épinglée",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    if (current.isFavorite) {
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = "Favorite",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    current.title.ifBlank { "Sans titre" },
                    style = MaterialTheme.typography.headlineLarge,
                )

                if (current.tags.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        current.tags.split(",").filter { it.isNotBlank() }.take(4).forEach { tag ->
                            AssistChip(onClick = { }, label = { Text("#${tag.trim()}") })
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (current.isChecklist) {
                    val items = NoteEditorState(body = current.body).checklistItems
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items.forEachIndexed { index, item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                IconButton(onClick = { vm.toggleChecklistItem(index) }) {
                                    Icon(
                                        if (item.checked) Icons.Rounded.CheckBox
                                        else Icons.Rounded.CheckBoxOutlineBlank,
                                        contentDescription = if (item.checked) "Décocher" else "Cocher",
                                        tint = if (item.checked) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                    )
                                }
                                Text(
                                    item.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                                    color = if (item.checked) MaterialTheme.colorScheme.outline
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                } else if (current.body.isNotBlank()) {
                    MarkdownViewer(current.body)
                } else {
                    Text(
                        "Cette note est vide.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                Spacer(Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
    }
}

private fun Long.readableDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMMM yyyy · HH:mm", Locale.FRENCH))
