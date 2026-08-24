plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.rovena.garage"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rovena.garage"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
                arguments["room.incremental"] = "true"
            }
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ROVENA_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ROVENA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ROVENA_KEY_ALIAS")
                keyPassword = System.getenv("ROVENA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val keystorePath = System.getenv("ROVENA_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("boolean", "SAMPLE_DATA_ENABLED", "true")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("boolean", "SAMPLE_DATA_ENABLED", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            // Default Gradle console reporting only prints "Class > test FAILED" plus the
            // exception type and a line number (often just the enclosing suspend function's
            // signature line for a runTest {} body, not the actual failing assertion) - never
            // the assertion's own message, so a CI failure alone isn't enough to diagnose.
            all { test ->
                test.testLogging {
                    events("failed")
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    showCauses = true
                    showStackTraces = true
                }
            }
        }
    }
}

dependencies {
    implementation(project(":domain"))

    // Core / Kotlin
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // UI
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.activity:activity-ktx:1.9.1")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore (current vehicle / settings prefs)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager (reminder scheduling)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Biometric (App Lock)
    implementation("androidx.biometric:biometric:1.1.0")

    // Image loading (thumbnails, vehicle photos)
    implementation("io.coil-kt:coil:2.6.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.7")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}
