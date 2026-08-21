package com.rovena.garage.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for the PBKDF2WithHmacSHA256 PIN hashing (spec #10). Uses only
 * java.security/javax.crypto/java.util.Base64, so no Android framework or
 * Robolectric is needed to exercise it.
 */
class PinHasherTest {

    @Test
    fun `correct pin verifies`() {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash("123456", salt)
        assertTrue(PinHasher.verify("123456", salt, hash))
    }

    @Test
    fun `wrong pin does not verify`() {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash("123456", salt)
        assertFalse(PinHasher.verify("654321", salt, hash))
    }

    @Test
    fun `same pin with different salts produces different hashes`() {
        val saltA = PinHasher.generateSalt()
        val saltB = PinHasher.generateSalt()
        assertNotEquals(saltA, saltB)
        assertNotEquals(PinHasher.hash("123456", saltA), PinHasher.hash("123456", saltB))
    }

    @Test
    fun `hashing the same pin and salt twice is deterministic`() {
        val salt = PinHasher.generateSalt()
        assertEquals(PinHasher.hash("987654", salt), PinHasher.hash("987654", salt))
    }

    @Test
    fun `salt is not the raw pin and hash is not the raw pin`() {
        val pin = "246810"
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash(pin, salt)
        assertFalse(salt.contains(pin))
        assertFalse(hash.contains(pin))
    }

    @Test
    fun `longer pins are supported`() {
        val salt = PinHasher.generateSalt()
        val pin = "1234567890".take(PinHasher.MAX_PIN_LENGTH)
        val hash = PinHasher.hash(pin, salt)
        assertTrue(PinHasher.verify(pin, salt, hash))
    }

    @Test
    fun `minimum pin length is 6 digits`() {
        assertEquals(6, PinHasher.MIN_PIN_LENGTH)
    }
}
