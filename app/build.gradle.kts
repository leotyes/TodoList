plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

configurations.all {
    resolutionStrategy {
        force("androidx.core:core-ktx:1.13.1")
        force("androidx.core:core:1.13.1")
    }
}

android {
    namespace = "com.example.todolist"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.todolist"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
}

secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}

dependencies {
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.places)
    implementation(libs.maps.utils.ktx)
    val lifecycle_version = "2.7.0"
    val room_version = "2.6.1"
    val work_version = "2.9.0"
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.work:work-runtime:$work_version")
    implementation("androidx.work:work-runtime-ktx:$work_version")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.squareup.moshi:moshi:1.15.2")
    //annotationProcessor("androidx.room:room-compiler:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
//    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core:1.13.1")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}