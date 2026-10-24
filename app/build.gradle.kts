plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.andrew67.ddrfinder"
    compileSdk = 37
    buildFeatures {
        buildConfig = true
        compose = true
    }
    defaultConfig {
        applicationId ="com.andrew67.ddrfinder"
        versionCode = 69
        versionName = "3.0.29"
        minSdk = 24
        targetSdk = 37

        buildConfigField("String", "API_BASE_URL",
                "\"https://ddrfinder-api.andrew67.com/locate.php\"")
        buildConfigField("String", "ABOUT_BASE_URL",
                "\"https://ddrfinder.andrew67.com/android/about/\"")
        buildConfigField("String", "PRIVACY_POLICY_URL",
                "\"https://ddrfinder.andrew67.com/android/privacy-policy/\"")
        buildConfigField("String", "APPLINK_BASE_URL",
                "\"https://ddrfinder.andrew67.com/app\"")
        buildConfigField("String", "FALLBACK_INFO_URL",
            $$"\"https://ddrfinder.andrew67.com/info.php?id=${id}&android=1\""
        )
        buildConfigField("String", "DDR_CALC_URL",
                "\"https://ddrcalc.andrew67.com/?df=1\"")
    }
    androidResources {
        // Add to test pseudo-locales (no way to specify just for debug config)
        // "en-rXA", "ar-rXB",
        localeFilters += listOf("en", "en-rGB",
            "es", "es-rUS", "b+es+419",
            "ja",
            "zh", "zh-rCN", "zh-rSG", "zh-rTW", "zh-rHK")
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            isPseudoLocalesEnabled = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
}

dependencies {
    implementation("androidx.compose.material3:material3:1.4.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("com.google.android.gms:play-services-base:18.11.1")
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("com.google.android.libraries.places:places:5.3.0")
    implementation("com.google.maps.android:android-maps-utils:5.1.0")

    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.browser:browser:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.11.0")

    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Material Design 3
    implementation("androidx.compose.material3:material3")
    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // UI Tests
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.compose.material3.adaptive:adaptive")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.fragment:fragment-compose:1.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.compose.runtime:runtime-livedata")
}
