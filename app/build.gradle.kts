plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kover)
}

android {
    namespace = "io.github.gitron.quintana"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.gitron.quintana"
        minSdk = 29
        targetSdk = 35
        // CI numbers its builds, so every APK from GitHub Actions has a higher version than the last.
        val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        val commit = System.getenv("GITHUB_SHA")?.take(7) ?: "local"
        versionCode = buildNumber
        versionName = "0.1.$buildNumber ($commit)"
    }

    // CI signs release builds with a fixed key from the repository secrets, so a
    // new APK installs as an update. Without the key only debug builds are signed.
    val keystore = System.getenv("QUINTANA_KEYSTORE")?.let(::file)
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = keystore
                storePassword = System.getenv("QUINTANA_KEYSTORE_PASSWORD")
                keyAlias = "quintana"
                keyPassword = System.getenv("QUINTANA_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        warningsAsErrors = true
        // CI prints this file, since the build log shows only the first finding.
        textReport = true
        textOutput = file("build/reports/lint-results-debug.txt")
        // These compare against the newest releases, so they would fail CI out of the blue.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable", "OldTargetApi")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
}

// Coverage is reported, not enforced: most code is UI or needs a device, so a
// threshold would mostly reward tests written for the number.
kover {
    reports {
        filters {
            excludes {
                classes("*.BuildConfig", "*.ComposableSingletons*")
            }
        }
    }
}
