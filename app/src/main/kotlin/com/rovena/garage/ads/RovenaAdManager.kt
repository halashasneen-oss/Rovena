package com.rovena.garage.ads

import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.rovena.garage.BuildConfig

/** Activity-owned controller: only banner, interstitial and App Open; no rewarded requests. */
class RovenaAdManager(private val activity: AppCompatActivity) {
    private val consent by lazy { UserMessagingPlatform.getConsentInformation(activity) }
    private val mainHandler = Handler(Looper.getMainLooper())

    private var consentStarted = false
    private var consentFormInProgress = false
    private var initializing = false
    private var sdkInitialized = false
    private var interstitialLoading = false
    private var appOpenLoading = false
    private var interstitial: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var appOpenLoadedAt = 0L
    private var lastAppOpenShownAt = 0L
    private var lastBackgroundAt = 0L
    private var lastInterstitialAt = 0L
    private var ignoreAppOpenUntil = 0L
    private var fullScreenAdShowing = false
    private var userFlowActive = false
    private var skipNextForeground = false
    private var initialForegroundHandled = false
    private var startupExpiresAt = 0L
    private var completedActions = 0

    var sdkReady by mutableStateOf(false)
        private set
    var privacyOptionsRequired by mutableStateOf(false)
        private set
    var startupVisible by mutableStateOf(false)
        private set

    val configured: Boolean get() = BuildConfig.ADS_CONFIGURED
    val isTestAds: Boolean get() = BuildConfig.ADS_ARE_TEST
    val mayDisplayAds: Boolean get() = configured && sdkReady && !consentFormInProgress

    /** Don't show the startup ad during the first-ever onboarding session. */
    fun markOnboardingInThisActivity() {
        initialForegroundHandled = true
        startupVisible = false
    }

    fun setUserFlowActive(active: Boolean) {
        userFlowActive = active
    }

    fun suppressNextReturn() {
        skipNextForeground = true
    }

    fun onActivityStopped() {
        startupVisible = false
        if (!fullScreenAdShowing && !consentFormInProgress) {
            lastBackgroundAt = System.currentTimeMillis()
        }
    }

    /** Cold start: an existing user's short branded loading window; never show late. */
    fun onActivityResumed() {
        val now = System.currentTimeMillis()
        if (skipNextForeground) {
            skipNextForeground = false
            return
        }
        if (!initialForegroundHandled) {
            initialForegroundHandled = true
            if (configured && !userFlowActive && !activity.isFinishing) {
                startupVisible = true
                startupExpiresAt = now + 2_500L
                mainHandler.postDelayed({ startupVisible = false }, 2_500L)
                if (sdkReady) preloadAppOpen()
            }
            return
        }
        if (!mayDisplayAds || startupVisible || now < ignoreAppOpenUntil) return
        if (appOpenAd != null && now - appOpenLoadedAt >= RovenaAdPolicy.APP_OPEN_EXPIRY_MS) {
            appOpenAd = null
            appOpenLoadedAt = 0L
        }
        val ad = appOpenAd ?: run {
            preloadAppOpen()
            return
        }
        if (!RovenaAdPolicy.mayShowAppOpen(
                lastBackgroundAt, lastAppOpenShownAt, appOpenLoadedAt, now,
                userFlowActive, fullScreenAdShowing
            )
        ) return
        showAppOpen(ad, now)
    }

    /** Call only after onboarding. Production requests require fresh UMP consent. */
    fun requestConsent() {
        if (!configured || consentStarted || activity.isFinishing || activity.isDestroyed) return
        consentStarted = true
        if (BuildConfig.DEBUG) {
            initializeSdk()
            return
        }
        consentFormInProgress = true
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                updatePrivacyStatus()
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    consentFormInProgress = false
                    updatePrivacyStatus()
                    if (consent.canRequestAds()) initializeSdk() else revokeAdAccess()
                }
                if (consent.canRequestAds()) initializeSdk()
            },
            {
                consentFormInProgress = false
                updatePrivacyStatus()
                // UMP may provide an existing valid decision if its network request fails.
                if (consent.canRequestAds()) initializeSdk() else revokeAdAccess()
            }
        )
    }

    fun showPrivacyOptions() {
        if (!configured || !privacyOptionsRequired || BuildConfig.DEBUG) return
        startupVisible = false
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
        appOpenAd = null
        appOpenLoadedAt = 0L
        startupVisible = false
    }

    private fun initializeSdk() {
        if (sdkInitialized) {
            sdkReady = BuildConfig.DEBUG || consent.canRequestAds()
            if (sdkReady) preloadAds()
            return
        }
        if (initializing) return
        initializing = true
        Thread {
            MobileAds.initialize(activity) {
                mainHandler.post {
                    initializing = false
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        sdkInitialized = true
                        sdkReady = BuildConfig.DEBUG || consent.canRequestAds()
                        if (sdkReady) preloadAds()
                    }
                }
            }
        }.start()
    }

    private fun preloadAds() {
        preloadInterstitial()
        preloadAppOpen()
    }

    private fun preloadAppOpen() {
        if (!sdkReady || consentFormInProgress || appOpenLoading || appOpenAd != null) return
        appOpenLoading = true
        AppOpenAd.load(
            activity,
            BuildConfig.ADMOB_APP_OPEN_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenLoading = false
                    if (!mayDisplayAds) return
                    appOpenAd = ad
                    appOpenLoadedAt = System.currentTimeMillis()
                    // The app is still genuinely displaying its startup loading UI.
                    if (startupVisible && appOpenLoadedAt < startupExpiresAt &&
                        activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                        !userFlowActive && !fullScreenAdShowing
                    ) {
                        showAppOpen(ad, appOpenLoadedAt)
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenLoading = false
                    appOpenAd = null
                    appOpenLoadedAt = 0L
                }
            }
        )
    }

    private fun showAppOpen(ad: AppOpenAd, now: Long) {
        if (!mayDisplayAds || fullScreenAdShowing || userFlowActive ||
            activity.isFinishing || activity.isDestroyed
        ) return
        startupVisible = false
        appOpenAd = null
        appOpenLoadedAt = 0L
        lastBackgroundAt = 0L
        lastAppOpenShownAt = now
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

    private fun finishFullScreenAd() {
        fullScreenAdShowing = false
        ignoreAppOpenUntil = System.currentTimeMillis() + 60_000L
        lastBackgroundAt = 0L
    }

    private fun preloadInterstitial() {
        if (!sdkReady || consentFormInProgress || interstitialLoading || interstitial != null) return
        interstitialLoading = true
        InterstitialAd.load(
            activity,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    if (sdkReady) interstitial = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    interstitial = null
                }
            }
        )
    }

    /** Natural break only, after a successful save closes its entry sheet. */
    fun onCompletedRecord() {
        completedActions++
        val now = System.currentTimeMillis()
        if (!mayDisplayAds || startupVisible || userFlowActive || fullScreenAdShowing ||
            now - lastAppOpenShownAt < 60_000L
        ) return
        if (!RovenaAdPolicy.mayShowInterstitial(completedActions, lastInterstitialAt, now)) {
            if (interstitial == null) preloadInterstitial()
            return
        }
        val ad = interstitial ?: run {
            preloadInterstitial()
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return
        interstitial = null
        lastInterstitialAt = now
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

    fun release() {
        startupVisible = false
        interstitial = null
        appOpenAd = null
    }
}
