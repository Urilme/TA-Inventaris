import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Kredensial penandatanganan rilis dibaca dari keystore.properties (di-.gitignore, tak pernah
// di-commit). Bila berkas tak ada (mis. mesin dev/CI tanpa keystore), rilis ditandatangani debug
// agar `assembleRelease` tetap berhasil untuk verifikasi build.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
}

android {
    namespace = "com.ta.konstruksi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ta.konstruksi"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Pakai keystore rilis bila tersedia; jika tidak, jatuh ke debug agar build tetap jalan.
            signingConfig = if (keystorePropsFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            // Unggah mapping R8 ke Crashlytics dilakukan manual/CI, bukan saat build lokal, agar
            // `assembleRelease` tetap mandiri (tanpa jaringan/kredensial). Aktifkan di pipeline rilis.
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true   // wajib utk Robolectric (akses resource + manifest)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // ViewModel + Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // AndroidX Core (NotificationCompat / NotificationManagerCompat / ContextCompat)
    implementation("androidx.core:core-ktx:1.15.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    // Pelaporan kerusakan (C5). Hanya stacktrace + info perangkat; TIDAK ada data inventaris/PII
    // yang di-log (tak ada custom key/log berisi data pengguna).
    implementation("com.google.firebase:firebase-crashlytics")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Testing (unit — JVM, src/test/)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0") // runTest
    testImplementation("com.google.truth:truth:1.4.4")                        // assertion assertThat
    testImplementation("io.mockk:mockk:1.13.13")                              // mocking
    testImplementation("app.cash.turbine:turbine:1.1.0")                      // uji Flow/StateFlow
    testImplementation("androidx.arch.core:core-testing:2.2.0")               // InstantTaskExecutor
    // Compose UI test di JVM lewat Robolectric (F3 — validasi formulir di lapisan Composable)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.2.1")                       // runner AndroidJUnit4 (JVM)
    testImplementation("androidx.work:work-testing:2.10.0")                   // TestListenableWorkerBuilder (No.41)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")   // host activity utk createComposeRule()
}

// §10 — cetak status tiap test ke konsol sebagai checklist pass/fail per fitur
tasks.withType<Test> {
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
    }
}
