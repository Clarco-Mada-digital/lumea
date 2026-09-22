package net.mada.lumea.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import net.mada.lumea.di.container

/**
 * État de déverrouillage d'un contenu protégé.
 *
 * Il est hoisté jusqu'à l'écran pour que celui-ci puisse aussi masquer ses actions
 * (« Modifier », bascule du verrou) : laisser le bouton d'édition accessible
 * derrière un cadenas reviendrait à ne rien protéger du tout.
 */
class UnlockState(val required: Boolean) {
    var unlocked by mutableStateOf(!required)
        internal set

    /** Vrai quand le contenu doit rester caché. */
    val blocked: Boolean get() = required && !unlocked
}

/**
 * Prépare le verrou d'un contenu. Si aucun code n'est défini dans l'app, il n'y a
 * rien à vérifier : le contenu s'affiche et les réglages expliquent comment créer
 * un code.
 */
@Composable
fun rememberUnlockState(locked: Boolean): UnlockState {
    val context = LocalContext.current
    val hasPin = remember { context.applicationContext.container().lock.isPinSet() }
    // rememberSaveable garde le déverrouillage au fil d'une rotation, mais pas
    // au retour sur l'écran : revenir sur une note protégée redemande le code.
    val unlockedOnce = rememberSaveable(locked, hasPin) { mutableStateOf(false) }

    return remember(locked, hasPin) {
        UnlockState(required = locked && hasPin)
            .apply { if (unlockedOnce.value) unlocked = true }
    }.also { state ->
        if (state.unlocked) unlockedOnce.value = true
    }
}

/** Écran de saisie affiché à la place du contenu protégé. */
@Composable
fun UnlockPrompt(
    state: UnlockState,
    biometricEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = remember { context.applicationContext.container() }
    val activity = context as? FragmentActivity
    val canUseBiometrics = remember(biometricEnabled) {
        biometricEnabled && activity != null && biometricsAvailable(activity)
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Contenu protégé",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))

        PinPad(
            title = "Déverrouiller",
            subtitle = "Entre ton code pour lire",
            verify = { candidate ->
                // `check` et non `verify` : ouvrir une note protégée ne doit pas
                // changer l'état de verrouillage global de l'application.
                app.lock.check(candidate).also { if (it) state.unlocked = true }
            },
            onBiometrics = if (canUseBiometrics && activity != null) {
                { promptBiometrics(activity) { state.unlocked = true } }
            } else null,
            // Même compteur que le verrou de l'app : on ne contourne pas la
            // limitation en passant par une note protégée.
            remainingLockoutMillis = app.lock::remainingLockoutMillis,
        )
    }
}
