plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.allen.restocker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.allen.restocker"
        minSdk = 24
        targetSdk = 34
        // versionCode must only ever go up, or Android refuses the update. Minutes since 1970
        // always increases, even if the repo or the workflow is ever recreated.
        versionCode = (System.currentTimeMillis() / 60000L).toInt()
        versionName = "1.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "dev")
    }

    // One fixed key (app/restocker.jks, committed on purpose). Every build, on any machine,
    // is signed with it, so each new APK installs over the previous one as a normal update.
    // Never replace this file after you have installed the app, or updates will be refused.
    signingConfigs {
        create("release") {
            storeFile = file("restocker.jks")
            storeType = "pkcs12"
            storePassword = "restocker-fp"
            keyAlias = "restocker"
            keyPassword = "restocker-fp"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
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
        compose = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}
