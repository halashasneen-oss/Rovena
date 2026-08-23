package com.rovena.garage.utils.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for AES-256-GCM backup encryption (spec: encrypted backup
 * format). Uses only java.security/javax.crypto, so no Android framework or
 * Robolectric is needed - same approach as PinHasherTest.
 */
class BackupEncryptionTest {

    @Test
    fun `decrypting with the correct password recovers the original bytes`() {
        val plain = "not a real zip, just some bytes to round-trip".toByteArray()
        val encrypted = BackupEncryption.encrypt(plain, "correct horse battery staple")
        val decrypted = BackupEncryption.decrypt(encrypted, "correct horse battery staple")
        assertArrayEquals(plain, decrypted)
    }

    @Test
    fun `decrypting with the wrong password returns null instead of garbage`() {
        val plain = "some backup bytes".toByteArray()
        val encrypted = BackupEncryption.encrypt(plain, "correct password")
        assertNull(BackupEncryption.decrypt(encrypted, "wrong password"))
    }

    @Test
    fun `tampered ciphertext fails the GCM auth tag check`() {
        val plain = "some backup bytes".toByteArray()
        val encrypted = BackupEncryption.encrypt(plain, "a password")
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1] + 1).toByte()
        assertNull(BackupEncryption.decrypt(encrypted, "a password"))
    }

    @Test
    fun `isEncrypted recognizes the magic header`() {
        val encrypted = BackupEncryption.encrypt("x".toByteArray(), "pw")
        assertTrue(BackupEncryption.isEncrypted(encrypted))
        assertFalse(BackupEncryption.isEncrypted("PK not encrypted".toByteArray()))
        assertFalse(BackupEncryption.isEncrypted(ByteArray(2)))
    }

    @Test
    fun `two encryptions of the same plaintext produce different ciphertext`() {
        // Random salt + IV each time - encrypting the same bytes twice must not
        // look identical, or a stolen file would leak whether backups repeat.
        val a = BackupEncryption.encrypt("same content".toByteArray(), "pw")
        val b = BackupEncryption.encrypt("same content".toByteArray(), "pw")
        assertFalse(a.contentEquals(b))
    }
}
