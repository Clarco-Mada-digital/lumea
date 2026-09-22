package net.mada.lumea.ui.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.mada.lumea.domain.cycle.CycleInsight
import net.mada.lumea.domain.pregnancy.TEST_RECOMMENDED_DAYS
import net.mada.lumea.domain.pregnancy.TEST_SUGGESTION_DAYS
import net.mada.lumea.ui.theme.CycleColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * La carte de suivi des règles : le point où l'app demande confirmation au lieu
 * de supposer.
 *
 * Jusqu'ici, passé la date prévue, le moteur décalait la prévision d'un cycle
 * entier et faisait comme si les règles étaient arrivées le jour dit. Résultat :
 * un retard ne se voyait jamais, et la moyenne se calculait sur des dates
 * inventées. Ici, rien n'avance sans un « oui » — et chaque « oui » dit en retour
 * si le cycle a été plus court ou plus long que prévu.
 */
@Composable
fun PeriodTrackerCard(
    insight: CycleInsight,
    onConfirmStart: (LocalDate) -> Unit,
    onEndPeriod: (LocalDate) -> Unit,
    onReopen: () -> Unit,
    onOfferTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        // 1. Les règles sont en cours : reste à savoir quand elles s'arrêtent.
        insight.currentPeriodStart != null -> OngoingCard(
            insight = insight,
            onEndPeriod = onEndPeriod,
            modifier = modifier,
        )

        // 2. Elles étaient attendues et ne sont pas enregistrées : on demande.
        insight.isLate || insight.expectedPeriodStart == LocalDate.now() -> DueCard(
            insight = insight,
            onConfirmStart = onConfirmStart,
            onOfferTest = onOfferTest,
            modifier = modifier,
        )

        // 3. Rien à confirmer : on résume le dernier cycle, et on laisse rouvrir
        //    des règles déclarées finies trop tôt.
        else -> RecapCard(insight = insight, onReopen = onReopen, modifier = modifier)
    }
}

@Composable
private fun OngoingCard(
    insight: CycleInsight,
    onEndPeriod: (LocalDate) -> Unit,
    modifier: Modifier,
) {
    val day = insight.currentPeriodDay ?: 1
    val expected = insight.averagePeriodLength

    TrackerShell(
        container = CycleColors.period.copy(alpha = 0.16f),
        emoji = "🩸",
        title = "Jour $day de tes règles",
        body = when {
            day < expected -> "D'habitude elles durent $expected jours."
            day == expected -> "C'est la durée habituelle. Toujours là ?"
            else -> "C'est ${day - expected} jour${plural(day - expected)} de plus " +
                "que d'habitude ($expected j). Si ça se prolonge beaucoup, parles-en."
        },
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onEndPeriod(LocalDate.now()) },
                colors = ButtonDefaults.buttonColors(containerColor = CycleColors.period),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("C'est fini")
            }
            OutlinedButton(
                onClick = { onEndPeriod(LocalDate.now().minusDays(1)) },
                modifier = Modifier.weight(1f),
            ) { Text("Fini hier") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueCard(
    insight: CycleInsight,
    onConfirmStart: (LocalDate) -> Unit,
    onOfferTest: () -> Unit,
    modifier: Modifier,
) {
    var pickingDate by remember { mutableStateOf(false) }
    val expected = insight.expectedPeriodStart
    val late = insight.daysLate

    TrackerShell(
        container = MaterialTheme.colorScheme.tertiaryContainer,
        emoji = if (late > 0) "⏳" else "📅",
        title = if (late > 0) {
            "Règles en retard de $late jour${plural(late)}"
        } else {
            "Tes règles sont attendues aujourd'hui"
        },
        body = buildString {
            if (expected != null) append("Prévues le ${expected.format(longDate)}. ")
            append("Sont-elles arrivées ? ")
            append(
                when {
                    late == 0 ->
                        "Confirme dès le premier jour de saignement : c'est ce qui garde " +
                            "les prévisions justes."
                    // Le cas le plus fréquent de loin : on rassure, on ne propose rien.
                    late < TEST_SUGGESTION_DAYS ->
                        "Un décalage de quelques jours arrive à tout le monde — stress, " +
                            "sommeil, sport, rien de particulier. Pas de quoi s'alarmer " +
                            "à ce stade."
                    late < TEST_RECOMMENDED_DAYS ->
                        "Le retard commence à se voir. Si un rapport non protégé a eu " +
                            "lieu, un test est fiable dès maintenant."
                    else ->
                        "Plus d'une semaine de retard : un test de grossesse lève le " +
                            "doute, dans un sens comme dans l'autre."
                }
            )
        },
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onConfirmStart(LocalDate.now()) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.WaterDrop, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Oui, aujourd'hui")
            }
            OutlinedButton(
                onClick = { pickingDate = true },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.EventAvailable, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Un autre jour")
            }
        }

        /*
         * Le lien n'apparaît qu'à partir de cinq jours. En dessous, proposer un
         * test à chaque retard d'un ou deux jours — ce qui arrive plusieurs fois
         * par an — ne ferait que fabriquer de l'inquiétude.
         */
        if (late >= TEST_SUGGESTION_DAYS) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOfferTest, modifier = Modifier.fillMaxWidth()) {
                Text("Pas arrivées… et si c'était une grossesse ?")
            }
        }
    }

    if (pickingDate) {
        // On ne laisse pas choisir une date future : on confirme ce qui est arrivé.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.now().toUtcMillis(),
            selectableDates = PastDatesOnly,
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onConfirmStart(it.toLocalDate()) }
                    pickingDate = false
                }) { Text("Confirmer") }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) { Text("Annuler") }
            },
        ) {
            DatePicker(state = pickerState, title = {
                Text(
                    "Premier jour de saignement",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 20.dp),
                )
            })
        }
    }
}

@Composable
private fun RecapCard(insight: CycleInsight, onReopen: () -> Unit, modifier: Modifier) {
    val next = insight.expectedPeriodStart ?: return
    val daysUntil = insight.daysUntilNextPeriod ?: 0

    TrackerShell(
        container = MaterialTheme.colorScheme.surfaceContainer,
        emoji = "📅",
        title = if (daysUntil <= 0) "Prochaines règles bientôt"
        else "Prochaines règles dans $daysUntil jour${plural(daysUntil)}",
        body = buildString {
            append("Prévues le ${next.format(longDate)}")
            if (insight.regularityDays > 3) {
                append(", à ${insight.regularityDays} jours près vu tes cycles passés")
            }
            append(". ")
            val last = insight.lastPeriodLength
            if (last != null) {
                append("Tes dernières règles ont duré $last jour${plural(last)}.")
            }
        },
        modifier = modifier,
    ) {
        if (insight.lastPeriodLength != null) {
            TextButton(onClick = onReopen) {
                Icon(Icons.Rounded.Undo, contentDescription = null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Elles ne sont pas finies, en fait")
            }
        }
    }
}

/** Le contenant commun des trois états, pour qu'ils se ressemblent. */
@Composable
private fun TrackerShell(
    container: androidx.compose.ui.graphics.Color,
    emoji: String,
    title: String,
    body: String,
    modifier: Modifier,
    actions: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            actions()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private object PastDatesOnly : androidx.compose.material3.SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long) =
        utcTimeMillis <= LocalDate.now().toUtcMillis()
}

private fun plural(n: Int) = if (n > 1) "s" else ""

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.of("UTC")).toLocalDate()

private val longDate = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
