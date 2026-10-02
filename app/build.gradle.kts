plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Separate Rovena AdMob credentials. Other apps' unit IDs must never be reused.
val admobAppId = System.getenv("ROVENA_ADMOB_APP_ID").orEmpty().trim()
val admobBannerId = System.getenv("ROVENA_ADMOB_BANNER_ID").orEmpty().trim()
val admobInterstitialId = System.getenv("ROVENA_ADMOB_INTERSTITIAL_ID").orEmpty().trim()
val admobRewardedId = System.getenv("ROVENA_ADMOB_REWARDED_ID").orEmpty().trim()
val admobValues = listOf(admobAppId, admobBannerId, admobInterstitialId, admobRewardedId)
val appIdPattern = Regex("ca-app-pub-[0-9]{16}~[0-9]{10}")
val unitIdPattern = Regex("ca-app-pub-[0-9]{16}/[0-9]{10}")
val productionAdsReady = appIdPattern.matches(admobAppId) &&
    listOf(admobBannerId, admobInterstitialId, admobRewardedId).all(unitIdPattern::matches) &&
    listOf(admobBannerId, admobInterstitialId, admobRewardedId).all {
        it.substringBefore('/') == admobAppId.substringBefore('~')
    }
if (admobValues.any(String::isNotBlank) && !productionAdsReady) {
    throw GradleException("Rovena AdMob: supply a valid app ID and all three unit IDs from the SAME AdMob app.")
}
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
fun javaString(value: String): String = "\"" + value + "\""


android {
    namespace = "com.rovena.garage"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rovena.garage"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "2.2.0"
    }

    val releaseKeystorePath = System.getenv("ROVENA_KEYSTORE_PATH")
    val releaseStorePassword = System.getenv("ROVENA_STORE_PASSWORD")
    val releaseKeyAlias = System.getenv("ROVENA_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("ROVENA_KEY_PASSWORD")
    val hasReleaseSigning = listOf(
        releaseKeystorePath,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword
    ).all { !it.isNullOrBlank() }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            manifestPlaceholders["rovenaAdmobAppId"] = testAdmobAppId
            buildConfigField("boolean", "ADS_CONFIGURED", "true")
            buildConfigField("boolean", "ADS_ARE_TEST", "true")
            buildConfigField("String", "ADMOB_BANNER_ID", javaString("ca-app-pub-3940256099942544/9214589741"))
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", javaString("ca-app-pub-3940256099942544/1033173712"))
            buildConfigField("String", "ADMOB_REWARDED_ID", javaString("ca-app-pub-3940256099942544/5224354917"))
        }
        release {
            // Missing credentials: release compiles, but NO ad requests are sent.
            // The test app ID only satisfies SDK manifest metadata on an ads-disabled build.
            manifestPlaceholders["rovenaAdmobAppId"] = if (productionAdsReady) admobAppId else testAdmobAppId
            buildConfigField("boolean", "ADS_CONFIGURED", productionAdsReady.toString())
            buildConfigField("boolean", "ADS_ARE_TEST", "false")
            buildConfigField("String", "ADMOB_BANNER_ID", javaString(if (productionAdsReady) admobBannerId else ""))
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", javaString(if (productionAdsReady) admobInterstitialId else ""))
            buildConfigField("String", "ADMOB_REWARDED_ID", javaString(if (productionAdsReady) admobRewardedId else ""))
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
}

// Run this task before sending a production bundle to Google Play.
tasks.register("verifyProductionAds") {
    doLast {
        check(productionAdsReady) {
            "Production AdMob IDs for com.rovena.garage are missing. Set ROVENA_ADMOB_APP_ID, _BANNER_ID, _INTERSTITIAL_ID and _REWARDED_ID."
        }
    }
}
