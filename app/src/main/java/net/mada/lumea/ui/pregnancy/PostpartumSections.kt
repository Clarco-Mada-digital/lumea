package net.mada.lumea.ui.pregnancy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.mada.lumea.data.db.PrenatalCareEntity
import net.mada.lumea.domain.pregnancy.INFANT_VACCINES
import net.mada.lumea.domain.pregnancy.MamaCondition
import net.mada.lumea.domain.pregnancy.MamaStatus
import net.mada.lumea.domain.pregnancy.POSTNATAL_VISITS
import net.mada.lumea.domain.pregnancy.POSTPARTUM_ADVICE
import net.mada.lumea.domain.pregnancy.POSTPARTUM_CONTRACEPTION
import net.mada.lumea.domain.pregnancy.PostpartumProgress
import net.mada.lumea.domain.pregnancy.CareStatus
import net.mada.lumea.domain.pregnancy.careStatus
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * La contraception par allaitement, vérifiée condition par condition.
 *
 * C'est le cœur de l'après-naissance. La MAMA protège à environ 98 % — autant
 * qu'une pilule bien prise — mais uniquement si les trois conditions tiennent
 * ensemble. Les cocher une par une est le seul moyen honnête de le présenter :
 * afficher « protégée » sans vérifier reviendrait à promettre quelque chose que
 * l'app ne sait pas.
 */
@Composable
fun MamaSection(
    status: MamaStatus,
    care: Map<String, PrenatalCareEntity>,
    onToggle: (String, Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (status.protected)
                MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                status.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (status.protected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                status.body,
                style = MaterialTheme.typography.bodyMedium,
                color = if (status.protected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }

    Spacer(Modifier.height(14.dp))
    Text(
        "Les trois conditions — il faut les trois",
        style = MaterialTheme.typography.titleSmall,
    )
    Spacer(Modifier.height(6.dp))

    MamaCondition.entries.forEach { condition ->
        val checked = care.containsKey(condition.code)
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Icon(
                if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (checked) "Oui" else "Non",
                tint = if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(condition.question, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(2.dp))
                Text(
                    condition.why,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(modifier = Modifier.padding(start = 36.dp, bottom = 6.dp)) {
            FilterChip(
                selected = checked,
                onClick = { onToggle(condition.code, true) },
                label = { Text("Oui") },
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = !checked,
                onClick = { onToggle(condition.code, false) },
                label = { Text("Non") },
            )
        }
    }

    Spacer(Modifier.height(10.dp))
    Text(
        "À revérifier tous les mois. L'ovulation revient avant les règles : on peut " +
            "retomber enceinte sans avoir eu un seul cycle entre les deux.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Les visites postnatales et les vaccins du bébé, cochés comme le carnet prénatal. */
@Composable
fun PostpartumChecklist(
    progress: PostpartumProgress,
    care: Map<String, PrenatalCareEntity>,
    onToggle: (String, Boolean) -> Unit,
) {
    Text("Consultations après la naissance", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    POSTNATAL_VISITS.forEach { act ->
        PostpartumRow(
            title = act.title,
            body = act.why,
            done = care.containsKey(act.code),
            late = careStatus(act, progress.weeksSince, care.containsKey(act.code)) == CareStatus.EN_RETARD,
            doneOn = care[act.code]?.doneOn?.format(shortDate),
            onToggle = { onToggle(act.code, it) },
        )
    }

    Spacer(Modifier.height(16.dp))
    Text("Vaccins du bébé", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Text(
        "Un retard n'annule jamais la protection, il la décale : un rattrapage est " +
            "toujours possible, n'abandonne pas le calendrier.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
    INFANT_VACCINES.forEach { act ->
        PostpartumRow(
            title = act.title,
            body = act.why,
            done = care.containsKey(act.code),
            late = careStatus(act, progress.weeksSince, care.containsKey(act.code)) == CareStatus.EN_RETARD,
            doneOn = care[act.code]?.doneOn?.format(shortDate),
            onToggle = { onToggle(act.code, it) },
        )
    }
}

@Composable
private fun PostpartumRow(
    title: String,
    body: String,
    done: Boolean,
    late: Boolean,
    doneOn: String?,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
    ) {
        Icon(
            if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = if (done) "Fait" else "À faire",
            tint = when {
                done -> MaterialTheme.colorScheme.primary
                late -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.outline
            },
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (doneOn != null) {
                Text(
                    "Fait le $doneOn",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (late) {
                Text(
                    "En retard",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Checkbox(checked = done, onCheckedChange = onToggle)
    }
}

/** Les conseils pour la mère, et les contraceptions compatibles avec l'allaitement. */
@Composable
fun PostpartumAdviceSections() {
    POSTPARTUM_ADVICE.forEach { (title, body) -> InfoBlock(title, body) }
}

@Composable
fun ContraceptionSections() {
    Text(
        "Toutes ces méthodes sont compatibles avec l'allaitement. L'app ne prescrit " +
            "rien : elle te donne les mots pour demander.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    POSTPARTUM_CONTRACEPTION.forEach { (title, body) -> InfoBlock(title, body) }
}

private val shortDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)
