plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.cycle.ktlint)
}

// The convention plugins live in an included build, which `./gradlew ktlintCheck` would skip.
tasks.named("ktlintCheck") {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:ktlintCheck"))
}
