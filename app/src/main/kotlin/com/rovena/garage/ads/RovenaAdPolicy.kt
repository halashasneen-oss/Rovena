package com.rovena.garage.ads

/** Interstitials only after completed user actions, with a strict cooldown. */
object RovenaAdPolicy {
    const val ACTIONS_BETWEEN_INTERSTITIALS = 4
    const val INTERSTITIAL_COOLDOWN_MS = 5 * 60 * 1000L
    const val AD_FREE_REWARD_MS = 60 * 60 * 1000L
    const val APP_OPEN_MIN_BACKGROUND_MS = 90 * 1000L
    const val APP_OPEN_COOLDOWN_MS = 30 * 60 * 1000L
    const val APP_OPEN_EXPIRY_MS = 4 * 60 * 60 * 1000L

    fun mayShowInterstitial(actions: Int, lastShownAt: Long, now: Long, adsSuppressed: Boolean): Boolean =
        !adsSuppressed &&
        actions >= ACTIONS_BETWEEN_INTERSTITIALS &&
        actions % ACTIONS_BETWEEN_INTERSTITIALS == 0 &&
        (lastShownAt == 0L || now - lastShownAt >= INTERSTITIAL_COOLDOWN_MS)

    fun adsSuppressed(adFreeUntil: Long, now: Long): Boolean = adFreeUntil > now

    fun mayShowAppOpen(
        backgroundedAt: Long,
        lastShownAt: Long,
        loadedAt: Long,
        now: Long,
        adsSuppressed: Boolean,
        userFlowActive: Boolean,
        fullScreenAdShowing: Boolean
    ): Boolean = !adsSuppressed && !userFlowActive && !fullScreenAdShowing &&
        backgroundedAt > 0L && now - backgroundedAt >= APP_OPEN_MIN_BACKGROUND_MS &&
        loadedAt > 0L && now >= loadedAt && now - loadedAt < APP_OPEN_EXPIRY_MS &&
        (lastShownAt == 0L || now - lastShownAt >= APP_OPEN_COOLDOWN_MS)
}
