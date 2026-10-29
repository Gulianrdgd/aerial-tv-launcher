plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "nl.julian.aerial"
    compileSdk = 37

    defaultConfig {
        applicationId = "nl.julian.aerial"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.0"
    }

    // CI releases (see .github/workflows/build.yml): the keystore comes from a secret.
    if (project.hasProperty("ciKeystore")) {
        signingConfigs.create("ci") {
            storeFile = file(project.property("ciKeystore") as String)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Unsigned by default: F-Droid signs with its own key. Pass
            // -PlocalSign to sign with the debug key for installing yourself.
            signingConfig = when {
                project.hasProperty("ciKeystore") -> signingConfigs.getByName("ci")
                project.hasProperty("localSign") -> signingConfigs.getByName("debug")
                else -> null
            }
        }
    }

    // Reproducible builds for F-Droid: leave out Google's encrypted
    // dependency metadata block.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.tv.material)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.core.ktx)
    implementation(libs.profileinstaller)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.database)
    implementation(libs.okhttp)
    implementation(libs.material.color.utilities)
    implementation(libs.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    baselineProfile(project(":baselineprofile"))
}
