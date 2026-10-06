plugins {
    alias(libs.plugins.cycle.android.library)
    alias(libs.plugins.cycle.robolectric.tests)
}

// Helpers for every module's Robolectric and Roborazzi tests, never for the app itself: rendering in
// one of Cycle's languages, and the copy check in each of them (docs/decisions/0008-languages.md).
// Modules use it as a testImplementation dependency.
android {
    namespace = "com.tonypine.cycle.core.testing"
}

dependencies {
    api(projects.core.model)
    implementation(libs.junit)
    implementation(libs.robolectric)
    implementation(libs.androidx.test.ext.junit)
}
