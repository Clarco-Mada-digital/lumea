package net.mada.lumea.ui.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.mada.lumea.domain.pregnancy.CareAct
import net.mada.lumea.domain.pregnancy.PregnancyProgress
import net.mada.lumea.domain.pregnancy.emoji
import net.mada.lumea.domain.pregnancy.milestoneFor
import net.mada.lumea.domain.pregnancy.pregnancyProgress
import net.mada.lumea.ui.components.RingSegment
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * L'écran Cycle, relu pendant une grossesse.
 *
 * Tant qu'un suivi est ouvert, parler de jour de cycle, de fenêtre fertile et de
 * risque de grossesse n'a plus aucun sens — c'est même déplacé. L'anneau compte
 * alors les semaines, le calendrier montre les trimestres, et la fiche d'un jour
 * dit où en sera la grossesse ce jour-là.
 */

/** Les couleurs des trois trimestres : une progression douce, sans alerte. */
object TrimesterColors {
    val first = Color(0xFF9A7DE0)
    val second = Color(0xFF5A9BD8)
    val third = Color(0xFF3FB8AC)
}

fun trimesterColor(trimester: Int): Color = when (trimester) {
    1 -> TrimesterColors.first
    2 -> TrimesterColors.second
    else -> TrimesterColors.third
}

/** Les arcs de l'anneau : un par trimestre, sur les 40 semaines du terme. */
fun trimesterSegments(): List<RingSegment> = listOf(
    // L'anneau raisonne en « jours » : ici une unité = une semaine d'aménorrhée.
    RingSegment(1, 14, TrimesterColors.first.copy(alpha = 0.75f)),
    RingSegment(15, 28, TrimesterColors.second.copy(alpha = 0.75f)),
    RingSegment(29, 40, TrimesterColors.third.copy(alpha = 0.75f)),
)

/**
 * Une case du calendrier pendant la grossesse.
 *
 * Trois repères seulement : le premier jour des dernières règles (d'où part tout
 * le calcul), aujourd'hui, et le terme prévu. Le reste est teinté par trimestre,
 * du plus pâle au plus soutenu à mesure qu'on avance.
 */
@Composable
fun PregnancyDayCell(
    date: LocalDate,
    inMonth: Boolean,
    progress: PregnancyProgress,
    hasLog: Boolean,
    acts: List<CareAct> = emptyList(),
    allDone: Boolean = false,
) {
    val isToday = date == LocalDate.now()
    val isStart = date == progress.lastPeriodStart
    val isDue = date == progress.dueDate
    val inPregnancy = !date.isBefore(progress.lastPeriodStart) && !date.isAfter(progress.dueDate)

    val dayProgress = if (inPregnancy) pregnancyProgress(progress.lastPeriodStart, date) else null
    val fill = when {
        isDue -> MaterialTheme.colorScheme.primary
        isStart -> MaterialTheme.colorScheme.secondaryContainer
        dayProgress != null -> trimesterColor(dayProgress.trimester)
            // Le passé est plus dense que l'avenir : on voit le chemin parcouru.
            .copy(alpha = if (date.isAfter(LocalDate.now())) 0.14f else 0.32f)
        else -> Color.Transparent
    }
    val onFill = when {
        isDue -> MaterialTheme.colorScheme.onPrimary
        !inMonth -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(if (inMonth) fill else fill.copy(alpha = fill.alpha * 0.35f))
            .then(
                when {
                    isToday -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    acts.isNotEmpty() && !allDone ->
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isDue || acts.isNotEmpty()) FontWeight.Bold
                else FontWeight.Normal,
                color = onFill,
            )
            when {
                isDue && inMonth -> Text(
                    "🎯",
                    style = MaterialTheme.typography.labelSmall,
                )
                // Un rendez-vous attendu ce jour-là : l'emoji de sa catégorie,
                // barré d'un ✓ une fois coché dans le carnet.
                acts.isNotEmpty() && inMonth -> Text(
                    if (allDone) "✓" else acts.first().category.emoji,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (allDone) MaterialTheme.colorScheme.primary else onFill,
                )
                isStart && inMonth -> Text(
                    "J1",
                    style = MaterialTheme.typography.labelSmall,
                    color = onFill,
                )
                hasLog && inMonth -> {
                    Spacer(Modifier.height(1.dp))
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(onFill.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}

/** La légende du calendrier de grossesse : elle remplace celle des risques. */
@Composable
fun PregnancyLegend(progress: PregnancyProgress, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendLine(
            TrimesterColors.first.copy(alpha = 0.32f),
            "1ᵉʳ trimestre · jusqu'à 14 SA",
            "Fatigue, nausées, et l'échographie de datation",
        )
        LegendLine(
            TrimesterColors.second.copy(alpha = 0.32f),
            "2ᵉ trimestre · 15 à 28 SA",
            "Souvent le plus confortable ; écho morphologique vers 22 SA",
        )
        LegendLine(
            TrimesterColors.third.copy(alpha = 0.32f),
            "3ᵉ trimestre · à partir de 29 SA",
            "Préparation à la naissance et consultation du 9ᵉ mois",
        )
        LegendLine(
            MaterialTheme.colorScheme.secondaryContainer,
            "J1 · ${progress.lastPeriodStart.format(dayMonthShort)}",
            "Premier jour des dernières règles : tout le calcul part de là",
        )
        LegendLine(
            MaterialTheme.colorScheme.primary,
            "🎯 Terme prévu · ${progress.dueDate.format(dayMonthShort)}",
            "40 SA pile. Un accouchement à terme va de 37 à 42 SA",
        )
        LegendLine(
            Color.Transparent,
            "🩺 💉 🦟  Rendez-vous attendu",
            "CPN, vaccin, dose de TPIg — cerclé tant que ce n'est pas coché, " +
                "✓ une fois fait",
            outline = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun LegendLine(color: Color, label: String, hint: String, outline: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (outline != null) Modifier.border(1.5.dp, outline, CircleShape)
                    else Modifier
                )
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * La fiche d'un jour pendant la grossesse.
 *
 * Même geste que la fiche de risque, contenu inversé : au lieu de « quel danger »,
 * elle répond à « où en sera la grossesse ce jour-là ».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PregnancyDaySheet(
    date: LocalDate,
    progress: PregnancyProgress,
    acts: List<CareAct> = emptyList(),
    doneCodes: Set<String> = emptySet(),
    onToggleAct: (String, Boolean) -> Unit = { _, _ -> },
    onWriteLog: () -> Unit,
    onOpenPregnancy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val atDate = pregnancyProgress(progress.lastPeriodStart, date)
    val inPregnancy = !date.isBefore(progress.lastPeriodStart) && atDate.weeks <= 42
    val color = trimesterColor(atDate.trimester)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                date.format(longDate).replaceFirstChar { it.titlecase(Locale.FRENCH) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            if (!inPregnancy) {
                Text("Hors grossesse", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ce jour est en dehors de la période suivie. Le suivi part du " +
                        "${progress.lastPeriodStart.format(longDate)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(atDate.label, style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${atDate.trimester}ᵉ trimestre · environ ${atDate.weeksOfPregnancy} " +
                        "semaines de grossesse",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )

                val daysToDue = ChronoUnit.DAYS.between(date, progress.dueDate)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        daysToDue > 0 -> "$daysToDue jours avant le terme prévu"
                        daysToDue == 0L -> "C'est le terme prévu"
                        else -> "${-daysToDue} jours après le terme prévu"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )

                // Les rendez-vous attendus ce jour-là, cochables sur place : on
                // regarde souvent le calendrier en sortant de consultation.
                if (acts.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Prévu ce jour-là", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    acts.forEach { act ->
                        val done = act.code in doneCodes
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (done)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp),
                            ) {
                                Text(
                                    act.category.emoji,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(act.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        act.why,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Checkbox(
                                    checked = done,
                                    onCheckedChange = { onToggleAct(act.code, it) },
                                )
                            }
                        }
                    }
                }

                milestoneFor(atDate.weeks)?.let { step ->
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                step.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(step.body, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onWriteLog, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Noter ce jour")
                }
                OutlinedButton(onClick = onOpenPregnancy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.MenuBook, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mon suivi")
                }
            }
        }
    }
}

private val longDate = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
private val dayMonthShort = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)

// ------------------------------------------------------- Après la naissance

/**
 * Une case du calendrier après la naissance.
 *
 * Il n'y a plus de cycle à peindre tant que les règles ne sont pas revenues, mais
 * il reste beaucoup à ne pas oublier : les quatre visites postnatales et les
 * vaccins du bébé. Ce sont eux que la grille affiche.
 */
@Composable
fun PostpartumDayCell(
    date: LocalDate,
    inMonth: Boolean,
    birthDate: LocalDate,
    hasLog: Boolean,
    acts: List<CareAct> = emptyList(),
    allDone: Boolean = false,
) {
    val isToday = date == LocalDate.now()
    val isBirth = date == birthDate

    val fill = when {
        isBirth -> MaterialTheme.colorScheme.primary
        acts.isNotEmpty() && allDone -> MaterialTheme.colorScheme.secondaryContainer
        date.isBefore(birthDate) -> Color.Transparent
        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
    }
    val onFill = when {
        isBirth -> MaterialTheme.colorScheme.onPrimary
        !inMonth -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(if (inMonth) fill else fill.copy(alpha = fill.alpha * 0.35f))
            .then(
                when {
                    isToday -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    acts.isNotEmpty() && !allDone ->
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isBirth || acts.isNotEmpty()) FontWeight.Bold
                else FontWeight.Normal,
                color = onFill,
            )
            when {
                isBirth && inMonth -> Text("👶", style = MaterialTheme.typography.labelSmall)
                acts.isNotEmpty() && inMonth -> Text(
                    if (allDone) "✓" else acts.first().category.emoji,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (allDone) MaterialTheme.colorScheme.primary else onFill,
                )
                hasLog && inMonth -> {
                    Spacer(Modifier.height(1.dp))
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(onFill.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}

@Composable
fun PostpartumLegend(birthDate: LocalDate, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendLine(
            MaterialTheme.colorScheme.primary,
            "👶 Naissance · ${birthDate.format(dayMonthShort)}",
            "Tout le suivi d'après-naissance se compte depuis ce jour",
        )
        LegendLine(
            Color.Transparent,
            "🏥 💉  Rendez-vous attendu",
            "Visite postnatale ou vaccin du bébé, à cocher une fois fait",
            outline = MaterialTheme.colorScheme.tertiary,
        )
        LegendLine(
            MaterialTheme.colorScheme.secondaryContainer,
            "✓ Fait",
            "Ce qui est confirmé dans ton carnet",
        )
        LegendLine(
            Color.Transparent,
            "Pas de prévision de cycle",
            "Tant que tes règles ne sont pas revenues, il n'y a rien à prévoir. " +
                "Indique leur retour depuis l'écran de suivi pour tout relancer.",
        )
    }
}

/** La fiche d'un jour après la naissance : l'âge du bébé et ce qui est prévu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostpartumDaySheet(
    date: LocalDate,
    birthDate: LocalDate,
    acts: List<CareAct> = emptyList(),
    doneCodes: Set<String> = emptySet(),
    onToggleAct: (String, Boolean) -> Unit = { _, _ -> },
    onWriteLog: () -> Unit,
    onOpenPregnancy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val days = ChronoUnit.DAYS.between(birthDate, date).toInt()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                date.format(longDate).replaceFirstChar { it.titlecase(Locale.FRENCH) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    days < 0 -> "Avant la naissance"
                    days == 0 -> "Jour de la naissance"
                    days < 14 -> "$days jours après la naissance"
                    days < 60 -> "${days / 7} semaines après la naissance"
                    else -> "${days / 30} mois après la naissance"
                },
                style = MaterialTheme.typography.headlineSmall,
            )

            if (acts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Prévu ce jour-là", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                acts.forEach { act ->
                    val done = act.code in doneCodes
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (done)
                                MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp),
                        ) {
                            Text(act.category.emoji, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(act.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    act.why,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Checkbox(
                                checked = done,
                                onCheckedChange = { onToggleAct(act.code, it) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onWriteLog, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Noter ce jour")
                }
                OutlinedButton(onClick = onOpenPregnancy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.MenuBook, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mon suivi")
                }
            }
        }
    }
}
