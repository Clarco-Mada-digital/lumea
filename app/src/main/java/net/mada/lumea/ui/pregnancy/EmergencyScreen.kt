package net.mada.lumea.ui.pregnancy

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.domain.pregnancy.SYMPTOM_TRIAGE
import net.mada.lumea.domain.pregnancy.Urgency
import net.mada.lumea.domain.pregnancy.postpartumProgress
import net.mada.lumea.domain.pregnancy.pregnancyProgress
import net.mada.lumea.domain.pregnancy.riskFactor
import net.mada.lumea.ui.containerViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * La fiche d'urgence : tout ce qu'un soignant doit savoir, en un écran.
 *
 * Elle existe pour le moment où la personne ne va pas bien et n'a ni le carnet
 * papier, ni la tête à chercher dans des menus. Gros caractères, contraste élevé,
 * et l'essentiel en haut : où en est la grossesse, le groupe sanguin, les
 * allergies, qui suit, qui prévenir.
 *
 * **Pourquoi elle reste derrière le code.** Une fiche accessible avant le
 * déverrouillage serait plus utile dans le cas extrême — la personne inconsciente
 * — mais elle exposerait une grossesse, un statut VIH ou un traitement à
 * quiconque ramasse le téléphone. Pour le public de cette app, ce risque-là est
 * quotidien ; l'autre est rare. La fiche est donc à deux touches après le code, et
 * l'écran explique comment recopier l'essentiel dans la fiche médicale d'Android
 * pour qui veut couvrir le cas inconsciente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(onBack: () -> Unit, onExportPdf: () -> Unit) {
    var qrOpen by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm = containerViewModel { PregnancyViewModel(it.pregnancy, it.cycle, it.events, it.settings) }
    val state by vm.state.collectAsStateWithLifecycle()
    val pregnancy = state.ongoing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fiche d'urgence") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "À montrer au soignant",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Les informations que tu as enregistrées, rassemblées ici pour " +
                        "ne pas avoir à les chercher.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // --- L'état, en très gros ---
            item {
                val headline = when {
                    pregnancy == null -> "Aucun suivi en cours"
                    pregnancy.status == "POSTPARTUM" -> pregnancy.birthDate?.let {
                        val after = postpartumProgress(it)
                        "Accouchement il y a ${after.daysSince} jours"
                    } ?: "Après la naissance"
                    else -> pregnancy.lastPeriodStart?.let {
                        val p = pregnancyProgress(it)
                        "Enceinte — ${p.label}"
                    } ?: "Suivi en cours"
                }
                val detail = when {
                    pregnancy == null -> ""
                    pregnancy.status == "POSTPARTUM" -> pregnancy.birthDate
                        ?.format(longDate).orEmpty()
                    else -> pregnancy.lastPeriodStart?.let {
                        val p = pregnancyProgress(it)
                        "${p.trimester}ᵉ trimestre · terme prévu le " +
                            p.dueDate.format(longDate)
                    }.orEmpty()
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            headline,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        if (detail.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                detail,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        pregnancy?.lastPeriodStart?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Dernières règles : ${it.format(longDate)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            // --- Les quatre réponses qu'on demande en premier ---
            item {
                val fields = listOf(
                    "Groupe sanguin" to state.care["GROUPE_SANGUIN"]?.note,
                    "Allergies" to state.care["ALLERGIES"]?.note,
                    "Traitements en cours" to state.care["TRAITEMENTS"]?.note,
                    "Hémoglobine" to state.care["HEMOGLOBINE"]?.note,
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        fields.forEachIndexed { index, (label, value) ->
                            if (index > 0) Spacer(Modifier.height(12.dp))
                            Text(
                                label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                value?.takeIf { it.isNotBlank() } ?: "— non renseigné",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (value.isNullOrBlank())
                                    MaterialTheme.colorScheme.outline
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // --- Qui appeler ---
            if (pregnancy != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Qui contacter", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))

                            ContactLine(
                                role = pregnancy.caregiverRole.ifBlank { "Soignant" },
                                name = pregnancy.caregiverName,
                                phone = pregnancy.caregiverPhone,
                                place = pregnancy.facility,
                                onCall = { number ->
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                    )
                                },
                            )

                            val contact = state.care["CONTACT_URGENCE"]?.note
                            if (!contact.isNullOrBlank()) {
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    "PERSONNE À PRÉVENIR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(contact, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            // --- Ce qui a été signalé ---
            val risks = pregnancy?.riskFactors.orEmpty()
                .split(",").filter { it.isNotBlank() }.mapNotNull { riskFactor(it)?.label }
            if (risks.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Points signalés",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Spacer(Modifier.height(6.dp))
                            risks.forEach {
                                Text(
                                    "• $it",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            // --- Quand partir sans attendre ---
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Partir au CSB sans attendre si :",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.height(8.dp))
                        SYMPTOM_TRIAGE
                            .filter { it.urgency == Urgency.PARTIR_MAINTENANT }
                            .forEach {
                                Text(
                                    "• ${it.label}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                    }
                }
            }

            item {
                /*
                 * Le QR d'abord : c'est l'option qui marche quand il n'y a ni
                 * imprimante, ni réseau, ni batterie pour chercher dans des menus.
                 */
                Button(onClick = { qrOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.QrCode2, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Montrer en QR code")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onExportPdf, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Exporter cette fiche en PDF")
                }
            }

            item {
                Text(
                    "Cette fiche est protégée par ton code : personne ne la voit sans " +
                        "déverrouiller le téléphone. Si tu veux qu'elle reste lisible même " +
                        "sans déverrouillage — par exemple si tu perds connaissance — " +
                        "recopie l'essentiel dans la fiche médicale de ton téléphone " +
                        "(Réglages Android → Sécurité et urgence → Informations médicales). " +
                        "C'est un choix à faire en connaissance de cause : ce qui y est " +
                        "écrit est visible par toute personne qui ramasse ton téléphone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
    }

    if (qrOpen) {
        EmergencyQrSheet(
            loadQr = { includeName -> vm.emergencyQr(includeName) },
            onDismiss = { qrOpen = false },
        )
    }
}

@Composable
private fun ContactLine(
    role: String,
    name: String,
    phone: String,
    place: String,
    onCall: (String) -> Unit,
) {
    Column {
        Text(
            role.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            name.ifBlank { "— non renseigné" },
            style = MaterialTheme.typography.titleMedium,
            color = if (name.isBlank()) MaterialTheme.colorScheme.outline
            else MaterialTheme.colorScheme.onSurface,
        )
        if (place.isNotBlank()) {
            Text(place, style = MaterialTheme.typography.bodyMedium)
        }
        if (phone.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onCall(phone) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Call, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Appeler $phone")
            }
        }
    }
}

private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

