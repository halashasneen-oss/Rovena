package com.rovena.garage.ads

/** Interstitials only after completed user actions, with a strict cooldown. */
object RovenaAdPolicy {
    const val ACTIONS_BETWEEN_INTERSTITIALS = 4
    const val INTERSTITIAL_COOLDOWN_MS = 5 * 60 * 1000L
    const val AD_FREE_REWARD_MS = 60 * 60 * 1000L

    fun mayShowInterstitial(actions: Int, lastShownAt: Long, now: Long, adsSuppressed: Boolean): Boolean =
        !adsSuppressed &&
        actions >= ACTIONS_BETWEEN_INTERSTITIALS &&
        actions % ACTIONS_BETWEEN_INTERSTITIALS == 0 &&
        (lastShownAt == 0L || now - lastShownAt >= INTERSTITIAL_COOLDOWN_MS)

    fun adsSuppressed(adFreeUntil: Long, now: Long): Boolean = adFreeUntil > now
}
