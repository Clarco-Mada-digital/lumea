package net.mada.lumea.ui.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.data.prefs.AssistantTone
import net.mada.lumea.ui.components.DebouncedTextField
import net.mada.lumea.ui.containerViewModel
import net.mada.lumea.ui.settings.SettingsViewModel

/**
 * Personnaliser l'assistant : lui donner un nom, et lui dire à qui il parle.
 *
 * L'app n'ayant aucun accès automatisé au service de chat, elle ne peut pas
 * configurer l'assistant à distance. Elle fait donc la seule chose honnête :
 * composer une introduction, la montrer en entier, et la copier sur demande. C'est
 * l'utilisatrice qui la colle — rien ne part sans ce geste.
 *
 * Le profil ne contient **aucune donnée de santé**. Un métier et des centres
 * d'intérêt suffisent à changer radicalement la qualité des réponses : une même
 * question sur l'anatomie n'appelle pas la même réponse pour un infirmier et pour
 * un lycéen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiProfileScreen(onBack: () -> Unit, onOpenAssistant: () -> Unit) {
    val context = LocalContext.current
    val vm = containerViewModel { SettingsViewModel(it.settings, it.lock, it.backup) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var copied by remember { mutableStateOf(false) }

    // Incrémenté par les puces de suggestion : c'est le signal qui dit aux champs
    // de reprendre la valeur, puisqu'elle ne vient pas du clavier.
    var suggestionTick by remember { mutableStateOf(0) }

    LaunchedEffect(copied) {
        if (copied) {
            snackbar.showSnackbar("Introduction copiée — colle-la dans la conversation")
            copied = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personnaliser l'assistant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                "Plus tu en dis, mieux il répond",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Une même question n'appelle pas la même réponse selon qui la pose. " +
                    "Dis-lui qui tu es, et il adaptera son vocabulaire et ses exemples.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            DebouncedTextField(
                value = settings.assistantName,
                onValueChange = vm::setAssistantName,
                label = "Son nom",
                placeholder = AiProfile.DEFAULT_NAME,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            DebouncedTextField(
                value = settings.userAlias,
                onValueChange = vm::setUserAlias,
                label = "Comment il t'appelle",
                placeholder = "Ton prénom, ou un surnom",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            DebouncedTextField(
                value = settings.occupation,
                onValueChange = vm::setOccupation,
                label = "Ton métier ou tes études",
                placeholder = "Infirmier, lycéenne, développeuse…",
                resetKey = suggestionTick,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AiProfile.OCCUPATION_SUGGESTIONS.forEach { suggestion ->
                    SuggestionChip(
                        onClick = {
                            vm.setOccupation(suggestion)
                            suggestionTick++
                        },
                        label = { Text(suggestion) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            DebouncedTextField(
                value = settings.interests,
                onValueChange = vm::setInterests,
                label = "Tes centres d'intérêt",
                placeholder = "Séparés par des virgules",
                singleLine = false,
                resetKey = suggestionTick,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AiProfile.INTEREST_SUGGESTIONS.forEach { suggestion ->
                    SuggestionChip(
                        onClick = {
                            val current = settings.interests
                                .split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            if (suggestion !in current) {
                                vm.setInterests((current + suggestion).joinToString(", "))
                                suggestionTick++
                            }
                        },
                        label = { Text(suggestion) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Comment il te répond", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistantTone.entries.forEach { tone ->
                    FilterChip(
                        selected = settings.assistantTone == tone,
                        onClick = { vm.setAssistantTone(tone) },
                        label = { Text(tone.label) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            DebouncedTextField(
                value = settings.assistantNotes,
                onValueChange = vm::setAssistantNotes,
                label = "Autre chose à lui dire ?",
                placeholder = "« Je prépare le bac », « explique-moi comme à un débutant »…",
                singleLine = false,
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
            Text("Ce qu'il recevra", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    AiProfile.introduction(settings),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Spacer(Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "🔒  ${AiProfile.PRIVACY_REMINDER}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(14.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        copyToClipboard(context, AiProfile.introduction(settings))
                        copied = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copier")
                }
                Button(onClick = onOpenAssistant, modifier = Modifier.weight(1f)) {
                    Text("Ouvrir l'assistant")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Mode d'emploi : copie l'introduction, ouvre l'assistant, colle-la " +
                    "comme premier message. Il gardera ce rôle pendant toute la " +
                    "conversation. À recoller si tu démarres une nouvelle discussion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText("Introduction assistant", text))
}

/** Le nom affiché partout ailleurs dans l'app, une fois choisi. */
@Composable
fun rememberAssistantLabel(): String {
    val vm = containerViewModel { SettingsViewModel(it.settings, it.lock, it.backup) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    return AiProfile.assistantName(settings)
}
