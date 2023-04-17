plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.andrew67.ddrfinder"
    compileSdk = 33
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId ="com.andrew67.ddrfinder"
        versionCode = 55
        versionName = "3.0.25"
        minSdk = 23
        targetSdk = 33
        // Add to test pseudo-locales (no way to specify just for debug config)
        // "en-rXA", "ar-rXB",
        resourceConfigurations += listOf("en", "en-rGB",
                "es", "es-rUS", "b+es+419",
                "ja",
                "zh", "zh-rCN", "zh-rSG", "zh-rTW", "zh-rHK")
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "API_BASE_URL",
                "\"https://ddrfinder.andrew67.com/locate.php\"")
        buildConfigField("String", "ABOUT_BASE_URL",
                "\"https://ddrfinder.andrew67.com/android/about.php\"")
        buildConfigField("String", "PRIVACY_POLICY_URL",
                "\"https://ddrfinder.andrew67.com/android/privacy-policy.html\"")
        buildConfigField("String", "APPLINK_BASE_URL",
                "\"https://ddrfinder.andrew67.com/app\"")
        buildConfigField("String", "FALLBACK_INFO_URL",
                "\"https://ddrfinder.andrew67.com/info.php?id=\${id}&android=1\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"),
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
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.libraries.places:places:3.1.0")
    implementation("com.google.maps.android:android-maps-utils:3.4.0")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.browser:browser:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.preference:preference:1.2.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.6.1")

    implementation("com.squareup.moshi:moshi:1.14.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
}
