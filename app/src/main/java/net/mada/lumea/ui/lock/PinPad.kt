package net.mada.lumea.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

const val MIN_PIN = 4
const val MAX_PIN = 6

/**
 * Saisie d'un code à 4–6 chiffres : les pastilles, le pavé, et la logique de
 * vérification. Partagé par l'écran de verrouillage de l'app et par l'ouverture
 * d'une note ou d'une journée protégée.
 *
 * [verify] est appelé en silence dès 4 chiffres, puis bruyamment à 6 ou sur
 * « Valider » : le code peut faire n'importe quelle longueur dans cet intervalle.
 */
@Composable
fun PinPad(
    title: String,
    subtitle: String,
    verify: (String) -> Boolean,
    modifier: Modifier = Modifier,
    onBiometrics: (() -> Unit)? = null,
    /** Millisecondes restantes avant de pouvoir réessayer ; 0 = saisie ouverte. */
    remainingLockoutMillis: () -> Long = { 0L },
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    // Décompte visible : rester devant un pavé qui refuse tout sans explication
    // est bien pire que d'attendre en sachant combien de temps.
    var lockoutMillis by remember { mutableLongStateOf(remainingLockoutMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            lockoutMillis = remainingLockoutMillis()
            delay(500)
        }
    }
    val lockedOut = lockoutMillis > 0

    fun attempt(candidate: String, loud: Boolean) {
        if (candidate.length < MIN_PIN || lockedOut) return
        if (verify(candidate)) {
            pin = ""
            error = false
        } else if (loud) {
            error = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            pin = ""
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                lockedOut -> "Trop d'essais. Réessaie dans ${formatDelay(lockoutMillis)}."
                error -> "Code incorrect"
                else -> subtitle
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (error || lockedOut) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(MAX_PIN) { index ->
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < pin.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("123", "456", "789").forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { digit ->
                        Key(label = digit.toString(), enabled = !lockedOut) {
                            if (pin.length < MAX_PIN) {
                                error = false
                                pin += digit
                                attempt(pin, loud = pin.length == MAX_PIN)
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onBiometrics != null) {
                    Key(icon = Icons.Rounded.Fingerprint, enabled = !lockedOut, onClick = onBiometrics)
                } else {
                    Spacer(Modifier.width(68.dp))
                }
                Key(label = "0", enabled = !lockedOut) {
                    if (pin.length < MAX_PIN) {
                        error = false
                        pin += '0'
                        attempt(pin, loud = pin.length == MAX_PIN)
                    }
                }
                Key(icon = Icons.Rounded.Backspace, enabled = !lockedOut) { pin = pin.dropLast(1) }
            }
        }

        if (pin.length in MIN_PIN until MAX_PIN && !lockedOut) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { attempt(pin, loud = true) }) { Text("Valider") }
        }
    }
}

@Composable
private fun Key(
    label: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(68.dp)
            .alpha(if (enabled) 1f else 0.4f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                label != null -> Text(label, style = MaterialTheme.typography.headlineSmall)
                icon != null -> Icon(icon, contentDescription = null)
            }
        }
    }
}

private fun formatDelay(millis: Long): String {
    val seconds = (millis / 1000).toInt() + 1
    return when {
        seconds < 60 -> "$seconds s"
        seconds < 3600 -> "${seconds / 60} min"
        else -> "${seconds / 3600} h"
    }
}
