import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.labfreezer"
    compileSdk = 37
    // 本机仅安装 build-tools 36.0.0，AGP 默认版本需联网下载，显式指定本机版本离线可用
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.labfreezer"
        minSdk = 26
        targetSdk = 37
        versionCode = 14
        versionName = "1.3.1"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }

    signingConfigs {
        create("nosig") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            // debug.keystore 内实际别名是 androiddebugkey，而非 debug
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            enableV3Signing = false
            enableV4Signing = false
        }
        create("release") {
            storeFile = rootProject.file("release.keystore")
            storePassword = "labfreezer"
            keyAlias = "release"
            keyPassword = "labfreezer"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("nosig")
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
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

    // Kotlin 2.3 起 kotlinOptions 废弃，改用 compilerOptions DSL
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2025.03.01")
    implementation(composeBom)

    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.7")

    // Room
    val roomVersion = "2.7.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Hilt
    val hiltVersion = "2.58"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // CameraX
    val cameraxVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Liquid Glass (Kyant0 backdrop 2.0.0，Android 目标)
    implementation("io.github.kyant0:backdrop-android:2.0.0")
    implementation("io.github.kyant0:shapes:1.2.0")

    // Lifecycle ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // PaddleOCR Lite (Paddle-Lite)
    implementation("com.github.equationl.paddleocr4android:paddleocr4android:v1.2.9")

}
