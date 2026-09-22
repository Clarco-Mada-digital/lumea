package net.mada.lumea.ui.today

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.data.prefs.Settings
import net.mada.lumea.domain.cycle.CycleInsight
import net.mada.lumea.domain.cycle.hint
import net.mada.lumea.domain.cycle.label
import net.mada.lumea.ui.Routes
import net.mada.lumea.ui.agenda.EventRow
import net.mada.lumea.ui.components.CycleRing
import net.mada.lumea.ui.components.FloatingNavBarSpace
import net.mada.lumea.ui.components.Overtitle
import net.mada.lumea.ui.components.Reveal
import net.mada.lumea.ui.components.cycleSegments
import net.mada.lumea.domain.pregnancy.PregnancyProgress
import net.mada.lumea.domain.pregnancy.NextCare
import net.mada.lumea.domain.pregnancy.headline
import net.mada.lumea.domain.learn.lessonTopics
import net.mada.lumea.ui.containerViewModel
import net.mada.lumea.ui.cycle.trimesterSegments
import net.mada.lumea.ui.pregnancy.PinInvitationCard
import net.mada.lumea.ui.pregnancy.PregnancyViewModel
import net.mada.lumea.ui.pregnancy.deservesPin
import net.mada.lumea.ui.theme.accent
import net.mada.lumea.ui.theme.heroBrush
import net.mada.lumea.ui.theme.onHeroColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    settings: Settings,
    onOpenNote: (Long) -> Unit,
    onOpenEvent: (Long) -> Unit,
    onOpenDayLog: (LocalDate) -> Unit,
    onReadDayLog: (LocalDate) -> Unit,
    onOpenTab: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLessons: () -> Unit,
    onOpenPregnancy: () -> Unit = {},
    onOpenAdvisor: () -> Unit = {},
    onOpenAi: () -> Unit = {},
    onSetPin: () -> Unit = {},
) {
    val pregnancyVm = containerViewModel {
        PregnancyViewModel(it.pregnancy, it.cycle, it.events, it.settings)
    }
    val pregnancy by pregnancyVm.state.collectAsStateWithLifecycle()
    val vm = containerViewModel {
        TodayViewModel(it.notes, it.events, it.cycle, it.journal, it.settings)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(greeting(settings.displayName), style = MaterialTheme.typography.headlineMedium)
                        Text(
                            today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                                .replaceFirstChar { it.titlecase(Locale.FRENCH) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Réglages")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = FloatingNavBarSpace),
        ) {
            /*
             * L'invitation à poser un code, au-dessus de tout le reste.
             *
             * C'est ici qu'elle a sa place : l'anneau juste en dessous annonce
             * « 12 SA » en grand dès l'ouverture. Sans code, c'est la première
             * chose que voit quiconque prend le téléphone.
             */
            if (!settings.lockEnabled &&
                !settings.pinInvitationDismissed &&
                deservesPin(
                    hasOngoingFollowUp = pregnancy.ongoing != null,
                    care = pregnancy.care,
                    caregiverPhone = pregnancy.ongoing?.caregiverPhone.orEmpty(),
                    riskFactors = pregnancy.ongoing?.riskFactors.orEmpty(),
                )
            ) {
                item {
                    PinInvitationCard(
                        onSetPin = onSetPin,
                        onDismiss = pregnancyVm::dismissPinInvitation,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            if (settings.cycleTabVisible) {
                item {
                    Reveal {
                        CycleHeroCard(
                            insight = state.insight,
                            expecting = pregnancy.progress?.takeIf { !pregnancy.isPostpartum },
                            nextCare = pregnancy.nextCare,
                            onClick = {
                                if (pregnancy.ongoing != null) onOpenPregnancy()
                                else onOpenTab(Routes.CYCLE)
                            },
                        )
                    }
                }
            }

            /*
             * Trois raccourcis sur une ligne, au lieu de trois cartes pleine
             * largeur avec un paragraphe chacune.
             *
             * Ces cartes expliquaient longuement ce qu'elles faisaient et
             * occupaient tout l'écran, repoussant sous la ligne de flottaison ce
             * pour quoi on ouvre « Aujourd'hui » : la journée elle-même. Le détail
             * appartient aux écrans de destination, pas à l'accueil.
             */
            item {
                Reveal {
                    QuickActions(
                        onAdvisor = onOpenAdvisor,
                        onAi = onOpenAi,
                        onLessons = onOpenLessons,
                    )
                }
            }

            item {
                SectionTitle(
                    title = "Aujourd'hui",
                    action = "Voir l'agenda",
                    onAction = { onOpenTab(Routes.AGENDA) },
                )
            }

            if (state.events.isEmpty()) {
                item {
                    Text(
                        "Rien de prévu. La journée t'appartient.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(state.events, key = { "${it.id}-${it.startAt}" }) { event ->
                    EventRow(
                        event = event,
                        onClick = { onOpenEvent(event.id) },
                        onToggleDone = { vm.toggleEventDone(event) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            if (state.habits.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "Mes habitudes",
                        action = "Journal",
                        onAction = { onOpenTab(Routes.JOURNAL) },
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.habits, key = { it.habit.id }) { item ->
                            Card(
                                onClick = { vm.toggleHabit(item.habit.id, !item.doneToday) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (item.doneToday)
                                        accent(item.habit.colorIndex).copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceContainer
                                ),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Text(item.habit.emoji)
                                    Spacer(Modifier.width(8.dp))
                                    Text(item.habit.name, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        if (item.doneToday) Icons.Rounded.CheckCircle
                                        else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (item.doneToday) accent(item.habit.colorIndex)
                                        else MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle(
                    title = "Ma journée",
                    action = null,
                    onAction = {},
                )
                Card(
                    // Une journée déjà écrite s'ouvre en lecture ; une journée vierge
                    // va droit à l'écriture, comme le promet le libellé de la carte.
                    onClick = {
                        if (state.log?.hasContent == true) onReadDayLog(today)
                        else onOpenDayLog(today)
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (state.log?.journal.isNullOrBlank()) "Écrire ma journée"
                                else "Modifier ma journée",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                state.log?.journal?.takeIf { it.isNotBlank() }
                                    ?: "Humeur, énergie, symptômes, gratitude.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    }
                }
            }

            if (state.notes.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "Notes récentes",
                        action = "Toutes",
                        onAction = { onOpenTab(Routes.NOTES) },
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.notes, key = { it.id }) { note ->
                            Card(
                                onClick = { onOpenNote(note.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (note.colorIndex == 0)
                                        MaterialTheme.colorScheme.surfaceContainer
                                    else accent(note.colorIndex).copy(alpha = 0.18f)
                                ),
                                modifier = Modifier.width(190.dp),
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(
                                        if (note.isLocked) note.title.ifBlank { "Note protégée" }
                                        else note.title.ifBlank { "Sans titre" },
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        if (note.isLocked) "🔒 Touche pour déverrouiller"
                                        else note.body.lines().firstOrNull { it.isNotBlank() }.orEmpty()
                                            .removePrefix("[x] ").removePrefix("[ ] "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * La carte signature de l'app : dégradé, anneau de cycle animé et phrase du jour.
 * C'est la première chose qu'on voit en ouvrant Lumea.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleHeroCard(
    insight: CycleInsight?,
    expecting: PregnancyProgress?,
    nextCare: NextCare?,
    onClick: () -> Unit,
) {
    val onHero = onHeroColor
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(heroBrush())
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Overtitle(
                        if (expecting != null) "Ma grossesse" else "Mon cycle",
                        Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = onHero.copy(alpha = 0.7f),
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pendant une grossesse, l'anneau de l'accueil compte les
                    // semaines comme celui de l'écran Cycle : les deux doivent dire
                    // la même chose, sinon c'est l'accueil qu'on croit.
                    CycleRing(
                        dayOfCycle = expecting?.weeks ?: insight?.dayOfCycle ?: 0,
                        cycleLength = if (expecting != null) 40
                        else insight?.averageCycleLength ?: 28,
                        segments = when {
                            expecting != null -> trimesterSegments()
                            insight?.lastPeriodStart != null -> cycleSegments(
                                periodLength = insight.averagePeriodLength,
                                ovulationDay = insight.ovulationDayOfCycle,
                                cycleLength = insight.averageCycleLength,
                            )
                            else -> emptyList()
                        },
                        label = if (expecting != null) "SEMAINES (SA)" else "JOUR DU CYCLE",
                        size = 148.dp,
                        thickness = 13.dp,
                        contentColor = onHero,
                        progressColor = onHero,
                        trackColor = onHero.copy(alpha = 0.16f),
                    )

                    Spacer(Modifier.width(18.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            expecting?.label ?: insight?.phase?.label ?: "À découvrir",
                            style = MaterialTheme.typography.headlineSmall,
                            color = onHero,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            when {
                                // Le suivi réel, pas l'étape théorique de la semaine :
                                // une CPN cochée ne doit plus être réclamée.
                                nextCare != null -> nextCare.headline
                                expecting != null -> "Suivi à jour — rien à faire pour l'instant"
                                else -> insight?.phase?.hint
                                    ?: "Marque tes premières règles pour voir tes prévisions."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = onHero.copy(alpha = 0.82f),
                        )
                        if (expecting != null) {
                            Spacer(Modifier.height(12.dp))
                            Overtitle("Terme prévu", color = onHero.copy(alpha = 0.6f))
                            Text(
                                expecting.dueDate.format(
                                    DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = onHero,
                            )
                        } else if (insight?.nextPeriodStart != null) {
                            Spacer(Modifier.height(12.dp))
                            Overtitle(
                                if (insight.isLate) "Règles en retard" else "Prochaines règles",
                                color = onHero.copy(alpha = 0.6f),
                            )
                            Text(
                                if (insight.isLate) "${insight.daysLate} j"
                                else insight.nextPeriodStart.format(
                                    DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = onHero,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Carte d'accès aux leçons : un court texte + flèche, dans le même style que
 * les autres cartes de l'accueil.
 */
@Composable
private fun SectionTitle(title: String, action: String?, onAction: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 4.dp),
    ) {
        Overtitle(title, Modifier.weight(1f))
        if (action != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

/** Vrai dès qu'il y a quelque chose à relire dans la journée. */
private val net.mada.lumea.data.db.DailyLogEntity.hasContent: Boolean
    get() = journal.isNotBlank() || gratitude.isNotBlank() || mood > 0 ||
        energy > 0 || symptoms.isNotBlank() || flow > 0

private fun greeting(name: String): String {
    val hour = LocalTime.now().hour
    val base = when (hour) {
        in 5..11 -> "Bonjour"
        in 12..17 -> "Bon après-midi"
        in 18..22 -> "Bonsoir"
        else -> "Bonne nuit"
    }
    return if (name.isBlank()) base else "$base, $name"
}

/**
 * Accès au conseiller depuis l'accueil.
 *
 * Le texte insiste sur le hors-ligne : c'est ce qui le distingue de l'assistant
 * IA, et c'est ce qui rend acceptable qu'il connaisse le journal et le cycle.
 */
/**
 * Accès à l'assistant IA généraliste.
 *
 * Séparé du conseiller, et présenté comme tel : l'un reste sur le téléphone et
 * connaît tes données, l'autre sort du téléphone et ne les connaît pas. Les
 * confondre serait le seul vrai danger de cette fonctionnalité.
 */

/**
 * Les trois portes d'entrée secondaires de l'accueil, sur une seule ligne.
 *
 * Le conseiller est mis en avant : c'est le seul des trois qui répond avec les
 * données de la personne, et celui qu'on cherche quand quelque chose ne va pas.
 * L'assistant IA reste distinct visuellement — les confondre serait le seul vrai
 * risque, puisque l'un ne sort pas du téléphone et l'autre si.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActions(
    onAdvisor: () -> Unit,
    onAi: () -> Unit,
    onLessons: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        QuickAction(
            emoji = "💬",
            label = "Conseiller",
            hint = "hors ligne",
            highlighted = true,
            onClick = onAdvisor,
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            emoji = "✨",
            label = "Assistant IA",
            hint = "en ligne",
            highlighted = false,
            onClick = onAi,
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            emoji = "📖",
            label = "Apprendre",
            hint = "$LESSON_COUNT leçons",
            highlighted = false,
            onClick = onLessons,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAction(
    emoji: String,
    label: String,
    hint: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = if (highlighted) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Compté depuis le contenu : écrit en dur, le chiffre finissait par être faux. */
private val LESSON_COUNT = lessonTopics.sumOf { it.lessons.size }
