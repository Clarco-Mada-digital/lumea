package net.mada.lumea.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mada.lumea.data.db.DailyLogEntity
import net.mada.lumea.data.prefs.Settings
import net.mada.lumea.data.repo.JournalRepository
import net.mada.lumea.ui.components.Overtitle
import net.mada.lumea.ui.components.richtext.MarkdownViewer
import net.mada.lumea.ui.containerViewModel
import net.mada.lumea.ui.lock.UnlockPrompt
import net.mada.lumea.ui.lock.rememberUnlockState
import net.mada.lumea.ui.theme.CycleColors
import net.mada.lumea.ui.theme.moodColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val moodEmojis = listOf("😔", "😕", "🙂", "😊", "🤩")
private val energyEmojis = listOf("🪫", "🔅", "⚡", "🔆", "🚀")
private val flowLabels = listOf("", "Léger", "Moyen", "Abondant", "Très abondant")

class DayReaderViewModel(
    private val repo: JournalRepository,
    date: LocalDate,
) : ViewModel() {

    val log: StateFlow<DailyLogEntity?> = repo.observeLog(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleLock() {
        val current = log.value ?: return
        viewModelScope.launch { repo.saveLog(current.copy(isLocked = !current.isLocked)) }
    }
}

/** Relire une journée sans risquer de la modifier par inadvertance. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayReaderScreen(
    date: LocalDate,
    settings: Settings,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val vm = containerViewModel(key = "dayread-$date") { DayReaderViewModel(it.journal, date) }
    val log by vm.log.collectAsStateWithLifecycle()
    val unlock = rememberUnlockState(log?.isLocked == true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                            .replaceFirstChar { it.titlecase(Locale.FRENCH) }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (log != null && !unlock.blocked) {
                        IconButton(onClick = vm::toggleLock) {
                            Icon(
                                if (log?.isLocked == true) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = if (log?.isLocked == true) "Retirer la protection"
                                else "Protéger cette journée",
                                tint = if (log?.isLocked == true) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
        val current = log
        if (current == null) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Rien d'enregistré ce jour-là", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Touche « Modifier » pour écrire.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (current.mood > 0) {
                        MoodBubble("Humeur", moodEmojis[current.mood - 1], moodColors[current.mood - 1])
                    }
                    if (current.energy > 0) {
                        MoodBubble("Énergie", energyEmojis[current.energy - 1], moodColors[current.energy - 1])
                    }
                    if (current.flow > 0) {
                        MoodBubble("Flux", "🩸", CycleColors.period)
                    }
                }

                if (current.flow > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Flux ${flowLabels.getOrElse(current.flow) { "" }.lowercase(Locale.FRENCH)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (current.journal.isNotBlank()) {
                    Section("Ma journée")
                    MarkdownViewer(current.journal)
                }

                if (current.gratitude.isNotBlank()) {
                    Section("Gratitude")
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "✨ ${current.gratitude}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                val symptoms = current.symptoms.split(",").filter { it.isNotBlank() }
                if (symptoms.isNotEmpty()) {
                    Section("Symptômes")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        symptoms.forEach { AssistChip(onClick = { }, label = { Text(it) }) }
                    }
                }

                if (current.waterGlasses > 0 || current.sleepHours > 0f) {
                    Section("Bien-être")
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        if (current.waterGlasses > 0) {
                            StatBlock("${current.waterGlasses}", "verres d'eau")
                        }
                        if (current.sleepHours > 0f) {
                            StatBlock("%.1f h".format(current.sleepHours), "de sommeil")
                        }
                    }
                }

                Spacer(Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(24.dp))
    Overtitle(title)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun MoodBubble(label: String, emoji: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatBlock(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
