package net.mada.lumea.ui.pregnancy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.mada.lumea.domain.pregnancy.NEGATIVE_CAUSES
import net.mada.lumea.domain.pregnancy.TEST_HOW_TO
import net.mada.lumea.domain.pregnancy.testReliability

/**
 * La feuille « tu veux faire un test ? », ouverte depuis un retard de règles.
 *
 * Elle répond dans l'ordre aux trois questions que tout le monde se pose : est-ce
 * que c'est le bon moment, comment on s'y prend, et qu'est-ce que je fais du
 * résultat. Aucune des trois n'a de réponse évidente quand on a seize ans.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PregnancyTestSheet(
    daysLate: Int,
    onResult: (TestResult) -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmingPositive by remember { mutableStateOf(false) }
    val reliability = testReliability(daysLate)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Faire un test de grossesse", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (reliability.reliable)
                        MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        reliability.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(reliability.body, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Comment ça marche", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            TEST_HOW_TO.forEach { (title, body) ->
                InfoBlock(title = title, body = body)
            }

            Spacer(Modifier.height(24.dp))
            Text("Tu as déjà le résultat ?", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "L'app s'en sert pour adapter ce qu'elle te propose ensuite. Rien ne " +
                    "sort de ce téléphone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { confirmingPositive = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Done, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Positif")
                }
                OutlinedButton(
                    onClick = { onResult(TestResult.NEGATIVE) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Négatif")
                }
            }
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = { onResult(TestResult.UNCLEAR) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Pas clair, ou pas encore fait") }

            Spacer(Modifier.height(16.dp))
            Text(
                "Un test négatif fait trop tôt est fréquent ; un faux positif est rare. " +
                    "En cas de doute, une prise de sang en laboratoire tranche.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmingPositive) {
        AlertDialog(
            onDismissRequest = { confirmingPositive = false },
            title = { Text("Enregistrer un test positif ?") },
            text = {
                Text(
                    "L'app va ouvrir un suivi de grossesse : semaines d'aménorrhée, " +
                        "rendez-vous, alimentation, signes qui doivent alerter. Tu " +
                        "pourras le fermer à tout moment, sans avoir à te justifier.\n\n" +
                        "Quel que soit ton choix ensuite, il t'appartient : l'app " +
                        "présente toutes les options sans en privilégier aucune."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingPositive = false
                    onResult(TestResult.POSITIVE)
                }) { Text("Ouvrir le suivi") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingPositive = false }) { Text("Annuler") }
            },
        )
    }
}

/**
 * Ce qu'on dit après un test négatif : pourquoi le retard persiste quand même,
 * et à partir de quand ça mérite un avis médical.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NegativeResultSheet(daysLate: Int, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Test négatif", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "C'est noté. Si tes règles ne viennent toujours pas, voici ce qui " +
                    "l'explique le plus souvent.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            NEGATIVE_CAUSES.forEach { (title, body) -> InfoBlock(title = title, body = body) }

            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Quand consulter",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        buildString {
                            if (daysLate > 20) {
                                append("Tu en es à $daysLate jours de retard. ")
                            }
                            append(
                                "Un avis médical se justifie si : trois mois passent sans " +
                                    "règles, si un test reste négatif malgré un retard qui " +
                                    "dure, ou si tu as des douleurs inhabituelles. Un " +
                                    "médecin généraliste, une sage-femme ou un centre de " +
                                    "santé sexuelle suffisent — pas besoin de gynécologue " +
                                    "pour commencer."
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("J'ai compris") }
        }
    }
}

@Composable
internal fun InfoBlock(title: String, body: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
