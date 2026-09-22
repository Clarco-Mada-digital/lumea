package net.mada.lumea.ui.pregnancy

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import net.mada.lumea.data.db.PregnancyEntity
import net.mada.lumea.data.db.PrenatalCareEntity
import net.mada.lumea.domain.pregnancy.ALL_CARE_ACTS
import net.mada.lumea.domain.pregnancy.CareAct
import net.mada.lumea.domain.pregnancy.CareCategory
import net.mada.lumea.domain.pregnancy.CareStatus
import net.mada.lumea.domain.pregnancy.RECORDED_RESULTS
import net.mada.lumea.domain.pregnancy.RISK_FACTORS
import net.mada.lumea.domain.pregnancy.SYMPTOM_TRIAGE
import net.mada.lumea.domain.pregnancy.Urgency
import net.mada.lumea.domain.pregnancy.WHERE_TO_GO
import net.mada.lumea.domain.pregnancy.careStatus
import net.mada.lumea.domain.pregnancy.emoji
import net.mada.lumea.domain.pregnancy.hint
import net.mada.lumea.domain.pregnancy.label
import net.mada.lumea.domain.pregnancy.riskFactor
import net.mada.lumea.domain.pregnancy.title
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Le carnet de suivi : la liste des actes, cochés au fur et à mesure.
 *
 * C'est ce qui sépare une brochure d'un vrai suivi. Une CPN ratée ne se rattrape
 * pas, et personne ne retient de tête qu'il faut une troisième dose de TPIg. Ici,
 * chaque ligne dit où on en est : à faire, bientôt, en retard, ou fait tel jour.
 */
@Composable
fun CareChecklist(
    currentWeek: Int,
    care: Map<String, PrenatalCareEntity>,
    onToggle: (String, Boolean) -> Unit,
) {
    CareCategory.entries.forEach { category ->
        val acts = ALL_CARE_ACTS.filter { it.category == category }
        if (acts.isEmpty()) return@forEach

        Spacer(Modifier.height(10.dp))
        Text(
            "${category.emoji}  ${category.label}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        acts.forEach { act ->
            CareRow(
                act = act,
                status = careStatus(act, currentWeek, care.containsKey(act.code)),
                doneOn = care[act.code]?.doneOn,
                onToggle = { onToggle(act.code, it) },
            )
        }
    }
}

@Composable
private fun CareRow(
    act: CareAct,
    status: CareStatus,
    doneOn: LocalDate?,
    onToggle: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val done = status == CareStatus.FAIT

    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { expanded = !expanded }
            .padding(vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (done) "Fait" else "À faire",
                tint = if (done) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onToggle(!done) },
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    act.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (done) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    when {
                        doneOn != null -> "Fait le ${doneOn.format(shortDate)}"
                        else -> statusLabel(status, act)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor(status),
                )
            }
        }
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            Text(
                act.why,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 36.dp),
            )
        }
    }
}

private fun statusLabel(status: CareStatus, act: CareAct): String = when (status) {
    CareStatus.FAIT -> "Fait"
    CareStatus.EN_RETARD -> "En retard — c'était à faire avant ${act.toWeek} SA"
    CareStatus.A_FAIRE -> "À faire maintenant"
    CareStatus.BIENTOT -> "Bientôt — à partir de ${act.fromWeek} SA"
    CareStatus.PLUS_TARD -> "Plus tard — vers ${act.fromWeek} SA"
}

@Composable
private fun statusColor(status: CareStatus): Color = when (status) {
    CareStatus.FAIT -> MaterialTheme.colorScheme.primary
    CareStatus.EN_RETARD -> MaterialTheme.colorScheme.error
    CareStatus.A_FAIRE -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.outline
}

/**
 * Le soignant qui suit la grossesse, et le mode d'emploi de son numéro.
 *
 * Une sage-femme de CSB n'a pas de secrétariat : son téléphone est souvent son
 * numéro personnel. Le bouton d'appel est donc posé **après** le tri des
 * symptômes, jamais avant — l'ordre à l'écran fait partie du message.
 */
@Composable
fun CaregiverSection(
    pregnancy: PregnancyEntity,
    onSave: (String, String, String, String) -> Unit,
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }

    if (pregnancy.caregiverName.isBlank() && pregnancy.caregiverPhone.isBlank()) {
        Text(
            "Enregistre le nom et le numéro de la sage-femme ou du soignant qui te " +
                "suit. Le jour où ça ne va pas, on ne cherche pas un numéro.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Edit, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ajouter mon soignant")
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    pregnancy.caregiverName.ifBlank { "Soignant" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (pregnancy.caregiverRole.isNotBlank()) {
                    Text(
                        pregnancy.caregiverRole,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (pregnancy.facility.isNotBlank()) {
                    Text(
                        pregnancy.facility,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                if (pregnancy.caregiverPhone.isNotBlank()) {
                    Text(
                        pregnancy.caregiverPhone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            TextButton(onClick = { editing = true }) { Text("Modifier") }
        }
    }

    Spacer(Modifier.height(16.dp))
    Text("Quand l'appeler — et quand ne pas appeler", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))

    Urgency.entries.forEach { urgency ->
        val symptoms = SYMPTOM_TRIAGE.filter { it.urgency == urgency }
        if (symptoms.isEmpty()) return@forEach

        Card(
            colors = CardDefaults.cardColors(containerColor = urgencyColor(urgency)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    urgency.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = onUrgencyColor(urgency),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    urgency.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = onUrgencyColor(urgency).copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(8.dp))
                symptoms.forEach { symptom ->
                    Text(
                        "• ${symptom.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onUrgencyColor(urgency),
                    )
                    Text(
                        symptom.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = onUrgencyColor(urgency).copy(alpha = 0.85f),
                        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
                    )
                }
            }
        }
    }

    if (pregnancy.caregiverPhone.isNotBlank()) {
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                // ACTION_DIAL ouvre le clavier avec le numéro : pas de permission
                // requise, et c'est l'utilisatrice qui décide de lancer l'appel.
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pregnancy.caregiverPhone}"))
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Call, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Appeler ${pregnancy.caregiverName.ifBlank { "le soignant" }}")
        }
    }

    Spacer(Modifier.height(16.dp))
    Text("Où aller", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Text(
        "Il n'existe pas de numéro d'urgence santé national unique à Madagascar : " +
            "le recours est géographique.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    WHERE_TO_GO.forEach { (title, body) -> InfoBlock(title, body) }

    if (editing) {
        CaregiverDialog(
            pregnancy = pregnancy,
            onDismiss = { editing = false },
            onSave = { name, role, phone, facility ->
                onSave(name, role, phone, facility)
                editing = false
            },
        )
    }
}

@Composable
private fun urgencyColor(urgency: Urgency): Color = when (urgency) {
    Urgency.PARTIR_MAINTENANT -> MaterialTheme.colorScheme.errorContainer
    Urgency.APPELER_AUJOURDHUI -> MaterialTheme.colorScheme.tertiaryContainer
    Urgency.PROCHAINE_CPN -> MaterialTheme.colorScheme.surfaceContainerHigh
}

@Composable
private fun onUrgencyColor(urgency: Urgency): Color = when (urgency) {
    Urgency.PARTIR_MAINTENANT -> MaterialTheme.colorScheme.onErrorContainer
    Urgency.APPELER_AUJOURDHUI -> MaterialTheme.colorScheme.onTertiaryContainer
    Urgency.PROCHAINE_CPN -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun CaregiverDialog(
    pregnancy: PregnancyEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(pregnancy.caregiverName) }
    var role by remember { mutableStateOf(pregnancy.caregiverRole.ifBlank { "Sage-femme" }) }
    var phone by remember { mutableStateOf(pregnancy.caregiverPhone) }
    var facility by remember { mutableStateOf(pregnancy.facility) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mon soignant") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Sage-femme, médecin, agent…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = facility,
                    onValueChange = { facility = it },
                    label = { Text("CSB ou maternité") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, role, phone, facility) }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/**
 * Les situations qui font surveiller la grossesse de plus près.
 *
 * L'app ne diagnostique rien. Elle retient ce qui a été déclaré pour afficher le
 * conseil correspondant et pour que rien ne soit oublié à la première CPN.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RiskFactorSection(selected: Set<String>, onToggle: (String) -> Unit) {
    Text(
        "Coche ce qui te concerne. Rien n'est un diagnostic : ce sont des points à " +
            "signaler à ta sage-femme, et l'app te rappellera ce qu'ils impliquent.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RISK_FACTORS.forEach { factor ->
            FilterChip(
                selected = factor.code in selected,
                onClick = { onToggle(factor.code) },
                label = { Text(factor.label) },
            )
        }
    }

    if (selected.isNotEmpty()) {
        Spacer(Modifier.height(14.dp))
        selected.mapNotNull(::riskFactor).forEach { factor ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        factor.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(factor.advice, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/**
 * Les résultats à garder sous la main : groupe sanguin, échographies, hémoglobine.
 *
 * Saisis à la main, jamais interprétés par l'app. Les comptes rendus se perdent et
 * on oublie toujours son groupe sanguin au moment où il faudrait le donner vite.
 */
@Composable
fun ResultsSection(
    care: Map<String, PrenatalCareEntity>,
    onSaveNote: (String, String) -> Unit,
) {
    var editing by remember { mutableStateOf<CareAct?>(null) }

    RECORDED_RESULTS.forEach { act ->
        val recorded = care[act.code]
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { editing = act }
                .padding(vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(act.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        recorded?.note?.takeIf { it.isNotBlank() } ?: "Non renseigné",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (recorded?.note.isNullOrBlank())
                            MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.primary,
                    )
                }
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Modifier",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                act.why,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    editing?.let { act ->
        var text by remember(act.code) { mutableStateOf(care[act.code]?.note.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(act.title) },
            text = {
                Column {
                    Text(
                        act.why,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Ce qui t'a été dit") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveNote(act.code, text)
                    editing = null
                }) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text("Annuler") }
            },
        )
    }
}

/** Petit repère coloré, pour les listes de la fiche. */
@Composable
internal fun Dot(color: Color) {
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

private val shortDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)
