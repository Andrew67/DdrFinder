// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version("8.0.1") apply(false)
    id("org.jetbrains.kotlin.android") version("1.8.20") apply(false)
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
