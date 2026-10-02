package com.rovena.garage.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.rovena.garage.BuildConfig
import com.rovena.garage.data.AppPreferences
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * One Activity-owned ad controller. Live ads are impossible without five matching
 * Rovena-specific production IDs, a fresh UMP consent check, and SDK initialization.
 */
class RovenaAdManager(
    private val activity: AppCompatActivity,
    private val preferences: AppPreferences
) {
    private val consent by lazy { UserMessagingPlatform.getConsentInformation(activity) }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var attached = false
    private var consentStarted = false
    private var initializing = false
    private var sdkInitialized = false
    private var interstitialLoading = false
    private var rewardedLoading = false
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var appOpenLoading = false
    private var appOpenLoadedAt = 0L
    private var lastAppOpenShownAt = 0L
    private var lastBackgroundAt = 0L
    private var ignoreAppOpenUntil = 0L
    private var fullScreenAdShowing = false
    private var userFlowActive = false
    private var consentFormInProgress = false
    private var completedActions = 0
    private var lastInterstitialAt = 0L

    var sdkReady by mutableStateOf(false)
        private set
    var privacyOptionsRequired by mutableStateOf(false)
        private set
    var rewardedReady by mutableStateOf(false)
        private set
    var adFreeUntil by mutableLongStateOf(0L)
        private set
    var clock by mutableLongStateOf(System.currentTimeMillis())
        private set
    var preferencesLoaded by mutableStateOf(false)
        private set

    val configured: Boolean get() = BuildConfig.ADS_CONFIGURED
    val isTestAds: Boolean get() = BuildConfig.ADS_ARE_TEST
    val isAdFree: Boolean
        get() = RovenaAdPolicy.adsSuppressed(adFreeUntil, clock)
    val mayDisplayAds: Boolean
        get() = configured && sdkReady && preferencesLoaded && !isAdFree
    val mayOfferReward: Boolean
        get() = configured && sdkReady && preferencesLoaded && !isAdFree && rewardedReady

    fun attach() {
        if (attached) return
        attached = true
        activity.lifecycleScope.launch {
            preferences.adsHiddenUntil.collectLatest { until ->
                adFreeUntil = until
                preferencesLoaded = true
                updateClock()
            }
        }
    }

    fun setUserFlowActive(active: Boolean) {
        userFlowActive = active
    }

    fun onActivityStopped() {
        if (!fullScreenAdShowing && !consentFormInProgress) {
            lastBackgroundAt = System.currentTimeMillis()
        }
    }

    /** Never show on a cold launch; only a preloaded ad after a genuine return. */
    fun onActivityResumed() {
        updateClock()
        if (!mayDisplayAds || consentFormInProgress || clock < ignoreAppOpenUntil) return
        val ad = appOpenAd
        val canShow = ad != null && RovenaAdPolicy.mayShowAppOpen(
            backgroundedAt = lastBackgroundAt,
            lastShownAt = lastAppOpenShownAt,
            loadedAt = appOpenLoadedAt,
            now = clock,
            adsSuppressed = isAdFree,
            userFlowActive = userFlowActive,
            fullScreenAdShowing = fullScreenAdShowing
        )
        if (!canShow) {
            if (ad == null || clock - appOpenLoadedAt >= RovenaAdPolicy.APP_OPEN_EXPIRY_MS) preloadAppOpen()
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return
        appOpenAd = null
        appOpenLoadedAt = 0L
        lastBackgroundAt = 0L
        lastAppOpenShownAt = clock
        fullScreenAdShowing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                finishFullScreenAd()
                preloadAppOpen()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                finishFullScreenAd()
                preloadAppOpen()
            }
        }
        ad.show(activity)
    }

    fun updateClock() {
        clock = System.currentTimeMillis()
    }

    /** Call after onboarding, once per Activity launch. */
    fun requestConsent() {
        if (!configured || consentStarted || activity.isFinishing || activity.isDestroyed) return
        consentStarted = true
        // Google's published demo ad unit IDs do not belong to a production account.
        // A debug build serves only test creatives and does not require live consent forms.
        if (BuildConfig.DEBUG) {
            initializeSdk()
            return
        }
        consentFormInProgress = true
        val params = ConsentRequestParameters.Builder().build()
        consent.requestConsentInfoUpdate(
            activity,
            params,
            {
                updatePrivacyStatus()
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    updatePrivacyStatus()
                    consentFormInProgress = false
                    ignoreAppOpenUntil = System.currentTimeMillis() + 60_000L
                    if (consent.canRequestAds()) initializeSdk() else revokeAdAccess()
                }
                if (consent.canRequestAds()) initializeSdk()
            },
            {
                updatePrivacyStatus()
                consentFormInProgress = false
                // If an update fails, UMP may allow a valid prior consent decision.
                if (consent.canRequestAds()) initializeSdk() else revokeAdAccess()
            }
        )
    }

    fun showPrivacyOptions() {
        if (!configured || !privacyOptionsRequired || BuildConfig.DEBUG) return
        consentFormInProgress = true
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            consentFormInProgress = false
            ignoreAppOpenUntil = System.currentTimeMillis() + 60_000L
            updatePrivacyStatus()
            if (consent.canRequestAds()) initializeSdk() else revokeAdAccess()
        }
    }

    private fun updatePrivacyStatus() {
        privacyOptionsRequired = !BuildConfig.DEBUG &&
            consent.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun revokeAdAccess() {
        sdkReady = false
        interstitial = null
        rewarded = null
        rewardedReady = false
        appOpenAd = null
        appOpenLoadedAt = 0L
    }

    private fun initializeSdk() {
        if (sdkInitialized) {
            sdkReady = true
            preloadFullScreenAds()
            return
        }
        if (initializing) return
        initializing = true
        Thread {
            MobileAds.initialize(activity) {
                mainHandler.post {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        initializing = false
                        sdkInitialized = true
                        sdkReady = BuildConfig.DEBUG || consent.canRequestAds()
                        if (sdkReady) preloadFullScreenAds()
                    }
                }
            }
        }.start()
    }

    private fun preloadFullScreenAds() {
        preloadInterstitial()
        preloadRewarded()
        preloadAppOpen()
    }

    private fun preloadAppOpen() {
        if (!sdkReady || !preferencesLoaded || isAdFree || appOpenLoading || appOpenAd != null) return
        appOpenLoading = true
        AppOpenAd.load(
            activity,
            BuildConfig.ADMOB_APP_OPEN_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenLoading = false
                    if (!sdkReady) return
                    appOpenAd = ad
                    appOpenLoadedAt = System.currentTimeMillis()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenLoading = false
                    appOpenAd = null
                    appOpenLoadedAt = 0L
                }
            }
        )
    }

    private fun finishFullScreenAd() {
        fullScreenAdShowing = false
        ignoreAppOpenUntil = System.currentTimeMillis() + 60_000L
        lastBackgroundAt = 0L
    }

    private fun preloadInterstitial() {
        if (!sdkReady || interstitialLoading || interstitial != null) return
        interstitialLoading = true
        InterstitialAd.load(
            activity,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitial = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    interstitial = null
                    // No aggressive retry loops: try again at the next real user action.
                }
            }
        )
    }

    /** Only at a natural break AFTER a record was saved and its sheet closed. */
    fun onCompletedRecord() {
        completedActions++
        updateClock()
        if (!mayDisplayAds || fullScreenAdShowing || clock - lastAppOpenShownAt < 60_000L) return
        if (!RovenaAdPolicy.mayShowInterstitial(completedActions, lastInterstitialAt, clock, isAdFree)) {
            if (interstitial == null) preloadInterstitial()
            return
        }
        val ad = interstitial ?: run {
            preloadInterstitial()
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return
        interstitial = null
        lastInterstitialAt = clock
        fullScreenAdShowing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                finishFullScreenAd()
                preloadInterstitial()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                finishFullScreenAd()
                preloadInterstitial()
            }
        }
        ad.show(activity)
    }

    private fun preloadRewarded() {
        if (!sdkReady || rewardedLoading || rewarded != null) return
        rewardedLoading = true
        RewardedAd.load(
            activity,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedLoading = false
                    rewarded = ad
                    rewardedReady = true
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedLoading = false
                    rewarded = null
                    rewardedReady = false
                }
            }
        )
    }

    /** User-triggered ONLY. Grant the benefit only after the earned-reward callback. */
    fun watchAdToHideAds() {
        updateClock()
        if (!mayOfferReward || fullScreenAdShowing || activity.isFinishing || activity.isDestroyed) return
        val ad = rewarded ?: return
        rewarded = null
        rewardedReady = false
        fullScreenAdShowing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                finishFullScreenAd()
                preloadRewarded()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                finishFullScreenAd()
                preloadRewarded()
            }
        }
        ad.show(activity) {
            val until = System.currentTimeMillis() + RovenaAdPolicy.AD_FREE_REWARD_MS
            adFreeUntil = until
            updateClock()
            activity.lifecycleScope.launch { preferences.hideAdsUntil(until) }
        }
    }

    fun release() {
        appOpenAd = null
        appOpenLoadedAt = 0L
        interstitial = null
        rewarded = null
        rewardedReady = false
    }
}
