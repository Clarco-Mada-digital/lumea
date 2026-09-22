package net.mada.lumea.ui.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.notif.ReminderScheduler
import net.mada.lumea.ui.components.ColorPickerRow
import net.mada.lumea.ui.containerViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val reminderOptions = listOf(
    null to "Aucun",
    0 to "À l'heure",
    10 to "10 min avant",
    30 to "30 min avant",
    60 to "1 h avant",
    24 * 60 to "1 jour avant",
)

private val repeatOptions = listOf(
    "NONE" to "Jamais",
    "DAILY" to "Chaque jour",
    "WEEKLY" to "Chaque semaine",
    "MONTHLY" to "Chaque mois",
    "YEARLY" to "Chaque année",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(
    eventId: Long,
    initialDate: LocalDate?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val vm = containerViewModel(key = "event-$eventId") {
        EventEditorViewModel(it.events, eventId, initialDate)
    }
    val state by vm.state.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (eventId == 0L) "Nouvel événement" else "Modifier") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (eventId != 0L) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Supprimer")
                        }
                    }
                    IconButton(
                        onClick = {
                            vm.save { saved ->
                                ReminderScheduler.cancelEvent(context, saved.id)
                                ReminderScheduler.scheduleEvent(context, saved)
                            }
                            onBack()
                        },
                        enabled = state.title.isNotBlank(),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Enregistrer")
                    }
                },
            )
        },
        // safeDrawing inclut le clavier : les champs du bas restent visibles.
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::setTitle,
                label = { Text("Titre") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    state.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH))
                        .replaceFirstChar { it.titlecase(Locale.FRENCH) }
                )
            }

            Spacer(Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Toute la journée") },
                trailingContent = {
                    Switch(checked = state.allDay, onCheckedChange = vm::setAllDay)
                },
            )

            if (!state.allDay) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                        Text("Début ${state.startTime.format(hourFormatter)}")
                    }
                    OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
                        Text("Fin ${state.endTime.format(hourFormatter)}")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.location,
                onValueChange = vm::setLocation,
                label = { Text("Lieu (optionnel)") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.notes,
                onValueChange = vm::setNotes,
                label = { Text("Notes") },
                minLines = 3,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Text("Rappel", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reminderOptions) { (minutes, label) ->
                    FilterChip(
                        selected = state.reminderMinutes == minutes,
                        onClick = { vm.setReminder(minutes) },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Répétition", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(repeatOptions) { (value, label) ->
                    FilterChip(
                        selected = state.repeat == value,
                        onClick = { vm.setRepeat(value) },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Couleur", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))
            ColorPickerRow(selected = state.colorIndex, onSelect = vm::setColor)

            Spacer(Modifier.height(48.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        // Le DatePicker travaille en UTC : on relit la date telle quelle.
                        vm.setDate(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showStartPicker) {
        TimePickerDialog(
            initial = state.startTime,
            onDismiss = { showStartPicker = false },
            onConfirm = { vm.setStartTime(it); showStartPicker = false },
        )
    }

    if (showEndPicker) {
        TimePickerDialog(
            initial = state.endTime,
            onDismiss = { showEndPicker = false },
            onConfirm = { vm.setEndTime(it); showEndPicker = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer cet événement ?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete { id -> ReminderScheduler.cancelEvent(context, id) }
                    confirmDelete = false
                    onBack()
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text("OK")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                TimePicker(state = pickerState)
            }
        },
    )
}

private val hourFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH)
