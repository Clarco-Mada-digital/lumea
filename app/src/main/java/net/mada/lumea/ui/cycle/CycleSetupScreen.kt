package net.mada.lumea.ui.cycle

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.ui.components.Overtitle
import net.mada.lumea.ui.containerViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Assistant de configuration du cycle.
 *
 * Les réglages bruts (« durée du cycle », « phase lutéale ») supposent qu'on connaît
 * déjà son corps. Ici on ne pose que des questions auxquelles on peut répondre de
 * mémoire — des dates et des durées vécues — et l'app en déduit les chiffres.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleSetupScreen(onDone: () -> Unit) {
    val vm = containerViewModel { CycleSetupViewModel(it.cycle, it.settings) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurer mon cycle") },
                navigationIcon = {
                    IconButton(onClick = { if (!vm.back()) onDone() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            StepDots(current = state.step, total = SetupStep.entries.size)
            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val offset = if (forward) 1 else -1
                    (slideInHorizontally { it / 3 * offset } + fadeIn())
                        .togetherWith(slideOutHorizontally { -it / 3 * offset } + fadeOut())
                },
                label = "setupStep",
                modifier = Modifier.weight(1f),
            ) { step ->
                when (step) {
                    SetupStep.LAST_PERIOD -> LastPeriodStep(state, vm)
                    SetupStep.PERIOD_LENGTH -> PeriodLengthStep(state, vm)
                    SetupStep.PREVIOUS_PERIOD -> PreviousPeriodStep(state, vm)
                    SetupStep.SUMMARY -> SummaryStep(state)
                }
            }

            Button(
                onClick = {
                    if (state.step == SetupStep.SUMMARY) vm.finish(onDone) else vm.next()
                },
                enabled = vm.canContinue(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
            ) {
                Text(if (state.step == SetupStep.SUMMARY) "Enregistrer" else "Continuer")
            }
        }
    }
}

@Composable
private fun StepDots(current: SetupStep, total: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        repeat(total) { index ->
            val active = index <= current.ordinal
            Box(
                Modifier
                    .height(4.dp)
                    .weight(1f)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
            )
        }
    }
}

@Composable
private fun StepFrame(
    overtitle: String,
    question: String,
    help: String,
    content: @Composable () -> Unit,
) {
    Column {
        Overtitle(overtitle)
        Spacer(Modifier.height(10.dp))
        Text(question, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            help,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LastPeriodStep(state: CycleSetupState, vm: CycleSetupViewModel) {
    var showPicker by remember { mutableStateOf(false) }

    StepFrame(
        overtitle = "Étape 1",
        question = "Quand ont commencé tes dernières règles ?",
        help = "Le premier jour où tu as saigné, même un peu. Si tu hésites d'un jour " +
            "ou deux, ce n'est pas grave : l'app s'ajustera avec le temps.",
    ) {
        Column {
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    state.lastStart?.format(longDate)?.replaceFirstChar { it.titlecase(Locale.FRENCH) }
                        ?: "Choisir une date"
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0L to "Aujourd'hui", 7L to "Il y a 1 semaine", 14L to "Il y a 2 semaines")
                    .forEach { (days, label) ->
                        OutlinedButton(onClick = { vm.setLastStart(LocalDate.now().minusDays(days)) }) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
            }
        }
    }

    if (showPicker) {
        DateChooser(
            initial = state.lastStart ?: LocalDate.now(),
            onDismiss = { showPicker = false },
            onPick = { vm.setLastStart(it); showPicker = false },
        )
    }
}

@Composable
private fun PeriodLengthStep(state: CycleSetupState, vm: CycleSetupViewModel) {
    StepFrame(
        overtitle = "Étape 2",
        question = "Combien de jours durent tes règles ?",
        help = "Du premier au dernier jour de saignement. La plupart des femmes sont entre 3 et 7 jours.",
    ) {
        ChoiceGrid(
            options = (2..9).toList(),
            selected = state.periodLength,
            labelFor = { "$it j" },
            onSelect = vm::setPeriodLength,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviousPeriodStep(state: CycleSetupState, vm: CycleSetupViewModel) {
    var showPicker by remember { mutableStateOf(false) }

    StepFrame(
        overtitle = "Étape 3",
        question = "Et les règles d'avant, tu t'en souviens ?",
        help = "C'est l'écart entre deux règles qui donne la longueur de ton cycle. " +
            "Si tu ne sais plus, passe : on partira sur 28 jours et l'app corrigera " +
            "toute seule au fil des mois.",
    ) {
        Column {
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    state.previousStart?.format(longDate)?.replaceFirstChar { it.titlecase(Locale.FRENCH) }
                        ?: "Choisir une date"
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.setPreviousStart(null); vm.next() }) {
                Text("Je ne sais pas")
            }

            state.computedCycleLength?.let { cycle ->
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Cela fait un cycle de $cycle jours.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        // Tout l'écran Cycle découle de ce chiffre. Un écart inhabituel
                        // vient plus souvent d'une date approximative que d'un vrai
                        // cycle long : autant le dire tant qu'on peut encore corriger.
                        if (cycle !in 24..34) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (cycle > 34) {
                                    "C'est plus long que la moyenne (21 à 35 jours). Si tu " +
                                        "n'es pas sûre de la date, reviens la corriger — " +
                                        "c'est ce chiffre qui pilote toutes les prévisions."
                                } else {
                                    "C'est plus court que la moyenne (21 à 35 jours). Si tu " +
                                        "n'es pas sûre de la date, reviens la corriger — " +
                                        "c'est ce chiffre qui pilote toutes les prévisions."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        DateChooser(
            initial = state.previousStart ?: state.lastStart?.minusDays(28) ?: LocalDate.now().minusDays(28),
            onDismiss = { showPicker = false },
            onPick = { vm.setPreviousStart(it); showPicker = false },
            // On ne peut pas avoir eu ses règles précédentes après les dernières.
            maxDate = state.lastStart?.minusDays(10),
        )
    }
}

@Composable
private fun SummaryStep(state: CycleSetupState) {
    val cycle = state.computedCycleLength ?: 28
    StepFrame(
        overtitle = "C'est tout",
        question = "Voilà ce que j'ai compris",
        help = "Tu pourras tout modifier plus tard dans les réglages.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryRow("Dernières règles", state.lastStart?.format(longDate) ?: "—")
            SummaryRow("Durée des règles", "${state.periodLength} jours")
            SummaryRow(
                "Longueur du cycle",
                if (state.computedCycleLength != null) "$cycle jours"
                else "28 jours (estimation de départ)",
            )
            SummaryRow(
                "Ovulation estimée",
                state.lastStart?.plusDays((cycle - 14).toLong())?.format(shortDate) ?: "—",
            )
            SummaryRow(
                "Prochaines règles",
                state.lastStart?.plusDays(cycle.toLong())?.format(shortDate) ?: "—",
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Ces chiffres sont des estimations. Plus tu enregistres de cycles, " +
                    "plus ils deviennent justes.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ChoiceGrid(
    options: List<Int>,
    selected: Int,
    labelFor: (Int) -> String,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    val isSelected = option == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickable { onSelect(option) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            labelFor(option),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateChooser(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
    maxDate: LocalDate? = null,
) {
    val utc = ZoneId.of("UTC")
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(utc).toInstant().toEpochMilli(),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableYear(year: Int) = year <= LocalDate.now().year

            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(utc).toLocalDate()
                if (date.isAfter(LocalDate.now())) return false
                return maxDate == null || !date.isAfter(maxDate)
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(utc).toLocalDate())
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    ) { DatePicker(state = pickerState) }
}

private val longDate = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
private val shortDate = DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH)
