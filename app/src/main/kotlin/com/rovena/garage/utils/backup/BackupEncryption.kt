package com.rovena.garage.utils.backup

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Optional password-based encryption for `.rovena` backups, producing a
 * `.rovena.secure` file (spec: encrypted backup format). Wraps an already-built
 * plain backup's bytes rather than changing anything about how that zip is
 * built - encryption is a pure post-processing step [BackupManager] applies
 * only when the user opts in and supplies a password.
 *
 * Format: `MAGIC (13 bytes) | salt (16 bytes) | iv (12 bytes) | AES-256-GCM
 * ciphertext (includes the 16-byte auth tag)`. The password is stretched via
 * PBKDF2WithHmacSHA256 (same construction and iteration count as
 * [com.rovena.garage.utils.PinHasher], for the same reason: a fast hash makes
 * offline brute-forcing of a stolen file cheap). GCM's authentication tag
 * means a wrong password or any corruption/tampering fails [decrypt] cleanly
 * (returns null) rather than silently returning garbage bytes.
 *
 * Uses only `java.security`/`javax.crypto` - no Android framework dependency -
 * so it is directly unit-testable on the plain JVM, same as PinHasher.
 */
object BackupEncryption {

    val MAGIC: ByteArray = "ROVENASECURE1".toByteArray(Charsets.US_ASCII)

    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12

    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size >= MAGIC.size && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    fun encrypt(plain: ByteArray, password: String): ByteArray {
        val salt = randomBytes(SALT_LENGTH_BYTES)
        val iv = randomBytes(IV_LENGTH_BYTES)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plain)
        return MAGIC + salt + iv + ciphertext
    }

    /** Returns null for a wrong password, a corrupted file, or a file that isn't in this format at all. */
    fun decrypt(encrypted: ByteArray, password: String): ByteArray? {
        val headerLength = MAGIC.size + SALT_LENGTH_BYTES + IV_LENGTH_BYTES
        if (encrypted.size < headerLength || !isEncrypted(encrypted)) return null

        var offset = MAGIC.size
        val salt = encrypted.copyOfRange(offset, offset + SALT_LENGTH_BYTES)
        offset += SALT_LENGTH_BYTES
        val iv = encrypted.copyOfRange(offset, offset + IV_LENGTH_BYTES)
        offset += IV_LENGTH_BYTES
        val ciphertext = encrypted.copyOfRange(offset, encrypted.size)

        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (e: GeneralSecurityException) {
            null
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val keyBytes = SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
            SecretKeySpec(keyBytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun randomBytes(count: Int): ByteArray {
        val bytes = ByteArray(count)
        SecureRandom().nextBytes(bytes)
        return bytes
    }
}
