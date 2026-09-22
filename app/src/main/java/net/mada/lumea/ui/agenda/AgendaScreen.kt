package net.mada.lumea.ui.agenda

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.data.db.EventEntity
import net.mada.lumea.ui.components.EmptyState
import net.mada.lumea.ui.components.FloatingNavBarSpace
import net.mada.lumea.ui.components.Illustration
import net.mada.lumea.ui.containerViewModel
import net.mada.lumea.ui.components.MonthCalendar
import net.mada.lumea.ui.theme.accent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    onOpenEvent: (Long) -> Unit,
    onNewEvent: (LocalDate) -> Unit,
) {
    val vm = containerViewModel { AgendaViewModel(it.events) }
    val month by vm.month.collectAsStateWithLifecycle()
    val selectedDay by vm.selectedDay.collectAsStateWithLifecycle()
    val eventsByDay by vm.eventsByDay.collectAsStateWithLifecycle()
    val dayEvents by vm.dayEvents.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Agenda") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNewEvent(selectedDay) },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Événement") },
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
            item {
                MonthCalendar(
                    month = month,
                    onMonthChange = vm::setMonth,
                    onDayClick = vm::selectDay,
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) { date, inMonth ->
                    AgendaDayCell(
                        date = date,
                        inMonth = inMonth,
                        selected = date == selectedDay,
                        events = eventsByDay[date].orEmpty(),
                    )
                }
            }

            item {
                Text(
                    selectedDay.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                        .replaceFirstChar { it.titlecase(Locale.FRENCH) },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
                )
            }

            if (dayEvents.isEmpty()) {
                item {
                    EmptyState(
                        illustration = Illustration.AGENDA,
                        title = "Journée libre",
                        subtitle = "Rien de prévu ce jour-là. Profite, ou ajoute quelque chose.",
                    )
                }
            } else {
                // Une série produit plusieurs occurrences partageant le même id :
                // la clé doit inclure l'heure de début.
                items(dayEvents, key = { "${it.id}-${it.startAt}" }) { event ->
                    EventRow(
                        event = event,
                        onClick = { onOpenEvent(event.id) },
                        onToggleDone = { vm.toggleDone(event) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AgendaDayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    events: List<EventEntity>,
) {
    val isToday = date == LocalDate.now()
    Box(
        Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.small)
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> androidx.compose.ui.graphics.Color.Transparent
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    !inMonth -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    selected -> MaterialTheme.colorScheme.onPrimaryContainer
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            if (events.isNotEmpty() && inMonth) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    events.take(3).forEach { event ->
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(accent(event.colorIndex))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventRow(
    event: EventEntity,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 8.dp),
        ) {
            Box(
                Modifier
                    .width(5.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                    .background(accent(event.colorIndex))
            )
            Spacer(Modifier.width(12.dp))
            Column(
                Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (event.isDone) TextDecoration.LineThrough else null,
                    color = if (event.isDone) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    event.timeLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (event.location.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Place,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            event.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            // « Fait » n'a pas de sens sur une série : on cocherait toutes les occurrences.
            if (event.repeat == "NONE") {
                IconButton(onClick = onToggleDone) {
                    Icon(
                        if (event.isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (event.isDone) "Marquer à faire" else "Marquer fait",
                        tint = if (event.isDone) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    )
                }
            } else {
                Icon(
                    Icons.Rounded.Repeat,
                    contentDescription = "Événement récurrent",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

fun EventEntity.timeLabel(): String {
    if (allDay) return "Toute la journée"
    val zone = ZoneId.systemDefault()
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH)
    val start = Instant.ofEpochMilli(startAt).atZone(zone).format(formatter)
    val end = Instant.ofEpochMilli(endAt).atZone(zone).format(formatter)
    return if (start == end) start else "$start – $end"
}
