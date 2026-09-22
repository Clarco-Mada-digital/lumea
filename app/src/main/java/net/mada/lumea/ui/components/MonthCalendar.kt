package net.mada.lumea.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Grille mensuelle réutilisable (agenda et cycle s'en servent tous les deux).
 * L'affichage d'un jour est délégué à [dayContent] pour que chaque écran
 * décide de ses propres couleurs et pastilles.
 */
@Composable
fun MonthCalendar(
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    onDayClick: (LocalDate) -> Unit = {},
    showTodayShortcut: Boolean = true,
    dayContent: @Composable (date: LocalDate, inMonth: Boolean) -> Unit,
) {
    val locale = Locale.FRENCH
    // La semaine commence le lundi, comme dans un agenda papier français.
    val firstDayOfWeek = DayOfWeek.MONDAY

    Column(modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Mois précédent")
            }
            Text(
                month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
                    .replaceFirstChar { it.titlecase(locale) } + " " + month.year,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Mois suivant")
            }
        }

        if (showTodayShortcut && month != YearMonth.now()) {
            TextButton(
                onClick = { onMonthChange(YearMonth.now()) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Revenir à aujourd'hui") }
        }

        Spacer(Modifier.height(4.dp))

        Row(Modifier.fillMaxWidth()) {
            repeat(7) { index ->
                val day = firstDayOfWeek.plus(index.toLong())
                Text(
                    day.getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // On remplit la grille depuis le lundi qui précède (ou égale) le 1er du mois.
        val first = month.atDay(1)
        val lead = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val gridStart = first.minusDays(lead.toLong())
        val weeks = 6

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(weeks) { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { dow ->
                        val date = gridStart.plusDays((week * 7 + dow).toLong())
                        val inMonth = YearMonth.from(date) == month
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clickable(enabled = inMonth) { onDayClick(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            dayContent(date, inMonth)
                        }
                    }
                }
            }
        }
    }
}
