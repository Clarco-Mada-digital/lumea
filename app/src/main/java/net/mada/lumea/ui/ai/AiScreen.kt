package net.mada.lumea.ui.ai

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * L'assistant IA généraliste — pour apprendre, réviser, discuter.
 *
 * Ce qu'il est : un onglet du navigateur du téléphone, affiché par-dessus Lumea
 * aux couleurs de l'app. **L'application n'envoie rien.** Elle n'écrit aucune
 * question, ne lit aucune réponse, et ne transmet aucune donnée du journal, du
 * cycle ou de la grossesse. Ce qui part, c'est uniquement ce que la personne tape
 * elle-même.
 *
 * Le choix de l'onglet plutôt que d'une WebView embarquée vient du test sur
 * appareil : deux services sur trois refusaient de s'afficher dans une
 * application, et une WebView a sa propre réserve de cookies — il aurait fallu se
 * reconnecter à chaque ouverture et l'historique aurait été perdu.
 *
 * C'est pour cette raison qu'il est séparé du conseiller du cycle : celui-ci
 * connaît les données de santé et ne sort jamais du téléphone ; celui-là ne les
 * connaît pas et sort du téléphone. La frontière est nette, et l'écran de
 * transparence la rend explicite avant le premier usage.
 */
data class AiService(
    val name: String,
    val url: String,
    val emoji: String,
    val detail: String,
)

/**
 * Les services retenus : gratuits, utilisables sans créer de compte.
 *
 * Aucun n'est intégré par API : ce sont leurs pages publiques, ouvertes dans le
 * navigateur. Comme c'est le vrai navigateur, ils fonctionnent tous, et une
 * refonte de leur côté ne peut rien casser chez nous.
 */
val AI_SERVICES: List<AiService> = listOf(
    AiService(
        "ChatGPT",
        "https://chatgpt.com",
        "💬",
        "Le plus connu. Un compte est demandé pour discuter ; la connexion est gardée d'une fois sur l'autre.",
    ),
    AiService(
        "Le Chat",
        "https://chat.mistral.ai",
        "🐱",
        "Assistant de Mistral AI, entreprise française. Gratuit ; un compte peut " +
            "être demandé selon l'usage.",
    ),
    AiService(
        "Duck.ai",
        "https://duck.ai",
        "🦆",
        "Proposé par DuckDuckGo. Gratuit, sans compte, et les conversations ne " +
            "servent pas à entraîner les modèles.",
    ),
)

/**
 * La page qui explique ce qui part et ce qui reste.
 *
 * Elle s'affiche avant le premier usage, et reste accessible ensuite. Sans elle,
 * l'écran d'IA serait une boîte noire dans une application dont tout le reste
 * promet que rien ne sort du téléphone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTransparencyScreen(
    onBack: () -> Unit,
    onAccept: (AiService) -> Unit,
    onPersonalise: () -> Unit = {},
) {
    var chosen by remember { mutableStateOf<AiService?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistant IA") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                "Pour apprendre et discuter — pas pour ta santé",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Cet écran ouvre un service d'intelligence artificielle extérieur à " +
                    "Lumea. Il est utile pour réviser, comprendre un sujet, écrire ou " +
                    "discuter. Pour tes règles, ta grossesse ou un symptôme, utilise " +
                    "le conseiller de l'app : lui connaît tes données et ne les envoie " +
                    "nulle part.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            TransparencyCard(
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                title = "✅  Ce qui reste sur ton téléphone",
                lines = listOf(
                    "Ton journal intime, toujours et sans exception",
                    "Tes dates de règles, ton cycle et tes prévisions",
                    "Ton suivi de grossesse, ton carnet, tes résultats",
                    "Les coordonnées de ta sage-femme",
                    "Tes notes, ton agenda, tes habitudes",
                    "Tes échanges avec le conseiller du cycle",
                ),
            )

            Spacer(Modifier.height(12.dp))

            TransparencyCard(
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
                title = "⚠️  Ce qui part vers le service extérieur",
                lines = listOf(
                    "Uniquement ce que TU écris dans la conversation",
                    "Ton adresse IP, comme sur n'importe quel site",
                    "Rien d'autre : Lumea ne lui envoie aucune de tes données",
                ),
            )

            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Comment c'est fait",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Le service s'ouvre dans un onglet du navigateur de ton " +
                            "téléphone, affiché par-dessus Lumea. L'app n'écrit pas à " +
                            "ta place et ne lit pas les réponses : la page tourne en " +
                            "dehors de Lumea, qui ne peut techniquement rien lui " +
                            "transmettre.\n\n" +
                            "Comme c'est ton navigateur, une connexion faite une fois " +
                            "reste valable et ton historique est conservé chez le " +
                            "service. Pour tout effacer, passe par les réglages de ton " +
                            "navigateur.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onPersonalise, modifier = Modifier.fillMaxWidth()) {
                Text("Personnaliser mon assistant")
            }

            Spacer(Modifier.height(20.dp))
            Text("Choisis un service", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            AI_SERVICES.forEach { service ->
                Card(
                    onClick = { chosen = service },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(Modifier.padding(16.dp)) {
                        Text(service.emoji, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                service.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                service.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Rounded.OpenInBrowser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Ces services appartiennent à d'autres entreprises et ont leurs " +
                    "propres conditions. Une IA peut se tromper avec assurance : ne " +
                    "prends jamais une réponse médicale ou juridique pour argent " +
                    "comptant.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(40.dp))
        }
    }

    chosen?.let { service ->
        AlertDialog(
            onDismissRequest = { chosen = null },
            title = { Text("Ouvrir ${service.name} ?") },
            text = {
                Text(
                    buildString {
                        append("Tu quittes la partie hors ligne de Lumea. Ce que tu ")
                        append("écriras là-bas sera envoyé à ${service.name}.")
                        append(" Il s'ouvre dans un onglet du navigateur, ")
                        append("par-dessus Lumea : ta connexion et ton historique ")
                        append("y sont conservés d'une fois sur l'autre.")
                        append("\n\nN'y colle pas ton journal, tes dates de règles, ")
                        append("ton suivi de grossesse ni quoi que ce soit que tu ne ")
                        append("dirais pas à un inconnu.")
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAccept(service)
                    chosen = null
                }) { Text("J'ai compris, ouvrir") }
            },
            dismissButton = {
                TextButton(onClick = { chosen = null }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun TransparencyCard(
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    title: String,
    lines: List<String>,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = content,
            )
            Spacer(Modifier.height(8.dp))
            lines.forEach { line ->
                Text(
                    "•  $line",
                    style = MaterialTheme.typography.bodyMedium,
                    color = content,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

/**
 * Ouvre un service dans un onglet du navigateur, affiché par-dessus Lumea.
 *
 * Pourquoi pas une WebView : testé sur appareil, deux des trois services refusent
 * de s'afficher dans une application (protection anti-robot), et le troisième
 * rendait mal ses fenêtres. Surtout, une WebView embarquée a sa propre réserve de
 * cookies : il aurait fallu se reconnecter à chaque ouverture et l'historique
 * aurait été perdu.
 *
 * L'onglet du navigateur règle les trois problèmes d'un coup. C'est le vrai
 * navigateur, donc rien ne le bloque ; il partage sa session, donc une connexion
 * faite une fois reste valable et l'historique est conservé côté service ; et il
 * s'affiche aux couleurs de l'app, le retour ramenant directement ici.
 *
 * Et du point de vue des données, c'est plus net encore : la page n'est plus dans
 * notre processus. Lumea ne peut techniquement rien lui transmettre.
 */
fun openAiTab(context: Context, url: String, toolbarColor: Int) {
    val intent = CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(
            CustomTabColorSchemeParams.Builder()
                .setToolbarColor(toolbarColor)
                .build()
        )
        .setShowTitle(true)
        .setUrlBarHidingEnabled(true)
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        .build()

    try {
        intent.launchUrl(context, Uri.parse(url))
    } catch (e: ActivityNotFoundException) {
        // Aucun navigateur compatible : on retombe sur l'ouverture classique.
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
