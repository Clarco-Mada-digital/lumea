package net.mada.lumea.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mada.lumea.data.db.HabitEntity
import net.mada.lumea.data.repo.JournalRepository
import net.mada.lumea.ui.components.ColorPickerRow
import net.mada.lumea.ui.components.EmptyState
import net.mada.lumea.ui.components.Illustration
import net.mada.lumea.ui.containerViewModel
import net.mada.lumea.ui.theme.accent

class HabitsViewModel(private val repo: JournalRepository) : ViewModel() {

    val habits: StateFlow<List<HabitEntity>> = repo.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, emoji: String, colorIndex: Int) = viewModelScope.launch {
        repo.saveHabit(HabitEntity(name = name.trim(), emoji = emoji, colorIndex = colorIndex))
    }

    fun delete(id: Long) = viewModelScope.launch { repo.deleteHabit(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(onBack: () -> Unit) {
    val vm = containerViewModel { HabitsViewModel(it.journal) }
    val habits by vm.habits.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes habitudes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Habitude") },
            )
        },
    ) { padding ->
        if (habits.isEmpty()) {
            EmptyState(
                illustration = Illustration.HABITS,
                title = "Aucune habitude pour l'instant",
                subtitle = "Boire de l'eau, lire 10 pages, marcher… Choisis-en deux ou trois, pas vingt.",
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(habits, key = { it.id }) { habit ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = accent(habit.colorIndex).copy(alpha = 0.14f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                        ) {
                            Text(habit.emoji, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(12.dp))
                            Text(habit.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { vm.delete(habit.id) }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        NewHabitDialog(
            onDismiss = { showDialog = false },
            onCreate = { name, emoji, color ->
                vm.add(name, emoji, color)
                showDialog = false
            },
        )
    }
}

@Composable
private fun NewHabitDialog(onDismiss: () -> Unit, onCreate: (String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("✨") }
    var color by remember { mutableStateOf(0) }
    val emojis = listOf("✨", "💧", "📖", "🏃", "🧘", "🌙", "🥗", "💊", "🎨", "🎧")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle habitude") },
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
            TextButton(onClick = { onCreate(name, emoji, color) }, enabled = name.isNotBlank()) {
                Text("Créer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
