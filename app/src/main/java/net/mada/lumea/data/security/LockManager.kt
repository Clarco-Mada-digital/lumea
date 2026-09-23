package net.mada.lumea.data.security

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.min

/**
 * Verrou par code PIN. Le PIN n'est jamais stocké : seul un dérivé PBKDF2 salé l'est,
 * dans les préférences chiffrées.
 *
 * Les tentatives sont comptées et freinées. Un code à quatre chiffres n'offre que
 * 10 000 possibilités : sans délai croissant, on le force en quelques minutes. Le
 * compteur vit dans les préférences chiffrées, donc il survit à la fermeture de
 * l'app — sinon il suffirait de la relancer pour repartir à zéro.
 */
class LockManager(context: Context) {

    private val prefs = DatabaseKeyProvider.securePrefs(context)

    private val _unlocked = MutableStateFlow(!isPinSet())
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    fun isPinSet(): Boolean = prefs.getString(KEY_HASH, null) != null

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(derive(pin, salt), Base64.NO_WRAP))
            .apply()
        clearFailures()
        _unlocked.value = true
    }

    fun clearPin() {
        prefs.edit().remove(KEY_SALT).remove(KEY_HASH).apply()
        clearFailures()
        _unlocked.value = true
    }

    /**
     * Millisecondes restantes avant de pouvoir réessayer. 0 si la saisie est ouverte.
     *
     * Les quatre premiers essais passent sans délai — se tromper de doigt arrive.
     * Au-delà, l'attente double à chaque échec : 30 s, 1 min, 2 min… plafonnée à
     * 30 minutes, ce qui ramène une attaque exhaustive à plusieurs mois.
     */
    fun remainingLockoutMillis(): Long {
        val until = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun failedAttempts(): Int = prefs.getInt(KEY_FAILED, 0)

    /**
     * Vérifie le code sans rien déverrouiller : pour les contenus protégés un par un.
     *
     * [silent] sert aux essais que le pavé fait tout seul dès quatre chiffres, sans
     * que personne n'ait validé. Ces essais ne doivent **jamais** compter comme des
     * échecs : avec un code à six chiffres, ils en produisaient deux par saisie
     * (aux 4ᵉ et 5ᵉ chiffres), le quota de quatre tentatives était épuisé en deux
     * essais, et le verrouillage temporaire rejetait ensuite le bon code lui-même.
     * Le code devenait tout simplement impossible à entrer.
     */
    fun check(pin: String, silent: Boolean = false): Boolean {
        if (remainingLockoutMillis() > 0) return false

        val saltEncoded = prefs.getString(KEY_SALT, null) ?: return false
        val expected = prefs.getString(KEY_HASH, null) ?: return false
        val salt = Base64.decode(saltEncoded, Base64.NO_WRAP)
        val candidate = Base64.encodeToString(derive(pin, salt), Base64.NO_WRAP)

        // Comparaison à temps constant : pas de fuite par la durée.
        val ok = java.security.MessageDigest.isEqual(
            candidate.toByteArray(), expected.toByteArray()
        )
        if (ok) clearFailures() else if (!silent) registerFailure()
        return ok
    }

    /** Vérifie le code et déverrouille l'app si c'est le bon. */
    fun verify(pin: String, silent: Boolean = false): Boolean {
        val ok = check(pin, silent)
        if (ok) _unlocked.value = true
        return ok
    }

    fun unlockWithBiometrics() {
        clearFailures()
        _unlocked.value = true
    }

    /** Appelé quand l'app passe en arrière-plan si le verrou est actif. */
    fun lock() {
        if (isPinSet()) _unlocked.value = false
    }

    private fun registerFailure() {
        val attempts = failedAttempts() + 1
        val editor = prefs.edit().putInt(KEY_FAILED, attempts)
        if (attempts >= FREE_ATTEMPTS) {
            val step = attempts - FREE_ATTEMPTS
            val delay = min(FIRST_DELAY_MS shl step.coerceAtMost(12), MAX_DELAY_MS)
            editor.putLong(KEY_LOCKED_UNTIL, System.currentTimeMillis() + delay)
        }
        editor.apply()
    }

    private fun clearFailures() {
        prefs.edit().remove(KEY_FAILED).remove(KEY_LOCKED_UNTIL).apply()
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private companion object {
        const val KEY_SALT = "pin_salt"
        const val KEY_HASH = "pin_hash"
        const val KEY_FAILED = "pin_failed"
        const val KEY_LOCKED_UNTIL = "pin_locked_until"
        const val ITERATIONS = 120_000

        const val FREE_ATTEMPTS = 4
        const val FIRST_DELAY_MS = 30_000L
        const val MAX_DELAY_MS = 30 * 60_000L
    }
}
