package net.mada.lumea.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.mada.lumea.backup.Backup
import net.mada.lumea.backup.BackupSelection
import net.mada.lumea.backup.RestoreMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * La fiche d'une sauvegarde, avant d'y toucher.
 *
 * Restaurer était jusqu'ici irréversible et silencieux : choisir un fichier
 * suffisait à écraser toute la base. Ici on montre d'abord ce que le fichier
 * contient, catégorie par catégorie, on laisse décocher ce qu'on ne veut pas
 * reprendre, choisir entre remplacer et fusionner, et on demande une dernière
 * confirmation — nommée, pas un « OK » anonyme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreSheet(
    backup: Backup,
    restoring: Boolean,
    onRestore: (BackupSelection, RestoreMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var selection by remember { mutableStateOf(BackupSelection()) }
    var mode by remember { mutableStateOf(RestoreMode.REPLACE) }
    var confirming by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = { if (!restoring) onDismiss() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Restaurer une sauvegarde", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Fichier du ${formatDate(backup.exportedAt)} · ${backup.count()} éléments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))
            Text("Que veux-tu reprendre ?", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            CategoryRow(
                icon = Icons.Rounded.StickyNote2,
                label = "Notes",
                detail = countLabel(backup.notes.size, "note") +
                    if (backup.folders.isNotEmpty()) " · ${backup.folders.size} dossiers" else "",
                checked = selection.notes,
                onCheck = { selection = selection.copy(notes = it) },
            )
            CategoryRow(
                icon = Icons.Rounded.CalendarMonth,
                label = "Agenda",
                detail = countLabel(backup.events.size, "événement"),
                checked = selection.events,
                onCheck = { selection = selection.copy(events = it) },
            )
            CategoryRow(
                icon = Icons.Rounded.Favorite,
                label = "Cycle",
                detail = countLabel(backup.periods.size, "période de règles"),
                checked = selection.periods,
                onCheck = { selection = selection.copy(periods = it) },
            )
            CategoryRow(
                icon = Icons.Rounded.MenuBook,
                label = "Journal",
                detail = countLabel(backup.logs.size, "journée"),
                checked = selection.logs,
                onCheck = { selection = selection.copy(logs = it) },
            )
            CategoryRow(
                icon = Icons.Rounded.Tune,
                label = "Réglages",
                detail = if (backup.settings != null) {
                    "Prénom, thème, longueur de cycle, rappels, assistant"
                } else {
                    "aucune donnée"
                },
                checked = selection.settings,
                onCheck = { selection = selection.copy(settings = it) },
            )
            CategoryRow(
                icon = Icons.Rounded.TaskAlt,
                label = "Habitudes",
                detail = countLabel(backup.habits.size, "habitude") +
                    " · ${backup.habitChecks.size} cases cochées",
                checked = selection.habits,
                onCheck = { selection = selection.copy(habits = it) },
            )

            Spacer(Modifier.height(20.dp))
            Text("Comment ?", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            ModeCard(
                title = "Remplacer",
                body = "Les catégories cochées sont vidées, puis remplacées par la " +
                    "sauvegarde. Ce qui n'est pas coché n'est pas touché.",
                selected = mode == RestoreMode.REPLACE,
                onSelect = { mode = RestoreMode.REPLACE },
            )
            Spacer(Modifier.height(8.dp))
            ModeCard(
                title = "Fusionner",
                body = "Rien n'est supprimé : la sauvegarde s'ajoute à ce que tu as " +
                    "déjà. Un élément modifié des deux côtés prend la version du fichier.",
                selected = mode == RestoreMode.MERGE,
                onSelect = { mode = RestoreMode.MERGE },
            )

            if (mode == RestoreMode.REPLACE) {
                Spacer(Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "⚠️  Ce qui est actuellement dans les catégories cochées sera " +
                            "définitivement perdu. Il n'y a pas d'annulation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            if (restoring) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(
                    "Restauration en cours…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = { confirming = true },
                        enabled = !selection.isEmpty,
                        colors = if (mode == RestoreMode.REPLACE) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            )
                        } else ButtonDefaults.buttonColors(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (mode == RestoreMode.REPLACE) "Remplacer" else "Fusionner")
                    }
                }
                if (selection.isEmpty) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Coche au moins une catégorie.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (confirming) {
        val count = backup.count(selection)
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = {
                Text(
                    if (mode == RestoreMode.REPLACE) "Remplacer ces données ?"
                    else "Fusionner cette sauvegarde ?"
                )
            },
            text = {
                Text(
                    if (mode == RestoreMode.REPLACE) {
                        "$count éléments vont être écrits, et ${selectedLabel(selection)} " +
                            "de cet appareil sera définitivement effacé. Cette action " +
                            "ne peut pas être annulée."
                    } else {
                        "$count éléments vont s'ajouter à ${selectedLabel(selection)} " +
                            "de cet appareil. Rien ne sera supprimé."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onRestore(selection, mode)
                }) {
                    Text(
                        if (mode == RestoreMode.REPLACE) "Oui, remplacer" else "Oui, fusionner",
                        color = if (mode == RestoreMode.REPLACE) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun CategoryRow(
    icon: ImageVector,
    label: String,
    detail: String,
    checked: Boolean,
    onCheck: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onCheck(!checked) }
            .padding(vertical = 4.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (checked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Checkbox(checked = checked, onCheckedChange = onCheck)
    }
}

@Composable
private fun ModeCard(
    title: String,
    body: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
    ) {
        Row(Modifier.padding(14.dp)) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** « 12 notes », « 1 note », « aucune note ». */
private fun countLabel(count: Int, singular: String): String = when (count) {
    0 -> "aucune donnée"
    1 -> "1 $singular"
    else -> "$count ${singular}s"
}

/** Nomme les catégories cochées, pour que la confirmation dise ce qu'elle efface. */
private fun selectedLabel(selection: BackupSelection): String {
    val parts = buildList {
        if (selection.notes) add("les notes")
        if (selection.events) add("l'agenda")
        if (selection.periods) add("le cycle")
        if (selection.logs) add("le journal")
        if (selection.habits) add("les habitudes")
        if (selection.settings) add("les réglages")
    }
    return when (parts.size) {
        0 -> "rien"
        1 -> parts.first()
        else -> parts.dropLast(1).joinToString(", ") + " et " + parts.last()
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy 'à' HH'h'mm", Locale.FRENCH)

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateFormat)
