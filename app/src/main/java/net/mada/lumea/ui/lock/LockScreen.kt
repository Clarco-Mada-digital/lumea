package net.mada.lumea.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import net.mada.lumea.data.security.LockManager

/** Écran de saisie du code, affiché par-dessus tout le reste quand l'app est verrouillée. */
@Composable
fun LockScreen(
    lockManager: LockManager,
    biometricEnabled: Boolean,
    activity: FragmentActivity,
) {
    val canUseBiometrics = remember(biometricEnabled) {
        biometricEnabled && biometricsAvailable(activity)
    }

    LaunchedEffect(canUseBiometrics) {
        if (canUseBiometrics) promptBiometrics(activity) { lockManager.unlockWithBiometrics() }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        PinPad(
            title = "Lumea",
            subtitle = "Entre ton code",
            verify = lockManager::verify,
            remainingLockoutMillis = lockManager::remainingLockoutMillis,
            onBiometrics = if (canUseBiometrics) {
                { promptBiometrics(activity) { lockManager.unlockWithBiometrics() } }
            } else null,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
        )
    }
}

fun biometricsAvailable(activity: FragmentActivity): Boolean =
    BiometricManager.from(activity)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
        BiometricManager.BIOMETRIC_SUCCESS

fun promptBiometrics(activity: FragmentActivity, onSuccess: () -> Unit) {
    val prompt = BiometricPrompt(
        activity,
        androidx.core.content.ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Déverrouiller Lumea")
            .setNegativeButtonText("Utiliser le code")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
    )
}
