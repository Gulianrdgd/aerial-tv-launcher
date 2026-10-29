plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

// Generates app/src/main/generated/baselineProfiles: run
// ./gradlew :app:generateReleaseBaselineProfile with an emulator or TV connected.
android {
    namespace = "nl.julian.aerial.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.uiautomator)
    implementation(libs.benchmark.macro.junit4)
}
