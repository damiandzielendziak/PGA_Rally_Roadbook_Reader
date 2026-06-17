plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.roadbook"
    compileSdk = 35 // POPRAWKA: Uproszczona, standardowa i bezpieczna składnia dla API 36

    defaultConfig {
        applicationId = "com.example.roadbook"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // POPRAWKA: W oficjalnym standardzie Android Gradle Plugin wyłączenie
            // optymalizacji kodu/ProGuarda realizuje się przez poniższe flagi:
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.navigation:navigation-compose:2.7.7")
    // USŁUGI LOKALIZACJI GPS
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // SPLASH SCREEN API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // SILNIK ANIMACJI LOTTIE (POPRAWKA: Usunięto duplikat linii)
    implementation("com.airbnb.android:lottie-compose:6.4.0")
} // POPRAWKA: Usunięto nadmiarowy, zdublowany nawias klamrowy, który crashował Gradle