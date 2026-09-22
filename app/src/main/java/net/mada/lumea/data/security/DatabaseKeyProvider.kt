package net.mada.lumea.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.sqlcipher.database.SQLiteDatabase
import java.security.SecureRandom
import android.util.Base64

/**
 * Fournit la phrase de passe SQLCipher de la base.
 *
 * La clé est tirée au hasard à la première ouverture puis rangée dans des
 * SharedPreferences chiffrées par le Keystore matériel de l'appareil. Elle ne
 * dépend pas du code PIN : changer de PIN ne doit pas rendre les données illisibles.
 */
object DatabaseKeyProvider {

    private const val FILE = "lumea_keys"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"

    fun passphrase(context: Context): ByteArray {
        val prefs = securePrefs(context)
        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) {
            return SQLiteDatabase.getBytes(existing.toCharArray())
        }
        val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encoded = Base64.encodeToString(random, Base64.NO_WRAP)
        prefs.edit().putString(KEY_DB_PASSPHRASE, encoded).apply()
        return SQLiteDatabase.getBytes(encoded.toCharArray())
    }

    fun securePrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        FILE,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
