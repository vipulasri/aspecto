import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.vipulasri.aspecto.benchmark"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vipulasri.aspecto.benchmark"
        minSdk = libs.versions.android.minSdk.get().toInt()
        // MacrobenchmarkRule grants READ/WRITE_EXTERNAL_STORAGE to the app under test. These
        // legacy permissions are only grantable for targetSdk <= 32, so target 32 here (a thin
        // benchmark harness; irrelevant to library behavior).
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
        // Macrobenchmark requires a non-debuggable target app; the "benchmark" variant keeps
        // debuggable=false but signs with the debug key so it can be installed on a device.
        create("benchmark") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":aspecto"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    // Lets Macrobenchmark capture/replay profiles and clear the shader cache.
    implementation(libs.androidx.profileinstaller)
}