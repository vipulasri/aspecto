import com.android.build.api.dsl.ManagedVirtualDevice
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.test")
}

android {
    namespace = "com.vipulasri.aspecto.benchmark.test"
    targetProjectPath = ":benchmarkApp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    // Enable the benchmark to run separately from the app process.
    experimentalProperties["android.experimental.self-instrumenting"] = true

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Macrobenchmark treats emulators and low-battery as errors by default
        // (Android Studio passes these automatically when running on an emulator).
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] =
            "EMULATOR,LOW-BATTERY"
    }

    buildTypes {
        // Matches the target app's "benchmark" variant (non-debuggable, debug-signed).
        create("benchmark") {
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    testOptions.managedDevices.allDevices {
        create<ManagedVirtualDevice>("pixel8Api34") {
            device = "Pixel 8"
            apiLevel = 34
            systemImageSource = "google_apis_playstore"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.junit)
}