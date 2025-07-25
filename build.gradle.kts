// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.8.5" apply false
    //id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}