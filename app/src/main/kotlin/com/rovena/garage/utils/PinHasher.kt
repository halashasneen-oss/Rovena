package com.rovena.garage.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Salted-hash PIN storage for App Lock (spec #21). The raw PIN is never
 * persisted anywhere - only a random per-install salt and a SHA-256 hash of
 * `salt + pin` are written to [com.rovena.garage.data.local.entities.AppSettingsEntity].
 */
object PinHasher {

    const val MIN_PIN_LENGTH = 4
    private const val ITERATIONS = 10_000

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(pin: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        var digestInput = salt + pin.toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        repeat(ITERATIONS) {
            digestInput = digest.digest(digestInput)
        }
        return Base64.encodeToString(digestInput, Base64.NO_WRAP)
    }

    fun verify(pin: String, saltBase64: String, expectedHash: String): Boolean {
        val actual = hash(pin, saltBase64)
        return constantTimeEquals(actual, expectedHash)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
