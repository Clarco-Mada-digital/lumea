package net.mada.lumea.ui.pregnancy

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.mada.lumea.export.CarnetPdf

/**
 * Le QR de la fiche d'urgence, affiché en grand.
 *
 * Pensé pour le comptoir du CSB : le soignant scanne depuis son propre téléphone
 * et lit l'essentiel chez lui. Tout est dans le code lui-même, pas dans un lien —
 * ça marche sans réseau des deux côtés, et rien n'est déposé sur un serveur.
 *
 * La luminosité n'est pas forcée : sur un téléphone à 3 % de batterie, on préfère
 * que l'écran reste allumé. Un QR à correction moyenne se scanne très bien à
 * luminosité normale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyQrSheet(
    loadQr: suspend (Boolean) -> Bitmap?,
    onDismiss: () -> Unit,
) {
    var includeName by remember { mutableStateOf(false) }
    var qr by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(includeName) {
        loading = true
        qr = loadQr(includeName)
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Fiche d'urgence à scanner",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Le soignant scanne avec son téléphone et lit l'essentiel : grossesse, " +
                    "groupe sanguin, allergies, traitements, qui contacter.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            Box(
                Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(androidx.compose.ui.graphics.Color.White),
                contentAlignment = Alignment.Center,
            ) {
                val image = qr
                when {
                    loading -> CircularProgressIndicator()
                    image != null -> Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = "QR code de la fiche d'urgence",
                        modifier = Modifier.fillMaxWidth(0.92f),
                    )
                    else -> Text(
                        "Impossible de générer le code",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(checked = includeName, onCheckedChange = { includeName = it })
                Column(Modifier.weight(1f)) {
                    Text("Inclure mon prénom", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Décoché, le code reste anonyme. Un QR se photographie et " +
                            "se transfère : ce qu'il contient ne t'appartient plus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
            ) {
                Text(
                    "Hors ligne des deux côtés : tout est écrit dans le code, rien " +
                        "n'est envoyé ni déposé quelque part. Le même code figure en " +
                        "bas du PDF.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Fermer")
            }
        }
    }
}

/**
 * Ce qu'on met dans le PDF, avant de l'écrire.
 *
 * Le document sort de l'application dès qu'il est créé : il s'envoie, s'imprime,
 * se transfère. Choisir son contenu avant plutôt qu'après est la seule façon de
 * garder la main dessus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfOptionsSheet(
    onExport: (CarnetPdf.Options) -> Unit,
    onDismiss: () -> Unit,
) {
    var options by remember { mutableStateOf(CarnetPdf.Options()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Exporter mon carnet", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Un PDF à montrer ou à imprimer. Ton journal, ton humeur et tes notes " +
                    "n'y figurent jamais.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(18.dp))
            Text("Ce que le document contient", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))

            OptionRow(
                label = "L'essentiel",
                detail = "Grossesse, terme, groupe sanguin, allergies, soignant",
                checked = options.identity,
                onCheck = { options = options.copy(identity = it) },
            )
            OptionRow(
                label = "Examens",
                detail = "Échographies, dépistages, résultats notés",
                checked = options.results,
                onCheck = { options = options.copy(results = it) },
            )
            OptionRow(
                label = "Actes de suivi",
                detail = "CPN, vaccins, TPIg — cochés avec leur date",
                checked = options.checklist,
                onCheck = { options = options.copy(checklist = it) },
            )
            OptionRow(
                label = "Ma situation",
                detail = "Les points que tu as signalés",
                checked = options.riskFactors,
                onCheck = { options = options.copy(riskFactors = it) },
            )
            OptionRow(
                label = "Signes d'alerte",
                detail = "Quand partir au CSB sans attendre",
                checked = options.warningSigns,
                onCheck = { options = options.copy(warningSigns = it) },
            )
            OptionRow(
                label = "Mon prénom",
                detail = "Décoché, le document est anonyme",
                checked = options.includeName,
                onCheck = { options = options.copy(includeName = it) },
            )

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Annuler")
                }
                Button(onClick = { onExport(options) }, modifier = Modifier.weight(1f)) {
                    Text("Créer le PDF")
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    detail: String,
    checked: Boolean,
    onCheck: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheck)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
