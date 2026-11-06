plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
}

// Formatting with ktlint, configured in .editorconfig. `./gradlew spotlessApply` fixes it.
spotless {
    val ktlint = libs.versions.ktlint.get()
    val editorConfig = rootProject.file(".editorconfig")
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(ktlint).setEditorConfigPath(editorConfig)
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**")
        ktlint(ktlint).setEditorConfigPath(editorConfig)
    }
}
