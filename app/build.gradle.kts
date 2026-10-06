plugins {
    alias(libs.plugins.cycle.android.application)
    alias(libs.plugins.cycle.android.compose)
    alias(libs.plugins.cycle.screenshot.tests)
}

android {
    namespace = "com.tonypine.cycle"

    // Version and release signing come from CI inputs through cycle.android.application; see
    // docs/decisions/0002-release-distribution.md.
    defaultConfig {
        applicationId = "com.tonypine.cycle"
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.data)
    implementation(projects.feature.calendar)
    implementation(projects.feature.history)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.settings)
    implementation(projects.feature.today)
    implementation(libs.androidx.activity.compose)
    // "Lock Cycle": the phone's own fingerprint, face or screen-lock prompt. AndroidX, on the phone, no
    // network. Its prompt needs a FragmentActivity.
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // The lock tests run the real settings on a DataStore file of their own.
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(libs.kotlinx.coroutines.test)
    // The copy check in every language, and rendering in one (core:testing).
    testImplementation(projects.core.testing)
}
