plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.ben.periodt"
    compileSdk = 35
    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "com.ben.periodt"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.1.7"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"))
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Required for Reproducible Builds on F-Droid
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Strip debug symbols to keep the F-Droid build clean
            ndk.debugSymbolLevel = "NONE"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
        pickFirst("**/libc++_shared.so")
        pickFirst("**/libsqlcipher.so")
    }
}

dependencies {
    // --- JETPACK COMPOSE ---
    // Update BOM to a newer version compatible with SDK 35
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // FIX: Replaced "libs.androidx.compose.animation" with the BOM-managed version
    // This prevents the version conflict causing the crash.
    implementation("androidx.compose.animation:animation")
    implementation(libs.androidx.compose.foundation)

    debugImplementation("androidx.compose.ui:ui-tooling")
    // Update Navigation to match SDK 35 requirements
    implementation("androidx.navigation:navigation-compose:2.8.3")
    // Update Material Icons
    implementation("androidx.compose.material:material-icons-extended")

    // --- LIFECYCLE (Updated for SDK 35) ---
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // --- ROOM DATABASE ---
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // --- COROUTINES ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // --- THIRD PARTY LIBRARIES ---
    implementation("com.kizitonwose.calendar:compose:2.6.0")
    implementation("net.zetetic:sqlcipher-android:4.10.0@aar")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.sqlite:sqlite:2.4.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("nl.dionsegijn:konfetti-compose:2.0.4")
    implementation("dev.chrisbanes.haze:haze:0.5.4") // check for the latest version

    // --- TESTING ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}