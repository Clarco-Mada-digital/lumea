package net.mada.lumea.ui.components.richtext

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import net.mada.lumea.data.media.MediaStore
import net.mada.lumea.data.media.VoiceRecorder
import net.mada.lumea.data.media.audioDurationSeconds

/**
 * Le bouton d'enregistrement vocal, posé à côté du champ de texte.
 *
 * Il n'y a pas d'écran séparé : parler et écrire sont deux façons de raconter la
 * même journée, et il faut pouvoir passer de l'une à l'autre sans changer de
 * contexte. L'enregistrement s'insère dans le texte comme une photo — un
 * marqueur que le lecteur transforme en lecteur audio.
 */
@Composable
fun VoiceRecordButton(
    store: MediaStore,
    onRecorded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<String?>(null) }
    var deniedTwice by remember { mutableStateOf(false) }

    // Le micro doit être relâché quand l'écran disparaît : un enregistrement
    // qui continue en arrière-plan est exactement ce qu'on ne veut pas ici.
    DisposableEffect(Unit) { onDispose { recorder.release() } }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pending = recorder.start(store)
            recording = pending != null
            seconds = 0
        } else {
            deniedTwice = true
        }
    }

    LaunchedEffect(recording) {
        while (recording) {
            delay(1000)
            seconds++
            if (seconds >= recorder.maxDurationMs / 1000) {
                val name = pending
                if (recorder.stopRecording() && name != null) onRecorded(name)
                recording = false
            }
        }
    }

    fun toggle() {
        if (recording) {
            val name = pending
            if (recorder.stopRecording() && name != null) onRecorded(name)
            recording = false
            pending = null
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            pending = recorder.start(store)
            recording = pending != null
            seconds = 0
        } else {
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(modifier.fillMaxWidth()) {
        if (recording) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp),
                ) {
                    PulsingDot()
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Enregistrement… ${format(seconds)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            "Maximum ${recorder.maxDurationMs / 60_000} minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    IconButton(onClick = { toggle() }) {
                        Icon(
                            Icons.Rounded.Stop,
                            contentDescription = "Arrêter",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        } else {
            TextButton(onClick = { toggle() }) {
                Icon(Icons.Rounded.Mic, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Raconter à voix haute")
            }
        }
    }

    if (deniedTwice) {
        AlertDialog(
            onDismissRequest = { deniedTwice = false },
            title = { Text("Le micro est refusé") },
            text = {
                Text(
                    "Sans l'accès au micro, l'enregistrement vocal ne peut pas " +
                        "fonctionner. Tu peux l'autoriser dans les réglages Android " +
                        "de Lumea, ou continuer à écrire — les deux se valent.\n\n" +
                        "Les enregistrements restent sur ce téléphone, comme le reste."
                )
            },
            confirmButton = {
                TextButton(onClick = { deniedTwice = false }) { Text("D'accord") }
            },
        )
    }
}

/**
 * Un enregistrement dans le corps d'une note ou d'une journée.
 *
 * Affiche la durée plutôt qu'une forme d'onde : la durée est l'information utile
 * quand on relit, la forme d'onde n'est que décorative.
 */
@Composable
fun VoiceNotePlayer(
    reference: String,
    store: MediaStore,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    val file = remember(reference) { store.resolve(reference) }
    var playing by remember { mutableStateOf(false) }
    val duration = remember(reference) { if (file.exists()) audioDurationSeconds(file) else 0 }

    DisposableEffect(Unit) { onDispose { recorder.release() } }

    if (!file.exists()) {
        Text(
            "Enregistrement introuvable",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = modifier.padding(vertical = 8.dp),
        )
        return
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            IconButton(
                onClick = {
                    if (playing) {
                        recorder.stopPlaying()
                        playing = false
                    } else {
                        recorder.play(file) { playing = false }
                        playing = true
                    }
                }
            ) {
                Icon(
                    if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Arrêter" else "Écouter",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Enregistrement vocal", style = MaterialTheme.typography.bodyMedium)
                Text(
                    format(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Supprimer",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

/** Le point rouge qui bat pendant l'enregistrement : on voit que ça tourne. */
@Composable
private fun PulsingDot() {
    val transition = rememberInfiniteTransition(label = "rec")
    val scale by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        Modifier
            .size(12.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error)
    )
}

private fun format(seconds: Int): String =
    "%d:%02d".format(seconds / 60, seconds % 60)
