plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android) // Keep if you have Kotlin files elsewhere; otherwise optional
}

android {
    namespace = "com.davidauz.zzpal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.davidauz.zzpal"
        minSdk = 34
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // ❌ REMOVE Compose-specific blocks
    // buildFeatures { compose = true } → DELETED
    // composeOptions { ... } → DELETED

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    kotlinOptions {
        jvmTarget = "17"
        // ❌ Remove Compose compiler suppression flag — no longer needed
        // freeCompilerArgs += listOf("-P", "plugin:...") → DELETED
    }
}

dependencies {

    // Core Android + Material (Java/XML friendly)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)           // androidx.activity:activity (NOT activity-compose)
    implementation(libs.constraintlayout)

    // RecyclerView (essential for your migration)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    // ❌ implementation("androidx.room:room-ktx:2.6.1") → Remove if no Kotlin Coroutines needed
    // → Replace with room-runtime only if using Java

    // WorkManager
    implementation("androidx.work:work-runtime:2.9.0")

    // Audio & Vibration
    implementation("androidx.media:media:1.7.0")

    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.51.1")
    annotationProcessor("com.google.dagger:hilt-compiler:2.51.1")
    annotationProcessor("androidx.hilt:hilt-compiler:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Standard AndroidX test libs (keep these)
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // https://mvnrepository.com/artifact/androidx.fragment/fragment
    implementation("androidx.fragment:fragment:1.8.9")
    // https://mvnrepository.com/artifact/androidx.activity/activity
    implementation("androidx.activity:activity:1.9.3")

}
