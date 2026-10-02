package com.rovena.garage.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RovenaAdPolicyTest {
    @Test fun neverShowOnAppLaunchOrFirstThreeActions() {
        for (n in 0..3) {
            assertFalse(RovenaAdPolicy.mayShowInterstitial(n, 0, 1_000_000, false))
        }
    }

    @Test fun capCompletedActionsAndFiveMinuteCooldown() {
        assertTrue(RovenaAdPolicy.mayShowInterstitial(4, 0, 1_000_000, false))
        assertFalse(RovenaAdPolicy.mayShowInterstitial(8, 1_000_000, 1_100_000, false))
        assertTrue(RovenaAdPolicy.mayShowInterstitial(8, 1_000_000, 1_300_000, false))
        assertFalse(RovenaAdPolicy.mayShowInterstitial(9, 0, 1_300_000, false))
    }

    @Test fun rewardedAdFreePeriodSuppressesEveryAdFormat() {
        assertTrue(RovenaAdPolicy.adsSuppressed(3_600_000, 3_500_000))
        assertFalse(RovenaAdPolicy.adsSuppressed(3_600_000, 3_600_000))
        assertFalse(RovenaAdPolicy.mayShowInterstitial(8, 0, 3_500_000, true))
    }
}
