package com.rovena.garage.utils

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN storage for App Lock (spec #10/#21). The raw PIN is never persisted -
 * only a random per-install salt and a PBKDF2WithHmacSHA256-derived key are
 * written to [com.rovena.garage.data.local.entities.AppSettingsEntity].
 *
 * PBKDF2 is a real, purpose-built password/PIN key-derivation function: it
 * is deliberately slow via its iteration count, which is what makes offline
 * brute-forcing of a stolen hash+salt expensive. A bare repeated-hash loop
 * (the previous implementation here) is not an equivalent construction - it
 * has no standardized security analysis behind it.
 *
 * Uses only `java.security`/`javax.crypto`/`java.util.Base64` - no Android
 * framework dependency - so it is directly unit-testable on the plain JVM.
 */
object PinHasher {

    const val MIN_PIN_LENGTH = 6
    const val MAX_PIN_LENGTH = 10
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(pin: String, saltBase64: String): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val derived = factory.generateSecret(spec).encoded
            Base64.getEncoder().encodeToString(derived)
        } finally {
            spec.clearPassword()
        }
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
