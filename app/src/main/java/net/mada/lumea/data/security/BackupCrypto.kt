package net.mada.lumea.data.security

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Chiffrement des fichiers de sauvegarde.
 *
 * Une sauvegarde contient tout : journal intime, symptômes, dates de règles. La
 * laisser en clair dans le dossier Téléchargements ou sur un cloud reviendrait à
 * annuler le chiffrement de la base.
 *
 * Format du fichier :
 * ```
 * "LUMEA1" (6) | version (1) | itérations (4, big endian) | sel (16) | IV (12) | chiffré+tag
 * ```
 * L'en-tête est en clair à dessein : il permet de reconnaître un fichier Lumea
 * chiffré à l'import, et de relire d'anciennes sauvegardes même si le nombre
 * d'itérations change un jour.
 *
 * La clé est dérivée par PBKDF2-HMAC-SHA256, puis le contenu est scellé en
 * AES-256-GCM : une sauvegarde modifiée ou tronquée est rejetée au déchiffrement
 * plutôt que d'être importée à moitié.
 */
object BackupCrypto {

    private val MAGIC = "LUMEA1".toByteArray(Charsets.US_ASCII)
    private const val VERSION: Byte = 1
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val KEY_BITS = 256

    /** Recommandation OWASP pour PBKDF2-HMAC-SHA256. Environ 0,3 s sur un téléphone d'entrée de gamme. */
    private const val ITERATIONS = 210_000

    /** Vrai si [data] commence par l'en-tête Lumea : le fichier demande une phrase de passe. */
    fun isEncrypted(data: ByteArray): Boolean =
        data.size > MAGIC.size + 1 && data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    fun encrypt(plain: ByteArray, passphrase: CharArray): ByteArray {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt, ITERATIONS)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val sealed = cipher.doFinal(plain)

        return ByteBuffer
            .allocate(MAGIC.size + 1 + 4 + SALT_BYTES + IV_BYTES + sealed.size)
            .put(MAGIC)
            .put(VERSION)
            .putInt(ITERATIONS)
            .put(salt)
            .put(iv)
            .put(sealed)
            .array()
    }

    /**
     * @throws IllegalArgumentException si le fichier n'est pas une sauvegarde Lumea chiffrée
     * @throws javax.crypto.AEADBadTagException si la phrase de passe est fausse
     *         ou le fichier abîmé
     */
    fun decrypt(data: ByteArray, passphrase: CharArray): ByteArray {
        require(isEncrypted(data)) { "Ce fichier n'est pas une sauvegarde Lumea chiffrée" }

        val buffer = ByteBuffer.wrap(data)
        buffer.position(MAGIC.size)
        val version = buffer.get()
        require(version == VERSION) { "Version de sauvegarde inconnue ($version)" }

        val iterations = buffer.int
        require(iterations in 1_000..2_000_000) { "Sauvegarde corrompue" }

        val salt = ByteArray(SALT_BYTES).also { buffer.get(it) }
        val iv = ByteArray(IV_BYTES).also { buffer.get(it) }
        val sealed = ByteArray(buffer.remaining()).also { buffer.get(it) }

        val key = deriveKey(passphrase, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(sealed)
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return try {
            SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } finally {
            // Efface la copie interne de la phrase de passe dès qu'elle a servi.
            spec.clearPassword()
        }
    }
}
