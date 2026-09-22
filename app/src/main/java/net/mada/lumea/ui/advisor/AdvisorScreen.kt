package net.mada.lumea.ui.advisor

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.domain.advisor.Advice
import net.mada.lumea.domain.advisor.AdviceAction
import net.mada.lumea.domain.advisor.AdviceLevel
import net.mada.lumea.domain.advisor.Topic
import net.mada.lumea.domain.advisor.advise
import net.mada.lumea.domain.advisor.adviseOn
import net.mada.lumea.domain.advisor.label
import net.mada.lumea.domain.advisor.suggestedQuestions
import net.mada.lumea.ui.components.FloatingNavBarSpace
import net.mada.lumea.ui.containerViewModel

/**
 * Le conseiller du cycle.
 *
 * Il répond à partir des données déjà présentes dans l'app — dates de règles,
 * semaines de grossesse, carnet de suivi, soignant enregistré — et **rien ne sort
 * du téléphone** : aucune question, aucune réponse, aucun chiffre n'est envoyé où
 * que ce soit. C'est aussi pour ça qu'il fonctionne sans connexion.
 *
 * Ses réponses sont écrites à l'avance plutôt que générées : sur du tri médical,
 * mieux vaut un conseiller qui dit « je ne sais pas » qu'un qui improvise.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvisorScreen(
    onBack: () -> Unit,
    onOpenPregnancy: () -> Unit,
    onOpenLessons: () -> Unit,
    onWriteLog: () -> Unit,
    onOpenAi: () -> Unit,
) {
    val context = LocalContext.current
    val vm = containerViewModel {
        AdvisorViewModel(it.cycle, it.settings, it.pregnancy, it.advisorConversation)
    }
    val advisorContext by vm.context.collectAsStateWithLifecycle()
    val turns by vm.turns.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    fun ask(text: String, topic: Topic? = null) {
        vm.ask(text, topic)
        input = ""
    }

    // On suit la conversation : une nouvelle réponse doit être visible sans défiler.
    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty()) listState.animateScrollToItem(turns.size)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon conseiller") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (turns.isNotEmpty()) {
                        IconButton(onClick = vm::clearConversation) {
                            Icon(
                                Icons.Rounded.DeleteSweep,
                                contentDescription = "Effacer la conversation",
                            )
                        }
                    }
                    IconButton(onClick = onOpenAi) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = "Assistant IA")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (turns.isEmpty()) {
                    item { Intro() }
                }

                items(turns.size) { index ->
                    when (val turn = turns[index]) {
                        is Turn.Question -> QuestionBubble(turn.text)
                        is Turn.Answer -> AnswerCard(
                            advice = turn.advice,
                            onAction = { action ->
                                when (action) {
                                    AdviceAction.APPELER_SOIGNANT ->
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_DIAL,
                                                Uri.parse("tel:${advisorContext.caregiverPhone}"),
                                            )
                                        )
                                    AdviceAction.OUVRIR_SUIVI,
                                    AdviceAction.OUVRIR_CARNET,
                                    AdviceAction.FAIRE_TEST -> onOpenPregnancy()
                                    AdviceAction.NOTER_JOURNEE -> onWriteLog()
                                    AdviceAction.OUVRIR_LECONS -> onOpenLessons()
                                    AdviceAction.DEMANDER_IA -> onOpenAi()
                                }
                            },
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Questions fréquentes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestedQuestions(advisorContext).forEach { (label, topic) ->
                            SuggestionChip(
                                onClick = { ask(label, topic) },
                                label = { Text(label) },
                            )
                        }
                    }
                    Spacer(Modifier.height(FloatingNavBarSpace / 2))
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Pose ta question…") },
                    shape = MaterialTheme.shapes.large,
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { ask(input) },
                    enabled = input.isNotBlank(),
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    Icon(Icons.Rounded.Send, contentDescription = "Envoyer")
                }
            }
        }
    }
}

@Composable
private fun Intro() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Je réponds avec tes données, sans les envoyer nulle part",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Je connais tes dates de règles, ton suivi de grossesse et ton carnet, " +
                    "et je m'en sers pour te répondre précisément. Tout le calcul se " +
                    "fait sur ce téléphone : rien n'est envoyé sur Internet, et je " +
                    "fonctionne sans connexion.\n\n" +
                    "Je ne suis pas un soignant. Pour tout ce qui est urgent, je te " +
                    "dirai d'aller au CSB plutôt que de te rassurer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuestionBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            Modifier
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerCard(advice: Advice, onAction: (AdviceAction) -> Unit) {
    val accent = levelColor(advice.level)

    /*
     * Une urgence doit se voir sans lire.
     *
     * La première version teintait la carte à 16 % : sur le thème sombre, ça
     * donnait un gris indistinguable des réponses ordinaires. Une réponse « va au
     * CSB » et une réponse « rien d'inquiétant » ne peuvent pas se ressembler.
     */
    val urgent = advice.level == AdviceLevel.URGENT
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (urgent) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    levelLabel(advice.level),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (urgent) MaterialTheme.colorScheme.onErrorContainer else accent,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                advice.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (urgent) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                advice.body,
                style = MaterialTheme.typography.bodyMedium,
                color = if (urgent) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurface,
            )

            if (advice.facts.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    advice.facts.forEach { (label, value) ->
                        Box(
                            Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "$label : $value",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            if (advice.actions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    advice.actions.forEach { action ->
                        AssistChip(
                            onClick = { onAction(action) },
                            label = { Text(action.label) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun levelColor(level: AdviceLevel): Color = when (level) {
    AdviceLevel.URGENT -> MaterialTheme.colorScheme.error
    AdviceLevel.CONSULTER -> MaterialTheme.colorScheme.tertiary
    AdviceLevel.ATTENTION -> MaterialTheme.colorScheme.tertiary
    AdviceLevel.INFO -> MaterialTheme.colorScheme.primary
    AdviceLevel.RASSURANT -> MaterialTheme.colorScheme.primary
}

private fun levelLabel(level: AdviceLevel): String = when (level) {
    AdviceLevel.URGENT -> "VA AU CSB"
    AdviceLevel.CONSULTER -> "À FAIRE VOIR"
    AdviceLevel.ATTENTION -> "À SURVEILLER"
    AdviceLevel.INFO -> "INFORMATION"
    AdviceLevel.RASSURANT -> "RIEN D'INQUIÉTANT"
}
