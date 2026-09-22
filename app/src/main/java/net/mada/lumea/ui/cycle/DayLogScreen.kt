package net.mada.lumea.ui.cycle

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.ui.components.richtext.RichTextToolbar
import net.mada.lumea.ui.containerViewModel
import net.mada.lumea.ui.theme.CycleColors
import net.mada.lumea.ui.theme.moodColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val flowLabels = listOf("Aucun", "Léger", "Moyen", "Abondant", "Très abondant")
private val moodEmojis = listOf("😔", "😕", "🙂", "😊", "🤩")
private val energyEmojis = listOf("🪫", "🔅", "⚡", "🔆", "🚀")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayLogScreen(
    date: LocalDate,
    onBack: () -> Unit,
) {
    val vm = containerViewModel(key = "log-$date") {
        DayLogViewModel(it.cycle, it.journal, it.settings, date)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val habits by vm.habits.collectAsStateWithLifecycle()
    val checkedHabits by vm.checkedHabits.collectAsStateWithLifecycle()

    // Comme dans l'éditeur de note : la barre de formatage a besoin de la position
    // du curseur, sinon elle écrit toujours au début du texte.
    var journal by remember { mutableStateOf(TextFieldValue()) }
    LaunchedEffect(state.journal) {
        if (state.journal != journal.text) {
            journal = journal.copy(text = state.journal, selection = TextRange(state.journal.length))
        }
    }

    var trackingOpen by remember { mutableStateOf(false) }
    LaunchedEffect(state.flow, state.energy, state.symptoms) {
        if (state.flow > 0 || state.energy > 0 || state.symptoms.isNotEmpty()) {
            trackingOpen = true
        }
    }

    fun leave() {
        vm.save()
        onBack()
    }

    BackHandler { leave() }

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
                    IconButton(onClick = { leave() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = vm::toggleLock) {
                        Icon(
                            if (state.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = "Protéger cette journée",
                            tint = if (state.isLocked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { leave() }) {
                        Icon(Icons.Rounded.Check, contentDescription = "Enregistrer")
                    }
                },
            )
        },
        /*
         * safeDrawing inclut la hauteur du clavier : la zone de contenu rétrécit
         * quand il s'ouvre, et le défilement amène le curseur au-dessus. Sans ça,
         * on écrivait à l'aveugle derrière le clavier.
         */
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            /*
             * L'écriture d'abord.
             *
             * Cet écran affichait six sections de suivi — flux, humeur, énergie,
             * symptômes, habitudes, bien-être — avant d'arriver au champ de texte.
             * Or on vient ici pour écrire : le reste est du suivi, utile mais
             * secondaire. Il est donc replié derrière un seul bouton, et ce qui se
             * garde en vue est l'humeur du jour, qui se coche en une seconde.
             */
            Spacer(Modifier.height(8.dp))

            Text(
                "Comment tu te sens aujourd'hui ?",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            EmojiScale(
                emojis = moodEmojis,
                selected = state.mood,
                onSelect = vm::setMood,
            )

            Spacer(Modifier.height(20.dp))

            RichTextToolbar(
                textFieldValue = journal,
                onValueChange = {
                    journal = it
                    vm.setJournal(it.text)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = journal,
                onValueChange = {
                    journal = it
                    vm.setJournal(it.text)
                },
                placeholder = {
                    Text("Raconte ta journée… personne d'autre ne la lira.")
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                // Grand dès l'ouverture : c'est la zone principale de l'écran, elle
                // doit donner envie d'écrire plutôt que de tenir sur trois lignes.
                minLines = 10,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.gratitude,
                onValueChange = vm::setGratitude,
                label = { Text("Une chose pour laquelle je suis reconnaissante") },
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            // Tout le suivi du corps, replié. Un seul appui pour l'ouvrir, et il
            // reste ouvert tant qu'on est sur l'écran.
            TextButton(
                onClick = { trackingOpen = !trackingOpen },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    if (trackingOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (trackingOpen) "Masquer le suivi du corps"
                    else "Ajouter flux, symptômes, énergie…" + summaryOf(state)
                )
            }

            if (trackingOpen) {
                /*
                 * Le flux n'a pas de sens pour un jour à venir : le renseigner
                 * marquerait des règles futures et fausserait toutes les moyennes.
                 * Le reste du carnet (humeur, notes) reste ouvert : on peut vouloir
                 * préparer une journée.
                 */
                if (date.isAfter(LocalDate.now())) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        Text(
                            "Ce jour n'est pas encore arrivé : le flux se note le jour " +
                                "même ou après, jamais à l'avance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else SectionCard(title = "Flux") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        flowLabels.forEachIndexed { index, label ->
                            if (index == 0) return@forEachIndexed
                            val selected = state.flow == index
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        if (selected) CycleColors.period.copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .clickable { vm.setFlow(index) }
                                    .padding(vertical = 10.dp),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                    repeat(index) {
                                        Box(
                                            Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(CycleColors.period)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    if (state.flow > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ce jour est compté comme jour de règles dans ton calendrier.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                SectionCard(title = "Énergie") {
                    EmojiScale(
                        emojis = energyEmojis,
                        selected = state.energy,
                        onSelect = vm::setEnergy,
                    )
                }

                SectionCard(title = "Symptômes") {
                    symptomGroups.forEach { (group, symptoms) ->
                        Text(
                            group,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            symptoms.forEach { symptom ->
                                FilterChip(
                                    selected = symptom in state.symptoms,
                                    onClick = { vm.toggleSymptom(symptom) },
                                    label = { Text(symptom) },
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                if (habits.isNotEmpty()) {
                    SectionCard(title = "Mes habitudes") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            habits.forEach { habit ->
                                val checked = habit.id in checkedHabits
                                FilterChip(
                                    selected = checked,
                                    onClick = { vm.toggleHabit(habit.id, !checked) },
                                    label = { Text("${habit.emoji} ${habit.name}") },
                                )
                            }
                        }
                    }
                }

                SectionCard(title = "Bien-être") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.LocalDrink,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Eau : ${state.waterGlasses} verre${if (state.waterGlasses > 1) "s" else ""}")
                    }
                    Slider(
                        value = state.waterGlasses.toFloat(),
                        onValueChange = { vm.setWater(it.roundToInt()) },
                        valueRange = 0f..12f,
                        steps = 11,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Sommeil : ${"%.1f".format(state.sleepHours)} h")
                    Slider(
                        value = state.sleepHours,
                        onValueChange = vm::setSleep,
                        valueRange = 0f..14f,
                        steps = 27,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EmojiScale(emojis: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth(),
    ) {
        emojis.forEachIndexed { index, emoji ->
            val value = index + 1
            val isSelected = selected == value
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) moodColors[index].copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * Un résumé discret de ce qui est déjà renseigné, collé au bouton de dépliage.
 *
 * Sans lui, replier le suivi donnerait l'impression que les données ont disparu.
 */
private fun summaryOf(state: DayLogState): String {
    val parts = buildList {
        if (state.flow > 0) add(flowLabels[state.flow].lowercase())
        if (state.energy > 0) add("énergie")
        if (state.symptoms.isNotEmpty()) add("${state.symptoms.size} symptôme${if (state.symptoms.size > 1) "s" else ""}")
        if (state.waterGlasses > 0) add("eau")
        if (state.sleepHours > 0f) add("sommeil")
    }
    return if (parts.isEmpty()) "" else "  ·  ${parts.joinToString(", ")}"
}
