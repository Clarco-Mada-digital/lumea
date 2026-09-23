package net.mada.lumea.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.mada.lumea.backup.EraseSelection

/**
 * Effacer ses données, catégorie par catégorie.
 *
 * Il manquait un moyen simple de repartir de zéro. On y arrivait en restaurant
 * une sauvegarde vide, ce qui était détourné et laissait des restes — notamment
 * les rendez-vous que le carnet de suivi avait posés dans l'agenda, qui
 * survivaient à l'effacement du suivi lui-même.
 *
 * Deux principes : on choisit ce qu'on efface, et on nomme ce qu'on perd. Pas de
 * bouton « tout effacer » sans dire ce que « tout » recouvre.
 */
@Composable
fun EraseSection(onErase: (EraseSelection) -> Unit) {
    var choosing by remember { mutableStateOf(false) }

    Text(
        "Repartir de zéro, en choisissant ce qui part. L'effacement est immédiat " +
            "et définitif : s'il y a quelque chose à garder, exporte une sauvegarde " +
            "avant.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(10.dp))
    OutlinedButton(onClick = { choosing = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Effacer mes données…")
    }

    if (choosing) {
        EraseDialog(
            onDismiss = { choosing = false },
            onConfirm = {
                choosing = false
                onErase(it)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EraseDialog(onDismiss: () -> Unit, onConfirm: (EraseSelection) -> Unit) {
    var selection by remember { mutableStateOf(EraseSelection(false, false, false, false, false)) }
    var confirming by remember { mutableStateOf(false) }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Effacer définitivement ?") },
            text = {
                Text(
                    "Tu vas perdre ${describe(selection)}.\n\n" +
                        "Il n'y a pas d'annulation, et aucune copie n'est gardée."
                )
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(selection) }) {
                    Text("Oui, effacer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Annuler") }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Que veux-tu effacer ?") },
        text = {
            Column {
                EraseRow(
                    label = "Notes et dossiers",
                    checked = selection.notes,
                    onCheck = { selection = selection.copy(notes = it) },
                )
                EraseRow(
                    label = "Agenda",
                    detail = "Y compris les rendez-vous posés par le carnet de suivi",
                    checked = selection.events,
                    onCheck = { selection = selection.copy(events = it) },
                )
                EraseRow(
                    label = "Cycle, grossesse et carnet",
                    detail = "Règles, prévisions, suivi, résultats — et les rendez-vous " +
                        "que le carnet avait ajoutés à l'agenda",
                    checked = selection.cycle,
                    onCheck = { selection = selection.copy(cycle = it) },
                )
                EraseRow(
                    label = "Journal",
                    detail = "Humeur, symptômes, textes, gratitude",
                    checked = selection.journal,
                    onCheck = { selection = selection.copy(journal = it) },
                )
                EraseRow(
                    label = "Habitudes",
                    checked = selection.habits,
                    onCheck = { selection = selection.copy(habits = it) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { confirming = true },
                enabled = !selection.isEmpty,
            ) {
                Text("Continuer", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun EraseRow(
    label: String,
    detail: String? = null,
    checked: Boolean,
    onCheck: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheck)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Nomme ce qui part, pour que la confirmation dise vraiment quelque chose. */
private fun describe(selection: EraseSelection): String {
    val parts = buildList {
        if (selection.notes) add("tes notes")
        if (selection.events) add("ton agenda")
        if (selection.cycle) add("ton cycle et ton suivi de grossesse")
        if (selection.journal) add("ton journal")
        if (selection.habits) add("tes habitudes")
    }
    return when (parts.size) {
        0 -> "rien"
        1 -> parts.first()
        else -> parts.dropLast(1).joinToString(", ") + " et " + parts.last()
    }
}
