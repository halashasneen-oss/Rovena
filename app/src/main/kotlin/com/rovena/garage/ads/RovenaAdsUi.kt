package com.rovena.garage.ads

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.rovena.garage.BuildConfig
import com.rovena.garage.R
import com.rovena.garage.ui.theme.RovenaPalette

/** Outside the navigation bar: visible, clearly labeled, no overlay on actions. */
@Composable
fun RovenaAdaptiveBanner(ads: RovenaAdManager) {
    if (!ads.mayDisplayAds) return
    val context = LocalContext.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val adWidth = (screenWidth - 24).coerceAtLeast(1)
    val adView = remember(context, adWidth) {
        AdView(context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth))
        }
    }
    var loaded by remember(adView) { mutableStateOf(false) }
    DisposableEffect(adView) {
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() { loaded = true }
            override fun onAdFailedToLoad(error: LoadAdError) {
                loaded = false
                Log.w("RovenaAds", "Banner load failed (" + error.code + "): " + error.message)
            }
        }
        adView.loadAd(AdRequest.Builder().build())
        onDispose {
            adView.destroy()
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().background(RovenaPalette.DeepNavy),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (loaded) {
            Text(
                stringResource(R.string.ad_label),
                style = MaterialTheme.typography.labelSmall,
                color = RovenaPalette.TextSecondary
            )
        }
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth().height((adView.adSize?.height ?: 50).dp)
        )
    }
}

@Composable
fun RovenaAdsSettingsPanel(ads: RovenaAdManager) {
    if (!ads.configured) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = RovenaPalette.SurfaceRaised,
        border = BorderStroke(1.dp, RovenaPalette.Cyan.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.ad_settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                stringResource(R.string.ad_settings_description),
                color = RovenaPalette.TextSecondary
            )
            if (ads.privacyOptionsRequired) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = ads::showPrivacyOptions
                ) {
                    Icon(Icons.Rounded.PrivacyTip, contentDescription = null)
                    Text("  " + stringResource(R.string.ad_privacy_options))
                }
            }
            if (ads.isTestAds) {
                Text(
                    stringResource(R.string.ad_test_mode),
                    color = RovenaPalette.Warning,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Branded, bounded startup loading screen; late ads never interrupt main content. */
@Composable
fun RovenaOpeningOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(RovenaPalette.Midnight, RovenaPalette.DeepNavy, RovenaPalette.Surface)
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(
                shape = CircleShape,
                color = RovenaPalette.Accent.copy(alpha = 0.17f),
                border = BorderStroke(1.dp, RovenaPalette.Cyan.copy(alpha = 0.6f))
            ) {
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "R",
                        color = RovenaPalette.Cyan,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Text(stringResource(R.string.app_display_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(
                stringResource(R.string.p5b_home_subtitle),
                color = RovenaPalette.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
