plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.andrew67.ddrfinder"
    compileSdk = 36
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId ="com.andrew67.ddrfinder"
        versionCode = 68
        versionName = "3.0.28"
        minSdk = 23
        targetSdk = 35
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "API_BASE_URL",
                "\"https://ddrfinder-api.andrew67.com/locate.php\"")
        buildConfigField("String", "ABOUT_BASE_URL",
                "\"https://ddrfinder.andrew67.com/android/about/\"")
        buildConfigField("String", "PRIVACY_POLICY_URL",
                "\"https://ddrfinder.andrew67.com/android/privacy-policy/\"")
        buildConfigField("String", "APPLINK_BASE_URL",
                "\"https://ddrfinder.andrew67.com/app\"")
        buildConfigField("String", "FALLBACK_INFO_URL",
                "\"https://ddrfinder.andrew67.com/info.php?id=\${id}&android=1\"")
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
    kotlinOptions {
        jvmTarget = "11"
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
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("com.google.android.gms:play-services-base:18.10.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.libraries.places:places:4.1.0")
    implementation("com.google.maps.android:android-maps-utils:3.4.0")

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.browser:browser:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.10.0")

    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
