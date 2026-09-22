package net.mada.lumea.ui.journal

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.data.db.DailyLogEntity
import net.mada.lumea.ui.components.EmptyState
import net.mada.lumea.ui.components.FloatingNavBarSpace
import net.mada.lumea.ui.components.Illustration
import net.mada.lumea.ui.containerViewModel
import net.mada.lumea.ui.theme.accent
import net.mada.lumea.ui.theme.moodColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val moodEmojis = listOf("😔", "😕", "🙂", "😊", "🤩")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    onOpenDayLog: (LocalDate) -> Unit,
    onWriteToday: (LocalDate) -> Unit,
    onManageHabits: () -> Unit,
) {
    val vm = containerViewModel { JournalViewModel(it.journal) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal") },
                actions = {
                    IconButton(onClick = onManageHabits) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Gérer mes habitudes")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onWriteToday(LocalDate.now()) },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Aujourd'hui") },
                modifier = Modifier.padding(bottom = FloatingNavBarSpace - 26.dp),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = FloatingNavBarSpace + 76.dp),
        ) {
            /*
             * Les statistiques tenaient trois grandes cartes en haut de l'écran,
             * avant même la première journée écrite. Elles sont vraies mais
             * secondaires : on vient ici pour relire ses journées, pas pour
             * consulter un tableau de bord. Une ligne suffit.
             */
            item {
                Text(
                    buildString {
                        append("${state.entriesThisMonth} entrée")
                        if (state.entriesThisMonth > 1) append("s")
                        append(" ce mois")
                        if (state.currentJournalStreak > 0) {
                            append("  ·  ${state.currentJournalStreak} j d'affilée")
                        }
                        if (state.moodAverage > 0) {
                            append("  ·  humeur ")
                            append(moodEmojis[(state.moodAverage.toInt() - 1).coerceIn(0, 4)])
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
                )
            }

            item {
                Text(
                    "Mes journées",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
                )
            }

            if (state.entries.isEmpty()) {
                item {
                    EmptyState(
                        illustration = Illustration.JOURNAL,
                        title = "Ton journal est vierge",
                        subtitle = "Note une humeur, quelques lignes, une gratitude. " +
                            "Dans six mois, tu seras contente de les relire.",
                    )
                }
            } else {
                items(state.entries, key = { it.date.toEpochDay() }) { entry ->
                    JournalEntryCard(
                        entry = entry,
                        onClick = { onOpenDayLog(entry.date) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            // Les habitudes descendent sous les journées : elles ont leur propre
            // écran, et leur place ici est un rappel, pas le sujet principal.
            if (state.habits.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Mes habitudes du jour",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
                    )
                }
                items(state.habits, key = { it.habit.id }) { item ->
                    HabitRow(
                        item = item,
                        onToggle = { vm.toggleHabitToday(item.habit.id, !item.doneToday) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitRow(item: HabitStreak, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = if (item.doneToday) accent(item.habit.colorIndex).copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(item.habit.emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.habit.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (item.streak > 0) "${item.streak} jours d'affilée · ${item.last30}/30"
                    else "${item.last30} fois ces 30 derniers jours",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                if (item.doneToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (item.doneToday) accent(item.habit.colorIndex)
                else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalEntryCard(
    entry: DailyLogEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp)) {
            if (entry.mood > 0 && !entry.isLocked) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(moodColors[entry.mood - 1].copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(moodEmojis[entry.mood - 1])
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                            .replaceFirstChar { it.titlecase(Locale.FRENCH) },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (entry.isLocked) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = "Journée protégée",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (entry.isLocked) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Contenu protégé — touche pour déverrouiller.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else if (entry.journal.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        entry.journal,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!entry.isLocked && entry.gratitude.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "✨ ${entry.gratitude}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!entry.isLocked && entry.symptoms.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        entry.symptoms.split(",").filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
