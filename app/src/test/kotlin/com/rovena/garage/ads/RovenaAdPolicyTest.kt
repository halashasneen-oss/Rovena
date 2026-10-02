package com.rovena.garage.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RovenaAdPolicyTest {
    @Test fun interstitialAppearsOnlyAtNaturalBreaks() {
        (0..3).forEach { assertFalse(RovenaAdPolicy.mayShowInterstitial(it, 0L, 1_000_000L)) }
        assertTrue(RovenaAdPolicy.mayShowInterstitial(4, 0L, 1_000_000L))
        assertFalse(RovenaAdPolicy.mayShowInterstitial(8, 1_000_000L, 1_100_000L))
        assertTrue(RovenaAdPolicy.mayShowInterstitial(8, 1_000_000L, 1_300_000L))
        assertFalse(RovenaAdPolicy.mayShowInterstitial(9, 0L, 1_300_000L))
    }

    @Test fun appOpenRequiresEligibleReturnAndFreshAd() {
        val now = 10_000_000L
        fun allowed(background: Long, previous: Long = 0L, loaded: Long = now - 1000,
                    active: Boolean = false, showing: Boolean = false): Boolean =
            RovenaAdPolicy.mayShowAppOpen(background, previous, loaded, now, active, showing)

        assertFalse(allowed(0L))
        assertFalse(allowed(now - 89_999L))
        assertTrue(allowed(now - 91_000L))
        assertFalse(allowed(now - 91_000L, previous = now - 1000L))
        assertFalse(allowed(now - 91_000L, loaded = now - RovenaAdPolicy.APP_OPEN_EXPIRY_MS))
        assertFalse(allowed(now - 91_000L, active = true))
        assertFalse(allowed(now - 91_000L, showing = true))
    }
}
